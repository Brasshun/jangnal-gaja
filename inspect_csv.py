
import csv

# Try reading with euc-kr which is standard for older Korean gov data
encodings = ['euc-kr', 'cp949', 'utf-8']

file_path = "app/src/main/assets/markets.csv"

for enc in encodings:
    try:
        print(f"--- Trying encoding: {enc} ---")
        with open(file_path, 'r', encoding=enc) as f:
            reader = csv.reader(f)
            headers = next(reader)
            print("Headers:", headers)
            for i, row in enumerate(reader):
                if i < 3:
                    print(f"Row {i}:", row)
                else:
                    break
        print("\nSuccess!")
        break
    except Exception as e:
        print(f"Failed with {enc}: {e}\n")
