import csv

file_path = "app/src/main/assets/markets.csv"
markets = []

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        headers = next(reader)
        for row in reader:
            if len(row) > 0:
                markets.append(row)
except Exception:
    with open(file_path, 'r', encoding='cp949') as f:
        reader = csv.reader(f)
        headers = next(reader)
        for row in reader:
            if len(row) > 0:
                markets.append(row)

# Let's write the first 100 market names to a file
with open("scratch/first_100_names.txt", "w", encoding="utf-8") as out_f:
    for idx, m in enumerate(markets):
        out_f.write(f"{idx}: {m[0]} | Type: {m[1]} | Addr: {m[2]} | Lat/Lon: {m[5]},{m[6]} | Cycle: {m[4]}\n")

# Find any names containing "주차" or "5일" or other suffixes
with open("scratch/noisy_names.txt", "w", encoding="utf-8") as out_f:
    for idx, m in enumerate(markets):
        name = m[0]
        # Any name that has parentheses, parking lot, or looks like a duplicate
        if "(" in name or ")" in name or "주차" in name or "5일장" in name or "임시" in name or "공영" in name or "화장실" in name or "시장" not in name:
            out_f.write(f"{idx}: {name} | Type: {m[1]} | Addr: {m[2]} | Lat/Lon: {m[5]},{m[6]} | Cycle: {m[4]}\n")

print("Done listing names.")
