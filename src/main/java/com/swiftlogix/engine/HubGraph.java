package com.swiftlogix.engine;

import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Route;

import java.util.*;

/**
 * HubGraph - Weighted Adjacency List graph representing hubs (nodes) and transport routes (edges).
 */
public class HubGraph {
    private Map<String, Hub> nodes;
    private Map<String, List<Route>> adjacencyList;

    public HubGraph() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    public void addHub(String id, String name, String city, double lat, double lng, int capacity) {
        if (!nodes.containsKey(id)) {
            Hub hub = new Hub(id, name, city, lat, lng, capacity);
            nodes.put(id, hub);
            adjacencyList.put(id, new ArrayList<>());
        }
    }

    public void addRoute(String sourceHubId, String targetHubId, double distanceKm, double timeHours, boolean bidirectional) {
        if (!nodes.containsKey(sourceHubId) || !nodes.containsKey(targetHubId)) {
            throw new IllegalArgumentException("Both source (" + sourceHubId + ") and target (" + targetHubId + ") hubs must exist in HubGraph.");
        }

        double hours = (timeHours > 0) ? timeHours : Math.round((distanceKm / 65.0) * 10.0) / 10.0;

        adjacencyList.get(sourceHubId).add(new Route(targetHubId, distanceKm, hours));
        if (bidirectional) {
            adjacencyList.get(targetHubId).add(new Route(sourceHubId, distanceKm, hours));
        }
    }

    public Hub getHub(String id) {
        return nodes.get(id);
    }

    public Collection<Hub> getAllHubs() {
        return nodes.values();
    }

    public Set<String> getHubIds() {
        return nodes.keySet();
    }

    public List<Route> getNeighbors(String hubId) {
        return adjacencyList.getOrDefault(hubId, Collections.emptyList());
    }

    public void updateHubLoad(String hubId, int delta) {
        Hub hub = nodes.get(hubId);
        if (hub != null) {
            hub.updateLoad(delta);
        }
    }
}
