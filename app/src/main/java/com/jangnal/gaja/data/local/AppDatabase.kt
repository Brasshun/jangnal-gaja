package com.jangnal.gaja.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jangnal.gaja.data.local.dao.MarketDao
import com.jangnal.gaja.data.local.entity.Market

/**
 * The Room Database for the application.
 * Defines the entities and version.
 */
@Database(entities = [Market::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun marketDao(): MarketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "market_database"
                )
                // In a real app, you might want to use a callback to populate data on creation
                // .addCallback(MarketDatabaseCallback(scope)) 
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
