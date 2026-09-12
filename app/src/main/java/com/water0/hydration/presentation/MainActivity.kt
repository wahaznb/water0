package com.water0.hydration.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedDefaults()
        setContent {
            Water0 {
                var screen by remember { mutableStateOf(Routes.HOME) }
                val navigate = { route: String -> screen = route }
                when (screen) {
                    Routes.HISTORY -> HistoryScreen(onNavigate = navigate)
                    Routes.SETTINGS -> SettingsScreen(
                        onBackClick = { screen = Routes.HOME },
                        onNavigate = navigate
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