# 🚚 SwiftLogix: Enterprise Logistics & Express Routing Engine

**SwiftLogix** is a high-performance Java logistics optimization and shipment management engine. It combines computer science data structures (Graphs, Dijkstra shortest-path algorithms, Binary Search Trees, Priority Sorting Queues) with MySQL JDBC persistence and an interactive terminal CLI.

---

## 🚀 Key Features

1. **Multi-Criteria Dijkstra Routing Engine**:
   - **Shortest Distance (km)**: Minimizes physical highway distance.
   - **Fastest Transit Time (hrs)**: Optimizes for express highways with high average speeds.
   - **Congestion & Load Avoidance**: Automatically steers shipments away from overloaded logistics hubs (>85% capacity).
   - **Route Closure / Maintenance Handling**: Dynamically avoids blocked routes.
   - **Dynamic ETA Calculation**: Computes live estimated time of delivery by aggregating transit times and warehouse handling buffers (+1.5 hrs per intermediate hop).

2. **Dynamic Shipping Rate & Cost Calculator**:
   - Computes transparent shipping quotes using: `(Base Fee + Weight Charge + Distance Charge + Fuel Surcharge) * Priority Multiplier`.
   - Supports 3 Priority Tiers:
     - 🚀 **Same-Day Priority** (2.2x rate, top sorting priority)
     - ⚡ **Express Delivery** (1.5x rate)
     - 📦 **Standard Delivery** (1.0x rate)

3. **Priority Warehouse Sorting Queue (Min-Heap)**:
   - Hubs organize incoming parcels by shipment priority rank (`SAME_DAY` > `EXPRESS` > `STANDARD`), while preserving FIFO order within the same tier.

4. **ParcelBST for Fast $O(\log N)$ Tracking**:
   - Binary Search Tree for rapid tracking code lookup, search, and in-order traversal.

5. **Complete Parcel Lifecycle & Multi-Hop Simulation**:
   - Lifecycle States: `BOOKED` ➔ `IN_TRANSIT` ➔ `ARRIVED_AT_HUB` ➔ `OUT_FOR_DELIVERY` ➔ `DELIVERED` (or `CANCELLED`).
   - Automated end-to-end multi-hop simulation with real-time progress bars, hop sorting, checkpoint logging, and destination delivery handover.

6. **Logistics Network Management & Bottleneck Analytics**:
   - Add new hubs and highway routes dynamically from CLI.
   - Toggle route availability (simulate road blocks/weather issues).
   - Real-time capacity monitor identifying network bottlenecks and hub utilization percentages.

7. **MySQL JDBC Persistence & In-Memory Fallback**:
   - Persists hubs, routes, parcels (with route path, priority, cost, ETA), and tracking checkpoints to XAMPP MySQL (`swiftlogix_db`).
   - Seamlessly switches to high-performance **In-Memory Mode** if MySQL is offline.

---

## 📂 Project Architecture

```
src/
├── main/java/com/swiftlogix/
│   ├── Main.java                          # Interactive 9-Option CLI Terminal
│   ├── database/
│   │   └── DatabaseManager.java           # JDBC Connection, Schema Migrations & Persistence
│   ├── engine/
│   │   ├── DijkstraRouter.java            # Multi-Criteria Pathfinding & ETA Engine
│   │   ├── HubGraph.java                  # Adjacency List Graph of Hubs & Highway Edges
│   │   ├── NetworkSimulator.java          # Multi-Hop Dispatch & Delivery Simulator
│   │   ├── ParcelBST.java                 # Binary Search Tree for O(log N) Parcel Search
│   │   ├── RoutingStrategy.java           # Routing Objective Enum
│   │   ├── ShippingCostCalculator.java    # Rate Breakdown & Quote Engine
│   │   └── SortingQueue.java              # Priority-Aware Sorting Queue (Min-Heap)
│   ├── web/
│   │   └── SwiftLogixWebServer.java       # Embedded HTTP Server & REST API Provider
│   └── model/
│       ├── Checkpoint.java                # Tracking Milestone Event Model
│       ├── Hub.java                       # Logistics Hub Model (Load, Capacity, Coords)
│       ├── Parcel.java                    # Shipment Model (Priority, Cost, Status, Route)
│       ├── ParcelPriority.java            # Priority Tier Enum (SAME_DAY, EXPRESS, STANDARD)
│       └── Route.java                     # Highway Connection Edge Model (Distance, Time, Active)
├── main/resources/web/
│   ├── index.html                         # Web Dashboard UI
│   ├── style.css                          # Glassmorphic Dark Design System
│   └── app.js                             # Interactive Client & Leaflet Cartography
└── test/java/com/swiftlogix/
    └── EngineTest.java                    # Unit Tests (Routing, Rates, Priority Queue, BST, Simulation)
```

---

## 🛠️ Quick Start

### 1. Launch Web Dashboard & Control Center
```bash
java -cp "target/classes;lib/mysql-connector-j-8.3.0.jar" com.swiftlogix.web.SwiftLogixWebServer
```
Open **`http://localhost:8080`** in your browser.

### 2. Launch Interactive Terminal CLI
```bash
java -cp "target/classes;lib/mysql-connector-j-8.3.0.jar" com.swiftlogix.Main
```

### 3. Run Test Suite
```bash
java -ea -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.3.0.jar" com.swiftlogix.EngineTest
```