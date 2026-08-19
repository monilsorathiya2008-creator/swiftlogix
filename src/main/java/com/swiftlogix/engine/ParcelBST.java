package com.swiftlogix.engine;

import com.swiftlogix.model.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * ParcelBST - Binary Search Tree for O(log N) parcel tracking lookup by tracking code.
 */
public class ParcelBST {
    private static class Node {
        String trackingCode;
        Parcel parcel;
        Node left;
        Node right;

        Node(Parcel parcel) {
            this.trackingCode = parcel.getTrackingCode();
            this.parcel = parcel;
        }
    }

    private Node root;
    private int size;

    public ParcelBST() {
        this.root = null;
        this.size = 0;
    }

    public void insert(Parcel parcel) {
        if (parcel == null || parcel.getTrackingCode() == null) return;
        this.root = insertRecursive(this.root, parcel);
    }

    private Node insertRecursive(Node current, Parcel parcel) {
        if (current == null) {
            size++;
            return new Node(parcel);
        }

        int cmp = parcel.getTrackingCode().compareTo(current.trackingCode);
        if (cmp < 0) {
            current.left = insertRecursive(current.left, parcel);
        } else if (cmp > 0) {
            current.right = insertRecursive(current.right, parcel);
        } else {
            current.parcel = parcel;
        }
        return current;
    }

    public Parcel search(String trackingCode) {
        if (trackingCode == null) return null;
        return searchRecursive(this.root, trackingCode.trim().toUpperCase());
    }

    private Parcel searchRecursive(Node current, String trackingCode) {
        if (current == null) return null;

        int cmp = trackingCode.compareTo(current.trackingCode);
        if (cmp < 0) {
            return searchRecursive(current.left, trackingCode);
        } else if (cmp > 0) {
            return searchRecursive(current.right, trackingCode);
        } else {
            return current.parcel;
        }
    }

    public List<Parcel> getAllInOrder() {
        List<Parcel> result = new ArrayList<>();
        inOrderTraversal(root, result);
        return result;
    }

    private void inOrderTraversal(Node current, List<Parcel> result) {
        if (current != null) {
            inOrderTraversal(current.left, result);
            result.add(current.parcel);
            inOrderTraversal(current.right, result);
        }
    }

    public int getSize() { return size; }
}
