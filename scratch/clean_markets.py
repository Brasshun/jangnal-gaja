import csv
import re
from math import radians, cos, sin, asin, sqrt

def haversine(lon1, lat1, lon2, lat2):
    """Calculate the great circle distance between two points in meters"""
    if lon1 is None or lat1 is None or lon2 is None or lat2 is None:
        return 999999.0
    lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
    c = 2 * asin(sqrt(a))
    r = 6371000 # Radius of earth in meters
    return c * r

def clean_name(name):
    # Remove text in parentheses
    cleaned = re.sub(r'\(.*?\)', '', name)
    # Remove common suffixes that indicate duplicates/facilities
    cleaned = re.sub(r'\s*(주차장|5일장|정기시장|시장주차장|공영주차장|고객편의시설|화장실|고객지원센터|상인회|번영회|편의시설|고객센터|주차|사무실|관리실|공영주차).*$', '', cleaned)
    return cleaned.strip()

def parse_float(val):
    try:
        return float(val) if val else None
    except ValueError:
        return None

def is_yes(val):
    if not val:
        return False
    val = val.strip().upper()
    return val in ["Y", "네", "YES", "1", "TRUE"]

def main():
    input_file = "app/src/main/assets/markets.csv"
    output_file = "app/src/main/assets/markets.csv" # Overwrite
    backup_file = "app/src/main/assets/markets_backup.csv"
    
    rows = []
    headers = []
    
    # Try reading with utf-8 or cp949
    encoding = 'utf-8'
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            headers = next(reader)
            for r in reader:
                if r:
                    rows.append(r)
    except Exception:
        encoding = 'cp949'
        with open(input_file, 'r', encoding='cp949') as f:
            reader = csv.reader(f)
            headers = next(reader)
            for r in reader:
                if r:
                    rows.append(r)
                    
    print(f"Loaded {len(rows)} markets using {encoding} encoding.")
    
    # Save a backup first
    with open(backup_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        writer.writerows(rows)
    print(f"Created backup at {backup_file}")
    
    merged_markets = []
    merge_count = 0
    
    for idx, row in enumerate(rows):
        name = row[0]
        m_type = row[1]
        road_addr = row[2]
        jibun_addr = row[3]
        cycle = row[4]
        lat = parse_float(row[5])
        lon = parse_float(row[6])
        specialty = row[8]
        toilet = "Y" if is_yes(row[11]) else "N"
        parking = "Y" if is_yes(row[12]) else "N"
        phone = row[14]
        
        base_name = clean_name(name)
        
        # Check if we can merge with an existing market in merged_markets
        merged = False
        for m in merged_markets:
            m_lat = parse_float(m[5])
            m_lon = parse_float(m[6])
            
            # Distance check
            dist = haversine(lon, lat, m_lon, m_lat)
            
            # Base name match
            m_base_name = clean_name(m[0])
            name_match = (base_name in m_base_name) or (m_base_name in base_name)
            
            # Address similarity check (same city/gu/dong/eup/myeon)
            # Just base name + location within 250m is a very strong signal.
            if dist < 250 and name_match:
                # Merge!
                merge_count += 1
                
                # 1. Name: Prefer the cleaner/shorter name, or if one is the base name, use that
                if len(base_name) < len(m_base_name) and base_name:
                    m[0] = base_name
                
                # 2. Type & Cycle:
                # If one is 상설/매일 and the other is periodic, combine them
                curr_type = m[1]
                new_type = m_type
                
                curr_cycle = m[4]
                new_cycle = cycle
                
                # Merge type
                types = set(curr_type.split("+") + new_type.split("+"))
                # Clean up types
                types = {t.strip() for t in types if t.strip()}
                m[1] = "+".join(sorted(list(types)))
                
                # Merge cycle
                # If one has cycle "매일" or "상설" and another has digits (e.g. "2+7"), make it "상설+2+7"
                cycles = set()
                for c in [curr_cycle, new_cycle]:
                    if c:
                        c_clean = c.replace("시장", "").replace("장", "").strip()
                        cycles.add(c_clean)
                
                has_perm = any("매일" in c or "상설" in c for c in cycles)
                periodic_cycles = [c for c in cycles if any(d.isdigit() for d in c)]
                
                if has_perm and periodic_cycles:
                    m[4] = f"상설장+{periodic_cycles[0]}"
                elif has_perm:
                    m[4] = "상설시장"
                elif periodic_cycles:
                    m[4] = periodic_cycles[0]
                
                # 3. Amenities
                if toilet == "Y":
                    m[11] = "Y"
                if parking == "Y":
                    m[12] = "Y"
                
                # 4. Specialty
                spec_set = set()
                for s in [m[8], specialty]:
                    if s:
                        # Split by comma or plus
                        parts = re.split(r'[,+]', s)
                        for p in parts:
                            p_clean = p.strip()
                            if p_clean and p_clean != "N/A" and p_clean != "없음":
                                spec_set.add(p_clean)
                m[8] = "+".join(sorted(list(spec_set))) if spec_set else ""
                
                # 5. Phone
                if not m[14] and phone:
                    m[14] = phone
                
                # 6. Latitude/Longitude: If A contains "주차장", keep B's coordinates
                if "주차장" in name or "화장실" in name or "공영" in name:
                    # Keep original coordinates (already in m)
                    pass
                else:
                    # Update to B's coordinates if B is a main market name
                    if lat and lon:
                        m[5] = str(lat)
                        m[6] = str(lon)
                
                merged = True
                break
                
        if not merged:
            # Add to list (copy row elements to avoid referencing issues)
            new_row = list(row)
            # Normalize Toilet & Parking representation
            new_row[11] = "Y" if toilet == "Y" else "N"
            new_row[12] = "Y" if parking == "Y" else "N"
            merged_markets.append(new_row)
            
    print(f"Deduplicated: {len(rows)} -> {len(merged_markets)} markets. Merged {merge_count} rows.")
    
    # Write output to CSV in UTF-8
    with open(output_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        writer.writerows(merged_markets)
        
    print(f"Successfully wrote clean CSV to {output_file}")

if __name__ == "__main__":
    main()
