package com.swiftlogix;

import com.swiftlogix.engine.*;
import com.swiftlogix.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * EngineTest - Comprehensive unit tests for SwiftLogix algorithms, data structures,
 * routing strategies, rate calculator, and dispatch lifecycle.
 */
public class EngineTest {
    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("🧪 Running SwiftLogix Enterprise Engine Unit Tests");
        System.out.println("=========================================================");

        testHubGraphAndDijkstra();
        testMultiCriteriaDijkstra();
        testShippingCostCalculator();
        testPrioritySortingQueue();
        testParcelBST();
        testEndToEndSimulation();
        testShipmentCancellation();

        System.out.println("\n=========================================================");
        System.out.println("🎉 All SwiftLogix Engine Unit Tests Passed Cleanly (7/7)!");
        System.out.println("=========================================================");
    }

    private static void testHubGraphAndDijkstra() {
        HubGraph graph = new HubGraph();
        graph.addHub("A", "Hub A", "Alpha", 10, 10, 500);
        graph.addHub("B", "Hub B", "Beta", 12, 12, 500);
        graph.addHub("C", "Hub C", "Gamma", 15, 15, 500);

        graph.addRoute("A", "C", 500, 10, true);
        graph.addRoute("A", "B", 100, 2, true);
        graph.addRoute("B", "C", 150, 3, true);

        DijkstraRouter router = new DijkstraRouter(graph);
        DijkstraRouter.PathResult result = router.findShortestPath("A", "C");

        assert result.found : "Path should be found";
        assert result.totalDistanceKm == 250.0 : "Shortest distance should be 250.0km (A->B->C)";
        assert result.path.size() == 3 : "Path length should be 3 hubs";
        System.out.println("  ✔ 1. testHubGraphAndDijkstra passed");
    }

    private static void testMultiCriteriaDijkstra() {
        HubGraph graph = new HubGraph();
        graph.addHub("H1", "Hub 1", "City1", 10, 10, 1000);
        graph.addHub("H2", "Hub 2", "City2", 12, 12, 1000);
        graph.addHub("H3", "Hub 3", "City3", 15, 15, 1000);

        // H1 -> H3 direct is long (400km) but fast via highway (4 hrs)
        graph.addRoute("H1", "H3", 400, 4.0, true);
        // H1 -> H2 -> H3 is short (200km) but slower city roads (8 hrs)
        graph.addRoute("H1", "H2", 100, 4.0, true);
        graph.addRoute("H2", "H3", 100, 4.0, true);

        DijkstraRouter router = new DijkstraRouter(graph);

        // Strategy 1: Shortest Distance should choose H1 -> H2 -> H3 (200km)
        DijkstraRouter.PathResult distResult = router.findOptimalRoute("H1", "H3", RoutingStrategy.SHORTEST_DISTANCE);
        assert distResult.totalDistanceKm == 200.0 : "Shortest distance route should be 200km";
        assert distResult.path.size() == 3 : "Should go via intermediate hub H2";

        // Strategy 2: Fastest Time should choose direct H1 -> H3 (4.0 hrs transit)
        DijkstraRouter.PathResult timeResult = router.findOptimalRoute("H1", "H3", RoutingStrategy.FASTEST_TIME);
        assert timeResult.path.size() == 2 : "Fastest time should take direct highway route H1 -> H3";

        System.out.println("  ✔ 2. testMultiCriteriaDijkstra passed");
    }

    private static void testShippingCostCalculator() {
        double weight = 5.0; // 5 kg
        double distance = 1000.0; // 1000 km

        // Base 100 + Weight (5 * 35 = 175) + Distance (1000 * 0.75 = 750) = 1025. Fuel 8% (82) = 1107.0
        double standardCost = ShippingCostCalculator.calculateCost(weight, distance, ParcelPriority.STANDARD);
        double expressCost = ShippingCostCalculator.calculateCost(weight, distance, ParcelPriority.EXPRESS);
        double sameDayCost = ShippingCostCalculator.calculateCost(weight, distance, ParcelPriority.SAME_DAY);

        assert standardCost == 1107.0 : "Standard cost calculation mismatch: " + standardCost;
        assert expressCost == Math.round(1107.0 * 1.5 * 100.0) / 100.0 : "Express multiplier mismatch";
        assert sameDayCost > expressCost && expressCost > standardCost : "Priority cost ordering mismatch";

        ShippingCostCalculator.Quote quote = ShippingCostCalculator.generateQuote(weight, distance, ParcelPriority.EXPRESS);
        assert quote.totalCost == expressCost : "Quote totalCost mismatch";
        System.out.println("  ✔ 3. testShippingCostCalculator passed");
    }

    private static void testPrioritySortingQueue() {
        SortingQueue queue = new SortingQueue("HUB_TEST");

        Parcel pStandard = new Parcel("SLX-STD", "Sender1", "Receiver1", 2.0, "H1", "H2", ParcelPriority.STANDARD);
        Parcel pExpress = new Parcel("SLX-EXP", "Sender2", "Receiver2", 3.0, "H1", "H2", ParcelPriority.EXPRESS);
        Parcel pSameDay = new Parcel("SLX-SMD", "Sender3", "Receiver3", 1.0, "H1", "H2", ParcelPriority.SAME_DAY);

        // Enqueue in reverse order: Standard first, then Express, then Same-Day
        queue.enqueue(pStandard);
        queue.enqueue(pExpress);
        queue.enqueue(pSameDay);

        assert queue.size() == 3 : "Queue size should be 3";

        // Dequeue should return SAME_DAY first, then EXPRESS, then STANDARD
        Parcel firstOut = queue.dequeue();
        assert firstOut.getTrackingCode().equals("SLX-SMD") : "Same-Day package must be dequeued first";

        Parcel secondOut = queue.dequeue();
        assert secondOut.getTrackingCode().equals("SLX-EXP") : "Express package must be dequeued second";

        Parcel thirdOut = queue.dequeue();
        assert thirdOut.getTrackingCode().equals("SLX-STD") : "Standard package must be dequeued third";

        assert queue.isEmpty() : "Queue should now be empty";
        System.out.println("  ✔ 4. testPrioritySortingQueue passed");
    }

    private static void testParcelBST() {
        ParcelBST bst = new ParcelBST();
        bst.insert(new Parcel("SLX-50000", "Alice", "Bob", 1.0, "A", "B"));
        bst.insert(new Parcel("SLX-20000", "Charlie", "David", 2.0, "A", "B"));
        bst.insert(new Parcel("SLX-80000", "Eve", "Frank", 3.0, "A", "B"));

        Parcel found = bst.search("SLX-20000");
        assert found != null && found.getSender().equals("Charlie") : "BST search should find Charlie";

        Parcel notFound = bst.search("SLX-99999");
        assert notFound == null : "BST search should return null for missing code";

        assert bst.getSize() == 3 : "BST size should be 3";
        assert bst.getAllInOrder().size() == 3 : "In-order traversal should return 3 elements";
        System.out.println("  ✔ 5. testParcelBST passed");
    }

    private static void testEndToEndSimulation() {
        HubGraph graph = new HubGraph();
        graph.addHub("HUB_1", "Hub 1", "City 1", 10, 10, 500);
        graph.addHub("HUB_2", "Hub 2", "City 2", 12, 12, 500);
        graph.addHub("HUB_3", "Hub 3", "City 3", 14, 14, 500);

        graph.addRoute("HUB_1", "HUB_2", 100, 2, true);
        graph.addRoute("HUB_2", "HUB_3", 150, 3, true);

        Map<String, SortingQueue> queues = new HashMap<>();
        queues.put("HUB_1", new SortingQueue("HUB_1"));
        queues.put("HUB_2", new SortingQueue("HUB_2"));
        queues.put("HUB_3", new SortingQueue("HUB_3"));

        Parcel parcel = new Parcel("SLX-TEST-SIM", "Sender", "Receiver", 2.0, "HUB_1", "HUB_3", ParcelPriority.EXPRESS);
        parcel.addCheckpoint(new Checkpoint("BOOKED", "HUB_1", "Hub 1"));
        graph.updateHubLoad("HUB_1", 1);
        queues.get("HUB_1").enqueue(parcel);

        NetworkSimulator.SimulationResult simResult = NetworkSimulator.simulateEndToEndDelivery(parcel, graph, queues, false);

        assert simResult.success : "Simulation should succeed";
        assert parcel.isDelivered() : "Parcel status should be DELIVERED";
        assert parcel.getStatus().equals("DELIVERED") : "Status string must be DELIVERED";
        assert parcel.getCheckpoints().size() >= 4 : "Checkpoints history should record all transit events";
        System.out.println("  ✔ 6. testEndToEndSimulation passed");
    }

    private static void testShipmentCancellation() {
        Parcel parcel = new Parcel("SLX-CANCEL-TEST", "Sender", "Receiver", 1.5, "HUB_A", "HUB_B");
        assert parcel.canBeCancelled() : "New parcel should be cancellable";

        parcel.setStatus("CANCELLED");
        assert parcel.isCancelled() : "Parcel should report isCancelled == true";
        assert !parcel.canBeCancelled() : "Cancelled parcel cannot be cancelled again";
        System.out.println("  ✔ 7. testShipmentCancellation passed");
    }
}
