package com.water0.hydration.data.fitness

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

/** Last night's sleep window, epoch millis. Display + training use. */
data class SleepWindow(val bedTimeMs: Long, val wakeTimeMs: Long)

/**
 * Fitness plugin (Health Connect 1.0.0-alpha11 API, optional). Every call
 * is total-failure safe: no provider, no permission, airplane-mode
 * weirdness — all collapse to null/false and the app carries on with
 * its own data. Callers never branch on availability; they branch on
 * results.
 */
class FitnessDataSource(private val context: Context) {

    fun isAvailable(): Boolean = try {
        HealthConnectClient.Companion.sdkStatus(context) ==
            HealthConnectClient.SDK_AVAILABLE
    } catch (_: Exception) {
        false
    }

    /** Raw permission strings (stable public API names). */
    fun readPermissions(): Set<String> = setOf(READ_SLEEP, READ_EXERCISE)

    suspend fun hasPermissions(): Boolean = try {
        client().permissionController.getGrantedPermissions()
            .containsAll(readPermissions())
    } catch (_: Exception) {
        false
    }

    /** Most recent finished sleep ≥3h in the last 2 days, else null. */
    suspend fun lastNightSleep(): SleepWindow? {
        return try {
            val end = Instant.now()
            val latest = client().readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        end.minusSeconds(2 * 24 * 3600L), end
                    )
                )
            ).records
                .filter {
                    it.endTime.isBefore(end) &&
                        Duration.between(it.startTime, it.endTime).toHours() >= 3
                }
                .maxByOrNull { it.endTime } ?: return null
            SleepWindow(
                bedTimeMs = latest.startTime.toEpochMilli(),
                wakeTimeMs = latest.endTime.toEpochMilli()
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Any workout finishing within the last [hoursBack] hours. */
    suspend fun hadRecentWorkout(hoursBack: Long = 2): Boolean {
        return try {
            val end = Instant.now()
            client().readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        end.minusSeconds(hoursBack * 3600L), end
                    )
                )
            ).records.any { !it.endTime.isAfter(end) }
        } catch (_: Exception) {
            false
        }
    }

    private fun client(): HealthConnectClient =
        HealthConnectClient.Companion.getOrCreate(context)

    companion object {
        const val READ_SLEEP = "android.health.permission.READ_SLEEP"
        const val READ_EXERCISE = "android.health.permission.READ_EXERCISE"

        fun requestContract() =
            PermissionController.createRequestPermissionResultContract()
    }
}
