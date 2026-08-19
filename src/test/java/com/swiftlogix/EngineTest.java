package com.swiftlogix;

import com.swiftlogix.engine.DijkstraRouter;
import com.swiftlogix.engine.HubGraph;
import com.swiftlogix.engine.ParcelBST;
import com.swiftlogix.engine.SortingQueue;
import com.swiftlogix.model.Parcel;

public class EngineTest {
    public static void main(String[] args) {
        System.out.println("🧪 Running SwiftLogix Java Engine Unit Tests...");

        testHubGraphAndDijkstra();
        testParcelBST();
        testSortingQueue();

        System.out.println("✅ All Java Engine Unit Tests Passed Cleanly (3/3)!");
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
        assert result.totalDistanceKm == 250.0 : "Shortest distance should be 250.0km";
        assert result.path.size() == 3 : "Path length should be 3 hubs";
        System.out.println("  ✔ testHubGraphAndDijkstra passed");
    }

    private static void testParcelBST() {
        ParcelBST bst = new ParcelBST();
        bst.insert(new Parcel("SLX-50000", "Alice", "Bob", 1.0, "A", "B"));
        bst.insert(new Parcel("SLX-20000", "Charlie", "David", 2.0, "A", "B"));

        Parcel found = bst.search("SLX-20000");
        assert found != null && found.getSender().equals("Charlie") : "BST search should return Charlie";

        Parcel notFound = bst.search("SLX-99999");
        assert notFound == null : "BST search should return null for missing tracking code";
        System.out.println("  ✔ testParcelBST passed");
    }

    private static void testSortingQueue() {
        SortingQueue queue = new SortingQueue("HUB_A");
        queue.enqueue(new Parcel("P1", "S1", "R1", 1.0, "A", "B"));
        queue.enqueue(new Parcel("P2", "S2", "R2", 2.0, "A", "B"));

        assert queue.size() == 2 : "Queue size should be 2";
        Parcel first = queue.dequeue();
        assert first.getTrackingCode().equals("P1") : "First dequeued should be P1";
        assert queue.size() == 1 : "Remaining size should be 1";
        System.out.println("  ✔ testSortingQueue passed");
    }
}
