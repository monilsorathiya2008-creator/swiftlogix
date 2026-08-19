package com.swiftlogix.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Parcel - Represents an express package shipped through the SwiftLogix logistics network.
 */
public class Parcel {
    private String trackingCode;     // Unique tracking ID (e.g. "SLX-90142")
    private String sender;           // Sender Name / Enterprise
    private String receiver;         // Recipient Name
    private double weightKg;         // Weight in Kilograms
    private String originHubId;      // Origin Hub ID
    private String destHubId;        // Destination Hub ID
    private String currentHubId;     // Current location Hub ID
    private String status;           // Status (BOOKED, IN_TRANSIT, ARRIVED_AT_HUB, OUT_FOR_DELIVERY)
    private List<String> route;      // Sequence of Hub IDs along optimal path
    private List<Checkpoint> checkpoints; // History of scan checkpoints

    public Parcel(String trackingCode, String sender, String receiver, double weightKg, String originHubId, String destHubId) {
        this.trackingCode = trackingCode;
        this.sender = sender;
        this.receiver = receiver;
        this.weightKg = weightKg;
        this.originHubId = originHubId;
        this.destHubId = destHubId;
        this.currentHubId = originHubId;
        this.status = "BOOKED";
        this.route = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
    }

    public String getTrackingCode() { return trackingCode; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public double getWeightKg() { return weightKg; }
    public String getOriginHubId() { return originHubId; }
    public String getDestHubId() { return destHubId; }
    public String getCurrentHubId() { return currentHubId; }
    public void setCurrentHubId(String currentHubId) { this.currentHubId = currentHubId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getRoute() { return route; }
    public void setRoute(List<String> route) { this.route = route; }
    public List<Checkpoint> getCheckpoints() { return checkpoints; }

    public void addCheckpoint(Checkpoint cp) {
        this.checkpoints.add(cp);
    }
}
