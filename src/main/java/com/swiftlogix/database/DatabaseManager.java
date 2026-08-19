package com.swiftlogix.database;

import com.swiftlogix.engine.HubGraph;
import com.swiftlogix.engine.ParcelBST;
import com.swiftlogix.engine.SortingQueue;
import com.swiftlogix.model.Checkpoint;
import com.swiftlogix.model.Hub;
import com.swiftlogix.model.Parcel;

import java.sql.*;
import java.util.*;

/**
 * DatabaseManager - Manages XAMPP MySQL JDBC Connection, table creation, and parcel/hub persistence.
 */
public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/swiftlogix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Driver loaded dynamically
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
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
                    "time_hours DOUBLE NOT NULL)");

            // 3. Parcels Table
            stmt.execute("CREATE TABLE IF NOT EXISTS parcels (" +
                    "tracking_code VARCHAR(50) PRIMARY KEY, " +
                    "sender VARCHAR(100) NOT NULL, " +
                    "receiver VARCHAR(100) NOT NULL, " +
                    "weight_kg DOUBLE NOT NULL, " +
                    "origin_hub_id VARCHAR(50) NOT NULL, " +
                    "dest_hub_id VARCHAR(50) NOT NULL, " +
                    "current_hub_id VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL)");

            // 4. Checkpoints Table
            stmt.execute("CREATE TABLE IF NOT EXISTS checkpoints (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "tracking_code VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "hub_id VARCHAR(50) NOT NULL, " +
                    "hub_name VARCHAR(100) NOT NULL, " +
                    "timestamp VARCHAR(50) NOT NULL)");

            System.out.println("✅ XAMPP MySQL database 'swiftlogix_db' tables verified.");
        } catch (SQLException e) {
            System.err.println("❌ Error initializing XAMPP MySQL database: " + e.getMessage());
        }
    }

    public static void saveHub(Hub hub) {
        String sql = "INSERT INTO hubs (id, name, city, lat, lng, capacity, current_load) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE current_load = VALUES(current_load)";
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
            System.err.println("❌ Error saving hub: " + e.getMessage());
        }
    }

    public static void saveRoute(String sourceId, String targetId, double distanceKm, double timeHours) {
        String sql = "INSERT INTO routes (source_hub_id, target_hub_id, distance_km, time_hours) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sourceId);
            pstmt.setString(2, targetId);
            pstmt.setDouble(3, distanceKm);
            pstmt.setDouble(4, timeHours);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error saving route: " + e.getMessage());
        }
    }

    public static void saveParcel(Parcel parcel) {
        String sql = "INSERT INTO parcels (tracking_code, sender, receiver, weight_kg, origin_hub_id, dest_hub_id, current_hub_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE current_hub_id = VALUES(current_hub_id), status = VALUES(status)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, parcel.getTrackingCode());
            pstmt.setString(2, parcel.getSender());
            pstmt.setString(3, parcel.getReceiver());
            pstmt.setDouble(4, parcel.getWeightKg());
            pstmt.setString(5, parcel.getOriginHubId());
            pstmt.setString(6, parcel.getDestHubId());
            pstmt.setString(7, parcel.getCurrentHubId());
            pstmt.setString(8, parcel.getStatus());
            pstmt.executeUpdate();

            // Save Checkpoints
            for (Checkpoint cp : parcel.getCheckpoints()) {
                saveCheckpoint(parcel.getTrackingCode(), cp);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error saving parcel to XAMPP MySQL: " + e.getMessage());
        }
    }

    public static void saveCheckpoint(String trackingCode, Checkpoint cp) {
        String sql = "INSERT INTO checkpoints (tracking_code, status, hub_id, hub_name, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trackingCode);
            pstmt.setString(2, cp.getStatus());
            pstmt.setString(3, cp.getHubId());
            pstmt.setString(4, cp.getHubName());
            pstmt.setString(5, cp.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Duplicate checkpoints ignored
        }
    }

    public static void loadNetworkAndParcels(HubGraph graph, ParcelBST bst, Map<String, SortingQueue> hubQueues) {
        try (Connection conn = getConnection()) {
            // Load Hubs
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

            // Load Routes
            ResultSet rsRoutes = stmt.executeQuery("SELECT * FROM routes");
            while (rsRoutes.next()) {
                graph.addRoute(rsRoutes.getString("source_hub_id"), rsRoutes.getString("target_hub_id"),
                        rsRoutes.getDouble("distance_km"), rsRoutes.getDouble("time_hours"), false);
            }

            // Load Parcels
            ResultSet rsParcels = stmt.executeQuery("SELECT * FROM parcels");
            while (rsParcels.next()) {
                String code = rsParcels.getString("tracking_code");
                Parcel p = new Parcel(code, rsParcels.getString("sender"), rsParcels.getString("receiver"),
                        rsParcels.getDouble("weight_kg"), rsParcels.getString("origin_hub_id"), rsParcels.getString("dest_hub_id"));
                p.setCurrentHubId(rsParcels.getString("current_hub_id"));
                p.setStatus(rsParcels.getString("status"));

                // Load Checkpoints
                PreparedStatement pstmtCp = conn.prepareStatement("SELECT * FROM checkpoints WHERE tracking_code = ? ORDER BY id ASC");
                pstmtCp.setString(1, code);
                ResultSet rsCp = pstmtCp.executeQuery();
                while (rsCp.next()) {
                    p.addCheckpoint(new Checkpoint(rsCp.getString("status"), rsCp.getString("hub_id"),
                            rsCp.getString("hub_name"), rsCp.getString("timestamp")));
                }

                bst.insert(p);
                if (p.getCurrentHubId() != null && hubQueues.containsKey(p.getCurrentHubId())) {
                    hubQueues.get(p.getCurrentHubId()).enqueue(p);
                }
            }
            System.out.println("✅ Data successfully loaded from XAMPP MySQL database.");
        } catch (SQLException e) {
            System.err.println("❌ Error loading from XAMPP MySQL: " + e.getMessage());
        }
    }
}
