package com.jangnal.gaja.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a traditional 5-day market (5일장)
 * 
 * @property id Unique identifier for the market
 * @property marketName Name of the market (시장 이름)
 * @property addressRoad Road address (도로명 주소)
 * @property addressJibun Jibun address (지번 주소)
 * @property latitude Latitude coordinate (위도)
 * @property longitude Longitude coordinate (경도)
 * @property openingCycle Opening cycle pattern (e.g., "2+7", "1+6") - represents which days of the month the market operates
 * @property specialty Main products/specialties sold at the market (주요 취급 품목/특산물)
 * @property phoneNumber Contact phone number (전화번호)
 */
@Entity(tableName = "markets")
data class Market(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val marketName: String,
    
    val addressRoad: String,
    
    val addressJibun: String,
    
    val latitude: Double,
    
    val longitude: Double,
    
    val openingCycle: String, // Format: "2+7" means opens on days ending in 2 and 7
    
    val specialty: String,
    
    val phoneNumber: String,
    
    val hasToilet: String = "N",
    
    val hasParking: String = "N",

    val isFavorite: Boolean = false,
    
    val voteOpenTodayCount: Int = 0,
    
    val voteClosedTodayCount: Int = 0,
    
    val lastVoteDate: String = ""
) {
    fun getCleanSpecialty(): String {
        return specialty.replace("+", ", ")
    }
    
    // 상설시장 여부 판단
    fun isPermanent(): Boolean {
        if (openingCycle.contains("매일") || openingCycle.contains("상설") || openingCycle.contains("0+1+2")) return true
        
        // 0부터 9까지 모든 숫자가 포함되어 있으면 상설시장으로 간주
        val digits = openingCycle.filter { it.isDigit() }.map { it.toString().toInt() }.toSet()
        return (0..9).all { digits.contains(it) }
    }
    
    // 주기와 시작일 파싱 (예: "1+6" or "1일+6일" -> Pair(5, listOf(1, 6)))
    fun parseCyclePublic(): Pair<Int?, List<Int>> {
        // 숫자만 추출하되, "10"과 같이 두 자리 수도 처리하기 위해 Regex 사용
        val startDays = Regex("\\d+").findAll(openingCycle)
            .map { it.value.toInt() }
            .distinct()
            .filter { it > 0 && it <= 31 } // 0일 제외, 1~31일만 유효
            .sorted()
            .toList()
        
        if (startDays.size >= 2) {
            val cycle = startDays[1] - startDays[0]
            return Pair(cycle, startDays)
        }
        
        return Pair(null, startDays)
    }
    
    // 한 달 동안의 실제 장날 계산
    fun getMarketDaysInMonth(maxDay: Int = 31): List<Int> {
        if (isPermanent()) {
            return (1..maxDay).toList()
        }
        
        val (cycle, startDays) = parseCyclePublic()
        
        // 유효하지 않은 데이터 처리
        if (startDays.isEmpty()) {
            return emptyList()
        }
        
        // cycle이 null이면 startDays를 그대로 반환 (예: "1+7+8" -> [1, 7, 8])
        if (cycle == null) {
            return startDays.filter { it <= maxDay }
        }
        
        // 주기가 0이거나 음수인 경우 (잘못된 데이터)
        if (cycle <= 0) {
            return startDays.filter { it <= maxDay }
        }
        
        val allDays = mutableSetOf<Int>()
        for (start in startDays) {
            if (start < 1 || start > 31) continue // 유효하지 않은 시작일 스킵
            
            var day = start
            while (day <= maxDay) {
                allDays.add(day)
                day += cycle
            }
        }
        
        return allDays.sorted()
    }
    
    // 시장 유형 이름 (3일장, 4일장, 5일장 등)
    fun getMarketTypeName(): String {
        if (isPermanent()) return "상설시장"
        
        val (cycle, _) = parseCyclePublic()
        return when (cycle) {
            3 -> "3일장"
            4 -> "4일장"
            5 -> "5일장"
            6 -> "6일장"
            7 -> "7일장"
            10 -> "10일장"
            else -> "정기시장"
        }
    }
    
    // 시작일 표시 (예: "1일부터" 또는 "1일 또는 6일부터")
    fun getStartDayText(): String {
        if (isPermanent()) return "매일"
        
        val (_, startDays) = parseCyclePublic()
        return when (startDays.size) {
            0 -> ""
            1 -> "${startDays[0]}일부터"
            2 -> "${startDays[0]}일 또는 ${startDays[1]}일부터"
            else -> startDays.joinToString(", ") { "${it}일" } + "부터"
        }
    }

    // 목록 화면용 간단 표기 (예: "5일장 (1일부터)")
    fun getSimpleTypeText(): String {
        if (isPermanent()) return "상설시장"
        return "${getMarketTypeName()} (${getStartDayText()})"
    }
    
    // 상세 화면용 장날 목록 (예: "매월 1, 6, 11, 16, 21, 26, 31일")
    fun getDetailedScheduleText(): String {
        if (isPermanent()) return "매일 운영"
        
        val days = getMarketDaysInMonth()
        return if (days.isNotEmpty()) {
            "매월 ${days.joinToString(", ")}일"
        } else {
            "일정 정보 없음"
        }
    }
    
    // 다음 장날 계산
    fun getNextMarketDate(): Pair<Int, Int>? { // (month, day)
        if (isPermanent()) return null
        
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        
        // 이번 달 남은 장날 확인
        val thisMonthDays = getMarketDaysInMonth()
        val nextDayThisMonth = thisMonthDays.firstOrNull { it > currentDay }
        
        if (nextDayThisMonth != null) {
            return Pair(currentMonth, nextDayThisMonth)
        }
        
        // 다음 달 첫 장날
        calendar.add(java.util.Calendar.MONTH, 1)
        val nextMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val maxDayNextMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val nextMonthDays = getMarketDaysInMonth(maxDayNextMonth)
        
        return if (nextMonthDays.isNotEmpty()) {
            Pair(nextMonth, nextMonthDays.first())
        } else {
            null
        }
    }
    
    // 다음 장날까지 남은 일수
    fun getDaysUntilNextMarket(): Int? {
        if (isPermanent()) return 0
        
        val nextDate = getNextMarketDate() ?: return null
        val (nextMonth, nextDay) = nextDate
        
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        if (nextMonth == currentMonth) {
            return nextDay - currentDay
        } else {
            // 이번 달 남은 일수 + 다음 달 날짜
            val daysLeftThisMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH) - currentDay
            return daysLeftThisMonth + nextDay
        }
    }
    
    // 오늘 기준 상태 텍스트 (목록용)
    fun getStatusText(): String {
        if (isPermanent()) return "매일 운영"
        
        if (isOpenToday()) {
            return "오늘 장날! 🎉"
        }
        
        val nextDate = getNextMarketDate()
        val daysUntil = getDaysUntilNextMarket()
        
        return if (nextDate != null && daysUntil != null) {
            val (month, day) = nextDate
            "다음 장날: ${month}/${day}"
        } else {
            "일정 확인 필요"
        }
    }
    
    // 상세 화면용 다음 장날 텍스트
    fun getNextMarketText(): String {
        if (isPermanent()) return "매일 운영"
        
        val nextDate = getNextMarketDate()
        val daysUntil = getDaysUntilNextMarket()
        
        return if (nextDate != null && daysUntil != null) {
            val (month, day) = nextDate
            if (daysUntil == 0) {
                "오늘!"
            } else {
                "${month}월 ${day}일 (${daysUntil}일 후)"
            }
        } else {
            "일정 확인 필요"
        }
    }

    fun isOpenToday(): Boolean {
        return isOpenOn(System.currentTimeMillis())
    }

    fun isOpenOn(dateInMillis: Long): Boolean {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = dateInMillis

        // 상설시장은 무조건 true? 날짜 필터링 시 '오늘'이 아니어도 열려있으므로 true.
        // 하지만 사용자가 '상설시장 제외'를 선택하면 화면단에서 isPermanent() 로 필터링됨.
        // 여기서는 '날짜상' 열렸는지 여부만.
        if (isPermanent()) return true
        
        val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val targetDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        return getMarketDaysInMonth(maxDay).contains(targetDay)
    }
}
