package com.swiftlogix;

import com.swiftlogix.engine.DijkstraRouter;
import com.swiftlogix.engine.HubGraph;
import com.swiftlogix.engine.ParcelBST;
import com.swiftlogix.engine.SortingQueue;
import com.swiftlogix.model.Checkpoint;
import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Parcel;

import java.util.stream.Collectors;

/**
 * Main - Pure Java entrypoint demonstrating HubGraph, Dijkstra Router, ParcelBST, and SortingQueue.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("🚚 SwiftLogix: Express Warehouse & Dijkstra Routing Engine (Java)");
        System.out.println("=========================================================\n");

        // Step 1: Initialize HubGraph with 9 Indian logistics hubs
        HubGraph graph = new HubGraph();
        graph.addHub("HUB_BOM", "Mumbai Central Hub", "Mumbai", 19.0760, 72.8777, 1500);
        graph.addHub("HUB_DEL", "Delhi NCR Mega Hub", "New Delhi", 28.6139, 77.2090, 2000);
        graph.addHub("HUB_BLR", "Bengaluru Tech Hub", "Bengaluru", 12.9716, 77.5946, 1200);
        graph.addHub("HUB_MAA", "Chennai Port Hub", "Chennai", 13.0827, 80.2707, 1000);
        graph.addHub("HUB_CCU", "Kolkata East Hub", "Kolkata", 22.5726, 88.3639, 1100);
        graph.addHub("HUB_HYD", "Hyderabad Logistics Hub", "Hyderabad", 17.3850, 78.4867, 1300);
        graph.addHub("HUB_AMD", "Ahmedabad Industrial Hub", "Ahmedabad", 23.0225, 72.5714, 900);
        graph.addHub("HUB_PNQ", "Pune Express Gateway", "Pune", 18.5204, 73.8567, 800);
        graph.addHub("HUB_JAI", "Jaipur North Logistics", "Jaipur", 26.9124, 75.7873, 750);

        // Step 2: Add Weighted Interconnecting Transport Routes (Distance in km, Hours)
        graph.addRoute("HUB_BOM", "HUB_PNQ", 150, 3.0, true);
        graph.addRoute("HUB_BOM", "HUB_AMD", 530, 9.0, true);
        graph.addRoute("HUB_BOM", "HUB_HYD", 710, 12.0, true);
        graph.addRoute("HUB_BOM", "HUB_BLR", 980, 16.0, true);
        graph.addRoute("HUB_DEL", "HUB_JAI", 280, 5.0, true);
        graph.addRoute("HUB_DEL", "HUB_AMD", 940, 15.0, true);
        graph.addRoute("HUB_DEL", "HUB_CCU", 1530, 24.0, true);
        graph.addRoute("HUB_DEL", "HUB_BOM", 1420, 22.0, true);
        graph.addRoute("HUB_BLR", "HUB_HYD", 570, 9.0, true);
        graph.addRoute("HUB_BLR", "HUB_MAA", 350, 6.0, true);
        graph.addRoute("HUB_MAA", "HUB_HYD", 630, 10.0, true);
        graph.addRoute("HUB_HYD", "HUB_CCU", 1490, 23.0, true);

        System.out.println("✅ HubGraph initialized with " + graph.getAllHubs().size() + " hubs.");

        // Step 3: Run Dijkstra Algorithm to calculate shortest route
        DijkstraRouter router = new DijkstraRouter(graph);
        String src = "HUB_BOM";
        String dest = "HUB_DEL";
        
        System.out.println("\n🔍 Running Dijkstra Algorithm (" + src + " -> " + dest + "):");
        DijkstraRouter.PathResult pathResult = router.findShortestPath(src, dest);

        if (pathResult.found) {
            String routeCities = pathResult.path.stream().map(Hub::getCity).collect(Collectors.joining(" -> "));
            System.out.println("   ▶ Optimal Path: " + routeCities);
            System.out.println("   ▶ Total Distance: " + pathResult.totalDistanceKm + " km");
            System.out.println("   ▶ Est. Transit Time: " + pathResult.totalHours + " hrs");
        }

        // Step 4: Test Parcel BST Indexing
        System.out.println("\n📦 Testing ParcelBST Search Index:");
        ParcelBST bst = new ParcelBST();
        Parcel p1 = new Parcel("SLX-90142", "TechCorp Electronics", "Aarav Sharma", 2.5, "HUB_BOM", "HUB_DEL");
        p1.addCheckpoint(new Checkpoint("BOOKED", "HUB_BOM", "Mumbai Central Hub"));
        bst.insert(p1);

        Parcel found = bst.search("SLX-90142");
        if (found != null) {
            System.out.println("   ▶ Found Parcel: " + found.getTrackingCode() + " (" + found.getSender() + " -> " + found.getReceiver() + ")");
        }

        // Step 5: Test FIFO Warehouse Queue
        System.out.println("\n🏭 Testing FIFO Warehouse Sorting Queue (HUB_BOM):");
        SortingQueue queue = new SortingQueue("HUB_BOM");
        queue.enqueue(p1);
        System.out.println("   ▶ Queue Size: " + queue.size());

        System.out.println("\n🎉 Pure Java Logistics Engine Execution Completed Successfully!");
    }
}
