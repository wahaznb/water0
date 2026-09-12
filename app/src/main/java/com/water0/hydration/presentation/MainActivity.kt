package com.water0.hydration.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.history.HistoryScreen
import com.water0.hydration.presentation.home.HomeScreen
import com.water0.hydration.presentation.home.HomeViewModel
import com.water0.hydration.presentation.navigation.GlassBottomBar
import com.water0.hydration.presentation.navigation.Routes
import com.water0.hydration.presentation.settings.SettingsScreen
import com.water0.hydration.ui.theme.GlassPrefs
import com.water0.hydration.ui.theme.GlassSnackbar
import com.water0.hydration.ui.theme.Water0
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
                    AppContainer.getCalculateRecommendationUseCase()
                ) as T
            }
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    @OptIn(
        androidx.compose.foundation.ExperimentalFoundationApi::class,
        androidx.compose.material3.ExperimentalMaterial3Api::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedDefaults()
        requestNotificationPermission()
        AppContainer.getNotificationScheduler(this).ensureScheduled()
        // Theme choice persists in plain SharedPreferences: a single
        // boolean flag is exactly what prefs are for (no DB migration).
        // Default is dark, Omarchy-style.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val glassPrefs = GlassPrefs.from(this)
        val glassConfig = glassPrefs.applied()
        setContent {
            var darkTheme by remember {
                mutableStateOf(prefs.getBoolean(KEY_DARK_THEME, true))
            }
            Water0(darkTheme = darkTheme) {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { 3 }
                )
                val scope = rememberCoroutineScope()
                // Keep bottom-bar selection in sync when user swipes.
                var selected by remember { mutableStateOf(Routes.HOME) }
                LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (!pagerState.isScrollInProgress) {
                        selected = Routes.fromIndex(pagerState.currentPage)
                    }
                }
                val navigate: (String) -> Unit = { route ->
                    selected = route
                    scope.launch {
                        pagerState.animateScrollToPage(Routes.indexOf(route))
                    }
                    Unit
                }
                // Single Scaffold for the whole pager shell: one top bar
                // (per page), one glass bottom bar, one snackbar host.
                // Screens are content-only and share the host.
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            GlassSnackbar(message = data.visuals.message)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        val topColors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                        when (pagerState.currentPage) {
                            1 -> TopAppBar(
                                title = {
                                    Text(
                                        "History",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                },
                                colors = topColors
                            )
                            2 -> TopAppBar(
                                title = {
                                    Text(
                                        "Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navigate(Routes.HOME) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                },
                                colors = topColors
                            )
                            else -> TopAppBar(
                                title = {
                                    Text(
                                        "Water0",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                },
                                colors = topColors,
                                actions = {
                                    IconButton(onClick = { navigate(Routes.SETTINGS) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        GlassBottomBar(
                            selected = selected,
                            onSelect = navigate,
                            config = glassConfig
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            // Motion-blur fake: while dragging (|offset|>0),
                            // fade + slight blur the outgoing page. At rest
                            // offset=0 so no cost. Max blur follows the Glass
                            // Lab setting (apply-on-restart). True motion blur
                            // is not available in Compose; this approximates it.
                            val pageOffset = (
                                (pagerState.currentPage - page) +
                                    pagerState.currentPageOffsetFraction
                                ).absoluteValue
                            val maxBlur = glassConfig.blurRadius.coerceAtMost(24.dp)
                            val motionBlur = (pageOffset * maxBlur.value).dp.coerceAtMost(maxBlur)
                            val motionModifier = if (pageOffset > 0.001f && android.os.Build.VERSION.SDK_INT >= 31) {
                                Modifier
                                    .graphicsLayer {
                                        alpha = 1f - (pageOffset * 0.4f).coerceIn(0f, 0.6f)
                                        translationX = -pagerState.currentPageOffsetFraction * 120f
                                    }
                                    .blur(motionBlur)
                            } else {
                                Modifier.graphicsLayer {
                                    alpha = 1f - (pageOffset * 0.25f).coerceIn(0f, 0.5f)
                                }
                            }
                            Box(modifier = motionModifier.fillMaxSize()) {
                                when (page) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState
                                    )
                                    1 -> HistoryScreen(
                                        snackbarHostState = snackbarHostState
                                    )
                                    else -> SettingsScreen(
                                        darkTheme = darkTheme,
                                        onToggleTheme = { enabled ->
                                            darkTheme = enabled
                                            prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "water0_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
    }

    // POST_NOTIFICATIONS is runtime-gated on API 33+. Without it the
    // worker still runs and reschedules — it just skips showing.
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Room Flow queries emit nothing until a row exists, so insert
    // defaults once. Without this the Home screen would load forever
    // on a fresh install.
    private fun seedDefaults() {
        lifecycleScope.launch {
            val db = AppContainer.getDatabase(this@MainActivity)
            if (db.userProfileDao().getProfileSuspend() == null) {
                db.userProfileDao().insert(UserProfile())
            }
            if (db.userBehaviorDao().getBehaviorSuspend() == null) {
                db.userBehaviorDao().insert(UserBehavior())
            }
        }
    }
}