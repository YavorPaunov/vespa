// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.container;

import com.yahoo.collections.Pair;
import com.yahoo.vespa.hosted.node.admin.nodeagent.NodeAgentContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static com.yahoo.vespa.hosted.node.admin.container.ContainerStatsCollector.userHzToMicroSeconds;

/**
 * Read and write interface to the CGroup of a podman container.
 *
 * @author freva
 */
public interface CGroup {

    Optional<Pair<Integer, Integer>> cpuQuotaPeriod(ContainerId containerId);
    OptionalInt cpuShares(ContainerId containerId);

    boolean updateCpuQuotaPeriod(NodeAgentContext context, ContainerId containerId, int cpuQuotaUs, int periodUs);
    boolean updateCpuShares(NodeAgentContext context, ContainerId containerId, int shares);

    Map<CpuStatField, Long> cpuStats(ContainerId containerId) throws IOException;

    /** @return Maximum amount of memory that can be used by the cgroup and its descendants. */
    long memoryLimitInBytes(ContainerId containerId) throws IOException;

    /** @return The total amount of memory currently being used by the cgroup and its descendants. */
    long memoryUsageInBytes(ContainerId containerId) throws IOException;

    /** @return Number of bytes used to cache filesystem data, including tmpfs and shared memory. */
    long memoryCacheInBytes(ContainerId containerId) throws IOException;

    enum CpuStatField {
        TOTAL_USAGE_USEC(null/* in a dedicated file */, "usage_usec"),
        USER_USAGE_USEC("user", "user_usec"),
        SYSTEM_USAGE_USEC("system", "system_usec"),
        TOTAL_PERIODS("nr_periods", "nr_periods"),
        THROTTLED_PERIODS("nr_throttled", "nr_throttled"),
        THROTTLED_TIME_USEC("throttled_time", "throttled_usec");

        private final String v1Name;
        private final String v2Name;
        CpuStatField(String v1Name, String v2Name) {
            this.v1Name = v1Name;
            this.v2Name = v2Name;
        }

        long parseValueV1(String value) {
            long longValue = Long.parseLong(value);
            switch (this) {
                case THROTTLED_TIME_USEC:
                case TOTAL_USAGE_USEC:
                    return longValue / 1000; // Value in ns
                case USER_USAGE_USEC:
                case SYSTEM_USAGE_USEC:
                    return userHzToMicroSeconds(longValue);
                default: return longValue;
            }
        }

        long parseValueV2(String value) {
            return Long.parseLong(value);
        }

        static Optional<CpuStatField> fromV1Field(String name) {
            return Arrays.stream(values())
                    .filter(field -> name.equals(field.v1Name))
                    .findFirst();
        }

        static Optional<CpuStatField> fromV2Field(String name) {
            return Arrays.stream(values())
                    .filter(field -> name.equals(field.v2Name))
                    .findFirst();
        }
    }
}
