package com.jangnal.gaja.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jangnal.gaja.data.local.entity.Market
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Market entity
 * Provides methods to interact with the markets table in the database
 */
@Dao
interface MarketDao {
    
    /**
     * Insert a single market into the database
     * @param market The market to insert
     * @return The row ID of the inserted market
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(market: Market): Long
    
    /**
     * Insert multiple markets into the database
     * @param markets List of markets to insert
     * @return List of row IDs of the inserted markets
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markets: List<Market>): List<Long>
    
    /**
     * Update an existing market
     * @param market The market to update
     */
    @Update
    suspend fun update(market: Market)
    
    /**
     * Delete a market from the database
     * @param market The market to delete
     */
    @Delete
    suspend fun delete(market: Market)
    
    /**
     * Get all markets as a Flow (reactive stream)
     * @return Flow of list of all markets
     */
    @Query("SELECT * FROM markets ORDER BY marketName ASC")
    fun getAllMarkets(): Flow<List<Market>>
    
    /**
     * Get all markets as a one-time list
     * @return List of all markets
     */
    @Query("SELECT * FROM markets ORDER BY marketName ASC")
    suspend fun getAllMarketsList(): List<Market>
    
    /**
     * Get a specific market by ID
     * @param marketId The ID of the market to retrieve
     * @return The market with the specified ID, or null if not found
     */
    @Query("SELECT * FROM markets WHERE id = :marketId")
    suspend fun getMarketById(marketId: Long): Market?
    
    /**
     * Get markets by opening cycle pattern
     * @param openingCycle The opening cycle pattern (e.g., "2+7")
     * @return Flow of list of markets with the specified opening cycle
     */
    @Query("SELECT * FROM markets WHERE openingCycle = :openingCycle ORDER BY marketName ASC")
    fun getMarketsByOpeningCycle(openingCycle: String): Flow<List<Market>>
    
    /**
     * Search markets by name
     * @param searchQuery The search query
     * @return Flow of list of markets matching the search query
     */
    @Query("SELECT * FROM markets WHERE marketName LIKE '%' || :searchQuery || '%' ORDER BY marketName ASC")
    fun searchMarketsByName(searchQuery: String): Flow<List<Market>>
    
    /**
     * Get markets within a bounding box (for map view)
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     * @return Flow of list of markets within the bounding box
     */
    @Query("""
        SELECT * FROM markets 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
        ORDER BY marketName ASC
    """)
    fun getMarketsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<Market>>
    
    /**
     * Delete all markets from the database
     */
    @Query("DELETE FROM markets")
    suspend fun deleteAll()
    
    /**
     * Get the total count of markets
     * @return The total number of markets in the database
     */
    @Query("SELECT COUNT(*) FROM markets")
    suspend fun getMarketCount(): Int

    /**
     * Update favorite status
     */
    @Query("UPDATE markets SET isFavorite = :isFavorite WHERE id = :marketId")
    suspend fun updateFavoriteStatus(marketId: Long, isFavorite: Boolean)

    /**
     * Get favorite markets
     */
    @Query("SELECT * FROM markets WHERE isFavorite = 1 ORDER BY marketName ASC")
    fun getFavoriteMarkets(): Flow<List<Market>>

    /**
     * Update today's vote counts for a market
     */
    @Query("UPDATE markets SET voteOpenTodayCount = :openCount, voteClosedTodayCount = :closedCount, lastVoteDate = :voteDate WHERE id = :marketId")
    suspend fun updateVoteCounts(marketId: Long, openCount: Int, closedCount: Int, voteDate: String)
}
