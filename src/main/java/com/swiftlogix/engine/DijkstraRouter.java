package com.swiftlogix.engine;

import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Route;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DijkstraRouter - Multi-criteria optimal route calculation between network hubs.
 */
public class DijkstraRouter {
    private HubGraph graph;
    private static final double HUB_HANDLING_HOURS_PER_HOP = 1.5; // Handling buffer per intermediate hub

    public static class PathResult {
        public boolean found;
        public List<Hub> path;
        public double totalDistanceKm;
        public double totalHours;
        public String etaString;
        public RoutingStrategy strategy;

        public PathResult(boolean found, List<Hub> path, double totalDistanceKm, double totalHours, String etaString, RoutingStrategy strategy) {
            this.found = found;
            this.path = path;
            this.totalDistanceKm = totalDistanceKm;
            this.totalHours = totalHours;
            this.etaString = etaString;
            this.strategy = strategy;
        }

        public int getHops() {
            return Math.max(0, path.size() - 1);
        }
    }

    public DijkstraRouter(HubGraph graph) {
        this.graph = graph;
    }

    public PathResult findShortestPath(String startHubId, String endHubId) {
        return findOptimalRoute(startHubId, endHubId, RoutingStrategy.SHORTEST_DISTANCE);
    }

    public PathResult findOptimalRoute(String startHubId, String endHubId, RoutingStrategy strategy) {
        if (strategy == null) strategy = RoutingStrategy.SHORTEST_DISTANCE;

        Map<String, Double> weights = new HashMap<>();
        Map<String, Double> actualDistances = new HashMap<>();
        Map<String, Double> actualTimes = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> unvisited = new HashSet<>();

        for (String hubId : graph.getHubIds()) {
            weights.put(hubId, Double.POSITIVE_INFINITY);
            actualDistances.put(hubId, 0.0);
            actualTimes.put(hubId, 0.0);
            previous.put(hubId, null);
            unvisited.add(hubId);
        }

        weights.put(startHubId, 0.0);

        while (!unvisited.isEmpty()) {
            String currentHubId = null;
            double minWeight = Double.POSITIVE_INFINITY;

            for (String hubId : unvisited) {
                if (weights.get(hubId) < minWeight) {
                    minWeight = weights.get(hubId);
                    currentHubId = hubId;
                }
            }

            if (currentHubId == null || currentHubId.equals(endHubId) || minWeight == Double.POSITIVE_INFINITY) {
                break;
            }

            unvisited.remove(currentHubId);

            for (Route edge : graph.getNeighbors(currentHubId)) {
                // Skip road closures / inactive routes
                if (!edge.isActive()) continue;
                if (!unvisited.contains(edge.getTargetHubId())) continue;

                Hub targetHub = graph.getHub(edge.getTargetHubId());
                double edgeWeight;

                switch (strategy) {
                    case FASTEST_TIME:
                        edgeWeight = edge.getTimeHours();
                        break;
                    case LEAST_CONGESTED:
                        double congestionFactor = (targetHub != null) ? (targetHub.getUtilizationPercent() / 100.0) : 0.0;
                        double overloadPenalty = (targetHub != null && targetHub.isOverloaded()) ? 10.0 : 0.0;
                        edgeWeight = edge.getTimeHours() * (1.0 + congestionFactor) + overloadPenalty;
                        break;
                    case SHORTEST_DISTANCE:
                    default:
                        edgeWeight = edge.getDistanceKm();
                        break;
                }

                double altWeight = weights.get(currentHubId) + edgeWeight;

                if (altWeight < weights.get(edge.getTargetHubId())) {
                    weights.put(edge.getTargetHubId(), altWeight);
                    actualDistances.put(edge.getTargetHubId(), actualDistances.get(currentHubId) + edge.getDistanceKm());
                    actualTimes.put(edge.getTargetHubId(), actualTimes.get(currentHubId) + edge.getTimeHours());
                    previous.put(edge.getTargetHubId(), currentHubId);
                }
            }
        }

        if (weights.get(endHubId) == Double.POSITIVE_INFINITY) {
            return new PathResult(false, Collections.emptyList(), 0.0, 0.0, "N/A", strategy);
        }

        LinkedList<Hub> path = new LinkedList<>();
        String current = endHubId;
        while (current != null) {
            Hub hub = graph.getHub(current);
            if (hub != null) path.addFirst(hub);
            current = previous.get(current);
        }

        int hops = Math.max(0, path.size() - 1);
        double totalDist = Math.round(actualDistances.get(endHubId) * 10.0) / 10.0;
        double transitTime = actualTimes.get(endHubId);
        double handlingBuffer = (hops > 1) ? (hops - 1) * HUB_HANDLING_HOURS_PER_HOP : 0.5;
        double totalTime = Math.round((transitTime + handlingBuffer) * 10.0) / 10.0;

        // Dynamic ETA calculation
        long hoursToAdd = (long) Math.floor(totalTime);
        long minutesToAdd = (long) Math.round((totalTime - hoursToAdd) * 60);
        LocalDateTime eta = LocalDateTime.now().plusHours(hoursToAdd).plusMinutes(minutesToAdd);
        String etaStr = eta.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return new PathResult(true, path, totalDist, totalTime, etaStr, strategy);
    }
}
