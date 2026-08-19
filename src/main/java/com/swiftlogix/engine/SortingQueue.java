package com.swiftlogix.engine;

import com.swiftlogix.model.Parcel;

import java.util.LinkedList;
import java.util.Queue;

/**
 * SortingQueue - FIFO Queue for warehouse hub parcel sorting.
 */
public class SortingQueue {
    private String hubId;
    private Queue<Parcel> items;

    public SortingQueue(String hubId) {
        this.hubId = hubId;
        this.items = new LinkedList<>();
    }

    public void enqueue(Parcel parcel) {
        items.add(parcel);
    }

    public Parcel dequeue() {
        return items.poll();
    }

    public Parcel peek() {
        return items.peek();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public String getHubId() { return hubId; }
}
