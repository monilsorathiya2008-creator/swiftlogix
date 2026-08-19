package com.swiftlogix.engine;

import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Route;

import java.util.*;

/**
 * DijkstraRouter - Calculates optimal shortest/fastest shipment route between network hubs using Dijkstra's algorithm.
 */
public class DijkstraRouter {
    private HubGraph graph;

    public static class PathResult {
        public boolean found;
        public List<Hub> path;
        public double totalDistanceKm;
        public double totalHours;

        public PathResult(boolean found, List<Hub> path, double totalDistanceKm, double totalHours) {
            this.found = found;
            this.path = path;
            this.totalDistanceKm = totalDistanceKm;
            this.totalHours = totalHours;
        }
    }

    public DijkstraRouter(HubGraph graph) {
        this.graph = graph;
    }

    public PathResult findShortestPath(String startHubId, String endHubId) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, Double> times = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> unvisited = new HashSet<>();

        for (String hubId : graph.getHubIds()) {
            distances.put(hubId, Double.POSITIVE_INFINITY);
            times.put(hubId, Double.POSITIVE_INFINITY);
            previous.put(hubId, null);
            unvisited.add(hubId);
        }

        distances.put(startHubId, 0.0);
        times.put(startHubId, 0.0);

        while (!unvisited.isEmpty()) {
            String currentHubId = null;
            double minDistance = Double.POSITIVE_INFINITY;

            for (String hubId : unvisited) {
                if (distances.get(hubId) < minDistance) {
                    minDistance = distances.get(hubId);
                    currentHubId = hubId;
                }
            }

            if (currentHubId == null || currentHubId.equals(endHubId) || minDistance == Double.POSITIVE_INFINITY) {
                break;
            }

            unvisited.remove(currentHubId);

            for (Route edge : graph.getNeighbors(currentHubId)) {
                if (!unvisited.contains(edge.getTargetHubId())) continue;

                double altDistance = distances.get(currentHubId) + edge.getDistanceKm();
                double altTime = times.get(currentHubId) + edge.getTimeHours();

                if (altDistance < distances.get(edge.getTargetHubId())) {
                    distances.put(edge.getTargetHubId(), altDistance);
                    times.put(edge.getTargetHubId(), altTime);
                    previous.put(edge.getTargetHubId(), currentHubId);
                }
            }
        }

        if (distances.get(endHubId) == Double.POSITIVE_INFINITY) {
            return new PathResult(false, Collections.emptyList(), 0.0, 0.0);
        }

        LinkedList<Hub> path = new LinkedList<>();
        String current = endHubId;
        while (current != null) {
            Hub hub = graph.getHub(current);
            if (hub != null) path.addFirst(hub);
            current = previous.get(current);
        }

        double totalDist = Math.round(distances.get(endHubId) * 10.0) / 10.0;
        double totalTime = Math.round(times.get(endHubId) * 10.0) / 10.0;

        return new PathResult(true, path, totalDist, totalTime);
    }
}
