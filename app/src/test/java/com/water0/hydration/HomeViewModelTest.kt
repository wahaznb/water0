package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import com.water0.hydration.domain.usecase.DeleteHydrationUseCase
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import com.water0.hydration.domain.usecase.LogHydrationUseCase
import com.water0.hydration.presentation.home.GlassKick
import com.water0.hydration.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel kick contract: every log pours, every delete sloshes, the cap
 * blocks silently-ish (notice, no kick). Unconfined Main makes
 * viewModelScope launches execute eagerly, so plain runBlocking suffices.
 */
class HomeViewModelTest {

    private lateinit var repository: FakeHydrationRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Male 70kg / moderate / temperate -> goal 2972 ml, cap 4458 ml.
        repository = FakeHydrationRepository(
            initialProfile = UserProfile(
                weightKg = 70f,
                activityLevel = UserProfile.ActivityLevel.MODERATE,
                climate = UserProfile.Climate.TEMPERATE,
                sex = UserProfile.Sex.MALE
            )
        )
        val engine = RecommendationEngine()
        viewModel = HomeViewModel(
            GetTodayProgressUseCase(repository, engine),
            LogHydrationUseCase(repository),
            CalculateRecommendationUseCase(engine),
            DeleteHydrationUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quickAdd stores entry and kicks Pour`() = runBlocking {
        viewModel.quickAdd(250)

        assertEquals(1, repository.getTodayEntries().first().size)
        val kick = viewModel.glassKick.first()
        assertTrue(kick is GlassKick.Pour)
        assertEquals(250, (kick as GlassKick.Pour).amountMl)

        viewModel.consumeGlassKick()
        assertNull(viewModel.glassKick.first())
    }

    @Test
    fun `deleteEntry removes entry and kicks Slosh`() = runBlocking {
        viewModel.quickAdd(250)
        viewModel.consumeGlassKick()
        val id = repository.getTodayEntries().first().single().id

        viewModel.deleteEntry(id)

        assertTrue(repository.getTodayEntries().first().isEmpty())
        val kick = viewModel.glassKick.first()
        assertTrue(kick is GlassKick.Slosh)
        // The tank needs the deleted amount for its away-balance math.
        assertEquals(250, (kick as GlassKick.Slosh).amountMl)
    }

    @Test
    fun `log over the cap raises notice without kick or write`() = runBlocking {
        repository.insertEntry(
            HydrationEntry(
                amountMl = 5000,
                timestamp = System.currentTimeMillis(),
                type = HydrationEntry.DrinkType.WATER
            )
        )
        val before = repository.getTodayEntries().first().size

        viewModel.logWater(250)

        assertEquals(before, repository.getTodayEntries().first().size)
        assertNull(viewModel.glassKick.first())
        assertTrue(viewModel.notice.first() != null)
    }
}
