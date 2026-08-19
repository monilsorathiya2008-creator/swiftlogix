package com.swiftlogix.model;

/**
 * Route - Represents a weighted connection (highway edge) between two hubs.
 */
public class Route {
    private String targetHubId;  // Target Hub ID
    private double distanceKm;   // Distance in kilometers
    private double timeHours;    // Estimated transit time in hours

    public Route(String targetHubId, double distanceKm, double timeHours) {
        this.targetHubId = targetHubId;
        this.distanceKm = distanceKm;
        this.timeHours = timeHours;
    }

    public String getTargetHubId() { return targetHubId; }
    public double getDistanceKm() { return distanceKm; }
    public double getTimeHours() { return timeHours; }
}
