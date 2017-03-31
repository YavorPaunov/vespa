// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision.node;

import com.google.common.collect.ImmutableMap;
import com.yahoo.vespa.hosted.provision.Node;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An immutable record of the last event of each type happening to this node.
 * Note that the history cannot be used to find the nodes current state - it will have a record of some
 * event happening in the past even if that event is later undone.
 *
 * @author bratseth
 */
public class History {

    private final ImmutableMap<Event.Type, Event> events;

    public History(Collection<Event> events) {
        this(toImmutableMap(events));
    }

    private History(ImmutableMap<Event.Type, Event> events) {
        this.events = events;
    }

    private static ImmutableMap<Event.Type, Event> toImmutableMap(Collection<Event> events) {
        ImmutableMap.Builder<Event.Type, Event> builder = new ImmutableMap.Builder<>();
        for (Event event : events)
            builder.put(event.type(), event);
        return builder.build();
    }

    /** Returns this event if it is present in this history */
    public Optional<Event> event(Event.Type type) { return Optional.ofNullable(events.get(type)); }

    public Collection<Event> events() { return events.values(); }

    /** Returns a copy of this history with the given event added */
    public History with(Event event) {
        ImmutableMap.Builder<Event.Type, Event> builder = builderWithout(event.type());
        builder.put(event.type(), event);
        return new History(builder.build());
    }

    /** Returns a copy of this history with the given event type removed (or an identical history if it was not present) */
    public History without(Event.Type type) {
        return new History(builderWithout(type).build());
    }

    private ImmutableMap.Builder<Event.Type, Event> builderWithout(Event.Type type) {
        ImmutableMap.Builder<Event.Type, Event> builder = new ImmutableMap.Builder<>();
        for (Event event : events.values())
            if (event.type() != type)
                builder.put(event.type(), event);
        return builder;
    }

    /** Returns a copy of this history with a record of this state transition added, if applicable */
    public History recordStateTransition(Node.State from, Node.State to, Instant at) {
        if (from == to) return this;
        switch (to) {
            case ready:    return this.withoutApplicationEvents().with(new Event(Event.Type.readied, at));
            case active:   return this.with(new Event(Event.Type.activated, at));
            case inactive: return this.with(new Event(Event.Type.deactivated, at));
            case reserved: return this.with(new Event(Event.Type.reserved, at));
            case failed:   return this.with(new Event(Event.Type.failed, at));
            case dirty:    return this.with(new Event(Event.Type.deallocated, at));
            default:       return this;
        }
    }
    
    /** 
     * Events can be application or node level. 
     * This returns a copy of this history with all application level events removed. 
     */
    private History withoutApplicationEvents() {
        return new History(events().stream().filter(e -> ! e.type().isApplicationLevel()).collect(Collectors.toList()));
    }

    /** Returns the empty history */
    public static History empty() { return new History(Collections.emptyList()); }

    @Override
    public String toString() {
        if (events.isEmpty()) return "history: (empty)";
        StringBuilder b = new StringBuilder("history: ");
        for (Event e : events.values())
            b.append(e).append(", ");
         b.setLength(b.length() -2); // remove last comma
        return b.toString();
    }
    
    /** An event which may happen to a node */
    public static class Event {

        private final Instant at;
        private final Type type;
        private final Agent agent;

        public enum Agent { system, application, operator }

        /** Creates an event caused by the system */
        public Event(Event.Type type, Instant at) {
            this(type, at, Agent.system);
        }

        public Event(Event.Type type, Instant at, Agent agent) {
            this.type = type;
            this.at = at;
            this.agent = agent;
        }

        /** Returns the type of event */
        public Event.Type type() { return type; }

        /** Returns the instant this even took place */
        public Instant at() { return at; }
        
        /** Returns the agent causing this event */
        public Agent agent() { return agent; }

        public enum Type { 
            // State move events
            readied, reserved, activated, deactivated, deallocated,
            // The active node was retired
            retired,
            // The active node went down according to the service monitor
            down, 
            // The node made a config request, indicating it is live
            requested,
            // The node was rebooted
            rebooted(false),
            // The node was failed
            failed(false);
            
            private final boolean applicationLevel;
            
            /** Creates an application level event */
            Type() {
                this.applicationLevel = true;
            }

            Type(boolean applicationLevel) {
                this.applicationLevel = applicationLevel;
            }
            
            /** Returns true if this is an application level event and false it it is a node level event */
            public boolean isApplicationLevel() { return applicationLevel; }
        }

        @Override
        public String toString() { return "'" + type + "' event at " + at; }

    }

}
