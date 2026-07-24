package com.jangnal.gaja.data.repository

import android.content.Context
import com.jangnal.gaja.data.local.dao.MarketDao
import com.jangnal.gaja.data.local.entity.Market
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/**
 * Repository for handling Market data.
 * Abstracts the source of data (currently only local Room DB) from the rest of the app.
 */
class MarketRepository(private val marketDao: MarketDao) {

    /**
     * Observable stream of all markets, sorted by name
     */
    val allMarkets: Flow<List<Market>> = marketDao.getAllMarkets()

    /**
     * Get a market by its ID
     */
    suspend fun getMarketById(id: Long): Market? {
        return marketDao.getMarketById(id)
    }

    /**
     * Get markets filtering by opening cycle (e.g., "2+7")
     */
    fun getMarketsByCycle(cycle: String): Flow<List<Market>> {
        return marketDao.getMarketsByOpeningCycle(cycle)
    }

    /**
     * Search markets by name
     */
    fun searchMarkets(query: String): Flow<List<Market>> {
        return marketDao.searchMarketsByName(query)
    }

    /**
     * Insert a new market
     */
    suspend fun insert(market: Market) {
        marketDao.insert(market)
    }

    /**
     * Insert multiple markets (useful for initial data loading)
     */
    suspend fun insertAll(markets: List<Market>) {
        marketDao.insertAll(markets)
    }
    
    /**
     * Get markets visible in a map area
     */
    fun getMarketsInArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Flow<List<Market>> {
        return marketDao.getMarketsInBounds(minLat, maxLat, minLon, maxLon)
    }

    /**
     * Update favorite status
     */
    suspend fun updateFavoriteStatus(marketId: Long, isFavorite: Boolean) {
        marketDao.updateFavoriteStatus(marketId, isFavorite)
    }

    /**
     * Get favorite markets
     */
    val favoriteMarkets: Flow<List<Market>> = marketDao.getFavoriteMarkets()

    /**
     * Load initial data from CSV file in Assets
     */
    suspend fun loadDataFromCsv(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            try {
                // Use UTF-8 as the CSV is now updated with UTF-8 encoding (and contains Emojis)
                val reader = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
                val marketList = mutableListOf<Market>()
                
                // Skip header
                reader.readLine() 
                
                var line: String? = reader.readLine()
                while (line != null) {
                    val tokens = parseCsvLine(line)
                    // Safety check for array size, based on inspection we seem to have ~12 cols
                    if (tokens.size >= 8) {
                        
                        // Standard Data Format Parsing
                        // 0: Name, 2: Road Addr, 3: Jibun Addr, 4: Cycle, 5: Lat, 6: Lon, 8: Specialty, 14: Phone
                        val name = tokens[0]
                        val roadAddr = tokens[2]
                        val jibunAddr = tokens[3]
                        val cycleRaw = tokens[4]
                        
                        // Parse Lat/Lon safely
                        val lat = tokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                        val lon = tokens.getOrNull(6)?.toDoubleOrNull() ?: 0.0
                        
                        val specialty = (tokens.getOrNull(8) ?: "").replace("+", ", ")
                        val toilet = tokens.getOrNull(11)?.trim() ?: "N"
                        val parking = tokens.getOrNull(12)?.trim() ?: "N"
                        val phone = tokens.getOrNull(14) ?: ""
                        
                        // Parse cycle text
                        // "1일+6일" -> "1+6"
                        // "매일" -> All days
                        val cycle = parseCycle(cycleRaw)
                        
                        val market = Market(
                            marketName = name,
                            addressRoad = roadAddr,
                            addressJibun = jibunAddr,
                            latitude = lat,
                            longitude = lon,
                            openingCycle = cycle,
                            specialty = specialty,
                            phoneNumber = phone.replace("Y", "").replace("N", ""), // Extra safety for Y/N
                            hasToilet = toilet,
                            hasParking = parking
                        )
                        marketList.add(market)
                    }
                    line = reader.readLine()
                }
                reader.close()
                
                if (marketList.isNotEmpty()) {
                    marketDao.deleteAll()
                    marketDao.insertAll(marketList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        
        for (c in line) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        
        // Remove wrapping quotes if present
        return tokens.map { 
            if (it.startsWith("\"") && it.endsWith("\"")) {
                it.removeSurrounding("\"").replace("\"\"", "\"") // Handle escaped quotes
            } else {
                it
            }
        }
    }
    
    // Helper to parse cycle strings
    // Helper to parse cycle strings
    private fun parseCycle(raw: String): String {
        // "1+2+3+4+5+6+7+8+9+0" 같은 케이스는 "상설시장"으로 변환
        if (raw.contains("1") && raw.contains("2") && raw.contains("3") && raw.contains("5") && raw.length > 10) {
            return "상설시장"
        }
        if (raw.contains("매일") || raw.contains("상설")) {
            return "상설시장"
        }
        
        // 그 외에는 숫자와 ',' 만 남기고 정리
        // 예: "1일+6일" -> "1+6"
        val regex = "[^0-9,+]".toRegex()
        var cleaned = regex.replace(raw, "")
        
        // 5일장의 경우 "1, 6" 형태로 보기 좋게 변환을 위해 그냥 둠.
        // UI에서 보여줄 때 처리가 필요할 수도 있지만, 일단 "1+6" 형태면 충분히 알아봄.
        if (cleaned.isEmpty()) return raw 
        
        return cleaned
    }

    /**
     * Submit a user vote for today's market status
     */
    suspend fun submitVote(marketId: Long, isOpenToday: Boolean, voteDate: String) {
        withContext(Dispatchers.IO) {
            val market = marketDao.getMarketById(marketId) ?: return@withContext
            
            // If the last vote date is not today, reset counts
            val isSameDay = market.lastVoteDate == voteDate
            val currentOpen = if (isSameDay) market.voteOpenTodayCount else 0
            val currentClosed = if (isSameDay) market.voteClosedTodayCount else 0
            
            if (isOpenToday) {
                marketDao.updateVoteCounts(marketId, currentOpen + 1, currentClosed, voteDate)
            } else {
                marketDao.updateVoteCounts(marketId, currentOpen, currentClosed + 1, voteDate)
            }
        }
    }

    /**
     * Sync market data from GitHub Gist JSON URL, preserving existing favorites
     */
    suspend fun syncDataFromJson(jsonUrl: String, context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val sharedPref = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val lastSync = sharedPref.getLong("last_sync_time", 0L)
            val currentTime = System.currentTimeMillis()
            
            // Sync at most once every 7 days to preserve battery and network bandwidth
            if (currentTime - lastSync < 7 * 24 * 60 * 60 * 1000) {
                android.util.Log.d("MarketSync", "Skip sync: last sync was less than 7 days ago")
                return@withContext false
            }
            
            try {
                android.util.Log.d("MarketSync", "Starting background data sync from Gist...")
                val url = URL(jsonUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    val newMarketsList = mutableListOf<Market>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        
                        val name = obj.optString("marketName", "")
                        val roadAddr = obj.optString("addressRoad", "")
                        val jibunAddr = obj.optString("addressJibun", "")
                        val lat = obj.optDouble("latitude", 0.0)
                        val lon = obj.optDouble("longitude", 0.0)
                        val cycle = obj.optString("openingCycle", "")
                        val specialty = obj.optString("specialty", "")
                        val toilet = obj.optString("hasToilet", "N")
                        val parking = obj.optString("hasParking", "N")
                        val phone = obj.optString("phoneNumber", "")
                        
                        val market = Market(
                            marketName = name,
                            addressRoad = roadAddr,
                            addressJibun = jibunAddr,
                            latitude = lat,
                            longitude = lon,
                            openingCycle = cycle,
                            specialty = specialty,
                            phoneNumber = phone,
                            hasToilet = toilet,
                            hasParking = parking
                        )
                        newMarketsList.add(market)
                    }
                    
                    if (newMarketsList.isNotEmpty()) {
                        // 1. Fetch current markets to extract and preserve favorites status
                        val existingMarkets = marketDao.getAllMarketsList()
                        val favoriteKeys = existingMarkets
                            .filter { it.isFavorite }
                            .map { "${it.marketName}_${it.addressRoad}" }
                            .toSet()
                        
                        // 2. Map new markets list, merging favorite status
                        val finalMarketsList = newMarketsList.map { market ->
                            val key = "${market.marketName}_${market.addressRoad}"
                            if (favoriteKeys.contains(key)) {
                                market.copy(isFavorite = true)
                            } else {
                                market
                            }
                        }
                        
                        // 3. Clear database and insert updated list
                        marketDao.deleteAll()
                        marketDao.insertAll(finalMarketsList)
                        
                        // 4. Update sync timestamp
                        sharedPref.edit().putLong("last_sync_time", currentTime).apply()
                        android.util.Log.d("MarketSync", "Data sync successful. Loaded ${finalMarketsList.size} markets.")
                        return@withContext true
                    }
                }
                android.util.Log.e("MarketSync", "Sync failed: Server response code ${connection.responseCode}")
                false
            } catch (e: Exception) {
                android.util.Log.e("MarketSync", "Sync failed with exception", e)
                e.printStackTrace()
                false
            }
        }
    }
}
