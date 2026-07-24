import os
import sys
import json
import requests
import re

# Read API_KEY from environment variables
api_key = os.environ.get("API_KEY")
if not api_key:
    print("Error: API_KEY environment variable is not set.")
    sys.exit(1)

# API Endpoint for national traditional market standard data
API_URL = "http://api.data.go.kr/openapi/tn_pubr_public_trdit_mrkt_api"

def fetch_all_markets():
    markets = []
    page = 1
    num_rows = 500
    
    while True:
        print(f"Fetching page {page}...")
        params = {
            "serviceKey": api_key,
            "pageNo": page,
            "numOfRows": num_rows,
            "type": "json"
        }
        
        try:
            response = requests.get(API_URL, params=params, timeout=15)
            if response.status_code != 200:
                print(f"Error fetching data: HTTP {response.status_code}")
                break
                
            data = response.json()
            body = data.get("response", {}).get("body", {})
            items = body.get("items", [])
            
            if not items:
                break
                
            markets.extend(items)
            
            total_count = int(body.get("totalCount", 0))
            print(f"Retrieved {len(markets)} / {total_count} items.")
            
            if len(markets) >= total_count or len(items) < num_rows:
                break
                
            page += 1
        except Exception as e:
            print("Exception during fetch:", e)
            break
            
    return markets

def clean_cycle(cycle_raw):
    if not cycle_raw:
        return "상설시장"
    if "매일" in cycle_raw or "상설" in cycle_raw or "0+1+2" in cycle_raw:
        return "상설시장"
        
    # Extract digits and clean cycle format like "1+6"
    digits = re.findall(r"\d+", cycle_raw)
    digits = sorted(list(set(map(int, digits))))
    if len(digits) >= 1:
        # Check if it has consecutive 1 to 9 (meaning daily)
        if len(digits) >= 8:
            return "상설시장"
        return "+".join(map(str, digits))
    return cycle_raw

def clean_specialty(spec):
    if not spec or spec == "null" or spec == "N" or spec == "Y":
        return ""
    return spec.replace("+", ", ")

def clean_and_deduplicate(raw_items):
    cleaned = []
    seen = {} # key: (name, clean_road_address)
    
    for item in raw_items:
        name = item.get("mrktNm", "").strip()
        if not name:
            continue
            
        # Filter out obvious helper facilities
        if any(x in name for x in ["주차장", "화장실", "고객지원센터", "상인회", "사무실"]):
            continue
            
        # Clean name by stripping common suffixes
        clean_name = re.sub(r"\s*(주차장|5일장|오일장|상설시장|전통시장|시장)$", "", name).strip()
        if not clean_name:
            clean_name = name
            
        road_addr = item.get("rdnmadr", "") or ""
        jibun_addr = item.get("lnmadr", "") or ""
        addr = road_addr if road_addr else jibun_addr
        
        # Clean address key to merge near-identical locations
        addr_clean = re.sub(r"\s+", "", addr)[:10] # first 10 chars without whitespace
        
        lat = float(item.get("latitude") or 37.5665)
        lon = float(item.get("longitude") or 126.9780)
        
        cycle = clean_cycle(item.get("opnngCycle", ""))
        specialty = clean_specialty(item.get("prdlctCtgry", ""))
        toilet = "Y" if "Y" in (item.get("publicToiletYn") or "") else "N"
        parking = "Y" if "Y" in (item.get("prkplceYn") or "") else "N"
        phone = item.get("phoneNumber", "") or ""
        
        key = (clean_name, addr_clean)
        
        if key in seen:
            # Merge logic: if either has toilet/parking/phone, preserve it
            idx = seen[key]
            existing = cleaned[idx]
            if existing["hasToilet"] == "N" and toilet == "Y":
                existing["hasToilet"] = "Y"
            if existing["hasParking"] == "N" and parking == "Y":
                existing["hasParking"] = "Y"
            if not existing["phoneNumber"] and phone:
                existing["phoneNumber"] = phone
            if len(specialty) > len(existing["specialty"]):
                existing["specialty"] = specialty
        else:
            market_obj = {
                "marketName": clean_name,
                "addressRoad": road_addr,
                "addressJibun": jibun_addr,
                "latitude": lat,
                "longitude": lon,
                "openingCycle": cycle,
                "specialty": specialty,
                "hasToilet": toilet,
                "hasParking": parking,
                "phoneNumber": phone
            }
            cleaned.append(market_obj)
            seen[key] = len(cleaned) - 1
            
    return cleaned

def main():
    raw_markets = fetch_all_markets()
    if not raw_markets:
        print("No markets fetched. Exiting.")
        sys.exit(1)
        
    print(f"Fetched {len(raw_markets)} raw markets. Cleaning...")
    cleaned_markets = clean_and_deduplicate(raw_markets)
    print(f"Cleaned and merged into {len(cleaned_markets)} markets.")
    
    # Save output to scratch directory
    output_path = "scratch/markets_clean.json"
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(cleaned_markets, f, ensure_ascii=False, indent=2)
    print(f"Successfully saved to {output_path}")

if __name__ == "__main__":
    main()
