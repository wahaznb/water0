package com.water0.hydration.presentation

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.history.HistoryScreen
import com.water0.hydration.presentation.history.HistoryViewModel
import com.water0.hydration.presentation.history.HistoryViewModelFactory
import com.water0.hydration.presentation.home.AmbientTank
import com.water0.hydration.presentation.home.HomeScreen
import com.water0.hydration.presentation.home.HomeViewModel
import com.water0.hydration.presentation.home.LogScreen
import com.water0.hydration.presentation.home.hydrationTintFor
import com.water0.hydration.presentation.navigation.GlassBottomBar
import com.water0.hydration.presentation.navigation.LensPlate
import com.water0.hydration.presentation.navigation.PlateRangeBar
import com.water0.hydration.presentation.navigation.Routes
import com.water0.hydration.presentation.settings.SettingsScreen
import com.water0.hydration.ui.theme.GlassPrefs
import com.water0.hydration.ui.theme.GlassSnackbar
import com.water0.hydration.ui.theme.AuroraBackground
import com.water0.hydration.ui.theme.Water0
import com.water0.hydration.ui.theme.liquidglass.LiquidGlassContainer
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = this@MainActivity
                return HomeViewModel(
                    AppContainer.getGetTodayProgressUseCase(context),
                    AppContainer.getLogHydrationUseCase(context),
                    AppContainer.getCalculateRecommendationUseCase(),
                    AppContainer.getDeleteHydrationUseCase(context)
                ) as T
            }
        }
    }

    // No install-time permission prompt: notifications are opt-in via the
    // Settings → Reminders toggle, which requests POST_NOTIFICATIONS only
    // when the user actually enables reminders. The worker still runs and
    // reschedules without it — it just skips showing.

    // Hoisted so the Logs lens plate drives the same range state the
    // History list renders (one source of truth, no duplicate ViewModels).
    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(this)
    }

    @OptIn(
        androidx.compose.foundation.ExperimentalFoundationApi::class,
        androidx.compose.material3.ExperimentalMaterial3Api::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full-bleed background under status/nav bars (kills the grey
        // system-bar bands); Scaffold insets keep content clear of them.
        // (WindowCompat instead of enableEdgeToEdge: our activity-ktx
        // predates that helper.)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        seedDefaults()
        AppContainer.getNotificationScheduler(this).ensureScheduled()
        // Theme choice persists in plain SharedPreferences: a single
        // boolean flag is exactly what prefs are for (no DB migration).
        // Default is dark, Omarchy-style.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val glassPrefs = GlassPrefs.from(this)
        setContent {
            var darkTheme by remember {
                mutableStateOf(prefs.getBoolean(KEY_DARK_THEME, true))
            }
            // Glass Lab state: sliders write through prefs and recompose
            // the lens live — no restart needed.
            var glassConfig by remember { mutableStateOf(glassPrefs.applied()) }
            // Status/nav icon contrast follows the theme (dark-first app).
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).let {
                    it.isAppearanceLightStatusBars = !darkTheme
                    it.isAppearanceLightNavigationBars = !darkTheme
                }
            }
            Water0(darkTheme = darkTheme) {
                // First run shows onboarding (no profile row yet) instead of
                // silently seeded guesses. Existing installs skip straight
                // to the pager.
                var onboarded by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) {
                    onboarded = AppContainer.getDatabase(this@MainActivity)
                        .userProfileDao().getProfileSuspend() != null
                }
                if (onboarded == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (onboarded == false) {
                    com.water0.hydration.presentation.onboarding.OnboardingScreen(
                        onFinish = { profile ->
                            lifecycleScope.launch {
                                AppContainer.getDatabase(this@MainActivity)
                                    .userProfileDao().insert(profile)
                                onboarded = true
                                if (profile.remindersEnabled) {
                                    val scheduler = AppContainer
                                        .getNotificationScheduler(this@MainActivity)
                                    scheduler.ensureScheduled()
                                    // Wanted + granted: visible within seconds.
                                    scheduler.poke()
                                }
                            }
                        }
                    )
                } else {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { 4 }
                )
                val scope = rememberCoroutineScope()
                // Keep bottom-bar selection in sync when user swipes.
                var selected by remember { mutableStateOf(Routes.HOME) }
                // Prefill waiting for the Update tab: a tapped recommendation
                // hands its midpoint here, Update opens it in the custom
                // dialog unlogged, then this clears (log or walk away — the
                // behavior tracking notices either way).
                var updatePrefill by remember { mutableStateOf<Int?>(null) }
                LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (!pagerState.isScrollInProgress) {
                        selected = Routes.fromIndex(pagerState.currentPage)
                    }
                }
                val navigate: (String) -> Unit = { route ->
                    selected = route
                    scope.launch {
                        // Slow deliberate page glide to match the dock.
                        pagerState.animateScrollToPage(
                            Routes.indexOf(route),
                            animationSpec = tween(durationMillis = 450)
                        )
                    }
                    Unit
                }
                // Single Scaffold for the whole pager shell: snackbar host
                // only — no top bar (headers live in content now) and no
                // bottom slot (the glass bar floats as glassContent over the
                // sampled content for the lens to refract it). Its measured
                // height reserves the content inset; the toast is lifted
                // above it for the same reason.
                val snackbarHostState = remember { SnackbarHostState() }
                var barHeightDp by remember { mutableStateOf(0.dp) }
                // Floating lenses everywhere (top bar + bottom dock): content
                // flows full-bleed UNDER them so scrolled text refracts
                // through the glass instead of clipping at its edge. Each
                // screen owns a scrollable top gutter (passed below) for
                // initial clearance — it scrolls away, then content glides
                // behind the lens like the bottom dock. Starts estimated so
                // the first frame already clears.
                var plateHeightDp by remember { mutableStateOf(68.dp) }
                val density = LocalDensity.current
                val platePage = pagerState.currentPage
                val topInsetTarget = plateHeightDp
                val topInset by animateDpAsState(
                    targetValue = topInsetTarget,
                    animationSpec = tween(durationMillis = 250),
                    label = "topInset"
                )
                val daysBack by historyViewModel.daysBack.collectAsStateWithLifecycle()
                // Scrollable room above the floating dock: measured off the
                // lens itself, so the last card on any tab scrolls clear
                // instead of dying underneath it.
                val bottomGutter = barHeightDp + 20.dp
                // Background state comes from the shared HomeViewModel so the
                // ONE root Aurora below matches the glass everywhere.
                val homeUiState by viewModel.uiState.collectAsStateWithLifecycle()
                val rootTintTarget = hydrationTintFor(homeUiState)
                val rootTint by animateColorAsState(
                    targetValue = rootTintTarget,
                    animationSpec = tween(durationMillis = 1000),
                    label = "rootTint"
                )
                val rootEnergy = ((homeUiState as? HomeViewModel.UiState.Success)
                    ?.percentage?.div(100f) ?: 0.35f).coerceIn(0f, 1f)
                LiquidGlassContainer(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                // Full-bleed living background behind EVERYTHING (including
                // the lens bar zone) so the lens always samples real pixels.
                Box(modifier = Modifier.fillMaxSize()) {
                    AuroraBackground(
                        modifier = Modifier.fillMaxSize(),
                        hydrationTint = rootTint,
                        energy = rootEnergy
                    )
                    // Ambient mega-tank behind the pages: hero on Home, dim
                    // backdrop elsewhere. NOT inside the pager (it would
                    // swipe away) and NOT in the lens layer (it is meant to
                    // BE refracted, not to refract).
                    AmbientTank(
                        viewModel = viewModel,
                        selectedRoute = selected
                    )
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            snackbarHostState,
                            modifier = Modifier.padding(bottom = barHeightDp + 16.dp)
                        ) { data ->
                            GlassSnackbar(message = data.visuals.message)
                        }
                    },
                    containerColor = Color.Transparent,
                    content = { paddingValues ->
                    // Full-bleed: lists flow UNDER both floating lenses
                    // (top bar + dock) instead of clipping at their edges.
                    // barHeightDp survives for the toast lift only.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            // Page glide: fade + slight parallax while dragging.
                            // No blur here — blurring a full page per frame
                            // drops frames on mid devices (stutter source).
                            val pageOffset = (
                                (pagerState.currentPage - page) +
                                    pagerState.currentPageOffsetFraction
                                ).absoluteValue
                            val motionModifier = Modifier.graphicsLayer {
                                alpha = 1f - (pageOffset * 0.3f).coerceIn(0f, 0.5f)
                                translationX = -pagerState.currentPageOffsetFraction * 80f
                            }
                            Box(modifier = motionModifier.fillMaxSize()) {
                                when (page) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        topGutter = topInset,
                                        bottomGutter = bottomGutter,
                                        onRecLog = { amount ->
                                            updatePrefill = amount
                                            navigate(Routes.UPDATE)
                                        }
                                    )
                                    1 -> LogScreen(
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        topGutter = topInset,
                                        bottomGutter = bottomGutter,
                                        prefillAmount = updatePrefill,
                                        onPrefillConsumed = { updatePrefill = null }
                                    )
                                    2 -> HistoryScreen(
                                        snackbarHostState = snackbarHostState,
                                        viewModel = historyViewModel,
                                        topGutter = topInset,
                                        bottomGutter = bottomGutter
                                    )
                                    else -> SettingsScreen(
                                        darkTheme = darkTheme,
                                        onToggleTheme = { enabled ->
                                            darkTheme = enabled
                                            prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
                                        },
                                        glassConfig = glassConfig,
                                        onGlassConfigChange = {
                                            glassPrefs.saveApplied(it)
                                            glassConfig = it
                                        },
                                        topGutter = topInset,
                                        bottomGutter = bottomGutter
                                    )
                                }
                                }
                            }
                        }
                    }
                    )
                }
                },
                    glassContent = scope@{
                        // Floating top lens on EVERY tab (Home wears
                        // "Water0"). Pager-driven so titles never disagree
                        // with what's displayed. Logs carries its range
                        // options in the plate. Content flows beneath it.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            this@scope.LensPlate(
                                title = when (platePage) {
                                    0 -> "Water0"
                                    1 -> "Update"
                                    2 -> "Logs"
                                    else -> "Settings"
                                },
                                config = glassConfig,
                                // Home only: big centered brand. Other tabs
                                // stay left-aligned.
                                centeredTitle = platePage == 0,
                                    modifier = Modifier.onSizeChanged {
                                        plateHeightDp = with(density) { it.height.toDp() } + 12.dp
                                    },
                                    options = if (platePage == 2) {
                                        {
                                            PlateRangeBar(
                                                options = historyViewModel.rangeOptions,
                                                selected = daysBack,
                                                onSelect = { historyViewModel.setDaysBack(it) }
                                            )
                                        }
                                    } else null
                            )
                        }
                        // Directly in GlassBoxScope (no nested Box receiver).
                        GlassBottomBar(
                            selected = selected,
                            onSelect = navigate,
                            config = glassConfig,
                            onHeight = { barHeightDp = it }
                        )
                    }
                )
                } // else onboarded == true: the pager shell above
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "water0_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
    }

    // Behavior row only: the profile row is born in onboarding now
    // (no silent default guesses). Without behavior the Home screen
    // would load forever on a fresh install.
    private fun seedDefaults() {
        lifecycleScope.launch {
            val db = AppContainer.getDatabase(this@MainActivity)
            if (db.userBehaviorDao().getBehaviorSuspend() == null) {
                db.userBehaviorDao().insert(UserBehavior())
            }
        }
    }
}