package com.swiftlogix.engine;

import com.swiftlogix.database.DatabaseManager;
import com.swiftlogix.model.Checkpoint;
import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NetworkSimulator - Simulates automated end-to-end multi-hop transit, warehouse sorting,
 * and delivery of parcels across the logistics graph.
 */
public class NetworkSimulator {

    public static class SimulationStep {
        public String status;
        public String hubId;
        public String hubName;
        public String description;

        public SimulationStep(String status, String hubId, String hubName, String description) {
            this.status = status;
            this.hubId = hubId;
            this.hubName = hubName;
            this.description = description;
        }
    }

    public static class SimulationResult {
        public boolean success;
        public List<SimulationStep> steps = new ArrayList<>();
        public String message;
    }

    public static SimulationResult simulateEndToEndDelivery(
            Parcel parcel,
            HubGraph graph,
            Map<String, SortingQueue> hubQueues,
            boolean delayAnimation) {

        SimulationResult result = new SimulationResult();

        if (parcel == null) {
            result.success = false;
            result.message = "Parcel is null.";
            return result;
        }

        if (parcel.isDelivered()) {
            result.success = false;
            result.message = "Parcel " + parcel.getTrackingCode() + " is already DELIVERED.";
            return result;
        }

        if (parcel.isCancelled()) {
            result.success = false;
            result.message = "Cannot simulate cancelled parcel " + parcel.getTrackingCode() + ".";
            return result;
        }

        List<String> route = parcel.getRoute();
        if (route == null || route.isEmpty()) {
            // Fallback: If route was empty, compute via Dijkstra
            DijkstraRouter router = new DijkstraRouter(graph);
            DijkstraRouter.PathResult pathRes = router.findShortestPath(parcel.getOriginHubId(), parcel.getDestHubId());
            if (!pathRes.found) {
                result.success = false;
                result.message = "No valid route exists between " + parcel.getOriginHubId() + " and " + parcel.getDestHubId();
                return result;
            }
            List<String> computedRoute = new ArrayList<>();
            for (Hub h : pathRes.path) computedRoute.add(h.getId());
            parcel.setRoute(computedRoute);
            route = computedRoute;
        }

        System.out.println("\n🚀 STARTING AUTOMATED DISPATCH SIMULATION FOR: " + parcel.getTrackingCode());
        System.out.println("   ▶ Priority: " + parcel.getPriority().getDisplayName());
        System.out.println("   ▶ Route: " + String.join(" ➔ ", route));
        System.out.println("   ------------------------------------------------------------");

        // 1. Initial Origin Dispatch if currently BOOKED
        String originId = route.get(0);
        Hub originHub = graph.getHub(originId);

        if ("BOOKED".equalsIgnoreCase(parcel.getStatus())) {
            parcel.setStatus("IN_TRANSIT");
            parcel.setCurrentHubId(originId);
            graph.updateHubLoad(originId, -1);
            if (hubQueues.containsKey(originId)) {
                hubQueues.get(originId).dequeue();
            }

            Checkpoint cp = new Checkpoint("DISPATCHED", originId, originHub != null ? originHub.getName() : originId);
            parcel.addCheckpoint(cp);
            DatabaseManager.saveParcel(parcel);
            if (originHub != null) DatabaseManager.saveHub(originHub);

            String log = "📦 Dispatched from Origin Hub [" + (originHub != null ? originHub.getCity() : originId) + "] ➔ IN_TRANSIT";
            result.steps.add(new SimulationStep("DISPATCHED", originId, originHub != null ? originHub.getName() : originId, log));
            System.out.println("   " + log);
            sleepIf(delayAnimation, 400);
        }

        // 2. Traverse Intermediate Hops
        for (int i = 1; i < route.size() - 1; i++) {
            String hopHubId = route.get(i);
            Hub hopHub = graph.getHub(hopHubId);
            String hubName = hopHub != null ? hopHub.getName() : hopHubId;
            String hubCity = hopHub != null ? hopHub.getCity() : hopHubId;

            // Arrival at intermediate hub
            parcel.setStatus("ARRIVED_AT_HUB");
            parcel.setCurrentHubId(hopHubId);
            graph.updateHubLoad(hopHubId, 1);
            if (hubQueues.containsKey(hopHubId)) {
                hubQueues.get(hopHubId).enqueue(parcel);
            }

            Checkpoint cpArrival = new Checkpoint("ARRIVED_AT_HUB", hopHubId, hubName);
            parcel.addCheckpoint(cpArrival);
            DatabaseManager.saveParcel(parcel);
            if (hopHub != null) DatabaseManager.saveHub(hopHub);

            String logArr = "🏢 Arrived at Transit Hub: " + hubCity + " (" + hopHubId + ") [Sorted in Priority Queue]";
            result.steps.add(new SimulationStep("ARRIVED_AT_HUB", hopHubId, hubName, logArr));
            System.out.println("   " + logArr);
            sleepIf(delayAnimation, 500);

            // Sorting & Dispatch to next leg
            parcel.setStatus("IN_TRANSIT");
            graph.updateHubLoad(hopHubId, -1);
            if (hubQueues.containsKey(hopHubId)) {
                hubQueues.get(hopHubId).dequeue();
            }

            Checkpoint cpDisp = new Checkpoint("DISPATCHED", hopHubId, hubName);
            parcel.addCheckpoint(cpDisp);
            DatabaseManager.saveParcel(parcel);
            if (hopHub != null) DatabaseManager.saveHub(hopHub);

            String logDisp = "🚚 Express Sort Complete. Dispatched from " + hubCity + " ➔ Next Transit Leg";
            result.steps.add(new SimulationStep("DISPATCHED", hopHubId, hubName, logDisp));
            System.out.println("   " + logDisp);
            sleepIf(delayAnimation, 400);
        }

        // 3. Arrive at Destination Hub
        String destId = route.get(route.size() - 1);
        Hub destHub = graph.getHub(destId);
        String destName = destHub != null ? destHub.getName() : destId;
        String destCity = destHub != null ? destHub.getCity() : destId;

        parcel.setStatus("ARRIVED_AT_HUB");
        parcel.setCurrentHubId(destId);
        graph.updateHubLoad(destId, 1);
        if (hubQueues.containsKey(destId)) {
            hubQueues.get(destId).enqueue(parcel);
        }

        Checkpoint cpDestArr = new Checkpoint("ARRIVED_AT_DESTINATION_HUB", destId, destName);
        parcel.addCheckpoint(cpDestArr);
        DatabaseManager.saveParcel(parcel);
        if (destHub != null) DatabaseManager.saveHub(destHub);

        String logDestArr = "🏁 Arrived at Destination Hub: " + destCity + " (" + destId + ")";
        result.steps.add(new SimulationStep("ARRIVED_AT_HUB", destId, destName, logDestArr));
        System.out.println("   " + logDestArr);
        sleepIf(delayAnimation, 500);

        // 4. Out for delivery
        parcel.setStatus("OUT_FOR_DELIVERY");
        Checkpoint cpOfd = new Checkpoint("OUT_FOR_DELIVERY", destId, destName);
        parcel.addCheckpoint(cpOfd);
        DatabaseManager.saveParcel(parcel);

        String logOfd = "🛵 Out for Delivery with Local Courier Agent in " + destCity;
        result.steps.add(new SimulationStep("OUT_FOR_DELIVERY", destId, destName, logOfd));
        System.out.println("   " + logOfd);
        sleepIf(delayAnimation, 500);

        // 5. Final Delivery
        parcel.setStatus("DELIVERED");
        graph.updateHubLoad(destId, -1);
        if (hubQueues.containsKey(destId)) {
            hubQueues.get(destId).dequeue();
        }

        Checkpoint cpDelivered = new Checkpoint("DELIVERED", destId, destName);
        parcel.addCheckpoint(cpDelivered);
        DatabaseManager.saveParcel(parcel);
        if (destHub != null) DatabaseManager.saveHub(destHub);

        String logDel = "🎉 DELIVERED SUCCESSFULLY to Recipient: " + parcel.getReceiver() + "!";
        result.steps.add(new SimulationStep("DELIVERED", destId, destName, logDel));
        System.out.println("   " + logDel);

        result.success = true;
        result.message = "Parcel " + parcel.getTrackingCode() + " delivered successfully across " + (route.size() - 1) + " hops.";
        return result;
    }

    private static void sleepIf(boolean delay, long ms) {
        if (delay) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {}
        }
    }
}
