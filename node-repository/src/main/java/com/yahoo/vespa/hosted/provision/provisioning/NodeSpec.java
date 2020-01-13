// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision.provisioning;

import com.yahoo.config.provision.Flavor;
import com.yahoo.config.provision.NodeFlavors;
import com.yahoo.config.provision.NodeResources;
import com.yahoo.config.provision.NodeType;
import com.yahoo.vespa.hosted.provision.Node;

import java.util.Objects;
import java.util.Optional;

/**
 * A specification of a set of nodes.
 * This reflects that nodes can be requested either by count and flavor or by type,
 * and encapsulates the differences in logic between these two cases.
 *
 * @author bratseth
 */
public interface NodeSpec {

    /** The node type this requests */
    NodeType type();

    /**
     * Returns whether the physical hosts running the nodes of this application can
     * also run nodes of other applications.
     */
    boolean isExclusive();

    /** Returns whether the given flavor is compatible with this spec */
    boolean isCompatible(Flavor flavor, NodeFlavors flavors);

    /** Returns whether the given node count is sufficient to consider this spec fulfilled to the maximum amount */
    boolean saturatedBy(int count);

    /** Returns whether the given node count is sufficient to fulfill this spec */
    default boolean fulfilledBy(int count) {
        return fulfilledDeficitCount(count) == 0;
    }

    /** Returns whether this should throw an exception if the requested nodes are not fully available */
    boolean canFail();

    /** Returns whether we should retire nodes at all when fulfilling this spec */
    boolean considerRetiring();

    /** Returns the ideal number of nodes that should be retired to fulfill this spec */
    int idealRetiredCount(int acceptedCount, int currentRetiredCount);

    /** Returns number of additional nodes needed for this spec to be fulfilled given the current node count */
    int fulfilledDeficitCount(int count);

    /** Returns a specification of a fraction of all the nodes of this. It is assumed the argument is a valid divisor. */
    NodeSpec fraction(int divisor);

    /** Returns the resources requested by this or empty if none are explicitly requested */
    Optional<NodeResources> resources();

    static NodeSpec from(int nodeCount, NodeResources resources, boolean exclusive, boolean canFail) {
        return new CountNodeSpec(nodeCount, resources, exclusive, canFail);
    }

    static NodeSpec from(NodeType type) {
        return new TypeNodeSpec(type);
    }

    /** A node spec specifying a node count and a flavor */
    class CountNodeSpec implements NodeSpec {

        private final int count;
        private final NodeResources requestedNodeResources;
        private final boolean exclusive;
        private final boolean canFail;

        private CountNodeSpec(int count, NodeResources resources, boolean exclusive, boolean canFail) {
            this.count = count;
            this.requestedNodeResources = Objects.requireNonNull(resources, "Resources must be specified");
            this.exclusive = exclusive;
            this.canFail = canFail;
        }

        @Override
        public Optional<NodeResources> resources() {
            return Optional.of(requestedNodeResources);
        }

        @Override
        public boolean isExclusive() { return exclusive; }

        @Override
        public NodeType type() { return NodeType.tenant; }

        @Override
        public boolean isCompatible(Flavor flavor, NodeFlavors flavors) {
            if (flavor.isDocker()) { // Docker nodes can satisfy a request for parts of their resources
                return flavor.resources().compatibleWith(requestedNodeResources);
            } else { // Other nodes must be matched exactly
                return requestedNodeResources.equals(flavor.resources());
            }
        }

        @Override
        public boolean saturatedBy(int count) { return fulfilledBy(count); } // min=max for count specs

        @Override
        public boolean canFail() { return canFail; }

        @Override
        public boolean considerRetiring() {
            // If we cannot fail we cannot retire as we may end up without sufficient replacement capacity
            return canFail();
        }

        @Override
        public int idealRetiredCount(int acceptedCount, int currentRetiredCount) { return acceptedCount - this.count; }

        @Override
        public int fulfilledDeficitCount(int count) {
            return Math.max(this.count - count, 0);
        }

        @Override
        public NodeSpec fraction(int divisor) {
            return new CountNodeSpec(count/divisor, requestedNodeResources, exclusive, canFail);
        }

        @Override
        public Node assignRequestedFlavor(Node node) {
            // Docker nodes can change flavor in place - disabled - see below
            // if (requestedFlavorCanBeAchievedByResizing(node.flavor()))
            //    return node.with(requestedFlavor);
            return node;
        }

        @Override
        public String toString() { return "request for " + count + " nodes with " + requestedNodeResources; }

    }

    /** A node spec specifying a node type. This will accept all nodes of this type. */
    class TypeNodeSpec implements NodeSpec {

        private final NodeType type;

        public TypeNodeSpec(NodeType type) {
            this.type = type;
        }

        @Override
        public NodeType type() { return type; }

        @Override
        public boolean isExclusive() { return false; }

        @Override
        public boolean isCompatible(Flavor flavor, NodeFlavors flavors) { return true; }

        @Override
        public boolean saturatedBy(int count) { return false; }

        @Override
        public boolean canFail() { return false; }

        @Override
        public boolean considerRetiring() { return true; }

        @Override
        public int idealRetiredCount(int acceptedCount, int currentRetiredCount) {
             // All nodes marked with wantToRetire get marked as retired just before this function is called,
             // the job of this function is to throttle the retired count. If no nodes are marked as retired
             // then continue this way, otherwise allow only 1 node to be retired
            return Math.min(1, currentRetiredCount);
        }

        @Override
        public int fulfilledDeficitCount(int count) {
            return 0;
        }

        @Override
        public NodeSpec fraction(int divisor) { return this; }

        @Override
        public Optional<NodeResources> resources() { return Optional.empty(); }

        @Override
        public String toString() { return "request for all nodes of type '" + type + "'"; }

    }

}
