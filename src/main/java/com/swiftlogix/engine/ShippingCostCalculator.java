package com.swiftlogix.engine;

import com.swiftlogix.model.ParcelPriority;

/**
 * ShippingCostCalculator - Dynamic rate and shipping fee calculation engine.
 */
public class ShippingCostCalculator {
    public static final double BASE_FEE = 100.0;           // Base handling fee in INR
    public static final double RATE_PER_KG = 35.0;         // ₹35 per kg
    public static final double RATE_PER_KM = 0.75;         // ₹0.75 per km
    public static final double FUEL_SURCHARGE_RATE = 0.08; // 8% fuel surcharge

    public static class Quote {
        public double baseFee;
        public double weightCharge;
        public double distanceCharge;
        public double subtotal;
        public double fuelSurcharge;
        public double priorityMultiplier;
        public double totalCost;
        public ParcelPriority priority;

        public Quote(double baseFee, double weightCharge, double distanceCharge, double fuelSurcharge, double priorityMultiplier, double totalCost, ParcelPriority priority) {
            this.baseFee = baseFee;
            this.weightCharge = weightCharge;
            this.distanceCharge = distanceCharge;
            this.subtotal = baseFee + weightCharge + distanceCharge;
            this.fuelSurcharge = fuelSurcharge;
            this.priorityMultiplier = priorityMultiplier;
            this.totalCost = totalCost;
            this.priority = priority;
        }

        public String getFormattedBreakdown() {
            return String.format(
                    "💰 Shipping Quote Breakdown (%s):\n" +
                    "   ├─ Base Handling Fee:    ₹%.2f\n" +
                    "   ├─ Weight Charge:        ₹%.2f\n" +
                    "   ├─ Distance Charge:      ₹%.2f\n" +
                    "   ├─ Fuel Surcharge (8%%):  ₹%.2f\n" +
                    "   ├─ Priority Multiplier:  x%.1f (%s)\n" +
                    "   └─ TOTAL COST:           ₹%.2f",
                    priority.getDisplayName(), baseFee, weightCharge, distanceCharge, fuelSurcharge,
                    priorityMultiplier, priority.name(), totalCost);
        }
    }

    public static Quote generateQuote(double weightKg, double distanceKm, ParcelPriority priority) {
        if (priority == null) priority = ParcelPriority.STANDARD;

        double weightCharge = Math.max(0, weightKg) * RATE_PER_KG;
        double distanceCharge = Math.max(0, distanceKm) * RATE_PER_KM;
        double subtotal = BASE_FEE + weightCharge + distanceCharge;
        double fuelSurcharge = subtotal * FUEL_SURCHARGE_RATE;
        double totalBeforePriority = subtotal + fuelSurcharge;
        double totalCost = Math.round(totalBeforePriority * priority.getCostMultiplier() * 100.0) / 100.0;

        return new Quote(BASE_FEE, weightCharge, distanceCharge, fuelSurcharge, priority.getCostMultiplier(), totalCost, priority);
    }

    public static double calculateCost(double weightKg, double distanceKm, ParcelPriority priority) {
        return generateQuote(weightKg, distanceKm, priority).totalCost;
    }
}
