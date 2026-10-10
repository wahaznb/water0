package com.water0.hydration.data.fitness

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.water0.hydration.data.local.entity.HydrationEntry
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** Last night's sleep window, epoch millis. Display + training use. */
data class SleepWindow(val bedTimeMs: Long, val wakeTimeMs: Long)

/**
 * Fitness plugin (Health Connect 1.1.0 API, optional). Every call
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
     * platform runtime-permissions sheet vs provider-app sheet. Built
     * from the full sheet set, matching what the button launches. */
    fun requestIntentLine(): String = try {
        val intent = requestContract().createIntent(context, allPermissions())
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
     * strings crash it with IllegalArgumentException.
     * Companion: needs no Context, unit-testable on plain JVM. */
    fun readPermissions(): Set<String> = Companion.readPermissions()

    /** Write-back set (hydration + nutrition). Instance delegate. */
    fun writePermissions(): Set<String> = Companion.writePermissions()

    /** Full sheet set: reads + writes in one user gesture. */
    fun allPermissions(): Set<String> = Companion.allPermissions()

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

    /**
     * Write-back: one logged drink -> Health Connect. Every drink becomes
     * a HydrationRecord (actual volume); coffee/tea additionally become a
     * NutritionRecord carrying estimated caffeine (see caffeineMgFor —
     * standard averages, not lab values). Alcohol is skipped: it
     * dehydrates, and a hydration record for it would be a lie.
     * Idempotent upserts via clientRecordId, so retries and reinstalls
     * can't duplicate. Best-effort Boolean, never throws.
     */
    suspend fun writeDrink(
        entryId: Long,
        amountMl: Int,
        type: HydrationEntry.DrinkType,
        timestampMs: Long
    ): Boolean {
        val client = clientOrNull() ?: return false
        return try {
            if (!hasWritePermissions()) return false
            if (amountMl <= 0) return false
            if (type == HydrationEntry.DrinkType.ALCOHOL) return false
            val zoneId = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(timestampMs)
            val end = start.plusSeconds(60)
            val records = mutableListOf<Record>(
                HydrationRecord(
                    startTime = start,
                    startZoneOffset = zoneId.rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = zoneId.rules.getOffset(end),
                    volume = Volume.liters(amountMl / 1000.0),
                    metadata = Metadata.manualEntry(
                        clientRecordId = drinkRecordId(entryId)
                    )
                )
            )
            val caffeineMg = caffeineMgFor(type, amountMl)
            if (caffeineMg > 0) {
                records += NutritionRecord(
                    startTime = start,
                    startZoneOffset = zoneId.rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = zoneId.rules.getOffset(end),
                    caffeine = Mass.milligrams(caffeineMg.toDouble()),
                    // Estimated, not measured — says so on the record.
                    name = type.name.lowercase(java.util.Locale.ROOT) + " (est.)",
                    metadata = Metadata.manualEntry(
                        clientRecordId = nutritionRecordId(entryId)
                    )
                )
            }
            client.insertRecords(records)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Best-effort mirror of [writeDrink]: removes our records by the
     * same clientRecordIds. Silent when the provider is gone. */
    suspend fun deleteDrink(entryId: Long) {
        try {
            val client = clientOrNull() ?: return
            client.deleteRecords(
                HydrationRecord::class, emptyList(), listOf(drinkRecordId(entryId))
            )
            client.deleteRecords(
                NutritionRecord::class, emptyList(), listOf(nutritionRecordId(entryId))
            )
        } catch (_: Exception) {
        }
    }

    suspend fun hasWritePermissions(): Boolean = try {
        clientOrNull()?.permissionController?.getGrantedPermissions()
            ?.containsAll(Companion.writePermissions()) == true
    } catch (_: Exception) {
        false
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

        /** Fallback package for the provider-app sheet (ROMs without
         * the platform Health module, where the platform contract
         * auto-denies with an empty result). */
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        /** Provider-app sheet, built via the Java shim (the class is
         * Kotlin-internal in 1.1.x but public bytecode — same current
         * provider protocol the old `...Legacy()` factory spoke). */
        fun requestContractApp(
            providerPackage: String = PROVIDER_PACKAGE
        ): androidx.activity.result.contract.ActivityResultContract<
            Set<out String>, Set<out String>
            > = HcAppContract.create(providerPackage)

        /** Static so plain-JVM unit tests can assert the exact set
         * (contract validates the `android.permission.health.*`
         * prefix; a stale hardcoded string crashes at runtime). */
        fun readPermissions(): Set<String> = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )

        /** Write-back set: hydration for every drink, nutrition for the
         * caffeine carriers. Requested together with reads in one sheet. */
        fun writePermissions(): Set<String> = setOf(
            HealthPermission.getWritePermission(HydrationRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class)
        )

        /** Everything the Connect sheet asks for in one go. */
        fun allPermissions(): Set<String> = readPermissions() + writePermissions()

        fun drinkRecordId(entryId: Long): String = "water0_entry_$entryId"

        fun nutritionRecordId(entryId: Long): String = "water0_entry_${entryId}_nutrition"

        /**
         * Estimated caffeine, mg: brewed coffee ~95mg/240ml, black tea
         * ~47mg/240ml (USDA averages). Estimates, not measurements —
         * the record carries no provenance flag, so this mapping (and
         * only this) decides. Pure + unit-tested.
         */
        fun caffeineMgFor(
            type: HydrationEntry.DrinkType,
            amountMl: Int
        ): Int = when (type) {
            HydrationEntry.DrinkType.COFFEE -> (amountMl * 0.396f).roundToInt()
            HydrationEntry.DrinkType.TEA -> (amountMl * 0.196f).roundToInt()
            else -> 0
        }
    }
}
