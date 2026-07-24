import csv
import re
from math import radians, cos, sin, asin, sqrt

file_path = "app/src/main/assets/markets.csv"

# Load markets
markets = []
try:
    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        headers = next(reader)
        for i, row in enumerate(reader):
            if len(row) > 0:
                markets.append((i, row))
except Exception:
    with open(file_path, 'r', encoding='cp949') as f:
        reader = csv.reader(f)
        headers = next(reader)
        for i, row in enumerate(reader):
            if len(row) > 0:
                markets.append((i, row))

def haversine(lon1, lat1, lon2, lat2):
    """Calculate the great circle distance between two points in meters"""
    lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
    c = 2 * asin(sqrt(a))
    r = 6371000 # Radius of earth in meters
    return c * r

out = []
# Find keywords in names
keywords = ["주차", "화장실", "공영", "5일장", "상인", "번영", "지원", "편의", "안내"]
special_entries = []
for idx, row in markets:
    name = row[0]
    if any(k in name for k in keywords):
        special_entries.append((idx, row))

out.append(f"Total entries with keywords in name: {len(special_entries)}")

# Now find if there's another market close by (e.g. < 500m) with a similar name
matched_pairs = []
for s_idx, s_row in special_entries:
    s_name = s_row[0]
    try:
        s_lat = float(s_row[5])
        s_lon = float(s_row[6])
    except ValueError:
        continue
    
    # Extract base name candidate
    s_base = re.sub(r'\s*(주차장|5일장|정기시장|시장주차장|공영주차장|고객편의시설|화장실|고객지원센터|상인회|번영회|편의시설|고객센터|주차).*$', '', s_name)
    
    for idx, row in markets:
        if idx == s_idx:
            continue
        name = row[0]
        try:
            lat = float(row[5])
            lon = float(row[6])
        except ValueError:
            continue
        
        # Check distance
        dist = haversine(s_lon, s_lat, lon, lat)
        if dist < 500: # within 500 meters
            # Check name similarity (e.g., base name is in the other name, or vice versa)
            if s_base in name or name in s_base or name in s_name:
                matched_pairs.append((s_row, row, dist))

out.append(f"Found {len(matched_pairs)} potential duplicate/closely related pairs:")
for s_row, row, dist in matched_pairs[:30]:
    out.append(f"\n- Pair (Distance: {dist:.1f}m):")
    out.append(f"  A: {s_row[0]} (Type: {s_row[1]}, Cycle: {s_row[4]}, Lat: {s_row[5]}, Lon: {s_row[6]}, Parking: {s_row[12]}, Toilet: {s_row[11]})")
    out.append(f"  B: {row[0]} (Type: {row[1]}, Cycle: {row[4]}, Lat: {row[5]}, Lon: {row[6]}, Parking: {row[12]}, Toilet: {row[11]})")

with open("scratch/close_duplicates.txt", "w", encoding="utf-8") as out_f:
    out_f.write("\n".join(out).encode('utf-8', 'replace').decode('utf-8'))

print("Finished searching close duplicates.")
