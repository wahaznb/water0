package com.water0.hydration

import com.water0.hydration.data.fitness.FitnessDataSource
import com.water0.hydration.data.fitness.SleepWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Health Connect wiring quirks, pinned as regression tests (plain JVM):
 * - the permission contract validates the `android.permission.health.*`
 *   prefix — a stale hardcoded string crashes at runtime;
 * - manifest + health_permissions.xml + request set must stay identical;
 * - the rationale activity must exist or newer providers fail grants
 *   silently (checked here via manifest parse, no device needed).
 */
class FitnessDataSourceTest {

    @Test
    fun readPermissions_exactSet() {
        assertEquals(
            setOf(
                "android.permission.health.READ_SLEEP",
                "android.permission.health.READ_EXERCISE"
            ),
            FitnessDataSource.readPermissions()
        )
    }

    @Test
    fun writePermissions_exactSet() {
        assertEquals(
            setOf(
                "android.permission.health.WRITE_HYDRATION",
                "android.permission.health.WRITE_NUTRITION"
            ),
            FitnessDataSource.writePermissions()
        )
    }

    @Test
    fun allPermissions_isReadsPlusWrites() {
        assertEquals(
            FitnessDataSource.readPermissions() +
                FitnessDataSource.writePermissions(),
            FitnessDataSource.allPermissions()
        )
        assertEquals(4, FitnessDataSource.allPermissions().size)
    }

    @Test
    fun caffeineEstimates_documentedAverages() {
        // Brewed coffee ~95mg/240ml, black tea ~47mg/240ml. Estimates.
        assertEquals(
            95,
            FitnessDataSource.caffeineMgFor(
                com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.COFFEE,
                240
            )
        )
        assertEquals(
            47,
            FitnessDataSource.caffeineMgFor(
                com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.TEA,
                240
            )
        )
        for (t in listOf(
            com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.WATER,
            com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.JUICE,
            com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.SODA,
            com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.ALCOHOL,
            com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.OTHER
        )) {
            assertEquals(0, FitnessDataSource.caffeineMgFor(t, 240))
        }
    }

    @Test
    fun recordIds_stablePerEntry() {
        assertEquals("water0_entry_42", FitnessDataSource.drinkRecordId(42))
        assertEquals(
            "water0_entry_42_nutrition", FitnessDataSource.nutritionRecordId(42)
        )
    }

    @Test
    fun readPermissions_allNewPrefix() {
        for (p in FitnessDataSource.readPermissions()) {
            assertTrue(
                "stale prefix would crash the contract: $p",
                p.startsWith("android.permission.health.")
            )
        }
    }

    @Test
    fun requestContract_builds() {
        assertNotNull(FitnessDataSource.requestContract())
    }

    @Test
    fun sleepWindow_holdsTimes() {
        val w = SleepWindow(bedTimeMs = 1000L, wakeTimeMs = 2000L)
        assertEquals(1000L, w.bedTimeMs)
        assertEquals(2000L, w.wakeTimeMs)
    }

    @Test
    fun manifest_declaresPermissionsAndRationale() {
        // Source-level check: manifest must declare both runtime
        // permissions (API 34+ auto-denies undeclared ones) and the
        // rationale activity newer providers require for grants.
        val manifest = java.io.File("src/main/AndroidManifest.xml")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/AndroidManifest.xml")
        val xml = manifest.readText()
        assertTrue(xml.contains("android.permission.health.READ_SLEEP"))
        assertTrue(xml.contains("android.permission.health.READ_EXERCISE"))
        assertTrue(xml.contains("HealthRationaleActivity"))
        assertTrue(xml.contains("VIEW_PERMISSION_USAGE"))
        assertTrue(xml.contains("ACTION_SHOW_PERMISSIONS_RATIONALE"))
        // The controller drops requests it can't resolve to a usage
        // screen: exact platform category + START_VIEW_PERMISSION_USAGE
        // are required (a wrong category = empty sheet, no UI).
        assertTrue(
            "wrong/missing usage category",
            xml.contains("android.intent.category.HEALTH_PERMISSIONS")
        )
        assertTrue(xml.contains("START_VIEW_PERMISSION_USAGE"))
        val declared = Regex(
            """<permission name="(android\.permission\.health\.[A-Z_]+)""""
        ).findAll(
            java.io.File(
                manifest.parent, "res/xml/health_permissions.xml"
            ).readText()
        ).map { it.groupValues[1] }.toSet()
        assertEquals(FitnessDataSource.allPermissions(), declared)
    }
}
