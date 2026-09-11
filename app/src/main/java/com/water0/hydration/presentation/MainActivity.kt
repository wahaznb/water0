package com.water0.hydration.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.water0.hydration.ui.theme.Water0

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Water0 {
                androidx.compose.material3.Text(text = "Water0 - Coming Soon")
            }
        }
    }
}