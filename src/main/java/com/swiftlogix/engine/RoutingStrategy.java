package com.swiftlogix.engine;

/**
 * RoutingStrategy - Optimization objectives for Dijkstra pathfinding.
 */
public enum RoutingStrategy {
    SHORTEST_DISTANCE("Shortest Distance (km)"),
    FASTEST_TIME("Fastest Transit Time (hrs)"),
    LEAST_CONGESTED("Congestion & Hub Load Avoidance");

    private final String description;

    RoutingStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
