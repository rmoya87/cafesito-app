package com.cafesito.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun writeCaffeine(mg: Double, timestamp: Instant, entryId: String) {
        val client = healthConnectClient ?: return
        if (!hasAllPermissions()) return

        val record = NutritionRecord(
            startTime = timestamp,
            startZoneOffset = ZonedDateTime.now().offset,
            endTime = timestamp.plusSeconds(1),
            endZoneOffset = ZonedDateTime.now().offset,
            caffeine = Mass.milligrams(mg),
            metadata = Metadata(clientRecordId = "cafesito_$entryId")
        )

        try {
            client.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun writeWater(ml: Double, timestamp: Instant, entryId: String) {
        val client = healthConnectClient ?: return
        if (!hasAllPermissions()) return

        val record = HydrationRecord(
            startTime = timestamp,
            startZoneOffset = ZonedDateTime.now().offset,
            endTime = timestamp.plusSeconds(1),
            endZoneOffset = ZonedDateTime.now().offset,
            volume = Volume.milliliters(ml),
            metadata = Metadata(clientRecordId = "cafesito_$entryId")
        )

        try {
            client.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun readRecentCaffeine(startTime: Instant, endTime: Instant): List<NutritionRecord> {
        val client = healthConnectClient ?: return emptyList()
        if (!hasAllPermissions()) return emptyList()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    NutritionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.filter { it.caffeine != null }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
