import http.server
import socketserver
import json
import csv
import re
import urllib.parse
from datetime import datetime
import os

PORT = 8081
CSV_PATH = "app/src/main/assets/markets.csv"

# Load markets into memory for the web preview
markets_db = []
headers = []

def load_data():
    global headers, markets_db
    markets_db = []
    encoding = 'utf-8'
    if not os.path.exists(CSV_PATH):
        return
    try:
        with open(CSV_PATH, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            headers = next(reader)
            for i, r in enumerate(reader):
                if r:
                    markets_db.append(parse_row(i, r))
    except Exception:
        with open(CSV_PATH, 'r', encoding='cp949') as f:
            reader = csv.reader(f)
            headers = next(reader)
            for i, r in enumerate(reader):
                if r:
                    markets_db.append(parse_row(i, r))

def parse_row(idx, row):
    name = row[0] if len(row) > 0 else ""
    m_type = row[1] if len(row) > 1 else ""
    road_addr = row[2] if len(row) > 2 else ""
    jibun_addr = row[3] if len(row) > 3 else ""
    cycle = row[4] if len(row) > 4 else ""
    lat = float(row[5]) if len(row) > 5 and row[5] else 37.5665
    lon = float(row[6]) if len(row) > 6 and row[6] else 126.9780
    specialty = row[8] if len(row) > 8 else ""
    toilet = row[11] if len(row) > 11 else "N"
    parking = row[12] if len(row) > 12 else "N"
    phone = row[14] if len(row) > 14 else ""
    
    return {
        "id": idx + 1,
        "marketName": name,
        "marketType": m_type,
        "addressRoad": road_addr,
        "addressJibun": jibun_addr,
        "openingCycle": cycle,
        "latitude": lat,
        "longitude": lon,
        "specialty": specialty,
        "hasToilet": toilet,
        "hasParking": parking,
        "phoneNumber": phone,
        "voteOpenTodayCount": 0,
        "voteClosedTodayCount": 0,
    }

class MarketPreviewHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        # Suppress request log pollution
        pass

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        if url.path == "/api/markets":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.end_headers()
            self.wfile.write(json.dumps(markets_db, ensure_ascii=False).encode('utf-8'))
        elif url.path == "/api/vote":
            # API endpoint to cast a vote
            query = urllib.parse.parse_qs(url.query)
            market_id = int(query.get("id", [0])[0])
            vote_type = query.get("type", ["open"])[0]
            
            success = False
            for m in markets_db:
                if m["id"] == market_id:
                    if vote_type == "open":
                        m["voteOpenTodayCount"] += 1
                    else:
                        m["voteClosedTodayCount"] += 1
                    success = True
                    break
            
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"success": success, "markets": markets_db}).encode('utf-8'))
        elif url.path.startswith("/drawable/"):
            # Serve Android drawable files directly to Leaflet
            file_name = url.path.split("/")[-1]
            file_path = os.path.join("app/src/main/res/drawable", file_name)
            if os.path.exists(file_path):
                self.send_response(200)
                self.send_header("Content-Type", "image/png")
                self.end_headers()
                with open(file_path, 'rb') as img:
                    self.wfile.write(img.read())
            else:
                self.send_error(404, "File not found")
        else:
            # Serve the dashboard page
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(HTML_CONTENT.encode('utf-8'))

HTML_CONTENT = """<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>장날 가자 (Jangnal Gaja) - Web Preview</title>
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&family=Noto+Sans+KR:wght@300;400;700&display=swap" rel="stylesheet">
    <!-- Leaflet.js CSS for Map -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin=""/>
    <style>
        :root {
            --bg-color: #0b0f19;
            --card-bg: rgba(22, 30, 49, 0.7);
            --border-color: rgba(255, 255, 255, 0.08);
            --primary: #ff7b39;
            --primary-glow: rgba(255, 123, 57, 0.4);
            --permanent: #10b981;
            --permanent-glow: rgba(16, 185, 129, 0.3);
            --inactive: #64748b;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --glass-blur: blur(16px);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Outfit', 'Noto Sans KR', sans-serif;
            -webkit-font-smoothing: antialiased;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-main);
            height: 100vh;
            display: flex;
            overflow: hidden;
        }

        /* Layout */
        .sidebar {
            width: 420px;
            height: 100%;
            background: rgba(15, 23, 42, 0.95);
            border-right: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            z-index: 10;
            box-shadow: 10px 0 30px rgba(0, 0, 0, 0.5);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .main-content {
            flex: 1;
            height: 100%;
            position: relative;
            display: flex;
            flex-direction: column;
        }

        #map {
            flex: 1;
            width: 100%;
            height: 100%;
            background: #0f172a;
        }

        /* Sidebar Header */
        .header {
            padding: 24px;
            background: linear-gradient(to bottom, rgba(255,123,57,0.1), transparent);
            border-bottom: 1px solid var(--border-color);
        }

        .header h1 {
            font-size: 24px;
            font-weight: 700;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .header h1 span {
            color: var(--primary);
        }

        .header p {
            font-size: 13px;
            color: var(--text-muted);
            margin-top: 4px;
        }

        /* Search & Tabs */
        .search-box {
            padding: 16px 24px;
            display: flex;
            gap: 8px;
        }

        .search-input {
            flex: 1;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-color);
            padding: 12px 16px;
            border-radius: 12px;
            color: var(--text-main);
            font-size: 14px;
            outline: none;
            transition: all 0.2s;
        }

        .search-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 10px var(--primary-glow);
        }

        .tabs {
            display: flex;
            padding: 0 24px 12px 24px;
            border-bottom: 1px solid var(--border-color);
            gap: 8px;
            overflow-x: auto;
        }

        .tab {
            padding: 8px 16px;
            background: transparent;
            border: none;
            color: var(--text-muted);
            font-weight: 600;
            font-size: 13px;
            cursor: pointer;
            border-radius: 8px;
            white-space: nowrap;
            transition: all 0.2s;
        }

        .tab.active {
            background: rgba(255, 123, 57, 0.15);
            color: var(--primary);
        }

        /* Market List */
        .list-container {
            flex: 1;
            overflow-y: auto;
            padding: 16px 24px;
        }

        .market-card {
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 16px;
            margin-bottom: 12px;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            backdrop-filter: var(--glass-blur);
        }

        .market-card:hover {
            transform: translateY(-2px);
            border-color: rgba(255, 123, 57, 0.3);
            box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
        }

        .market-card.selected {
            border-color: var(--primary);
            background: rgba(255, 123, 57, 0.05);
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
        }

        .market-name {
            font-size: 16px;
            font-weight: 700;
            color: var(--text-main);
        }

        .status-badge {
            font-size: 11px;
            padding: 4px 8px;
            border-radius: 6px;
            font-weight: 700;
        }

        .status-badge.open {
            background: rgba(255, 123, 57, 0.15);
            color: var(--primary);
        }

        .status-badge.permanent {
            background: rgba(16, 185, 129, 0.15);
            color: var(--permanent);
        }

        .status-badge.closed {
            background: rgba(255, 255, 255, 0.05);
            color: var(--text-muted);
        }

        .market-desc {
            font-size: 13px;
            color: var(--text-muted);
            margin: 8px 0;
        }

        .market-addr {
            font-size: 12px;
            color: var(--text-muted);
            display: flex;
            align-items: center;
            gap: 4px;
        }

        /* Detail Panel */
        .detail-panel {
            position: absolute;
            top: 24px;
            right: 24px;
            width: 440px;
            max-height: calc(100% - 48px);
            background: rgba(15, 23, 42, 0.95);
            border: 1px solid var(--border-color);
            border-radius: 24px;
            z-index: 100;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
            display: none;
            flex-direction: column;
            overflow-y: auto;
            backdrop-filter: var(--glass-blur);
            animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        @keyframes slideIn {
            from { transform: translateX(50px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }

        .detail-header {
            padding: 24px;
            border-bottom: 1px solid var(--border-color);
            position: relative;
        }

        .close-btn {
            position: absolute;
            top: 24px;
            right: 24px;
            background: rgba(255,255,255,0.05);
            border: none;
            color: var(--text-main);
            width: 32px;
            height: 32px;
            border-radius: 50%;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
        }

        .detail-body {
            padding: 24px;
        }

        .section-title {
            font-size: 16px;
            font-weight: 700;
            margin-bottom: 12px;
            color: var(--text-main);
            border-left: 3px solid var(--primary);
            padding-left: 8px;
        }

        /* Amenity Grid */
        .amenity-row {
            display: flex;
            gap: 12px;
            margin-bottom: 24px;
        }

        .amenity-card {
            flex: 1;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 16px;
            text-align: center;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 4px;
        }

        .amenity-card.active {
            background: rgba(255, 123, 57, 0.05);
            border-color: rgba(255, 123, 57, 0.3);
        }

        .amenity-icon {
            font-size: 24px;
        }

        .amenity-label {
            font-size: 13px;
            font-weight: 700;
            color: var(--text-main);
        }

        .amenity-status {
            font-size: 11px;
            color: var(--text-muted);
        }

        /* Mini Calendar Grid */
        .calendar-view {
            background: rgba(255, 255, 255, 0.02);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 16px;
            margin-bottom: 24px;
        }

        .calendar-header {
            font-size: 13px;
            font-weight: 700;
            color: var(--text-muted);
            margin-bottom: 12px;
            text-align: center;
        }

        .calendar-weekdays {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
            text-align: center;
            font-size: 11px;
            color: var(--text-muted);
            font-weight: 700;
            margin-bottom: 8px;
        }

        .calendar-days {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
            gap: 4px;
        }

        .cal-day {
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: 600;
            border-radius: 6px;
        }

        .cal-day.empty {
            visibility: hidden;
        }

        .cal-day.market {
            background: var(--primary-glow);
            color: var(--primary);
            border: 1px solid rgba(255, 123, 57, 0.3);
        }

        .cal-day.permanent {
            background: var(--permanent-glow);
            color: var(--permanent);
            border: 1px solid rgba(16, 185, 129, 0.3);
        }

        .cal-day.today {
            background: var(--primary) !important;
            color: white !important;
        }

        /* Voting System */
        .vote-section {
            background: rgba(255, 255, 255, 0.02);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 16px;
            margin-bottom: 24px;
        }

        .vote-desc {
            font-size: 12px;
            color: var(--text-muted);
            margin-bottom: 12px;
        }

        .vote-btns {
            display: flex;
            gap: 12px;
        }

        .vote-btn {
            flex: 1;
            padding: 12px;
            background: transparent;
            border: 1px solid var(--border-color);
            border-radius: 12px;
            color: var(--text-main);
            font-weight: 700;
            font-size: 13px;
            cursor: pointer;
            transition: all 0.2s;
        }

        .vote-btn.open {
            border-color: var(--primary);
            color: var(--primary);
        }

        .vote-btn.open:hover {
            background: rgba(255, 123, 57, 0.05);
        }

        .vote-btn.closed {
            border-color: #ef4444;
            color: #ef4444;
        }

        .vote-btn.closed:hover {
            background: rgba(239, 68, 68, 0.05);
        }

        /* Legend Panel */
        .legend-container {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 16px;
        }
        
        .legend-item {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
        }

        .legend-icon {
            width: 20px;
            height: 20px;
        }
    </style>
</head>
<body>

    <!-- Sidebar -->
    <div class="sidebar">
        <div class="header">
            <h1>🏪 장날 <span>가자</span></h1>
            <p>전국 전통시장 및 실시간 5일장 예측 안내</p>
        </div>
        
        <div class="search-box">
            <input type="text" id="search-input" class="search-input" placeholder="시장 이름 또는 주소 검색...">
        </div>

        <div class="tabs">
            <button class="tab active" onclick="switchTab('all')">전체 시장</button>
            <button class="tab" onclick="switchTab('today')">오늘 열리는 장</button>
            <button class="tab" onclick="switchTab('permanent')">상설 시장</button>
        </div>

        <div class="list-container" id="list-container">
            <!-- Market Cards injected here -->
        </div>
    </div>

    <!-- Main Content (Map) -->
    <div class="main-content">
        <div id="map"></div>

        <!-- Detail Sheet (Compose Simulator) -->
        <div class="detail-panel" id="detail-panel">
            <div class="detail-header">
                <h2 id="detail-name" style="font-size: 22px; font-weight: 700; margin-right: 40px;">중앙시장</h2>
                <div id="detail-type" style="margin-top: 8px;"></div>
                <button class="close-btn" onclick="closeDetail()">&times;</button>
            </div>
            
            <div class="detail-body">
                <!-- 1. Calendar predicting dates -->
                <div class="section-title">📅 개장 정보 및 예측 달력</div>
                <div class="calendar-view">
                    <div class="calendar-header" id="calendar-month-label">2026년 7월 예측 달력</div>
                    <div class="calendar-weekdays">
                        <div>일</div><div>월</div><div>화</div><div>수</div><div>목</div><div>금</div><div>토</div>
                    </div>
                    <div class="calendar-days" id="calendar-days-grid">
                        <!-- Days injected here -->
                    </div>
                </div>

                <!-- 2. Amenities Row -->
                <div class="section-title">🏗 편의 시설</div>
                <div class="amenity-row">
                    <div class="amenity-card" id="amenity-toilet">
                        <div class="amenity-icon">🚻</div>
                        <div class="amenity-label">공중화장실</div>
                        <div class="amenity-status" id="toilet-status">있음</div>
                    </div>
                    <div class="amenity-card" id="amenity-parking">
                        <div class="amenity-icon">🅿️</div>
                        <div class="amenity-label">주차 공간</div>
                        <div class="amenity-status" id="parking-status">있음</div>
                    </div>
                </div>

                <!-- 3. Realtime crowdsourced vote -->
                <div class="section-title">💬 실시간 장날 제보</div>
                <div class="vote-section">
                    <div class="vote-desc">오늘 시장이 열렸는지 실시간 정보를 제보해 주세요!</div>
                    <div class="vote-btns">
                        <button class="vote-btn open" onclick="castVote('open')">👍 오늘 열렸어요 (<span id="vote-open-count">0</span>명)</button>
                        <button class="vote-btn closed" onclick="castVote('closed')">👎 닫혔어요 (<span id="vote-closed-count">0</span>명)</button>
                    </div>
                </div>

                <!-- 4. Additional Info -->
                <div class="section-title">📍 상세 위치 및 정보</div>
                <div style="font-size: 14px; color: var(--text-muted); line-height: 1.6; margin-bottom: 24px;">
                    <div style="margin-bottom: 8px;"><strong>도로명 주소:</strong> <span id="detail-road-addr"></span></div>
                    <div style="margin-bottom: 8px;"><strong>지번 주소:</strong> <span id="detail-jibun-addr"></span></div>
                    <div style="margin-bottom: 8px;"><strong>전화번호:</strong> <span id="detail-phone"></span></div>
                    <div><strong>주요 품목:</strong> <span id="detail-specialty"></span></div>
                </div>
            </div>
        </div>

        <!-- Legend Card -->
        <div style="position: absolute; bottom: 24px; left: 24px; background: rgba(15, 23, 42, 0.9); border: 1px solid var(--border-color); border-radius: 16px; padding: 12px 18px; z-index: 100; backdrop-filter: var(--glass-blur);">
            <div class="legend-container">
                <div class="legend-item">
                    <img class="legend-icon" src="/drawable/ic_marker_today.png">
                    <span>오늘 장날 (주황)</span>
                </div>
                <div class="legend-item">
                    <img class="legend-icon" src="/drawable/ic_marker_permanent.png">
                    <span>상설 시장 (연두)</span>
                </div>
                <div class="legend-item">
                    <img class="legend-icon" src="/drawable/ic_marker_normal.png">
                    <span>쉬는 날 (회색)</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
    <script>
        let markets = [];
        let map;
        let markersGroup;
        let currentTab = 'all';
        let searchQuery = '';
        let selectedMarket = null;

        // Leaflet Icons loading the exact native Android drawables
        const todayIcon = L.icon({
            iconUrl: '/drawable/ic_marker_today.png',
            iconSize: [36, 36],
            iconAnchor: [18, 36]
        });

        const permanentIcon = L.icon({
            iconUrl: '/drawable/ic_marker_permanent.png',
            iconSize: [36, 36],
            iconAnchor: [18, 36]
        });

        const normalIcon = L.icon({
            iconUrl: '/drawable/ic_marker_normal.png',
            iconSize: [36, 36],
            iconAnchor: [18, 36]
        });

        // Initialize Map
        function initMap() {
            // Center of Korea
            map = L.map('map', {zoomControl: false}).setView([36.2681, 127.8924], 8);
            
            // Add zoom control at topright
            L.control.zoom({position: 'topright'}).addTo(map);

            // Use CartoDB Dark Matter tile layer for premium dark aesthetics
            L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                attribution: '&copy; CartoDB &copy; OpenStreetMap contributors'
            }).addTo(map);

            markersGroup = L.layerGroup().addTo(map);
        }

        // Fetch markets
        async function fetchMarkets() {
            const res = await fetch('/api/markets');
            markets = await res.json();
            renderList();
            renderMarkers();
        }

        // Cycle parsing logic in JS matching Kotlin logic
        function isPermanent(openingCycle) {
            if (!openingCycle) return false;
            return openingCycle.includes("매일") || openingCycle.includes("상설") || openingCycle.includes("0+1+2");
        }

        function parseCycle(openingCycle) {
            const digits = (openingCycle.match(/\\d+/g) || []).map(Number);
            const startDays = [...new Set(digits)].filter(n => n > 0 && n <= 31).sort((a,b) => a-b);
            if (startDays.length >= 2) {
                return { cycle: startDays[1] - startDays[0], startDays };
            }
            return { cycle: null, startDays };
        }

        function getMarketDaysInMonth(openingCycle, maxDay = 31) {
            if (isPermanent(openingCycle)) {
                return Array.from({length: maxDay}, (_, i) => i + 1);
            }
            const { cycle, startDays } = parseCycle(openingCycle);
            if (startDays.length === 0) return [];
            if (cycle === null || cycle <= 0) return startDays.filter(d => d <= maxDay);
            
            const allDays = new Set();
            for (let start of startDays) {
                let day = start;
                while (day <= maxDay) {
                    allDays.add(day);
                    day += cycle;
                }
            }
            return Array.from(allDays).sort((a,b) => a-b);
        }

        function isOpenOn(openingCycle, date) {
            if (isPermanent(openingCycle)) return true;
            const targetDay = date.getDate();
            const daysInMonth = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
            const marketDays = getMarketDaysInMonth(openingCycle, daysInMonth);
            return marketDays.includes(targetDay);
        }

        // Render functions
        function renderList() {
            const container = document.getElementById('list-container');
            container.innerHTML = '';

            const today = new Date();
            const filtered = markets.filter(m => {
                // Search filter
                const matchesSearch = searchQuery === '' || 
                    m.marketName.toLowerCase().includes(searchQuery) ||
                    m.addressRoad.toLowerCase().includes(searchQuery) ||
                    m.addressJibun.toLowerCase().includes(searchQuery);

                if (!matchesSearch) return false;

                // Tab filter
                if (currentTab === 'today') {
                    return isOpenOn(m.openingCycle, today) && !isPermanent(m.openingCycle);
                } else if (currentTab === 'permanent') {
                    return isPermanent(m.openingCycle);
                }
                return true;
            });

            filtered.forEach(m => {
                const card = document.createElement('div');
                card.className = `market-card ${selectedMarket && selectedMarket.id === m.id ? 'selected' : ''}`;
                
                const isPerm = isPermanent(m.openingCycle);
                const isOpenToday = isOpenOn(m.openingCycle, new Date());
                
                let badgeClass = 'closed';
                let badgeText = '휴장일 😴';
                
                if (isPerm) {
                    badgeClass = 'permanent';
                    badgeText = '상설시장 🟢';
                } else if (isOpenToday) {
                    badgeClass = 'open';
                    badgeText = '오늘 장날! 🔥';
                }

                card.innerHTML = `
                    <div class="card-header">
                        <div class="market-name">${m.marketName}</div>
                        <span class="status-badge ${badgeClass}">${badgeText}</span>
                    </div>
                    <div class="market-desc">${isPerm ? '매일 운영' : m.openingCycle + ' 주기'}</div>
                    <div class="market-addr">📍 ${m.addressRoad || m.addressJibun}</div>
                `;

                card.onclick = () => selectMarket(m);
                container.appendChild(card);
            });
        }

        function renderMarkers() {
            markersGroup.clearLayers();
            const today = new Date();

            markets.forEach(m => {
                if (m.latitude && m.longitude) {
                    const isPerm = isPermanent(m.openingCycle);
                    const isOpenToday = isOpenOn(m.openingCycle, today);
                    
                    let icon = normalIcon; // Gray sun face (closed today)
                    if (isPerm) {
                        icon = permanentIcon; // Green sun face (permanent)
                    } else if (isOpenToday) {
                        icon = todayIcon; // Orange sun face (open today)
                    }

                    const marker = L.marker([m.latitude, m.longitude], { icon: icon });
                    marker.on('click', () => selectMarket(m));
                    markersGroup.addLayer(marker);
                }
            });
        }

        function selectMarket(m) {
            selectedMarket = m;
            renderList();

            // Pan map
            if (m.latitude && m.longitude) {
                map.setView([m.latitude, m.longitude], 14);
            }

            // Fill Detail Panel
            document.getElementById('detail-name').innerText = m.marketName;
            
            const isPerm = isPermanent(m.openingCycle);
            const detailType = document.getElementById('detail-type');
            detailType.innerHTML = '';
            
            const badge = document.createElement('span');
            badge.className = `status-badge ${isPerm ? 'permanent' : isOpenOn(m.openingCycle, new Date()) ? 'open' : 'closed'}`;
            badge.innerText = isPerm ? '상설시장' : m.openingCycle + ' 주기';
            detailType.appendChild(badge);

            // Amenities
            const toilet = document.getElementById('amenity-toilet');
            const toiletStatus = document.getElementById('toilet-status');
            if (m.hasToilet === 'Y') {
                toilet.classList.add('active');
                toiletStatus.innerText = '이용 가능';
            } else {
                toilet.classList.remove('active');
                toiletStatus.innerText = '정보 없음';
            }

            const parking = document.getElementById('amenity-parking');
            const parkingStatus = document.getElementById('parking-status');
            if (m.hasParking === 'Y') {
                parking.classList.add('active');
                parkingStatus.innerText = '이용 가능';
            } else {
                parking.classList.remove('active');
                parkingStatus.innerText = '정보 없음';
            }

            // Addresses
            document.getElementById('detail-road-addr').innerText = m.addressRoad || '정보 없음';
            document.getElementById('detail-jibun-addr').innerText = m.addressJibun || '정보 없음';
            document.getElementById('detail-phone').innerText = m.phoneNumber || '정보 없음';
            document.getElementById('detail-specialty').innerText = m.specialty ? m.specialty.replace('+', ', ') : '정보 없음';

            // Vote Counts
            document.getElementById('vote-open-count').innerText = m.voteOpenTodayCount;
            document.getElementById('vote-closed-count').innerText = m.voteClosedTodayCount;

            // Render Calendar
            renderCalendar(m);

            // Open panel
            document.getElementById('detail-panel').style.display = 'flex';
        }

        function renderCalendar(m) {
            const calendarGrid = document.getElementById('calendar-days-grid');
            calendarGrid.innerHTML = '';
            
            const today = new Date();
            const year = today.getFullYear();
            const month = today.getMonth(); // 0-indexed
            
            document.getElementById('calendar-month-label').innerText = `${year}년 ${month + 1}월 장날 예측 달력`;

            const maxDays = new Date(year, month + 1, 0).getDate();
            const firstDayOfWeek = new Date(year, month, 1).getDay(); // 0: Sun, 6: Sat
            
            const marketDays = new Set(getMarketDaysInMonth(m.openingCycle, maxDays));
            const isPerm = isPermanent(m.openingCycle);

            // Empty cells before 1st of month
            for (let i = 0; i < firstDayOfWeek; i++) {
                const emptyCell = document.createElement('div');
                emptyCell.className = 'cal-day empty';
                calendarGrid.appendChild(emptyCell);
            }

            // Calendar days
            for (let day = 1; day <= maxDays; day++) {
                const dayCell = document.createElement('div');
                dayCell.className = 'cal-day';
                dayCell.innerText = day;

                const isMarket = marketDays.has(day);
                const isToday = day === today.getDate();

                if (isPerm) {
                    dayCell.classList.add('permanent');
                } else if (isMarket) {
                    dayCell.classList.add('market');
                }

                if (isToday) {
                    dayCell.classList.add('today');
                }

                calendarGrid.appendChild(dayCell);
            }
        }

        async function castVote(type) {
            if (!selectedMarket) return;
            const res = await fetch(`/api/vote?id=${selectedMarket.id}&type=${type}`);
            const data = await res.json();
            if (data.success) {
                // Update in local array
                const updated = data.markets.find(m => m.id === selectedMarket.id);
                if (updated) {
                    selectedMarket.voteOpenTodayCount = updated.voteOpenTodayCount;
                    selectedMarket.voteClosedTodayCount = updated.voteClosedTodayCount;
                    document.getElementById('vote-open-count').innerText = selectedMarket.voteOpenTodayCount;
                    document.getElementById('vote-closed-count').innerText = selectedMarket.voteClosedTodayCount;
                    
                    // Update main markets array
                    markets = data.markets;
                }
            }
        }

        function closeDetail() {
            document.getElementById('detail-panel').style.display = 'none';
            selectedMarket = null;
            renderList();
        }

        function switchTab(tab) {
            currentTab = tab;
            document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
            event.target.classList.add('active');
            renderList();
        }

        document.getElementById('search-input').oninput = (e) => {
            searchQuery = e.target.value.toLowerCase();
            renderList();
        };

        // Boot
        initMap();
        fetchMarkets();
    </script>
</body>
</html>
"""

def run_server():
    load_data()
    handler = MarketPreviewHandler
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), handler) as httpd:
        print("===========================================================")
        print("Jangnal Gaja Web Preview Server Started!")
        print(f"Open in browser: http://localhost:{PORT}")
        print("===========================================================")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server.")

if __name__ == "__main__":
    run_server()
