package com.swiftlogix.engine;

import com.swiftlogix.model.Parcel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * SortingQueue - Priority-Aware Min-Heap Queue for warehouse hub parcel sorting.
 * Parcels with higher priority (SAME_DAY > EXPRESS > STANDARD) are dequeued first,
 * with FIFO ordering preserved among packages with identical priority.
 */
public class SortingQueue {
    private static class QueueEntry {
        Parcel parcel;
        long sequenceNumber;

        QueueEntry(Parcel parcel, long sequenceNumber) {
            this.parcel = parcel;
            this.sequenceNumber = sequenceNumber;
        }
    }

    private String hubId;
    private PriorityQueue<QueueEntry> items;
    private long globalSequenceCounter = 0;

    public SortingQueue(String hubId) {
        this.hubId = hubId;
        this.items = new PriorityQueue<>(
                Comparator.<QueueEntry>comparingInt(e -> e.parcel.getPriority() != null ? e.parcel.getPriority().getRank() : 3)
                          .thenComparingLong(e -> e.sequenceNumber)
        );
    }

    public synchronized void enqueue(Parcel parcel) {
        if (parcel != null) {
            items.add(new QueueEntry(parcel, ++globalSequenceCounter));
        }
    }

    public synchronized Parcel dequeue() {
        QueueEntry entry = items.poll();
        return entry != null ? entry.parcel : null;
    }

    public synchronized Parcel peek() {
        QueueEntry entry = items.peek();
        return entry != null ? entry.parcel : null;
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized List<Parcel> getAllInPriorityOrder() {
        PriorityQueue<QueueEntry> copy = new PriorityQueue<>(items);
        List<Parcel> result = new ArrayList<>();
        while (!copy.isEmpty()) {
            result.add(copy.poll().parcel);
        }
        return result;
    }

    public String getHubId() { return hubId; }
}
