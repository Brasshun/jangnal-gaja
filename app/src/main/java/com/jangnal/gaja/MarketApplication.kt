package com.jangnal.gaja

import android.app.Application
import com.jangnal.gaja.data.local.AppDatabase
import com.jangnal.gaja.data.repository.MarketRepository

/**
 * Main Application class.
 * Initializes the database and repository.
 * Acts as a simple Service Locator for dependencies.
 */
import com.kakao.vectormap.KakaoMapSdk

class MarketApplication : Application() {
    // Lazy initialization of the database
    private val database by lazy { AppDatabase.getDatabase(this) }
    
    // Lazy initialization of the repository
    val repository by lazy { MarketRepository(database.marketDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // 키 해시 확인용 (안드로이드 기본 API 사용)
        try {
            val info = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            info.signatures?.forEach { signature ->
                val md = java.security.MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = android.util.Base64.encodeToString(md.digest(), android.util.Base64.NO_WRAP)
                android.util.Log.d("KeyHash", "Current Key Hash: $keyHash")
            }
        } catch (e: Exception) {
            android.util.Log.e("KeyHash", "Failed to get KeyHash", e)
        }
        
        // Kakao Map SDK 초기화
        KakaoMapSdk.init(this, "e88d8e377ad7672c2ecdc80df4123e1a")
    }
}
