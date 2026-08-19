package com.swiftlogix;

import com.swiftlogix.database.DatabaseManager;
import com.swiftlogix.engine.DijkstraRouter;
import com.swiftlogix.engine.HubGraph;
import com.swiftlogix.engine.ParcelBST;
import com.swiftlogix.engine.SortingQueue;
import com.swiftlogix.model.Checkpoint;
import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Parcel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main - Interactive Command-Line Interface (CLI) connected to XAMPP MySQL Database.
 */
public class Main {
    private static HubGraph graph = new HubGraph();
    private static DijkstraRouter router;
    private static ParcelBST bst = new ParcelBST();
    private static Map<String, SortingQueue> hubQueues = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("🚚 SwiftLogix: Interactive Java CLI & XAMPP MySQL Engine");
        System.out.println("=========================================================");

        // 1. Initialize XAMPP MySQL Database Tables
        DatabaseManager.initializeDatabase();

        // 2. Load existing hubs, routes, and parcels from XAMPP MySQL
        DatabaseManager.loadNetworkAndParcels(graph, bst, hubQueues);

        // 3. If database is empty, seed initial network
        if (graph.getAllHubs().isEmpty()) {
            initSeedNetworkData();
        }

        router = new DijkstraRouter(graph);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n---------------- MAIN MENU ----------------");
            System.out.println("1. 🏙️  List All Logistics Hubs & Capacity");
            System.out.println("2. 🧮 Run Dijkstra Shortest Path Search");
            System.out.println("3. 📦 Book New Express Shipment (Saved to MySQL)");
            System.out.println("4. 🔍 Track Parcel by Code (BST & MySQL Search)");
            System.out.println("5. 🏭 Simulate Warehouse Barcode Scan");
            System.out.println("6. 🚪 Exit Terminal");
            System.out.print("\nSelect Option (1-6): ");

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    listHubs();
                    break;
                case "2":
                    queryShortestPath(scanner);
                    break;
                case "3":
                    bookShipment(scanner);
                    break;
                case "4":
                    trackParcel(scanner);
                    break;
                case "5":
                    simulateScanner(scanner);
                    break;
                case "6":
                    System.out.println("\n👋 Thank you for using SwiftLogix Logistics Engine. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("❌ Invalid option. Please enter a number from 1 to 6.");
            }
        }
        scanner.close();
    }

    private static void initSeedNetworkData() {
        System.out.println("🌱 Seeding initial logistics network data into XAMPP MySQL...");

        graph.addHub("HUB_BOM", "Mumbai Central Hub", "Mumbai", 19.0760, 72.8777, 1500);
        graph.addHub("HUB_DEL", "Delhi NCR Mega Hub", "New Delhi", 28.6139, 77.2090, 2000);
        graph.addHub("HUB_BLR", "Bengaluru Tech Hub", "Bengaluru", 12.9716, 77.5946, 1200);
        graph.addHub("HUB_MAA", "Chennai Port Hub", "Chennai", 13.0827, 80.2707, 1000);
        graph.addHub("HUB_CCU", "Kolkata East Hub", "Kolkata", 22.5726, 88.3639, 1100);
        graph.addHub("HUB_HYD", "Hyderabad Logistics Hub", "Hyderabad", 17.3850, 78.4867, 1300);
        graph.addHub("HUB_AMD", "Ahmedabad Industrial Hub", "Ahmedabad", 23.0225, 72.5714, 900);
        graph.addHub("HUB_PNQ", "Pune Express Gateway", "Pune", 18.5204, 73.8567, 800);
        graph.addHub("HUB_JAI", "Jaipur North Logistics", "Jaipur", 26.9124, 75.7873, 750);

        for (Hub h : graph.getAllHubs()) {
            DatabaseManager.saveHub(h);
            hubQueues.put(h.getId(), new SortingQueue(h.getId()));
        }

        addAndSaveRoute("HUB_BOM", "HUB_PNQ", 150, 3.0);
        addAndSaveRoute("HUB_BOM", "HUB_AMD", 530, 9.0);
        addAndSaveRoute("HUB_BOM", "HUB_HYD", 710, 12.0);
        addAndSaveRoute("HUB_BOM", "HUB_BLR", 980, 16.0);
        addAndSaveRoute("HUB_DEL", "HUB_JAI", 280, 5.0);
        addAndSaveRoute("HUB_DEL", "HUB_AMD", 940, 15.0);
        addAndSaveRoute("HUB_DEL", "HUB_CCU", 1530, 24.0);
        addAndSaveRoute("HUB_DEL", "HUB_BOM", 1420, 22.0);
        addAndSaveRoute("HUB_BLR", "HUB_HYD", 570, 9.0);
        addAndSaveRoute("HUB_BLR", "HUB_MAA", 350, 6.0);
        addAndSaveRoute("HUB_MAA", "HUB_HYD", 630, 10.0);
        addAndSaveRoute("HUB_HYD", "HUB_CCU", 1490, 23.0);

        // Seed initial sample parcel
        Parcel sample = new Parcel("SLX-90142", "TechCorp Electronics", "Aarav Sharma", 2.5, "HUB_BOM", "HUB_DEL");
        sample.addCheckpoint(new Checkpoint("BOOKED", "HUB_BOM", "Mumbai Central Hub"));
        bst.insert(sample);
        graph.updateHubLoad("HUB_BOM", 1);
        hubQueues.get("HUB_BOM").enqueue(sample);
        DatabaseManager.saveParcel(sample);
        DatabaseManager.saveHub(graph.getHub("HUB_BOM"));
    }

    private static void addAndSaveRoute(String src, String dst, double dist, double hrs) {
        graph.addRoute(src, dst, dist, hrs, true);
        DatabaseManager.saveRoute(src, dst, dist, hrs);
        DatabaseManager.saveRoute(dst, src, dist, hrs);
    }

    private static void listHubs() {
        System.out.println("\n🏙️  LOGISTICS HUBS NETWORK STATUS (XAMPP MySQL Connected):");
        for (Hub h : graph.getAllHubs()) {
            SortingQueue q = hubQueues.get(h.getId());
            System.out.printf("  • %-8s | %-24s | %-12s | Load: %4d/%4d (%5.1f%%) | FIFO Queue: %d pkgs\n",
                    h.getId(), h.getName(), h.getCity(), h.getCurrentLoad(), h.getCapacity(), h.getUtilizationPercent(), q != null ? q.size() : 0);
        }
    }

    private static void queryShortestPath(Scanner scanner) {
        System.out.print("\nEnter Source Hub ID (e.g. HUB_BOM): ");
        String src = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter Destination Hub ID (e.g. HUB_DEL): ");
        String dst = scanner.nextLine().trim().toUpperCase();

        if (graph.getHub(src) == null || graph.getHub(dst) == null) {
            System.out.println("❌ Invalid Hub ID entered.");
            return;
        }

        DijkstraRouter.PathResult result = router.findShortestPath(src, dst);
        if (result.found) {
            String pathStr = result.path.stream().map(Hub::getCity).collect(Collectors.joining(" -> "));
            System.out.println("\n⚡ DIJKSTRA OPTIMAL ROUTE FOUND:");
            System.out.println("   ▶ Optimal Path: " + pathStr);
            System.out.println("   ▶ Total Distance: " + result.totalDistanceKm + " km");
            System.out.println("   ▶ Est. Transit Time: " + result.totalHours + " hrs");
        } else {
            System.out.println("❌ No route available between " + src + " and " + dst);
        }
    }

    private static void bookShipment(Scanner scanner) {
        System.out.println("\n📦 REGISTER NEW EXPRESS SHIPMENT:");
        System.out.print("Sender Name: ");
        String sender = scanner.nextLine().trim();
        System.out.print("Recipient Name: ");
        String receiver = scanner.nextLine().trim();
        System.out.print("Parcel Weight (kg): ");
        double weight = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("Origin Hub ID (e.g. HUB_BOM): ");
        String origin = scanner.nextLine().trim().toUpperCase();
        System.out.print("Destination Hub ID (e.g. HUB_DEL): ");
        String dest = scanner.nextLine().trim().toUpperCase();

        DijkstraRouter.PathResult pathResult = router.findShortestPath(origin, dest);
        if (!pathResult.found) {
            System.out.println("❌ Cannot book: No route exists between specified hubs.");
            return;
        }

        String trackingCode = "SLX-" + (10000 + new Random().nextInt(90000));
        Parcel parcel = new Parcel(trackingCode, sender, receiver, weight, origin, dest);
        parcel.setRoute(pathResult.path.stream().map(Hub::getId).collect(Collectors.toList()));
        parcel.addCheckpoint(new Checkpoint("BOOKED", origin, graph.getHub(origin).getName()));

        bst.insert(parcel);
        graph.updateHubLoad(origin, 1);
        if (hubQueues.containsKey(origin)) hubQueues.get(origin).enqueue(parcel);

        // Save to XAMPP MySQL
        DatabaseManager.saveParcel(parcel);
        DatabaseManager.saveHub(graph.getHub(origin));

        System.out.println("\n✅ SHIPMENT BOOKED & SAVED TO XAMPP MYSQL!");
        System.out.println("   ▶ Tracking Code: " + trackingCode);
        System.out.println("   ▶ Calculated Route: " + pathResult.path.stream().map(Hub::getCity).collect(Collectors.joining(" -> ")));
    }

    private static void trackParcel(Scanner scanner) {
        System.out.print("\nEnter Tracking Code (e.g. SLX-90142): ");
        String code = scanner.nextLine().trim().toUpperCase();

        Parcel parcel = bst.search(code);
        if (parcel == null) {
            System.out.println("❌ Parcel with tracking code '" + code + "' not found in BST/MySQL index.");
            return;
        }

        System.out.println("\n📦 PARCEL TRACKING DETAILS (Loaded from MySQL):");
        System.out.println("   ▶ Tracking Code: " + parcel.getTrackingCode());
        System.out.println("   ▶ Sender: " + parcel.getSender() + " | Recipient: " + parcel.getReceiver());
        System.out.println("   ▶ Weight: " + parcel.getWeightKg() + " kg | Status: " + parcel.getStatus());
        System.out.println("   ▶ Checkpoints History:");
        for (Checkpoint cp : parcel.getCheckpoints()) {
            System.out.println("      • " + cp);
        }
    }

    private static void simulateScanner(Scanner scanner) {
        System.out.print("\nEnter Tracking Code to Scan: ");
        String code = scanner.nextLine().trim().toUpperCase();

        Parcel parcel = bst.search(code);
        if (parcel == null) {
            System.out.println("❌ Parcel '" + code + "' not found.");
            return;
        }

        System.out.print("Enter Scanner Hub ID: ");
        String hubId = scanner.nextLine().trim().toUpperCase();
        Hub hub = graph.getHub(hubId);
        if (hub == null) {
            System.out.println("❌ Invalid Hub ID.");
            return;
        }

        System.out.print("Scan Action (1: ARRIVED_AT_HUB, 2: DISPATCHED): ");
        String action = scanner.nextLine().trim();

        String newStatus = action.equals("1") ? "ARRIVED_AT_HUB" : "IN_TRANSIT";
        parcel.setStatus(newStatus);
        parcel.setCurrentHubId(hubId);
        parcel.addCheckpoint(new Checkpoint(newStatus, hubId, hub.getName()));

        if (action.equals("1")) {
            graph.updateHubLoad(hubId, 1);
            if (hubQueues.containsKey(hubId)) hubQueues.get(hubId).enqueue(parcel);
        } else {
            graph.updateHubLoad(hubId, -1);
            if (hubQueues.containsKey(hubId)) hubQueues.get(hubId).dequeue();
        }

        // Save scan update to XAMPP MySQL
        DatabaseManager.saveParcel(parcel);
        DatabaseManager.saveHub(hub);

        System.out.println("✅ Barcode scan saved to MySQL: Parcel " + code + " -> " + newStatus + " at " + hub.getCity());
    }
}
