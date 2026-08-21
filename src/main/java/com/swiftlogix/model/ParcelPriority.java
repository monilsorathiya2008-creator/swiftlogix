package com.swiftlogix.model;

/**
 * ParcelPriority - Defines the priority tiers for shipments in SwiftLogix.
 */
public enum ParcelPriority {
    SAME_DAY(1, 2.2, "Same-Day Priority"),
    EXPRESS(2, 1.5, "Express Delivery"),
    STANDARD(3, 1.0, "Standard Delivery");

    private final int rank;             // Lower number = higher sorting priority
    private final double costMultiplier; // Multiplier applied to base shipping cost
    private final String displayName;

    ParcelPriority(int rank, double costMultiplier, String displayName) {
        this.rank = rank;
        this.costMultiplier = costMultiplier;
        this.displayName = displayName;
    }

    public int getRank() {
        return rank;
    }

    public double getCostMultiplier() {
        return costMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ParcelPriority fromString(String text) {
        if (text == null) return STANDARD;
        try {
            return ParcelPriority.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STANDARD;
        }
    }
}
