package com.swiftlogix.model;

/**
 * Hub - Represents a city logistics warehouse hub in the SwiftLogix network.
 */
public class Hub {
    private String id;          // Unique ID (e.g. "HUB_BOM")
    private String name;        // Full Name (e.g. "Mumbai Central Hub")
    private String city;        // City Name (e.g. "Mumbai")
    private double lat;         // Latitude coordinate
    private double lng;         // Longitude coordinate
    private int capacity;       // Total package storage capacity
    private int currentLoad;    // Current number of packages stored

    public Hub(String id, String name, String city, double lat, double lng, int capacity) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
        this.capacity = capacity;
        this.currentLoad = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public int getCapacity() { return capacity; }
    public int getCurrentLoad() { return currentLoad; }

    public void updateLoad(int delta) {
        this.currentLoad = Math.max(0, Math.min(capacity * 2, this.currentLoad + delta));
    }

    public double getUtilizationPercent() {
        if (capacity == 0) return 0.0;
        return Math.round(((double) currentLoad / capacity) * 100.0);
    }

    public boolean isOverloaded() {
        return getUtilizationPercent() >= 85.0;
    }

    public int getAvailableCapacity() {
        return Math.max(0, capacity - currentLoad);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) [Load: %d/%d (%.1f%%)%s]",
                name, city, currentLoad, capacity, getUtilizationPercent(), isOverloaded() ? " ⚠️ OVERLOADED" : "");
    }
}
