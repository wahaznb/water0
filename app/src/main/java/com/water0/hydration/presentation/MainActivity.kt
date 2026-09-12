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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.history.HistoryScreen
import com.water0.hydration.presentation.home.HomeScreen
import com.water0.hydration.presentation.home.HomeViewModel
import com.water0.hydration.presentation.navigation.Routes
import com.water0.hydration.presentation.settings.SettingsScreen
import com.water0.hydration.ui.theme.Water0
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedDefaults()
        requestNotificationPermission()
        AppContainer.getNotificationScheduler(this).ensureScheduled()
        // Theme choice persists in plain SharedPreferences: a single
        // boolean flag is exactly what prefs are for (no DB migration).
        // Default is dark, Omarchy-style.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        setContent {
            var darkTheme by remember {
                mutableStateOf(prefs.getBoolean(KEY_DARK_THEME, true))
            }
            Water0(darkTheme = darkTheme) {
                var screen by remember { mutableStateOf(Routes.HOME) }
                val navigate = { route: String -> screen = route }
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) +
                            slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(250)) { it / 5 }) togetherWith
                            (fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                                slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(200)) { -it / 5 })
                    },
                    label = "screen"
                ) { current ->
                    when (current) {
                        Routes.HISTORY -> HistoryScreen(onNavigate = navigate)
                        Routes.SETTINGS -> SettingsScreen(
                            onBackClick = { screen = Routes.HOME },
                            onNavigate = navigate,
                            darkTheme = darkTheme,
                            onToggleTheme = { enabled ->
                                darkTheme = enabled
                                prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
                            }
                        )
                        else -> HomeScreen(
                            viewModel = viewModel,
                            onSettingsClick = { screen = Routes.SETTINGS },
                            onNavigate = navigate
                        )
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