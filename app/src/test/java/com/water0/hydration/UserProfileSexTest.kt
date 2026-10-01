package com.water0.hydration

import com.water0.hydration.data.local.Converters
import com.water0.hydration.data.local.MIGRATION_1_2
import com.water0.hydration.data.local.entity.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileSexTest {

    private val converters = Converters()

    @Test
    fun `default sex is female for conservative goals`() {
        assertEquals(UserProfile.Sex.FEMALE, UserProfile().sex)
    }

    @Test
    fun `entity base need matches engine formula per sex`() {
        val female = UserProfile(
            weightKg = 70f,
            activityLevel = UserProfile.ActivityLevel.MODERATE,
            climate = UserProfile.Climate.TEMPERATE,
            sex = UserProfile.Sex.FEMALE
        )
        assertEquals(2804, female.baseWaterNeedMl)

        val male = female.copy(sex = UserProfile.Sex.MALE)
        assertEquals(2972, male.baseWaterNeedMl)
    }

    @Test
    fun `sex converter round-trips`() {
        assertEquals("MALE", converters.fromSex(UserProfile.Sex.MALE))
        assertEquals("FEMALE", converters.fromSex(UserProfile.Sex.FEMALE))
        assertEquals(UserProfile.Sex.MALE, converters.toSex("MALE"))
        assertEquals(UserProfile.Sex.FEMALE, converters.toSex("FEMALE"))
    }

    @Test
    fun `sex converter defaults null and unknown to female`() {
        assertEquals(UserProfile.Sex.FEMALE, converters.toSex(null))
        assertEquals(UserProfile.Sex.FEMALE, converters.toSex("NONBINARY"))
        assertNull(converters.fromSex(null))
    }

    @Test
    fun `migration covers version 1 to 2`() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }
}
