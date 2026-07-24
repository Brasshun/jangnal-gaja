import csv

encodings = ['euc-kr', 'cp949', 'utf-8']
file_path = "app/src/main/assets/markets.csv"

for enc in encodings:
    try:
        with open(file_path, 'r', encoding=enc) as f:
            reader = csv.reader(f)
            headers = next(reader)
            
            print("=== 1+5 패턴 찾기 ===\n")
            
            all_rows = list(reader)
            count = 0
            
            for row in all_rows:
                if len(row) > 4:
                    cycle_str = row[4]
                    market_name = row[0]
                    
                    # "1+5" 또는 "0+5" 같은 패턴 찾기
                    if cycle_str in ["1+5", "0+5", "5+0", "0+1+2+3+4+5+6+7+8+9"]:
                        print(f"시장: {market_name}")
                        print(f"주기: {cycle_str}")
                        print()
                        count += 1
                        
                        if count >= 10:
                            break
            
            print(f"\n총 {count}개 발견")
        break
    except Exception as e:
        print(f"Error: {e}")
        continue
