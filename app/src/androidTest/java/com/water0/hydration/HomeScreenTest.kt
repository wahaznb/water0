package com.water0.hydration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import com.water0.hydration.domain.usecase.DeleteHydrationUseCase
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import com.water0.hydration.domain.usecase.LogHydrationUseCase
import com.water0.hydration.presentation.home.GlassScreen
import com.water0.hydration.presentation.home.HomeScreen
import com.water0.hydration.presentation.home.HomeViewModel
import com.water0.hydration.presentation.home.LogScreen
import com.water0.hydration.ui.theme.Water0
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Self-contained fake: androidTest can't see src/test classes,
// so the fake lives here. Mirrors FakeHydrationRepository.
private class TestRepository : HydrationRepository {
    private val entries = MutableStateFlow<List<HydrationEntry>>(emptyList())
    private val profile = MutableStateFlow(
        // Explicit male to pin the goal: 70kg/moderate/temperate -> 2972 ml.
        // (Default profile is female -> 2804 ml.)
        UserProfile(
            weightKg = 70f,
            activityLevel = UserProfile.ActivityLevel.MODERATE,
            climate = UserProfile.Climate.TEMPERATE,
            sex = UserProfile.Sex.MALE
        )
    )
    private val behavior = MutableStateFlow(UserBehavior())
    private var nextId = 1L

    override fun getTodayEntries(): Flow<List<HydrationEntry>> = entries
    override fun getEntriesForDate(date: String) = entries
    override fun getEntriesInRange(s: Long, e: Long) =
        entries.map { list -> list.filter { it.timestamp in s..e } }
    override suspend fun insertEntry(entry: HydrationEntry): Long {
        val id = nextId++
        entries.value = entries.value + entry.copy(id = id)
        return id
    }
    override suspend fun insertEntries(newEntries: List<HydrationEntry>) {
        newEntries.forEach { insertEntry(it) }
    }
    override suspend fun updateEntry(entry: HydrationEntry) {}
    override suspend fun deleteEntry(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }
    override suspend fun deleteAllEntries() { entries.value = emptyList() }
    override suspend fun getTodayTotalEffectiveMl() = entries.value.sumOf { it.effectiveHydrationMl }
    override suspend fun getTodayEntryCount() = entries.value.size
    override suspend fun getRecentEntries(limit: Int) = entries.value.take(limit)
    override fun getUserProfile(): Flow<UserProfile> = profile
    override suspend fun getUserProfileSuspend() = profile.value
    override suspend fun updateUserProfile(p: UserProfile) { profile.value = p }
    override fun getUserBehavior(): Flow<UserBehavior> = behavior
    override suspend fun getUserBehaviorSuspend() = behavior.value
    override suspend fun updateUserBehavior(b: UserBehavior) { behavior.value = b }
    override suspend fun onWaterLogged(amountMl: Int, drinkType: HydrationEntry.DrinkType) {
        behavior.value = behavior.value.updateOnLog(amountMl, drinkType)
    }
}

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(repo: TestRepository = TestRepository()): HomeViewModel {
        val engine = RecommendationEngine()
        return HomeViewModel(
            GetTodayProgressUseCase(repo, engine),
            LogHydrationUseCase(repo),
            CalculateRecommendationUseCase(engine),
            DeleteHydrationUseCase(repo)
        )
    }

    @Test
    fun homeShowsStatusAndRecommendations() {
        composeRule.setContent {
            Water0 { HomeScreen(viewModel()) }
        }
        // Empty repo -> 0 ml -> BEHIND with a catch-up card.
        composeRule.onNodeWithText("Behind goal").assertIsDisplayed()
        composeRule.onNodeWithText("Recommendations").assertIsDisplayed()
    }

    @Test
    fun glassShowsQuickAddAndFillsOnLog() {
        composeRule.setContent {
            Water0 { GlassScreen(viewModel()) }
        }
        composeRule.onNodeWithText("250ml").performClick()
        // Pinned male profile goal is 2972 ml, so one 250 ml log shows "250 / 2972 ml".
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("250 / 2972 ml")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("250 / 2972 ml").assertIsDisplayed()
    }

    @Test
    fun logShowsEntriesAndDeletesThem() {
        val repo = TestRepository()
        composeRule.setContent {
            Water0 { LogScreen(viewModel(repo)) }
        }
        composeRule.onNodeWithText("Log intake").assertIsDisplayed()
        composeRule.onNodeWithText("250ml").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Today's entries (1)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Delete entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Today's entries (0)")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
