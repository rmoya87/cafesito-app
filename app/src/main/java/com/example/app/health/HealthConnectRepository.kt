package com.cafesito.app.health

import android.content.SharedPreferences
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepository @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val prefs: SharedPreferences
) {
    private val KEY_HEALTH_CONNECT_ENABLED = "health_connect_enabled"

    fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_HEALTH_CONNECT_ENABLED, false)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HEALTH_CONNECT_ENABLED, enabled).apply()
    }

    suspend fun syncDiaryEntry(mg: Int, ml: Int, timestamp: Long, entryId: String) {
        if (!isEnabled()) return
        
        val instant = Instant.ofEpochMilli(timestamp)
        
        if (mg > 0) {
            healthConnectManager.writeCaffeine(mg.toDouble(), instant, entryId)
        }
        
        if (ml > 0) {
            healthConnectManager.writeWater(ml.toDouble(), instant, entryId)
        }
    }

    suspend fun hasPermissions(): Boolean {
        return healthConnectManager.hasAllPermissions()
    }

    fun isAvailable(): Boolean {
        return healthConnectManager.isAvailable()
    }
    
    val permissions = healthConnectManager.permissions

    suspend fun readAndSyncExternalData(): List<Pair<Int, Long>> {
        if (!isEnabled()) return emptyList()
        
        val now = Instant.now()
        val yesterday = now.minusSeconds(24 * 3600)
        
        val caffeineRecords = healthConnectManager.readRecentCaffeine(yesterday, now)
        
        // Filter com.cafesito.app records created by Cafesito itself to avoid sync loops
        return caffeineRecords.filter { record ->
            record.metadata.clientRecordId?.startsWith("cafesito_") != true
        }.map { record ->
            (record.caffeine?.inMilligrams?.toInt() ?: 0) to record.startTime.toEpochMilli()
        }
    }
}
