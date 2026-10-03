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
        // 1.1.x API: explicit provider package (defaults to the Play
        // provider); this is the path that sees platform-bundled Health
        // Connect on Android 14+ and custom ROMs, where the old alpha11
        // sdkStatus() check reported "not there" and hid the feature.
        HealthConnectClient.getSdkStatus(context) ==
            HealthConnectClient.SDK_AVAILABLE
    } catch (_: Exception) {
        false
    }

    /** Provider present but too old: worth an update, not a dead end. */
    fun needsUpdate(): Boolean = try {
        HealthConnectClient.getSdkStatus(context) ==
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    } catch (_: Exception) {
        false
    }

    /** Raw provider state for the Settings status line (debug-honest:
     * tells the user exactly what the phone reports). */
    fun sdkStatusName(): String = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> "available"
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                "installed, needs update"

            else -> "not on this phone"
        }
    } catch (e: Exception) {
        "check failed (${e.javaClass.simpleName})"
    }

    /** Which permission road the 1.1.x contract takes on this phone:
     * platform runtime-permissions sheet vs provider-app sheet. */
    fun requestIntentLine(): String = try {
        val intent = requestContract().createIntent(context, readPermissions())
        "sheet: action=${intent.action} pkg=${intent.`package`}"
    } catch (e: Exception) {
        "sheet failed: ${e.javaClass.simpleName}"
    }

    /** What the provider says we hold right now (or why we can't ask). */
    suspend fun grantedLine(): String = try {
        val g = clientOrNull()?.permissionController?.getGrantedPermissions()
        "granted: " + (g?.sorted()?.joinToString() ?: "no client")
    } catch (e: Exception) {
        "granted check failed: ${e.javaClass.simpleName}"
    }

    /** Raw permission strings, built by the library so the prefix
     * (`android.permission.health.*` since 1.1.x) always matches what
     * the permission contract validates — hardcoded old-prefix
     * strings crash it with IllegalArgumentException. */
    fun readPermissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun hasPermissions(): Boolean = try {
        clientOrNull()?.permissionController?.getGrantedPermissions()
            ?.containsAll(readPermissions()) == true
    } catch (_: Exception) {
        false
    }

    /** Most recent finished sleep ≥3h in the last 2 days, else null. */
    suspend fun lastNightSleep(): SleepWindow? {
        return try {
            val end = Instant.now()
            val latest = (clientOrNull() ?: return null).readRecords(
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
            (clientOrNull() ?: return false).readRecords(
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

    /** Central guard: getOrCreate throws with no provider on board
     * (custom ROMs), so every caller goes through here instead of
     * remembering its own try/catch. */
    private fun clientOrNull(): HealthConnectClient? = try {
        HealthConnectClient.Companion.getOrCreate(context)
    } catch (_: Exception) {
        null
    }

    companion object {
        fun requestContract() =
            PermissionController.createRequestPermissionResultContract()
    }
}
