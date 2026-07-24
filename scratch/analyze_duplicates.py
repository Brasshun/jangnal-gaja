import csv
from collections import defaultdict
import re

file_path = "app/src/main/assets/markets.csv"

# Load markets
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

out_lines = []
out_lines.append(f"Total rows: {len(markets)}")
out_lines.append(f"Headers: {headers}")

# Group markets by approximate location or base name
# If market names are very similar and within a small distance, we group them.
# Let's first group by base name, then check coordinates.
name_groups = defaultdict(list)
for i, row in enumerate(markets):
    name = row[0]
    # base name cleaning
    base_name = re.sub(r'\s*(주차장|5일장|정기시장|시장주차장|공영주차장|고객편의시설|화장실|고객지원센터|상인회|번영회|편의시설|고객센터|주차).*$', '', name)
    name_groups[base_name].append((i, row))

multi_groups = {k: v for k, v in name_groups.items() if len(v) > 1}
out_lines.append(f"\nGroups with multiple entries: {len(multi_groups)}")

# Print a few examples
count = 0
for base_name, group in sorted(multi_groups.items(), key=lambda x: len(x[1]), reverse=True):
    if count >= 30:
        break
    out_lines.append(f"\nBase Name: {base_name} ({len(group)} entries)")
    for idx, row in group:
        out_lines.append(f"  - [Index {idx}] Name: {row[0]}, Cycle: {row[4]}, Addr: {row[2]}, Lat: {row[5]}, Lon: {row[6]}")
    count += 1

with open("scratch/duplicates_result.txt", "w", encoding="utf-8") as out_f:
    out_f.write("\n".join(out_lines))
print("Analysis finished. Written to scratch/duplicates_result.txt")
