package com.swiftlogix.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Checkpoint - Represents a tracking milestone event (e.g. BOOKED, ARRIVED_AT_HUB, DISPATCHED).
 */
public class Checkpoint {
    private String status;
    private String hubId;
    private String hubName;
    private String timestamp;

    public Checkpoint(String status, String hubId, String hubName) {
        this.status = status;
        this.hubId = hubId;
        this.hubName = hubName;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Checkpoint(String status, String hubId, String hubName, String timestamp) {
        this.status = status;
        this.hubId = hubId;
        this.hubName = hubName;
        this.timestamp = timestamp;
    }

    public String getStatus() { return status; }
    public String getHubId() { return hubId; }
    public String getHubName() { return hubName; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s", timestamp, status, hubName);
    }
}
