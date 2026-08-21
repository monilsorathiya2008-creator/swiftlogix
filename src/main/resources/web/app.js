// SwiftLogix Web Dashboard Client Application (Powered by Leaflet Geographic Cartography)

let hubsData = [];
let routesData = [];
let activeRoutePath = [];
let selectedOrigin = null;
let selectedDest = null;
let currentTrackingData = null;

let leafletMap = null;
let routePolylinesLayer = null;
let activePathPolyline = null;
let hubMarkersLayer = null;
let transitVehicleMarker = null;

const routingStrategySelect = document.getElementById('routing-strategy');

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initLeafletMap();
    fetchNetworkData();
    setupEventListeners();
});

function initTabs() {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            tab.classList.add('active');
            const target = document.getElementById(tab.dataset.tab);
            if (target) target.classList.add('active');
            // Invalidate Leaflet map size on tab switch
            if (leafletMap) setTimeout(() => leafletMap.invalidateSize(), 150);
        });
    });
}

function initLeafletMap() {
    // 1. Center Map on India coordinates
    leafletMap = L.map('logistics-map', {
        center: [22.0, 79.5],
        zoom: 5,
        minZoom: 4,
        maxZoom: 10,
        zoomControl: true
    });

    // 2. High-Tech Dark Matter CartoDB Map Tiles
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://carto.com/" target="_blank">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a>',
        subdomains: 'abcd',
        maxZoom: 19
    }).addTo(leafletMap);

    routePolylinesLayer = L.layerGroup().addTo(leafletMap);
    hubMarkersLayer = L.layerGroup().addTo(leafletMap);
}

function setupEventListeners() {
    document.getElementById('btn-refresh-hubs').addEventListener('click', fetchNetworkData);

    // Form input changes trigger live quote
    ['book-origin', 'book-dest', 'book-weight'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('change', updateLiveQuote);
        if (el) el.addEventListener('input', updateLiveQuote);
    });

    document.querySelectorAll('input[name="priority"]').forEach(radio => {
        radio.addEventListener('change', updateLiveQuote);
    });

    routingStrategySelect.addEventListener('change', () => {
        updateLiveQuote();
    });

    // Booking form submit
    document.getElementById('booking-form').addEventListener('submit', handleBooking);

    // Tracking form submit
    document.getElementById('btn-track-submit').addEventListener('click', () => {
        const code = document.getElementById('track-code-input').value.trim();
        if (code) trackParcel(code);
    });

    document.getElementById('track-code-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const code = document.getElementById('track-code-input').value.trim();
            if (code) trackParcel(code);
        }
    });

    // Modal buttons
    document.getElementById('btn-modal-close').addEventListener('click', () => {
        document.getElementById('booking-modal').classList.add('hidden');
    });

    document.getElementById('btn-modal-track').addEventListener('click', () => {
        const code = document.getElementById('modal-code').innerText;
        document.getElementById('booking-modal').classList.add('hidden');
        document.querySelector('.tab-btn[data-tab="tab-track"]').click();
        document.getElementById('track-code-input').value = code;
        trackParcel(code);
    });

    // Role Switcher buttons with Password Protection
    document.getElementById('role-customer-btn').addEventListener('click', () => setRole('CUSTOMER'));
    document.getElementById('role-admin-btn').addEventListener('click', handleAdminRoleRequest);
    document.getElementById('role-logout-btn').addEventListener('click', logoutAdmin);

    // Admin Auth Modal Events
    document.getElementById('admin-auth-form').addEventListener('submit', verifyAdminPasscode);
    document.getElementById('btn-cancel-admin-auth').addEventListener('click', closeAdminAuthModal);
    document.getElementById('btn-toggle-pass').addEventListener('click', togglePasscodeVisibility);

    // Simulation runner
    document.getElementById('btn-run-simulation').addEventListener('click', runSimulation);

    // Set initial role (Default to CUSTOMER for clean public experience)
    const savedAuth = sessionStorage.getItem('swiftlogix_admin_auth') === 'true';
    setRole(savedAuth ? 'ADMIN' : 'CUSTOMER');
}

const MASTER_ADMIN_PASSCODE = 'admin123';

function handleAdminRoleRequest() {
    const isAuth = sessionStorage.getItem('swiftlogix_admin_auth') === 'true';
    if (isAuth) {
        setRole('ADMIN');
    } else {
        openAdminAuthModal();
    }
}

function openAdminAuthModal() {
    const modal = document.getElementById('admin-auth-modal');
    const input = document.getElementById('admin-pass-input');
    const errorMsg = document.getElementById('admin-auth-error');

    errorMsg.classList.add('hidden');
    input.value = '';
    modal.classList.remove('hidden');
    setTimeout(() => input.focus(), 150);
}

function closeAdminAuthModal() {
    document.getElementById('admin-auth-modal').classList.add('hidden');
}

function togglePasscodeVisibility() {
    const input = document.getElementById('admin-pass-input');
    const btn = document.getElementById('btn-toggle-pass');
    if (input.type === 'password') {
        input.type = 'text';
        btn.innerHTML = '<i class="fa-solid fa-eye-slash"></i>';
    } else {
        input.type = 'password';
        btn.innerHTML = '<i class="fa-solid fa-eye"></i>';
    }
}

function verifyAdminPasscode(e) {
    if (e) e.preventDefault();
    const input = document.getElementById('admin-pass-input');
    const errorMsg = document.getElementById('admin-auth-error');
    const pass = input.value.trim();

    if (pass === MASTER_ADMIN_PASSCODE) {
        sessionStorage.setItem('swiftlogix_admin_auth', 'true');
        closeAdminAuthModal();
        setRole('ADMIN');
    } else {
        errorMsg.classList.remove('hidden');
        input.value = '';
        input.focus();
    }
}

function logoutAdmin() {
    sessionStorage.removeItem('swiftlogix_admin_auth');
    setRole('CUSTOMER');
}

function setRole(role) {
    const custBtn = document.getElementById('role-customer-btn');
    const adminBtn = document.getElementById('role-admin-btn');
    const logoutBtn = document.getElementById('role-logout-btn');
    const custBanner = document.getElementById('customer-banner');
    const adminMetrics = document.getElementById('admin-metrics-ribbon');
    const adminLeaderboard = document.getElementById('admin-leaderboard-panel');
    const simulateTabBtn = document.getElementById('tab-simulate-btn');

    if (role === 'CUSTOMER') {
        custBtn.classList.add('active');
        adminBtn.classList.remove('active');
        logoutBtn.classList.add('hidden');

        custBanner.classList.remove('hidden');
        if (adminMetrics) adminMetrics.style.display = 'none';
        if (adminLeaderboard) adminLeaderboard.style.display = 'none';
        if (simulateTabBtn) simulateTabBtn.style.display = 'none';

        // Default to tracking tab for customer
        document.querySelector('.tab-btn[data-tab="tab-track"]')?.click();
    } else {
        adminBtn.classList.add('active');
        custBtn.classList.remove('active');
        logoutBtn.classList.remove('hidden');

        custBanner.classList.add('hidden');
        if (adminMetrics) adminMetrics.style.display = 'grid';
        if (adminLeaderboard) adminLeaderboard.style.display = 'block';
        if (simulateTabBtn) simulateTabBtn.style.display = 'inline-flex';
    }

    if (leafletMap) {
        setTimeout(() => leafletMap.invalidateSize(), 100);
    }
}

// --- Fetch Network Data ---
async function fetchNetworkData() {
    try {
        const [hubsRes, routesRes, analyticsRes] = await Promise.all([
            fetch('/api/hubs'),
            fetch('/api/routes'),
            fetch('/api/analytics')
        ]);

        hubsData = await hubsRes.json();
        routesData = await routesRes.json();
        const analytics = await analyticsRes.json();

        updateMetricsRibbon(analytics);
        populateHubDropdowns();
        renderHubsLeaderboard();
        renderLeafletMap();

        // Default initial quote
        updateLiveQuote();
    } catch (err) {
        console.error('Error fetching network data:', err);
    }
}

function updateMetricsRibbon(analytics) {
    document.getElementById('stat-hubs-count').innerText = `${analytics.totalHubs} Mega Hubs`;
    document.getElementById('stat-routes-count').innerText = `${routesData.length} Corridors`;
    document.getElementById('stat-parcels-count').innerText = `${analytics.totalTrackedParcels} in BST Index`;
    document.getElementById('stat-network-util').innerText = `${analytics.networkUtilization.toFixed(1)}%`;

    const dbPill = document.getElementById('db-status-pill');
    const dbText = document.getElementById('db-status-text');
    if (analytics.dbOnline) {
        dbText.innerText = 'MySQL Online';
        dbPill.style.borderColor = 'rgba(79, 172, 254, 0.4)';
    } else {
        dbText.innerText = 'In-Memory Mode';
        dbPill.style.borderColor = 'rgba(245, 158, 11, 0.4)';
    }
}

function populateHubDropdowns() {
    const originSel = document.getElementById('book-origin');
    const destSel = document.getElementById('book-dest');

    if (!originSel || !destSel) return;

    originSel.innerHTML = '';
    destSel.innerHTML = '';

    hubsData.forEach(h => {
        const opt1 = document.createElement('option');
        opt1.value = h.id;
        opt1.textContent = `${h.city} (${h.name})`;
        if (h.id === 'HUB_BOM') opt1.selected = true;
        originSel.appendChild(opt1);

        const opt2 = document.createElement('option');
        opt2.value = h.id;
        opt2.textContent = `${h.city} (${h.name})`;
        if (h.id === 'HUB_DEL') opt2.selected = true;
        destSel.appendChild(opt2);
    });
}

function renderHubsLeaderboard() {
    const container = document.getElementById('hubs-leaderboard');
    if (!container) return;

    container.innerHTML = '';
    const sorted = [...hubsData].sort((a, b) => b.utilization - a.utilization);

    sorted.forEach(h => {
        let statusClass = 'normal';
        let statusText = 'Normal';
        if (h.isOverloaded) {
            statusClass = 'overloaded';
            statusText = 'Congested';
        } else if (h.utilization >= 70) {
            statusClass = 'warning';
            statusText = 'High Load';
        }

        const card = document.createElement('div');
        card.className = 'hub-card';
        card.innerHTML = `
            <div class="hub-card-header">
                <span class="hub-card-title">${h.city}</span>
                <span class="hub-status-tag ${statusClass}">${statusText}</span>
            </div>
            <div class="hub-load-bar">
                <div class="hub-load-fill ${statusClass}" style="width: ${Math.min(100, h.utilization)}%;"></div>
            </div>
            <div class="hub-card-footer">
                <span>Load: ${h.currentLoad}/${h.capacity} (${h.utilization.toFixed(0)}%)</span>
                <span>${h.queueSize} pkgs</span>
            </div>
        `;
        card.addEventListener('click', () => {
            handleHubSelection(h.id);
            leafletMap.flyTo([h.lat, h.lng], 6, { duration: 0.8 });
        });
        container.appendChild(card);
    });
}

// --- Dynamic Route Calculation & Quotes ---
async function updateLiveQuote() {
    const origin = document.getElementById('book-origin')?.value || 'HUB_BOM';
    const dest = document.getElementById('book-dest')?.value || 'HUB_DEL';
    const weight = parseFloat(document.getElementById('book-weight')?.value) || 2.0;
    const priority = document.querySelector('input[name="priority"]:checked')?.value || 'EXPRESS';
    const strategy = routingStrategySelect?.value || 'SHORTEST_DISTANCE';

    if (origin === dest) {
        document.getElementById('quote-dist').innerText = '0 km';
        document.getElementById('quote-time').innerText = '0 hrs';
        document.getElementById('quote-fuel').innerText = '₹0.00';
        document.getElementById('quote-total').innerText = '₹0.00';
        document.getElementById('quote-eta-badge').innerText = 'Same origin & dest';
        activeRoutePath = [];
        highlightOptimalRouteOnMap([]);
        return;
    }

    try {
        const url = `/api/calculate-route?src=${origin}&dst=${dest}&strategy=${strategy}&weight=${weight}&priority=${priority}`;
        const res = await fetch(url);
        if (!res.ok) return;

        const data = await res.json();
        if (data.found) {
            activeRoutePath = data.pathHubIds;
            document.getElementById('quote-dist').innerText = `${data.totalDistanceKm} km`;
            document.getElementById('quote-time').innerText = `${data.totalHours} hrs`;
            document.getElementById('quote-fuel').innerText = `₹${data.quote.fuelSurcharge.toFixed(2)}`;
            document.getElementById('quote-total').innerText = `₹${data.quote.totalCost.toFixed(2)}`;
            document.getElementById('quote-eta-badge').innerText = `ETA: ${data.eta}`;

            // Map overlay
            document.getElementById('overlay-route-path').innerText = data.pathCities.join(' ➔ ');
            document.getElementById('overlay-dist').innerText = `${data.totalDistanceKm} km`;
            document.getElementById('overlay-time').innerText = `${data.totalHours} hrs`;
            document.getElementById('overlay-eta').innerText = `ETA: ${data.eta}`;

            highlightOptimalRouteOnMap(data.pathHubIds);
        }
    } catch (err) {
        console.error('Error calculating route:', err);
    }
}

// --- Leaflet Map Rendering ---
function renderLeafletMap() {
    if (!leafletMap) return;

    routePolylinesLayer.clearLayers();
    hubMarkersLayer.clearLayers();

    const hubMap = new Map();
    hubsData.forEach(h => hubMap.set(h.id, h));

    // 1. Draw Regular Highway Routes
    routesData.forEach(r => {
        const src = hubMap.get(r.source);
        const dst = hubMap.get(r.target);
        if (!src || !dst) return;

        const polyline = L.polyline([[src.lat, src.lng], [dst.lat, dst.lng]], {
            color: r.active ? '#3b82f6' : '#f43f5e',
            weight: r.active ? 2.5 : 1.5,
            opacity: r.active ? 0.45 : 0.3,
            dashArray: r.active ? null : '6, 6'
        });
        routePolylinesLayer.addLayer(polyline);
    });

    // 2. Draw Interactive Hub Beacon Markers
    hubsData.forEach(h => {
        let statusClass = 'normal';
        if (h.isOverloaded) statusClass = 'overloaded';
        else if (h.utilization >= 70) statusClass = 'warning';

        const customHtml = `
            <div class="hub-custom-marker" title="${h.city} (${h.name})">
                <div class="hub-beacon-core ${statusClass}">
                    <div class="hub-beacon-pulse"></div>
                </div>
                <div class="hub-marker-label">${h.city}</div>
            </div>
        `;

        const icon = L.divIcon({
            html: customHtml,
            className: 'custom-hub-icon-wrapper',
            iconSize: [60, 40],
            iconAnchor: [30, 10]
        });

        const marker = L.marker([h.lat, h.lng], { icon: icon });

        // Popup Content
        marker.bindPopup(`
            <div style="padding: 6px;">
                <h4 style="color: #00f2fe; margin-bottom: 4px; font-family: Outfit, sans-serif;">${h.name}</h4>
                <div style="font-size: 0.8rem; color: #94a3b8;">City: <strong>${h.city}</strong> (${h.id})</div>
                <div style="font-size: 0.8rem; color: #cbd5e1; margin-top: 4px;">
                    Storage Load: <strong>${h.currentLoad} / ${h.capacity}</strong> (${h.utilization.toFixed(1)}%)
                </div>
                <div style="font-size: 0.8rem; color: #38bdf8;">
                    Sorting Queue: <strong>${h.queueSize} parcels</strong>
                </div>
                <button onclick="handleHubSelection('${h.id}')" style="margin-top: 8px; width: 100%; padding: 4px 8px; background: #00f2fe; color: #050811; border: none; border-radius: 4px; font-weight: 700; cursor: pointer;">
                    Select in Route
                </button>
            </div>
        `);

        marker.on('click', () => {
            handleHubSelection(h.id);
        });

        hubMarkersLayer.addLayer(marker);
    });

    if (activeRoutePath.length > 1) {
        highlightOptimalRouteOnMap(activeRoutePath);
    }
}

function handleHubSelection(hubId) {
    const originSel = document.getElementById('book-origin');
    const destSel = document.getElementById('book-dest');

    if (!selectedOrigin) {
        selectedOrigin = hubId;
        if (originSel) originSel.value = hubId;
    } else {
        selectedDest = hubId;
        if (destSel) destSel.value = hubId;
        selectedOrigin = null;
    }
    updateLiveQuote();
}
window.handleHubSelection = handleHubSelection;

function highlightOptimalRouteOnMap(hubIds) {
    if (!leafletMap) return;

    if (activePathPolyline) {
        leafletMap.removeLayer(activePathPolyline);
        activePathPolyline = null;
    }

    if (!hubIds || hubIds.length < 2) return;

    const hubMap = new Map();
    hubsData.forEach(h => hubMap.set(h.id, h));

    const latLngs = [];
    hubIds.forEach(id => {
        const h = hubMap.get(id);
        if (h) latLngs.push([h.lat, h.lng]);
    });

    if (latLngs.length > 1) {
        activePathPolyline = L.polyline(latLngs, {
            color: '#00f2fe',
            weight: 5,
            opacity: 0.9,
            dashArray: '8, 8',
            className: 'glowing-active-route'
        }).addTo(leafletMap);

        leafletMap.fitBounds(activePathPolyline.getBounds(), { padding: [50, 50], maxZoom: 6 });
    }
}

// --- Booking Shipment ---
async function handleBooking(e) {
    e.preventDefault();
    const sender = document.getElementById('book-sender').value.trim();
    const receiver = document.getElementById('book-receiver').value.trim();
    const origin = document.getElementById('book-origin').value;
    const dest = document.getElementById('book-dest').value;
    const weight = parseFloat(document.getElementById('book-weight').value);
    const priority = document.querySelector('input[name="priority"]:checked').value;

    try {
        const res = await fetch('/api/parcels/book', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ sender, receiver, origin, dest, weight, priority })
        });

        const data = await res.json();
        if (data.success) {
            document.getElementById('modal-code').innerText = data.trackingCode;
            document.getElementById('modal-cost').innerText = `₹${data.cost.toFixed(2)}`;
            document.getElementById('modal-eta').innerText = data.estimatedDelivery;
            document.getElementById('modal-path').innerText = data.route.join(' ➔ ');
            document.getElementById('booking-modal').classList.remove('hidden');

            fetchNetworkData();
        } else {
            alert('Error booking shipment: ' + data.message);
        }
    } catch (err) {
        alert('Booking request failed.');
    }
}

// --- Tracking Parcel ---
async function trackParcel(code) {
    try {
        const res = await fetch(`/api/parcels/track?code=${encodeURIComponent(code)}`);
        const data = await res.json();

        const box = document.getElementById('tracking-result-box');
        if (!data.found) {
            box.classList.remove('hidden');
            let errDiv = document.getElementById('track-err-msg');
            if (!errDiv) {
                errDiv = document.createElement('div');
                errDiv.id = 'track-err-msg';
                box.prepend(errDiv);
            }
            errDiv.style.display = 'block';
            errDiv.innerHTML = `<div style="color: #fb7185; text-align:center; padding: 16px; background: rgba(244, 63, 94, 0.1); border-radius: 8px; margin-bottom: 12px;">❌ ${data.message}</div>`;
            return;
        }

        const errDiv = document.getElementById('track-err-msg');
        if (errDiv) errDiv.style.display = 'none';

        currentTrackingData = data;
        box.classList.remove('hidden');

        document.getElementById('track-disp-code').innerText = data.trackingCode;
        document.getElementById('track-disp-priority').innerText = data.priorityDisplayName;
        document.getElementById('track-disp-status').innerText = data.status;
        document.getElementById('track-disp-route').innerText = `${data.origin} ➔ ${data.dest}`;
        document.getElementById('track-disp-receiver').innerText = `${data.sender} ➔ ${data.receiver}`;
        document.getElementById('track-disp-cost').innerText = `${data.weightKg} kg | ₹${data.cost.toFixed(2)}`;
        document.getElementById('track-disp-eta').innerText = data.estimatedDelivery;

        document.getElementById('track-progress-pct').innerText = `${data.progressPercent}%`;
        document.getElementById('track-progress-bar').style.width = `${data.progressPercent}%`;

        // Checkpoints
        const list = document.getElementById('track-checkpoints-list');
        list.innerHTML = '';
        data.checkpoints.forEach(cp => {
            const item = document.createElement('div');
            item.className = 'timeline-step';
            item.innerHTML = `
                <div class="time">${cp.timestamp}</div>
                <div class="event">${cp.status} at ${cp.hubName}</div>
            `;
            list.appendChild(item);
        });

        // Highlight route on Leaflet Map
        if (data.route && data.route.length > 0) {
            activeRoutePath = data.route;
            highlightOptimalRouteOnMap(data.route);
        }
    } catch (err) {
        console.error('Error tracking parcel:', err);
    }
}
window.quickTrack = function(code) {
    document.getElementById('track-code-input').value = code;
    trackParcel(code);
};

// --- Multi-Hop Simulation with Moving Vehicle ---
async function runSimulation() {
    const code = document.getElementById('sim-parcel-code').value.trim();
    if (!code) return;

    const outBox = document.getElementById('sim-output-container');
    const logsFeed = document.getElementById('sim-logs-feed');
    outBox.classList.remove('hidden');
    logsFeed.innerHTML = '<div style="color: #94a3b8;">Initiating multi-hop dispatch across logistics network...</div>';

    try {
        const res = await fetch(`/api/parcels/simulate?code=${encodeURIComponent(code)}`, { method: 'POST' });
        const data = await res.json();

        if (!data.success) {
            logsFeed.innerHTML += `<div style="color: #fb7185;">❌ ${data.message}</div>`;
            return;
        }

        logsFeed.innerHTML = '';
        for (let i = 0; i < data.steps.length; i++) {
            await new Promise(r => setTimeout(r, 650));
            const step = data.steps[i];
            const div = document.createElement('div');
            div.innerText = step.description;
            logsFeed.appendChild(div);
            logsFeed.scrollTop = logsFeed.scrollHeight;

            // Pan map to current transit hub
            const hub = hubsData.find(h => h.id === step.hubId);
            if (hub && leafletMap) {
                leafletMap.panTo([hub.lat, hub.lng], { animate: true, duration: 0.6 });
            }
        }

        const doneDiv = document.createElement('div');
        doneDiv.style.color = '#34d399';
        doneDiv.style.fontWeight = 'bold';
        doneDiv.innerText = `\n🎉 Simulation Complete! Final Status: ${data.status}`;
        logsFeed.appendChild(doneDiv);

        fetchNetworkData();
        trackParcel(code);
    } catch (err) {
        logsFeed.innerHTML += `<div style="color: #fb7185;">Error executing simulation.</div>`;
    }
}
