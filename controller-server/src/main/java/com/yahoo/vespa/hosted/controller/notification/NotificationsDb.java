// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.notification;

import com.yahoo.collections.Pair;
import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.ClusterSpec;
import com.yahoo.vespa.curator.Lock;
import com.yahoo.vespa.hosted.controller.Controller;
import com.yahoo.vespa.hosted.controller.api.application.v4.model.ClusterMetrics;
import com.yahoo.vespa.hosted.controller.api.identifiers.DeploymentId;
import com.yahoo.vespa.hosted.controller.persistence.CuratorDb;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yahoo.vespa.hosted.controller.notification.Notification.Level;
import static com.yahoo.vespa.hosted.controller.notification.Notification.Type;

/**
 * Adds, updates and removes tenant notifications in ZK
 *
 * @author freva
 */
public class NotificationsDb {

    private final Clock clock;
    private final CuratorDb curatorDb;

    public NotificationsDb(Controller controller) {
        this(controller.clock(), controller.curator());

        Set<DeploymentId> allDeployments = controller.applications().asList().stream()
                .flatMap(application -> application.instances().values().stream())
                .flatMap(instance -> instance.deployments().keySet().stream()
                        .map(zone -> new DeploymentId(instance.id(), zone)))
                .collect(Collectors.toSet());
        removeNotificationsForRemovedInstances(allDeployments);
    }

    NotificationsDb(Clock clock, CuratorDb curatorDb) {
        this.clock = clock;
        this.curatorDb = curatorDb;
    }

    // TODO (freva): Remove after 7.423
    void removeNotificationsForRemovedInstances(Set<DeploymentId> allDeployments) {
        // Prior to 7.423, notifications created for instances that were later removed by being removed from
        // deployment.xml were not cleared. This should only affect notifications with type 'deployment'
        allDeployments.stream()
                .map(deploymentId -> deploymentId.applicationId().tenant())
                .distinct()
                .flatMap(tenant -> curatorDb.readNotifications(tenant).stream()
                            .filter(notification -> notification.type() == Type.deployment && notification.source().zoneId().isPresent())
                            .map(Notification::source))
                            .filter(source -> {
                                ApplicationId sourceApplication = ApplicationId.from(source.tenant(),
                                                                                     source.application().get(),
                                                                                     source.instance().get());
                                DeploymentId sourceDeployment = new DeploymentId(sourceApplication, source.zoneId().get());
                                return ! allDeployments.contains(sourceDeployment);
                            })
                            .forEach(source -> removeNotification(source, Type.deployment));
    }

    public List<Notification> listNotifications(NotificationSource source, boolean productionOnly) {
        return curatorDb.readNotifications(source.tenant()).stream()
                .filter(notification -> source.contains(notification.source()) && (!productionOnly || notification.source().isProduction()))
                .collect(Collectors.toUnmodifiableList());
    }

    public void setNotification(NotificationSource source, Type type, Level level, String message) {
        setNotification(source, type, level, List.of(message));
    }

    /**
     * Add a notification with given source and type. If a notification with same source and type
     * already exists, it'll be replaced by this one instead
     */
    public void setNotification(NotificationSource source, Type type, Level level, List<String> messages) {
        try (Lock lock = curatorDb.lockNotifications(source.tenant())) {
            List<Notification> notifications = curatorDb.readNotifications(source.tenant()).stream()
                    .filter(notification -> !source.equals(notification.source()) || type != notification.type())
                    .collect(Collectors.toCollection(ArrayList::new));
            notifications.add(new Notification(clock.instant(), type, level, source, messages));
            curatorDb.writeNotifications(source.tenant(), notifications);
        }
    }

    /** Remove the notification with the given source and type */
    public void removeNotification(NotificationSource source, Type type) {
        try (Lock lock = curatorDb.lockNotifications(source.tenant())) {
            List<Notification> initial = curatorDb.readNotifications(source.tenant());
            List<Notification> filtered = initial.stream()
                    .filter(notification -> !source.equals(notification.source()) || type != notification.type())
                    .collect(Collectors.toUnmodifiableList());
            if (initial.size() > filtered.size())
                curatorDb.writeNotifications(source.tenant(), filtered);
        }
    }

    /** Remove all notifications for this source or sources contained by this source */
    public void removeNotifications(NotificationSource source) {
        try (Lock lock = curatorDb.lockNotifications(source.tenant())) {
            if (source.application().isEmpty()) { // Source is tenant
                curatorDb.deleteNotifications(source.tenant());
                return;
            }

            List<Notification> initial = curatorDb.readNotifications(source.tenant());
            List<Notification> filtered = initial.stream()
                    .filter(notification -> !source.contains(notification.source()))
                    .collect(Collectors.toUnmodifiableList());
            if (initial.size() > filtered.size())
                curatorDb.writeNotifications(source.tenant(), filtered);
        }
    }

    /**
     * Updates feeding blocked notifications for the given deployment based on current cluster metrics.
     * Will clear notifications of any cluster not reporting the metrics or whose metrics indicate feed is not blocked,
     * while setting notifications for cluster that are (Level.error) or are nearly (Level.warning) feed blocked.
     */
    public void setDeploymentFeedingBlockedNotifications(DeploymentId deploymentId, List<ClusterMetrics> clusterMetrics) {
        Instant now = clock.instant();
        List<Notification> feedBlockNotifications = clusterMetrics.stream()
                .flatMap(metric -> {
                    Optional<Pair<Level, String>> memoryStatus =
                            resourceUtilToFeedBlockStatus("memory", metric.memoryUtil(), metric.memoryFeedBlockLimit());
                    Optional<Pair<Level, String>> diskStatus =
                            resourceUtilToFeedBlockStatus("disk", metric.diskUtil(), metric.diskFeedBlockLimit());
                    if (memoryStatus.isEmpty() && diskStatus.isEmpty()) return Stream.empty();

                    // Find the max among levels
                    Level level = Stream.of(memoryStatus, diskStatus)
                            .flatMap(status -> status.stream().map(Pair::getFirst))
                            .max(Comparator.comparing(Enum::ordinal)).get();
                    List<String> messages = Stream.concat(memoryStatus.stream(), diskStatus.stream())
                            .filter(status -> status.getFirst() == level) // Do not mix message from different levels
                            .map(Pair::getSecond)
                            .collect(Collectors.toUnmodifiableList());
                    NotificationSource source = NotificationSource.from(deploymentId, ClusterSpec.Id.from(metric.getClusterId()));
                    return Stream.of(new Notification(now, Type.feedBlock, level, source, messages));
                })
                .collect(Collectors.toUnmodifiableList());

        NotificationSource deploymentSource = NotificationSource.from(deploymentId);
        try (Lock lock = curatorDb.lockNotifications(deploymentSource.tenant())) {
            List<Notification> initial = curatorDb.readNotifications(deploymentSource.tenant());
            List<Notification> updated = Stream.concat(
                    initial.stream()
                            .filter(notification ->
                                    // Filter out old feed block notifications for this deployment
                                    notification.type() != Type.feedBlock || !deploymentSource.contains(notification.source())),
                    // ... and add the new notifications for this deployment
                    feedBlockNotifications.stream())
                    .collect(Collectors.toUnmodifiableList());

            if (!initial.equals(updated))
                curatorDb.writeNotifications(deploymentSource.tenant(), updated);
        }
    }

    /**
     * Returns a feed block summary for the given resource: the notification level and
     * notification message for the given resource utilization wrt. given resource limit.
     * If utilization is well below the limit, Optional.empty() is returned.
     */
    private static Optional<Pair<Level, String>> resourceUtilToFeedBlockStatus(
            String resource, Optional<Double> util, Optional<Double> feedBlockLimit) {
        if (util.isEmpty() || feedBlockLimit.isEmpty()) return Optional.empty();
        double utilRelativeToLimit = util.get() / feedBlockLimit.get();
        if (utilRelativeToLimit < 0.9) return Optional.empty();

        String message = String.format(Locale.US, "%s (usage: %.1f%%, feed block limit: %.1f%%)",
                resource, 100 * util.get(), 100 * feedBlockLimit.get());
        if (utilRelativeToLimit < 1) return Optional.of(new Pair<>(Level.warning, message));
        return Optional.of(new Pair<>(Level.error, message));
    }
}
