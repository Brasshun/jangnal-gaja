import csv
from collections import defaultdict
import re

def parse_cycle(cycle_str):
    """주기 문자열을 파싱하여 시작일 리스트 반환"""
    if not cycle_str or cycle_str == '상설' or cycle_str == '매일':
        return None, []
    
    # "1+6", "2+7+12" 같은 형식에서 숫자 추출
    numbers = re.findall(r'\d+', cycle_str)
    if not numbers:
        return None, []
    
    start_days = [int(n) for n in numbers]
    
    # 주기 계산 (첫 번째와 두 번째 숫자의 차이)
    if len(start_days) >= 2:
        cycle = start_days[1] - start_days[0]
        return cycle, start_days
    
    return None, start_days

def generate_market_days(cycle, start_days, max_day=31):
    """주기와 시작일로부터 한 달 동안의 장날 생성"""
    all_days = set()
    for start in start_days:
        day = start
        while day <= max_day:
            all_days.add(day)
            day += cycle
    return sorted(all_days)

def format_cycle_smart(cycle_str):
    """주기를 사용자 친화적으로 표시"""
    cycle, start_days = parse_cycle(cycle_str)
    
    if cycle is None and not start_days:
        return "매일 열림"
    
    if cycle is None:
        return f"{', '.join(map(str, start_days))}일"
    
    # 주기별 명칭
    cycle_names = {
        3: "3일장",
        4: "4일장", 
        5: "5일장",
        6: "6일장",
        7: "7일장",
        10: "10일장"
    }
    
    cycle_name = cycle_names.get(cycle, f"{cycle}일 주기")
    
    # 시작일 표시
    if len(start_days) == 1:
        return f"{cycle_name} (매월 {start_days[0]}일부터)"
    elif len(start_days) == 2:
        return f"{cycle_name} ({start_days[0]}일 또는 {start_days[1]}일부터)"
    else:
        start_str = ', '.join(map(str, start_days))
        return f"{cycle_name} ({start_str}일부터)"

def format_cycle_with_dates(cycle_str):
    """주기를 날짜 예시와 함께 표시"""
    cycle, start_days = parse_cycle(cycle_str)
    
    if cycle is None and not start_days:
        return "매일 열림", []
    
    if cycle is None:
        days = sorted(start_days)
        return f"{', '.join(map(str, days))}일", days
    
    # 한 달 동안의 장날 생성
    market_days = generate_market_days(cycle, start_days)
    
    # 주기별 명칭
    cycle_names = {
        3: "3일장",
        4: "4일장",
        5: "5일장", 
        6: "6일장",
        7: "7일장",
        10: "10일장"
    }
    
    cycle_name = cycle_names.get(cycle, f"{cycle}일 주기")
    
    # 날짜 예시
    if len(market_days) <= 10:
        dates_str = ', '.join(map(str, market_days))
        description = f"{cycle_name} (매월 {dates_str}일)"
    else:
        first_few = ', '.join(map(str, market_days[:5]))
        description = f"{cycle_name} (매월 {first_few}일 등)"
    
    return description, market_days

# CSV 파일 읽기
encodings = ['euc-kr', 'cp949', 'utf-8']
file_path = "app/src/main/assets/markets.csv"

print("="*70)
print("[시장 주기 분석 및 표기법 제안]")
print("="*70 + "\n")

for enc in encodings:
    try:
        with open(file_path, 'r', encoding=enc) as f:
            reader = csv.reader(f)
            headers = next(reader)
            
            # 주기별 그룹화
            cycle_groups = defaultdict(list)
            
            all_rows = list(reader)
            
            print(f"[주기 패턴 분석]\n")
            print("-" * 70)
            
            # 샘플 데이터로 패턴 확인
            samples = {}
            for row in all_rows[:100]:  # 처음 100개만 샘플링
                if len(row) > 4:
                    cycle_str = row[4]
                    if cycle_str and cycle_str not in samples and cycle_str != '상설':
                        cycle, start_days = parse_cycle(cycle_str)
                        if cycle:
                            samples[cycle_str] = {
                                'cycle': cycle,
                                'start_days': start_days,
                                'market_name': row[0]
                            }
            
            # 패턴별로 정리
            print("\n[주기 패턴 예시]\n")
            for cycle_str, info in sorted(samples.items(), key=lambda x: (x[1]['cycle'] or 0, x[1]['start_days'])):
                cycle = info['cycle']
                start_days = info['start_days']
                market_name = info['market_name']
                
                description, market_days = format_cycle_with_dates(cycle_str)
                
                print(f"원본 데이터: {cycle_str}")
                print(f"  -> 주기: {cycle}일")
                print(f"  -> 시작일: {', '.join(map(str, start_days))}일")
                print(f"  -> 장날: {', '.join(map(str, market_days))}일")
                print(f"  -> 표기안 1: {format_cycle_smart(cycle_str)}")
                print(f"  -> 표기안 2: {description}")
                print(f"  -> 예시 시장: {market_name}")
                print()
            
            # 전체 통계
            print("\n" + "="*70)
            print("[전체 주기 유형 통계]")
            print("="*70 + "\n")
            
            cycle_type_count = defaultdict(int)
            for row in all_rows:
                if len(row) > 4:
                    cycle_str = row[4]
                    cycle, _ = parse_cycle(cycle_str)
                    if cycle:
                        cycle_type_count[cycle] += 1
                    elif cycle_str == '상설' or cycle_str == '매일':
                        cycle_type_count['상설'] += 1
            
            for cycle_type, count in sorted(cycle_type_count.items(), key=lambda x: (str(x[0]), x[1]), reverse=True):
                if cycle_type == '상설':
                    print(f"상설시장 (매일): {count}개")
                else:
                    print(f"{cycle_type}일장: {count}개")
            
            print("\n" + "="*70)
            print("[추천 표기 방식]")
            print("="*70 + "\n")
            
            print("1. 간단 표기 (목록용):")
            print("   - 5일장 (1일부터)")
            print("   - 5일장 (2일부터)")
            print("   - 3일장 (3일부터)")
            print()
            
            print("2. 상세 표기 (상세 페이지용):")
            print("   - 5일장 (매월 1, 6, 11, 16, 21, 26, 31일)")
            print("   - 5일장 (매월 2, 7, 12, 17, 22, 27일)")
            print("   - 3일장 (매월 3, 6, 9, 12, 15, 18, 21, 24, 27, 30일)")
            print()
            
            print("3. 오늘 기준 표기 (홈 화면용):")
            print("   - 5일장 (다음 장날: 1월 21일)")
            print("   - 3일장 (오늘 장날!)")
            print("   - 상설시장 (매일 운영)")
            
        break
        
    except Exception as e:
        print(f"[실패] {enc}: {e}\n")
        continue

print("\n\n[분석 완료!]")
