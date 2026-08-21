package com.swiftlogix.model;

/**
 * Route - Represents a weighted connection (highway edge) between two hubs.
 */
public class Route {
    private String targetHubId;  // Target Hub ID
    private double distanceKm;   // Distance in kilometers
    private double timeHours;    // Estimated transit time in hours
    private boolean active;      // Operational status (true = open, false = road blocked/maintenance)

    public Route(String targetHubId, double distanceKm, double timeHours) {
        this(targetHubId, distanceKm, timeHours, true);
    }

    public Route(String targetHubId, double distanceKm, double timeHours, boolean active) {
        this.targetHubId = targetHubId;
        this.distanceKm = distanceKm;
        this.timeHours = timeHours;
        this.active = active;
    }

    public String getTargetHubId() { return targetHubId; }
    public double getDistanceKm() { return distanceKm; }
    public double getTimeHours() { return timeHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return String.format("Route[-> %s, %.1f km, %.1f hrs, %s]",
                targetHubId, distanceKm, timeHours, active ? "ACTIVE" : "BLOCKED");
    }
}
