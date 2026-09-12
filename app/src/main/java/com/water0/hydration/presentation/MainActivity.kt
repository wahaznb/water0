package com.water0.hydration.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.home.HomeScreen
import com.water0.hydration.presentation.home.HomeViewModel
import com.water0.hydration.ui.theme.Water0

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
        setContent {
            Water0 {
                HomeScreen(viewModel)
            }
        }
    }
}