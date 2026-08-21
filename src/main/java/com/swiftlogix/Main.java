package com.swiftlogix;

import com.swiftlogix.database.DatabaseManager;
import com.swiftlogix.engine.*;
import com.swiftlogix.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main - Interactive Command-Line Interface (CLI) for SwiftLogix Logistics Engine.
 */
public class Main {
    private static HubGraph graph = new HubGraph();
    private static DijkstraRouter router;
    private static ParcelBST bst = new ParcelBST();
    private static Map<String, SortingQueue> hubQueues = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("🚚  SwiftLogix: Enterprise Logistics, Routing & Express Freight Engine");
        System.out.println("========================================================================");

        // 1. Initialize XAMPP MySQL Database Tables
        DatabaseManager.initializeDatabase();

        // 2. Load existing network and parcels from MySQL (or in-memory)
        DatabaseManager.loadNetworkAndParcels(graph, bst, hubQueues);

        // 3. If database is empty, seed initial network
        if (graph.getAllHubs().isEmpty()) {
            initSeedNetworkData();
        }

        router = new DijkstraRouter(graph);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMainMenu();
            System.out.print("Select Option (1-9): ");

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    listHubs();
                    break;
                case "2":
                    queryOptimalRouteAndRate(scanner);
                    break;
                case "3":
                    bookShipment(scanner);
                    break;
                case "4":
                    trackParcel(scanner);
                    break;
                case "5":
                    simulateSingleScan(scanner);
                    break;
                case "6":
                    simulateEndToEndJourney(scanner);
                    break;
                case "7":
                    cancelShipment(scanner);
                    break;
                case "8":
                    manageNetworkMenu(scanner);
                    break;
                case "9":
                    launchWebUI();
                    break;
                case "10":
                    System.out.println("\n👋 Thank you for using SwiftLogix Logistics Engine. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("❌ Invalid option. Please enter a number from 1 to 10.");
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("\n=========================== MAIN MENU ===========================");
        System.out.println("  1. 🏙️  Logistics Hubs & Real-Time Capacity Status");
        System.out.println("  2. 🧮 Multi-Criteria Dijkstra Route & Rate Calculator");
        System.out.println("  3. 📦 Book Express Shipment (Priority Tiers + Cost Quote + ETA)");
        System.out.println("  4. 🔍 Track Parcel (Journey Map, Progress Bar, Checkpoints)");
        System.out.println("  5. 🏭 Manual Warehouse Barcode Scanner Simulation");
        System.out.println("  6. 🚀 Automated End-to-End Route Dispatch Simulation");
        System.out.println("  7. ❌ Cancel Active Shipment");
        System.out.println("  8. ⚙️  Logistics Network Management & Bottleneck Analytics");
        System.out.println("  9. 🌐 Launch Interactive Web UI Dashboard (http://localhost:8080)");
        System.out.println("  10. 🚪 Exit Terminal");
        System.out.println("=================================================================");
    }

    private static void launchWebUI() {
        System.out.println("\n🌐 Starting SwiftLogix Web Server & Launching Dashboard...");
        try {
            // Start server in thread if not already running
            new Thread(() -> {
                try {
                    com.swiftlogix.web.SwiftLogixWebServer.main(new String[]{});
                } catch (Exception ignored) {}
            }).start();

            Thread.sleep(800);

            // Open browser
            String url = "http://localhost:8080";
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
            System.out.println("✅ Web Dashboard is LIVE at: " + url);
            System.out.println("   ▶ Opened in your default browser!");
        } catch (Exception e) {
            System.out.println("ℹ️ Web Server active at http://localhost:8080 (open in your browser)");
        }
    }

    private static void initSeedNetworkData() {
        System.out.println("🌱 Seeding initial logistics network data...");

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

        // Seed Sample Parcels
        seedParcel("SLX-90142", "TechCorp Electronics", "Aarav Sharma", 2.5, "HUB_BOM", "HUB_DEL", 
                ParcelPriority.EXPRESS, "BOOKED", Arrays.asList("HUB_BOM", "HUB_DEL"), "2026-08-23 16:00",
                Collections.singletonList(new Checkpoint("BOOKED", "HUB_BOM", "Consignment registered at Mumbai Central Hub")));

        seedParcel("SLX-84321", "Apollo Healthcare", "Dr. Ananya Iyer", 1.2, "HUB_BLR", "HUB_MAA", 
                ParcelPriority.SAME_DAY, "IN_TRANSIT", Arrays.asList("HUB_BLR", "HUB_MAA"), "2026-08-21 20:30",
                Arrays.asList(
                    new Checkpoint("BOOKED", "HUB_BLR", "Intake scan at Bengaluru Tech Hub"),
                    new Checkpoint("IN_TRANSIT", "HUB_BLR", "Dispatched on High-Speed Highway Corridor")
                ));

        seedParcel("SLX-77290", "Gujarat Textile Mills", "Subhash Chandra", 8.5, "HUB_AMD", "HUB_CCU", 
                ParcelPriority.STANDARD, "ARRIVED_AT_HUB", Arrays.asList("HUB_AMD", "HUB_BOM", "HUB_HYD", "HUB_CCU"), "2026-08-24 18:00",
                Arrays.asList(
                    new Checkpoint("BOOKED", "HUB_AMD", "Package received at Ahmedabad Industrial Hub"),
                    new Checkpoint("IN_TRANSIT", "HUB_BOM", "Transited through Mumbai Central Hub"),
                    new Checkpoint("ARRIVED_AT_HUB", "HUB_HYD", "Sorting & Staging at Hyderabad Logistics Hub")
                ));

        seedParcel("SLX-65104", "Bharat Auto Components", "Vikram Singh Rathore", 4.0, "HUB_PNQ", "HUB_JAI", 
                ParcelPriority.EXPRESS, "OUT_FOR_DELIVERY", Arrays.asList("HUB_PNQ", "HUB_BOM", "HUB_DEL", "HUB_JAI"), "2026-08-21 17:45",
                Arrays.asList(
                    new Checkpoint("BOOKED", "HUB_PNQ", "Received at Pune Express Gateway"),
                    new Checkpoint("IN_TRANSIT", "HUB_DEL", "Transited through Delhi NCR Mega Hub"),
                    new Checkpoint("ARRIVED_AT_HUB", "HUB_JAI", "Received at Jaipur North Logistics Hub"),
                    new Checkpoint("OUT_FOR_DELIVERY", "HUB_JAI", "Out for Delivery with Local Courier Agent (Van RJ-14-EA-2091)")
                ));

        seedParcel("SLX-52019", "Nexus Cloud Devices", "Pooja Verma", 3.2, "HUB_MAA", "HUB_HYD", 
                ParcelPriority.SAME_DAY, "DELIVERED", Arrays.asList("HUB_MAA", "HUB_HYD"), "2026-08-21 11:30",
                Arrays.asList(
                    new Checkpoint("BOOKED", "HUB_MAA", "Booked at Chennai Port Hub"),
                    new Checkpoint("IN_TRANSIT", "HUB_MAA", "Dispatched on Express Corridor"),
                    new Checkpoint("ARRIVED_AT_HUB", "HUB_HYD", "Arrived at Hyderabad Logistics Hub"),
                    new Checkpoint("OUT_FOR_DELIVERY", "HUB_HYD", "Out for Delivery in Hitec City"),
                    new Checkpoint("DELIVERED", "HUB_HYD", "Delivered to Recipient (Signed & OTP Verified)")
                ));
    }

    private static void seedParcel(String code, String sender, String receiver, double weight, String src, String dst,
                                   ParcelPriority priority, String status, List<String> route, String eta, List<Checkpoint> checkpoints) {
        Parcel p = new Parcel(code, sender, receiver, weight, src, dst, priority);
        p.setStatus(status);
        p.setRoute(route);
        p.setEstimatedDelivery(eta);
        p.setCost(ShippingCostCalculator.calculateCost(weight, 1000, priority));
        for (Checkpoint cp : checkpoints) {
            p.addCheckpoint(cp);
        }
        bst.insert(p);
        graph.updateHubLoad(src, 1);
        if (hubQueues.containsKey(src) && !"DELIVERED".equals(status)) {
            hubQueues.get(src).enqueue(p);
        }
        DatabaseManager.saveParcel(p);
    }

    private static void addAndSaveRoute(String src, String dst, double dist, double hrs) {
        graph.addRoute(src, dst, dist, hrs, true);
        DatabaseManager.saveRoute(src, dst, dist, hrs, true);
        DatabaseManager.saveRoute(dst, src, dist, hrs, true);
    }

    private static void listHubs() {
        System.out.println("\n🏙️  LOGISTICS HUBS NETWORK STATUS:");
        System.out.printf("  %-9s | %-24s | %-12s | %-16s | %-14s | %s\n",
                "HUB ID", "NAME", "CITY", "CAPACITY / LOAD", "PRIORITY QUEUE", "STATUS");
        System.out.println("  -----------------------------------------------------------------------------------------------");

        for (Hub h : graph.getAllHubs()) {
            SortingQueue q = hubQueues.get(h.getId());
            int qSize = (q != null) ? q.size() : 0;
            String statusTag = h.isOverloaded() ? "⚠️ CONGESTED" : "✅ NORMAL";
            System.out.printf("  %-9s | %-24s | %-12s | %4d/%4d (%5.1f%%) | %3d parcels   | %s\n",
                    h.getId(), h.getName(), h.getCity(), h.getCurrentLoad(), h.getCapacity(),
                    h.getUtilizationPercent(), qSize, statusTag);
        }
    }

    private static void queryOptimalRouteAndRate(Scanner scanner) {
        System.out.print("\nEnter Source Hub ID (e.g. HUB_BOM): ");
        String src = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter Destination Hub ID (e.g. HUB_DEL): ");
        String dst = scanner.nextLine().trim().toUpperCase();

        if (graph.getHub(src) == null || graph.getHub(dst) == null) {
            System.out.println("❌ Invalid Hub ID entered.");
            return;
        }

        System.out.println("\nSelect Routing Optimization Strategy:");
        System.out.println("  1. Shortest Distance (km)");
        System.out.println("  2. Fastest Transit Time (hrs)");
        System.out.println("  3. Congestion & Hub Load Avoidance");
        System.out.print("Choose Strategy (1-3, default 1): ");
        String stratChoice = scanner.nextLine().trim();

        RoutingStrategy strategy = RoutingStrategy.SHORTEST_DISTANCE;
        if ("2".equals(stratChoice)) strategy = RoutingStrategy.FASTEST_TIME;
        else if ("3".equals(stratChoice)) strategy = RoutingStrategy.LEAST_CONGESTED;

        DijkstraRouter.PathResult result = router.findOptimalRoute(src, dst, strategy);

        if (!result.found) {
            System.out.println("❌ No route available between " + src + " and " + dst + " (Check for blocked routes).");
            return;
        }

        String pathStr = result.path.stream().map(Hub::getCity).collect(Collectors.joining(" ➔ "));
        System.out.println("\n⚡ DIJKSTRA OPTIMAL ROUTE FOUND (" + strategy.getDescription() + "):");
        System.out.println("   ▶ Optimal Path:       " + pathStr);
        System.out.println("   ▶ Total Distance:     " + result.totalDistanceKm + " km");
        System.out.println("   ▶ Total Transit Time: " + result.totalHours + " hrs (including handling buffers)");
        System.out.println("   ▶ Expected ETA:       " + result.etaString);
        System.out.println("   ▶ Route Hops:         " + result.getHops() + " transit leg(s)");

        System.out.print("\nEnter parcel weight (kg) to view rate quotes (or press Enter to skip): ");
        String weightInput = scanner.nextLine().trim();
        if (!weightInput.isEmpty()) {
            try {
                double w = Double.parseDouble(weightInput);
                System.out.println("\n" + ShippingCostCalculator.generateQuote(w, result.totalDistanceKm, ParcelPriority.STANDARD).getFormattedBreakdown());
                System.out.println("\n" + ShippingCostCalculator.generateQuote(w, result.totalDistanceKm, ParcelPriority.EXPRESS).getFormattedBreakdown());
                System.out.println("\n" + ShippingCostCalculator.generateQuote(w, result.totalDistanceKm, ParcelPriority.SAME_DAY).getFormattedBreakdown());
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid weight format.");
            }
        }
    }

    private static void bookShipment(Scanner scanner) {
        System.out.println("\n📦 REGISTER NEW EXPRESS SHIPMENT:");
        System.out.print("Sender Name / Business: ");
        String sender = scanner.nextLine().trim();
        if (sender.isEmpty()) sender = "Standard Merchant";

        System.out.print("Recipient Name: ");
        String receiver = scanner.nextLine().trim();
        if (receiver.isEmpty()) receiver = "Consignee";

        System.out.print("Parcel Weight (kg): ");
        double weight = 1.0;
        try {
            weight = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Invalid weight, defaulting to 1.0 kg");
        }

        System.out.print("Origin Hub ID (e.g. HUB_BOM): ");
        String origin = scanner.nextLine().trim().toUpperCase();
        System.out.print("Destination Hub ID (e.g. HUB_DEL): ");
        String dest = scanner.nextLine().trim().toUpperCase();

        if (graph.getHub(origin) == null || graph.getHub(dest) == null) {
            System.out.println("❌ Invalid Hub ID entered.");
            return;
        }

        System.out.println("\nSelect Shipment Priority Tier:");
        System.out.println("  1. Standard Delivery (1.0x Base Rate)");
        System.out.println("  2. Express Delivery  (1.5x Base Rate)");
        System.out.println("  3. Same-Day Priority (2.2x Base Rate)");
        System.out.print("Choice (1-3, default 2): ");
        String pChoice = scanner.nextLine().trim();

        ParcelPriority priority = ParcelPriority.EXPRESS;
        if ("1".equals(pChoice)) priority = ParcelPriority.STANDARD;
        else if ("3".equals(pChoice)) priority = ParcelPriority.SAME_DAY;

        DijkstraRouter.PathResult pathResult = router.findOptimalRoute(origin, dest, RoutingStrategy.SHORTEST_DISTANCE);
        if (!pathResult.found) {
            System.out.println("❌ Cannot book: No route exists between specified hubs.");
            return;
        }

        ShippingCostCalculator.Quote quote = ShippingCostCalculator.generateQuote(weight, pathResult.totalDistanceKm, priority);

        String trackingCode = "SLX-" + (10000 + new Random().nextInt(90000));
        Parcel parcel = new Parcel(trackingCode, sender, receiver, weight, origin, dest, priority);
        parcel.setCost(quote.totalCost);
        parcel.setEstimatedDelivery(pathResult.etaString);
        parcel.setRoute(pathResult.path.stream().map(Hub::getId).collect(Collectors.toList()));
        parcel.addCheckpoint(new Checkpoint("BOOKED", origin, graph.getHub(origin).getName()));

        bst.insert(parcel);
        graph.updateHubLoad(origin, 1);
        if (hubQueues.containsKey(origin)) {
            hubQueues.get(origin).enqueue(parcel);
        }

        DatabaseManager.saveParcel(parcel);
        DatabaseManager.saveHub(graph.getHub(origin));

        System.out.println("\n✅ SHIPMENT SUCCESSFULLY BOOKED!");
        System.out.println("   ▶ Tracking Code:      " + trackingCode);
        System.out.println("   ▶ Priority:           " + priority.getDisplayName());
        System.out.println("   ▶ Total Shipping Fee: ₹" + String.format("%.2f", quote.totalCost));
        System.out.println("   ▶ Estimated Delivery: " + pathResult.etaString);
        System.out.println("   ▶ Calculated Route:   " + pathResult.path.stream().map(Hub::getCity).collect(Collectors.joining(" ➔ ")));
    }

    private static void trackParcel(Scanner scanner) {
        System.out.print("\nEnter Tracking Code (e.g. SLX-90142): ");
        String code = scanner.nextLine().trim().toUpperCase();

        Parcel parcel = bst.search(code);
        if (parcel == null) {
            System.out.println("❌ Parcel with tracking code '" + code + "' not found.");
            return;
        }

        System.out.println("\n======================== PARCEL TRACKING ========================");
        System.out.println("   ▶ Tracking Code:      " + parcel.getTrackingCode());
        System.out.println("   ▶ Priority:           " + parcel.getPriority().getDisplayName());
        System.out.println("   ▶ Status:             " + formatStatusWithEmoji(parcel.getStatus()));
        System.out.println("   ▶ Origin ➔ Dest:      " + parcel.getOriginHubId() + " ➔ " + parcel.getDestHubId());
        System.out.println("   ▶ Sender / Recipient: " + parcel.getSender() + " ➔ " + parcel.getReceiver());
        System.out.println("   ▶ Weight / Cost:      " + parcel.getWeightKg() + " kg | ₹" + String.format("%.2f", parcel.getCost()));
        System.out.println("   ▶ Est. Delivery ETA:  " + parcel.getEstimatedDelivery());

        if (parcel.getRoute() != null && !parcel.getRoute().isEmpty()) {
            System.out.println("   ▶ Planned Route:      " + String.join(" ➔ ", parcel.getRoute()));
        }

        System.out.println("\n   ▶ Journey Progress:");
        printProgressBar(parcel.getStatus());

        System.out.println("\n   ▶ Tracking Checkpoints History (" + parcel.getCheckpoints().size() + " events):");
        for (Checkpoint cp : parcel.getCheckpoints()) {
            System.out.println("      • " + cp);
        }
        System.out.println("=================================================================");
    }

    private static String formatStatusWithEmoji(String status) {
        if (status == null) return "UNKNOWN";
        switch (status.toUpperCase()) {
            case "BOOKED": return "📝 BOOKED (Awaiting Dispatch)";
            case "IN_TRANSIT": return "🚚 IN TRANSIT";
            case "ARRIVED_AT_HUB":
            case "ARRIVED_AT_DESTINATION_HUB": return "🏢 AT WAREHOUSE HUB";
            case "OUT_FOR_DELIVERY": return "🛵 OUT FOR DELIVERY";
            case "DELIVERED": return "🎉 DELIVERED";
            case "CANCELLED": return "❌ CANCELLED";
            default: return status;
        }
    }

    private static void printProgressBar(String status) {
        if ("CANCELLED".equalsIgnoreCase(status)) {
            System.out.println("      [❌ CANCELLED - SHIPMENT VOIDED]");
            return;
        }

        int percent = 10;
        if ("IN_TRANSIT".equalsIgnoreCase(status)) percent = 45;
        else if ("ARRIVED_AT_HUB".equalsIgnoreCase(status)) percent = 65;
        else if ("OUT_FOR_DELIVERY".equalsIgnoreCase(status)) percent = 85;
        else if ("DELIVERED".equalsIgnoreCase(status)) percent = 100;

        int filled = percent / 5;
        StringBuilder sb = new StringBuilder("      [");
        for (int i = 0; i < 20; i++) {
            if (i < filled) sb.append("█");
            else sb.append("-");
        }
        sb.append("] ").append(percent).append("%");
        System.out.println(sb.toString());
    }

    private static void simulateSingleScan(Scanner scanner) {
        System.out.print("\nEnter Tracking Code to Scan: ");
        String code = scanner.nextLine().trim().toUpperCase();

        Parcel parcel = bst.search(code);
        if (parcel == null) {
            System.out.println("❌ Parcel '" + code + "' not found.");
            return;
        }

        if (parcel.isDelivered() || parcel.isCancelled()) {
            System.out.println("❌ Parcel is already " + parcel.getStatus() + " and cannot be scanned.");
            return;
        }

        System.out.print("Enter Scanner Hub ID: ");
        String hubId = scanner.nextLine().trim().toUpperCase();
        Hub hub = graph.getHub(hubId);
        if (hub == null) {
            System.out.println("❌ Invalid Hub ID.");
            return;
        }

        System.out.println("Select Scan Event:");
        System.out.println("  1. ARRIVED_AT_HUB (Inbound Intake)");
        System.out.println("  2. DISPATCHED (Outbound Sort)");
        System.out.println("  3. OUT_FOR_DELIVERY (Courier Dispatch)");
        System.out.println("  4. DELIVERED (Final Handover)");
        System.out.print("Select (1-4): ");
        String action = scanner.nextLine().trim();

        String newStatus;
        switch (action) {
            case "1":
                newStatus = "ARRIVED_AT_HUB";
                graph.updateHubLoad(hubId, 1);
                if (hubQueues.containsKey(hubId)) hubQueues.get(hubId).enqueue(parcel);
                break;
            case "2":
                newStatus = "IN_TRANSIT";
                graph.updateHubLoad(hubId, -1);
                if (hubQueues.containsKey(hubId)) hubQueues.get(hubId).dequeue();
                break;
            case "3":
                newStatus = "OUT_FOR_DELIVERY";
                break;
            case "4":
                newStatus = "DELIVERED";
                graph.updateHubLoad(hubId, -1);
                if (hubQueues.containsKey(hubId)) hubQueues.get(hubId).dequeue();
                break;
            default:
                System.out.println("❌ Invalid scan option.");
                return;
        }

        parcel.setStatus(newStatus);
        parcel.setCurrentHubId(hubId);
        parcel.addCheckpoint(new Checkpoint(newStatus, hubId, hub.getName()));

        DatabaseManager.saveParcel(parcel);
        DatabaseManager.saveHub(hub);

        System.out.println("✅ Barcode scan saved: Parcel " + code + " ➔ " + newStatus + " at " + hub.getCity());
    }

    private static void simulateEndToEndJourney(Scanner scanner) {
        System.out.print("\nEnter Tracking Code to Simulate: ");
        String code = scanner.nextLine().trim().toUpperCase();

        Parcel parcel = bst.search(code);
        if (parcel == null) {
            System.out.println("❌ Parcel '" + code + "' not found.");
            return;
        }

        NetworkSimulator.simulateEndToEndDelivery(parcel, graph, hubQueues, true);
    }

    private static void cancelShipment(Scanner scanner) {
        System.out.print("\nEnter Tracking Code to Cancel: ");
        String code = scanner.nextLine().trim().toUpperCase();

        Parcel parcel = bst.search(code);
        if (parcel == null) {
            System.out.println("❌ Parcel '" + code + "' not found.");
            return;
        }

        if (!parcel.canBeCancelled()) {
            System.out.println("❌ Shipment cannot be cancelled because its status is: " + parcel.getStatus());
            return;
        }

        System.out.print("Confirm cancellation of shipment " + code + "? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!"yes".equalsIgnoreCase(confirm) && !"y".equalsIgnoreCase(confirm)) {
            System.out.println("Cancellation aborted.");
            return;
        }

        // Release current hub load
        if (parcel.getCurrentHubId() != null) {
            graph.updateHubLoad(parcel.getCurrentHubId(), -1);
            Hub hub = graph.getHub(parcel.getCurrentHubId());
            if (hub != null) DatabaseManager.saveHub(hub);
        }

        parcel.setStatus("CANCELLED");
        parcel.addCheckpoint(new Checkpoint("CANCELLED", parcel.getCurrentHubId(), "User/Admin Cancellation"));
        DatabaseManager.saveParcel(parcel);

        System.out.println("✅ Shipment " + code + " has been CANCELLED and hub capacity freed.");
    }

    private static void manageNetworkMenu(Scanner scanner) {
        System.out.println("\n⚙️  LOGISTICS NETWORK MANAGEMENT & ANALYTICS:");
        System.out.println("  1. ➕ Add New Logistics Hub");
        System.out.println("  2. 🔗 Add New Highway Route");
        System.out.println("  3. 🚧 Toggle Route Availability (Block / Open Road)");
        System.out.println("  4. 📊 Network Bottlenecks & Capacity Analytics");
        System.out.print("Select (1-4): ");
        String opt = scanner.nextLine().trim();

        switch (opt) {
            case "1":
                addNewHub(scanner);
                break;
            case "2":
                addNewRoute(scanner);
                break;
            case "3":
                toggleRoute(scanner);
                break;
            case "4":
                showBottlenecksAndAnalytics();
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private static void addNewHub(Scanner scanner) {
        System.out.print("Enter Hub ID (e.g. HUB_COK): ");
        String id = scanner.nextLine().trim().toUpperCase();
        if (graph.getHub(id) != null) {
            System.out.println("❌ Hub ID already exists.");
            return;
        }

        System.out.print("Enter Full Hub Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter City Name: ");
        String city = scanner.nextLine().trim();
        System.out.print("Enter Latitude: ");
        double lat = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Enter Longitude: ");
        double lng = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Enter Capacity (e.g. 1000): ");
        int cap = Integer.parseInt(scanner.nextLine().trim());

        graph.addHub(id, name, city, lat, lng, cap);
        Hub hub = graph.getHub(id);
        hubQueues.put(id, new SortingQueue(id));
        DatabaseManager.saveHub(hub);

        System.out.println("✅ New Hub " + id + " (" + city + ") added and persisted successfully!");
    }

    private static void addNewRoute(Scanner scanner) {
        System.out.print("Enter Source Hub ID: ");
        String src = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter Target Hub ID: ");
        String dst = scanner.nextLine().trim().toUpperCase();

        if (graph.getHub(src) == null || graph.getHub(dst) == null) {
            System.out.println("❌ Invalid Source or Target Hub.");
            return;
        }

        System.out.print("Enter Distance in km: ");
        double dist = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Enter Transit Time in hours (0 for auto): ");
        double hrs = Double.parseDouble(scanner.nextLine().trim());

        addAndSaveRoute(src, dst, dist, hrs);
        System.out.println("✅ Route " + src + " <--> " + dst + " (" + dist + " km) added and persisted!");
    }

    private static void toggleRoute(Scanner scanner) {
        System.out.print("Enter Source Hub ID: ");
        String src = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter Target Hub ID: ");
        String dst = scanner.nextLine().trim().toUpperCase();

        List<Route> routes = graph.getNeighbors(src);
        Route targetRoute = null;
        for (Route r : routes) {
            if (r.getTargetHubId().equals(dst)) {
                targetRoute = r;
                break;
            }
        }

        if (targetRoute == null) {
            System.out.println("❌ Route does not exist between " + src + " and " + dst);
            return;
        }

        boolean newStatus = !targetRoute.isActive();
        targetRoute.setActive(newStatus);
        // Also toggle reverse direction
        for (Route rev : graph.getNeighbors(dst)) {
            if (rev.getTargetHubId().equals(src)) rev.setActive(newStatus);
        }

        DatabaseManager.updateRouteStatus(src, dst, newStatus);
        System.out.println("✅ Route " + src + " <--> " + dst + " status changed to: " + (newStatus ? "🟢 ACTIVE (OPEN)" : "🔴 BLOCKED / MAINTENANCE"));
    }

    private static void showBottlenecksAndAnalytics() {
        System.out.println("\n📊 LOGISTICS NETWORK BOTTLENECKS & ANALYTICS:");
        List<Hub> hubs = new ArrayList<>(graph.getAllHubs());
        hubs.sort((a, b) -> Double.compare(b.getUtilizationPercent(), a.getUtilizationPercent()));

        int congestedCount = 0;
        int totalCapacity = 0;
        int totalLoad = 0;

        for (Hub h : hubs) {
            totalCapacity += h.getCapacity();
            totalLoad += h.getCurrentLoad();
            if (h.isOverloaded()) congestedCount++;
        }

        double networkUtil = totalCapacity > 0 ? ((double) totalLoad / totalCapacity) * 100.0 : 0.0;
        System.out.printf("   ▶ Total Hubs in Network:   %d\n", hubs.size());
        System.out.printf("   ▶ Total Network Load:      %d / %d (%.1f%% overall utilization)\n", totalLoad, totalCapacity, networkUtil);
        System.out.printf("   ▶ Congested Hubs (>85%%):   %d\n", congestedCount);
        System.out.printf("   ▶ Total Tracked Shipments: %d in BST index\n", bst.getSize());

        System.out.println("\n   Hub Utilization Leaderboard:");
        for (Hub h : hubs) {
            System.out.printf("      • %-8s | %-15s | Load: %4d/%4d (%5.1f%%) %s\n",
                    h.getId(), h.getCity(), h.getCurrentLoad(), h.getCapacity(),
                    h.getUtilizationPercent(), h.isOverloaded() ? "⚠️ CONGESTED" : "✅");
        }
    }
}
