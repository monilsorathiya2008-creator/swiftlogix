package com.swiftlogix.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.swiftlogix.database.DatabaseManager;
import com.swiftlogix.engine.*;
import com.swiftlogix.model.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SwiftLogixWebServer - Embedded HTTP server serving the Web Dashboard and REST API.
 */
public class SwiftLogixWebServer {
    private static final int PORT = 8080;
    private static HubGraph graph = new HubGraph();
    private static DijkstraRouter router;
    private static ParcelBST bst = new ParcelBST();
    private static Map<String, SortingQueue> hubQueues = new HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("========================================================================");
        System.out.println("🌐  Starting SwiftLogix Web Server & Control Center on Port " + PORT);
        System.out.println("========================================================================");

        // 1. Initialize Database
        DatabaseManager.initializeDatabase();

        // 2. Load existing network and parcels
        DatabaseManager.loadNetworkAndParcels(graph, bst, hubQueues);

        // 3. Seed initial network if empty
        if (graph.getAllHubs().isEmpty()) {
            initSeedNetworkData();
        }

        router = new DijkstraRouter(graph);

        // 4. Create and start HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Static Web UI Endpoints
        server.createContext("/", new StaticFileHandler());

        // REST API Endpoints
        server.createContext("/api/hubs", new HubsApiHandler());
        server.createContext("/api/routes", new RoutesApiHandler());
        server.createContext("/api/calculate-route", new CalculateRouteApiHandler());
        server.createContext("/api/parcels/book", new BookParcelApiHandler());
        server.createContext("/api/parcels/track", new TrackParcelApiHandler());
        server.createContext("/api/parcels/simulate", new SimulateParcelApiHandler());
        server.createContext("/api/analytics", new AnalyticsApiHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("🚀 SwiftLogix Web Dashboard is LIVE at: http://localhost:" + PORT);
        System.out.println("   ▶ REST APIs active on /api/*");
        System.out.println("   ▶ Press Ctrl+C in terminal to stop server.");
    }

    private static void initSeedNetworkData() {
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

    // ======================== HTTP HANDLERS ========================

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            String resourcePath = "/web" + path;
            InputStream is = getClass().getResourceAsStream(resourcePath);

            // Fallback to local workspace files if running outside jar
            if (is == null) {
                File localFile = new File("src/main/resources/web" + path);
                if (localFile.exists()) {
                    is = new FileInputStream(localFile);
                }
            }

            if (is == null) {
                String response = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = is.readAllBytes();
            is.close();

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class HubsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Hub h : graph.getAllHubs()) {
                if (!first) json.append(",");
                first = false;
                SortingQueue q = hubQueues.get(h.getId());
                int qSize = (q != null) ? q.size() : 0;
                json.append(String.format(Locale.US,
                        "{\"id\":\"%s\",\"name\":\"%s\",\"city\":\"%s\",\"lat\":%.4f,\"lng\":%.4f,\"capacity\":%d,\"currentLoad\":%d,\"utilization\":%.1f,\"queueSize\":%d,\"isOverloaded\":%b}",
                        escapeJson(h.getId()), escapeJson(h.getName()), escapeJson(h.getCity()),
                        h.getLat(), h.getLng(), h.getCapacity(), h.getCurrentLoad(),
                        h.getUtilizationPercent(), qSize, h.isOverloaded()));
            }
            json.append("]");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class RoutesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            Set<String> seenEdges = new HashSet<>();

            for (String srcId : graph.getHubIds()) {
                for (Route r : graph.getNeighbors(srcId)) {
                    String pairKey = srcId.compareTo(r.getTargetHubId()) < 0 ? srcId + "-" + r.getTargetHubId() : r.getTargetHubId() + "-" + srcId;
                    if (seenEdges.contains(pairKey)) continue;
                    seenEdges.add(pairKey);

                    if (!first) json.append(",");
                    first = false;
                    json.append(String.format(Locale.US,
                            "{\"source\":\"%s\",\"target\":\"%s\",\"distanceKm\":%.1f,\"timeHours\":%.1f,\"active\":%b}",
                            escapeJson(srcId), escapeJson(r.getTargetHubId()), r.getDistanceKm(), r.getTimeHours(), r.isActive()));
                }
            }
            json.append("]");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class CalculateRouteApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String src = params.getOrDefault("src", "HUB_BOM").toUpperCase();
            String dst = params.getOrDefault("dst", "HUB_DEL").toUpperCase();
            String stratStr = params.getOrDefault("strategy", "SHORTEST_DISTANCE");
            double weight = parseDoubleSafe(params.get("weight"), 2.0);
            String priorityStr = params.getOrDefault("priority", "EXPRESS");

            RoutingStrategy strategy = RoutingStrategy.SHORTEST_DISTANCE;
            try { strategy = RoutingStrategy.valueOf(stratStr.toUpperCase()); } catch (Exception ignored) {}

            ParcelPriority priority = ParcelPriority.fromString(priorityStr);

            DijkstraRouter.PathResult result = router.findOptimalRoute(src, dst, strategy);

            if (!result.found) {
                sendJsonResponse(exchange, 404, "{\"found\":false,\"message\":\"No route found between selected hubs.\"}");
                return;
            }

            ShippingCostCalculator.Quote quote = ShippingCostCalculator.generateQuote(weight, result.totalDistanceKm, priority);

            List<String> hubIds = result.path.stream().map(Hub::getId).collect(Collectors.toList());
            List<String> hubCities = result.path.stream().map(Hub::getCity).collect(Collectors.toList());

            StringBuilder json = new StringBuilder("{");
            json.append("\"found\":true,");
            json.append("\"strategy\":\"").append(escapeJson(strategy.name())).append("\",");
            json.append("\"strategyDescription\":\"").append(escapeJson(strategy.getDescription())).append("\",");
            json.append(String.format(Locale.US, "\"totalDistanceKm\":%.1f,", result.totalDistanceKm));
            json.append(String.format(Locale.US, "\"totalHours\":%.1f,", result.totalHours));
            json.append("\"eta\":\"").append(escapeJson(result.etaString)).append("\",");
            json.append("\"hops\":").append(result.getHops()).append(",");
            json.append("\"pathHubIds\":[\"").append(String.join("\",\"", hubIds)).append("\"],");
            json.append("\"pathCities\":[\"").append(String.join("\",\"", hubCities)).append("\"],");
            json.append("\"quote\":{");
            json.append(String.format(Locale.US, "\"baseFee\":%.2f,", quote.baseFee));
            json.append(String.format(Locale.US, "\"weightCharge\":%.2f,", quote.weightCharge));
            json.append(String.format(Locale.US, "\"distanceCharge\":%.2f,", quote.distanceCharge));
            json.append(String.format(Locale.US, "\"fuelSurcharge\":%.2f,", quote.fuelSurcharge));
            json.append(String.format(Locale.US, "\"priorityMultiplier\":%.1f,", quote.priorityMultiplier));
            json.append(String.format(Locale.US, "\"totalCost\":%.2f,", quote.totalCost));
            json.append("\"priority\":\"").append(escapeJson(priority.name())).append("\",");
            json.append("\"priorityDisplayName\":\"").append(escapeJson(priority.getDisplayName())).append("\"");
            json.append("}}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class BookParcelApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                params = parseBodyParams(body);
            } else {
                params = parseQueryParams(exchange.getRequestURI().getQuery());
            }

            String sender = params.getOrDefault("sender", "Express Client");
            String receiver = params.getOrDefault("receiver", "Recipient");
            double weight = parseDoubleSafe(params.get("weight"), 1.5);
            String origin = params.getOrDefault("origin", "HUB_BOM").toUpperCase();
            String dest = params.getOrDefault("dest", "HUB_DEL").toUpperCase();
            String priorityStr = params.getOrDefault("priority", "EXPRESS");
            ParcelPriority priority = ParcelPriority.fromString(priorityStr);

            if (graph.getHub(origin) == null || graph.getHub(dest) == null) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid origin or destination hub ID.\"}");
                return;
            }

            DijkstraRouter.PathResult pathResult = router.findOptimalRoute(origin, dest, RoutingStrategy.SHORTEST_DISTANCE);
            if (!pathResult.found) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"No operational highway route available.\"}");
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

            StringBuilder json = new StringBuilder("{");
            json.append("\"success\":true,");
            json.append("\"trackingCode\":\"").append(trackingCode).append("\",");
            json.append("\"sender\":\"").append(escapeJson(sender)).append("\",");
            json.append("\"receiver\":\"").append(escapeJson(receiver)).append("\",");
            json.append(String.format(Locale.US, "\"weightKg\":%.1f,", weight));
            json.append("\"origin\":\"").append(origin).append("\",");
            json.append("\"dest\":\"").append(dest).append("\",");
            json.append("\"priority\":\"").append(priority.name()).append("\",");
            json.append("\"priorityDisplayName\":\"").append(escapeJson(priority.getDisplayName())).append("\",");
            json.append(String.format(Locale.US, "\"cost\":%.2f,", quote.totalCost));
            json.append("\"estimatedDelivery\":\"").append(escapeJson(pathResult.etaString)).append("\",");
            json.append("\"route\":[\"").append(String.join("\",\"", parcel.getRoute())).append("\"]");
            json.append("}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class TrackParcelApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String code = params.getOrDefault("code", "").trim().toUpperCase();

            Parcel p = bst.search(code);
            if (p == null) {
                sendJsonResponse(exchange, 404, "{\"found\":false,\"message\":\"Parcel with tracking code '" + escapeJson(code) + "' not found in index.\"}");
                return;
            }

            int progressPercent = 10;
            String status = p.getStatus();
            if ("IN_TRANSIT".equalsIgnoreCase(status)) progressPercent = 45;
            else if ("ARRIVED_AT_HUB".equalsIgnoreCase(status) || "ARRIVED_AT_DESTINATION_HUB".equalsIgnoreCase(status)) progressPercent = 70;
            else if ("OUT_FOR_DELIVERY".equalsIgnoreCase(status)) progressPercent = 88;
            else if ("DELIVERED".equalsIgnoreCase(status)) progressPercent = 100;
            else if ("CANCELLED".equalsIgnoreCase(status)) progressPercent = 0;

            StringBuilder json = new StringBuilder("{");
            json.append("\"found\":true,");
            json.append("\"trackingCode\":\"").append(escapeJson(p.getTrackingCode())).append("\",");
            json.append("\"sender\":\"").append(escapeJson(p.getSender())).append("\",");
            json.append("\"receiver\":\"").append(escapeJson(p.getReceiver())).append("\",");
            json.append(String.format(Locale.US, "\"weightKg\":%.1f,", p.getWeightKg()));
            json.append("\"origin\":\"").append(escapeJson(p.getOriginHubId())).append("\",");
            json.append("\"dest\":\"").append(escapeJson(p.getDestHubId())).append("\",");
            json.append("\"currentHub\":\"").append(escapeJson(p.getCurrentHubId())).append("\",");
            json.append("\"status\":\"").append(escapeJson(p.getStatus())).append("\",");
            json.append("\"priority\":\"").append(p.getPriority() != null ? p.getPriority().name() : "STANDARD").append("\",");
            json.append("\"priorityDisplayName\":\"").append(p.getPriority() != null ? escapeJson(p.getPriority().getDisplayName()) : "Standard").append("\",");
            json.append(String.format(Locale.US, "\"cost\":%.2f,", p.getCost()));
            json.append("\"estimatedDelivery\":\"").append(escapeJson(p.getEstimatedDelivery())).append("\",");
            json.append("\"progressPercent\":").append(progressPercent).append(",");
            json.append("\"route\":[\"").append(p.getRoute() != null ? String.join("\",\"", p.getRoute()) : "").append("\"],");
            json.append("\"checkpoints\":[");
            boolean first = true;
            for (Checkpoint cp : p.getCheckpoints()) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format("{\"status\":\"%s\",\"hubId\":\"%s\",\"hubName\":\"%s\",\"timestamp\":\"%s\"}",
                        escapeJson(cp.getStatus()), escapeJson(cp.getHubId()), escapeJson(cp.getHubName()), escapeJson(cp.getTimestamp())));
            }
            json.append("]}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class SimulateParcelApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String code = params.getOrDefault("code", "").trim().toUpperCase();

            Parcel p = bst.search(code);
            if (p == null) {
                sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"Parcel not found: " + escapeJson(code) + "\"}");
                return;
            }

            NetworkSimulator.SimulationResult simRes = NetworkSimulator.simulateEndToEndDelivery(p, graph, hubQueues, false);

            StringBuilder json = new StringBuilder("{");
            json.append("\"success\":").append(simRes.success).append(",");
            json.append("\"message\":\"").append(escapeJson(simRes.message)).append("\",");
            json.append("\"status\":\"").append(escapeJson(p.getStatus())).append("\",");
            json.append("\"steps\":[");
            boolean first = true;
            for (NetworkSimulator.SimulationStep step : simRes.steps) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format("{\"status\":\"%s\",\"hubId\":\"%s\",\"hubName\":\"%s\",\"description\":\"%s\"}",
                        escapeJson(step.status), escapeJson(step.hubId), escapeJson(step.hubName), escapeJson(step.description)));
            }
            json.append("]}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class AnalyticsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            int totalCapacity = 0;
            int totalLoad = 0;
            int congestedCount = 0;
            for (Hub h : graph.getAllHubs()) {
                totalCapacity += h.getCapacity();
                totalLoad += h.getCurrentLoad();
                if (h.isOverloaded()) congestedCount++;
            }

            double networkUtil = totalCapacity > 0 ? ((double) totalLoad / totalCapacity) * 100.0 : 0.0;

            StringBuilder json = new StringBuilder("{");
            json.append("\"totalHubs\":").append(graph.getAllHubs().size()).append(",");
            json.append("\"totalCapacity\":").append(totalCapacity).append(",");
            json.append("\"totalLoad\":").append(totalLoad).append(",");
            json.append(String.format(Locale.US, "\"networkUtilization\":%.1f,", networkUtil));
            json.append("\"congestedHubs\":").append(congestedCount).append(",");
            json.append("\"totalTrackedParcels\":").append(bst.getSize()).append(",");
            json.append("\"dbOnline\":").append(DatabaseManager.isDatabaseAvailable());
            json.append("}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    // ======================== UTILITIES ========================

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } else if (pair.length == 1) {
                params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    private static Map<String, String> parseBodyParams(String body) {
        if (body == null || body.trim().isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>();
        // Handle URL-encoded or basic JSON
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON key-value extractor
            String content = body.substring(1, body.length() - 1);
            for (String part : content.split(",")) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replaceAll("^\"|\"$", "");
                    String v = kv[1].trim().replaceAll("^\"|\"$", "");
                    result.put(k, v);
                }
            }
        } else {
            result = parseQueryParams(body);
        }
        return result;
    }

    private static double parseDoubleSafe(String val, double def) {
        if (val == null || val.trim().isEmpty()) return def;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
