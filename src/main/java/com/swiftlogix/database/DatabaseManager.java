package com.swiftlogix.database;

import com.swiftlogix.engine.HubGraph;
import com.swiftlogix.engine.ParcelBST;
import com.swiftlogix.engine.SortingQueue;
import com.swiftlogix.model.Checkpoint;
import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Parcel;
import com.swiftlogix.model.ParcelPriority;

import java.sql.*;
import java.util.*;

/**
 * DatabaseManager - Manages XAMPP MySQL JDBC Connection, table migrations, and parcel/hub persistence.
 * Includes automatic in-memory fallback if MySQL is unreachable.
 */
public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/swiftlogix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static boolean dbAvailable = true;

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}
        DriverManager.setLoginTimeout(2);
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean isDatabaseAvailable() {
        return dbAvailable;
    }

    private static void handleDbException(SQLException e, String operation) {
        if (dbAvailable) {
            dbAvailable = false;
            System.out.println("ℹ️ MySQL offline (" + e.getMessage() + "). SwiftLogix switching to In-Memory Mode.");
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            dbAvailable = true;

            // 1. Hubs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS hubs (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "city VARCHAR(50) NOT NULL, " +
                    "lat DOUBLE NOT NULL, " +
                    "lng DOUBLE NOT NULL, " +
                    "capacity INT NOT NULL, " +
                    "current_load INT NOT NULL DEFAULT 0)");

            // 2. Routes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS routes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "source_hub_id VARCHAR(50) NOT NULL, " +
                    "target_hub_id VARCHAR(50) NOT NULL, " +
                    "distance_km DOUBLE NOT NULL, " +
                    "time_hours DOUBLE NOT NULL, " +
                    "is_active BOOLEAN DEFAULT TRUE)");

            // 3. Parcels Table
            stmt.execute("CREATE TABLE IF NOT EXISTS parcels (" +
                    "tracking_code VARCHAR(50) PRIMARY KEY, " +
                    "sender VARCHAR(100) NOT NULL, " +
                    "receiver VARCHAR(100) NOT NULL, " +
                    "weight_kg DOUBLE NOT NULL, " +
                    "origin_hub_id VARCHAR(50) NOT NULL, " +
                    "dest_hub_id VARCHAR(50) NOT NULL, " +
                    "current_hub_id VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "priority VARCHAR(20) DEFAULT 'STANDARD', " +
                    "cost DOUBLE DEFAULT 0.0, " +
                    "estimated_delivery VARCHAR(50), " +
                    "route_path TEXT)");

            // 4. Checkpoints Table
            stmt.execute("CREATE TABLE IF NOT EXISTS checkpoints (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "tracking_code VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "hub_id VARCHAR(50) NOT NULL, " +
                    "hub_name VARCHAR(100) NOT NULL, " +
                    "timestamp VARCHAR(50) NOT NULL)");

            // Automatic schema column migrations if tables pre-existed
            migrateSchema(conn);

            System.out.println("✅ XAMPP MySQL database 'swiftlogix_db' connected and schema verified.");
        } catch (SQLException e) {
            dbAvailable = false;
            System.out.println("ℹ️ MySQL not connected (" + e.getMessage() + "). Running in High-Performance IN-MEMORY mode.");
        }
    }

    private static void migrateSchema(Connection conn) {
        String[] migrations = {
                "ALTER TABLE parcels ADD COLUMN priority VARCHAR(20) DEFAULT 'STANDARD'",
                "ALTER TABLE parcels ADD COLUMN cost DOUBLE DEFAULT 0.0",
                "ALTER TABLE parcels ADD COLUMN estimated_delivery VARCHAR(50)",
                "ALTER TABLE parcels ADD COLUMN route_path TEXT",
                "ALTER TABLE routes ADD COLUMN is_active BOOLEAN DEFAULT TRUE"
        };

        for (String sql : migrations) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException ignored) {
                // Column already exists
            }
        }
    }

    public static void saveHub(Hub hub) {
        if (!dbAvailable || hub == null) return;
        String sql = "INSERT INTO hubs (id, name, city, lat, lng, capacity, current_load) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE current_load = VALUES(current_load), capacity = VALUES(capacity)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hub.getId());
            pstmt.setString(2, hub.getName());
            pstmt.setString(3, hub.getCity());
            pstmt.setDouble(4, hub.getLat());
            pstmt.setDouble(5, hub.getLng());
            pstmt.setInt(6, hub.getCapacity());
            pstmt.setInt(7, hub.getCurrentLoad());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            handleDbException(e, "saveHub");
        }
    }

    public static void saveRoute(String sourceId, String targetId, double distanceKm, double timeHours, boolean active) {
        if (!dbAvailable) return;
        String sql = "INSERT INTO routes (source_hub_id, target_hub_id, distance_km, time_hours, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sourceId);
            pstmt.setString(2, targetId);
            pstmt.setDouble(3, distanceKm);
            pstmt.setDouble(4, timeHours);
            pstmt.setBoolean(5, active);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            handleDbException(e, "saveRoute");
        }
    }

    public static void updateRouteStatus(String sourceId, String targetId, boolean active) {
        if (!dbAvailable) return;
        String sql = "UPDATE routes SET is_active = ? WHERE (source_hub_id = ? AND target_hub_id = ?) OR (source_hub_id = ? AND target_hub_id = ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, active);
            pstmt.setString(2, sourceId);
            pstmt.setString(3, targetId);
            pstmt.setString(4, targetId);
            pstmt.setString(5, sourceId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            handleDbException(e, "updateRouteStatus");
        }
    }

    public static void saveParcel(Parcel parcel) {
        if (!dbAvailable || parcel == null) return;
        String sql = "INSERT INTO parcels (tracking_code, sender, receiver, weight_kg, origin_hub_id, dest_hub_id, current_hub_id, status, priority, cost, estimated_delivery, route_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE current_hub_id = VALUES(current_hub_id), status = VALUES(status), priority = VALUES(priority), cost = VALUES(cost), estimated_delivery = VALUES(estimated_delivery), route_path = VALUES(route_path)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, parcel.getTrackingCode());
            pstmt.setString(2, parcel.getSender());
            pstmt.setString(3, parcel.getReceiver());
            pstmt.setDouble(4, parcel.getWeightKg());
            pstmt.setString(5, parcel.getOriginHubId());
            pstmt.setString(6, parcel.getDestHubId());
            pstmt.setString(7, parcel.getCurrentHubId());
            pstmt.setString(8, parcel.getStatus());
            pstmt.setString(9, parcel.getPriority() != null ? parcel.getPriority().name() : "STANDARD");
            pstmt.setDouble(10, parcel.getCost());
            pstmt.setString(11, parcel.getEstimatedDelivery());
            pstmt.setString(12, parcel.getRoute() != null ? String.join(",", parcel.getRoute()) : "");
            pstmt.executeUpdate();

            // Save Checkpoints
            for (Checkpoint cp : parcel.getCheckpoints()) {
                saveCheckpoint(parcel.getTrackingCode(), cp);
            }
        } catch (SQLException e) {
            handleDbException(e, "saveParcel");
        }
    }

    public static void saveCheckpoint(String trackingCode, Checkpoint cp) {
        if (!dbAvailable || cp == null) return;
        String sql = "INSERT INTO checkpoints (tracking_code, status, hub_id, hub_name, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trackingCode);
            pstmt.setString(2, cp.getStatus());
            pstmt.setString(3, cp.getHubId());
            pstmt.setString(4, cp.getHubName());
            pstmt.setString(5, cp.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {
            // Handled
        }
    }

    public static void loadNetworkAndParcels(HubGraph graph, ParcelBST bst, Map<String, SortingQueue> hubQueues) {
        if (!dbAvailable) return;
        try (Connection conn = getConnection()) {
            // 1. Load Hubs
            Statement stmt = conn.createStatement();
            ResultSet rsHubs = stmt.executeQuery("SELECT * FROM hubs");
            while (rsHubs.next()) {
                String id = rsHubs.getString("id");
                graph.addHub(id, rsHubs.getString("name"), rsHubs.getString("city"),
                        rsHubs.getDouble("lat"), rsHubs.getDouble("lng"), rsHubs.getInt("capacity"));
                graph.updateHubLoad(id, rsHubs.getInt("current_load"));
                if (!hubQueues.containsKey(id)) {
                    hubQueues.put(id, new SortingQueue(id));
                }
            }

            // 2. Load Routes
            ResultSet rsRoutes = stmt.executeQuery("SELECT * FROM routes");
            while (rsRoutes.next()) {
                boolean active = true;
                try {
                    active = rsRoutes.getBoolean("is_active");
                } catch (SQLException ignored) {}
                graph.addRoute(rsRoutes.getString("source_hub_id"), rsRoutes.getString("target_hub_id"),
                        rsRoutes.getDouble("distance_km"), rsRoutes.getDouble("time_hours"), false);
                // Set active flag on created route
                for (com.swiftlogix.model.Route r : graph.getNeighbors(rsRoutes.getString("source_hub_id"))) {
                    if (r.getTargetHubId().equals(rsRoutes.getString("target_hub_id"))) {
                        r.setActive(active);
                    }
                }
            }

            // 3. Load Parcels
            ResultSet rsParcels = stmt.executeQuery("SELECT * FROM parcels");
            while (rsParcels.next()) {
                String code = rsParcels.getString("tracking_code");
                ParcelPriority priority = ParcelPriority.STANDARD;
                try {
                    String pStr = rsParcels.getString("priority");
                    if (pStr != null) priority = ParcelPriority.fromString(pStr);
                } catch (SQLException ignored) {}

                Parcel p = new Parcel(code, rsParcels.getString("sender"), rsParcels.getString("receiver"),
                        rsParcels.getDouble("weight_kg"), rsParcels.getString("origin_hub_id"), rsParcels.getString("dest_hub_id"), priority);
                p.setCurrentHubId(rsParcels.getString("current_hub_id"));
                p.setStatus(rsParcels.getString("status"));

                try {
                    p.setCost(rsParcels.getDouble("cost"));
                    p.setEstimatedDelivery(rsParcels.getString("estimated_delivery"));
                    String routePath = rsParcels.getString("route_path");
                    if (routePath != null && !routePath.trim().isEmpty()) {
                        p.setRoute(new ArrayList<>(Arrays.asList(routePath.split(","))));
                    }
                } catch (SQLException ignored) {}

                // Load Checkpoints
                PreparedStatement pstmtCp = conn.prepareStatement("SELECT * FROM checkpoints WHERE tracking_code = ? ORDER BY id ASC");
                pstmtCp.setString(1, code);
                ResultSet rsCp = pstmtCp.executeQuery();
                while (rsCp.next()) {
                    p.addCheckpoint(new Checkpoint(rsCp.getString("status"), rsCp.getString("hub_id"),
                            rsCp.getString("hub_name"), rsCp.getString("timestamp")));
                }

                bst.insert(p);
                if (p.getCurrentHubId() != null && hubQueues.containsKey(p.getCurrentHubId()) && !p.isDelivered() && !p.isCancelled()) {
                    hubQueues.get(p.getCurrentHubId()).enqueue(p);
                }
            }
            System.out.println("✅ Data successfully loaded from XAMPP MySQL database.");
        } catch (SQLException e) {
            System.err.println("ℹ️ MySQL load note: " + e.getMessage());
        }
    }
}
