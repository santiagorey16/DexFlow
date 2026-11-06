package com.santiagorey.devflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.santiagorey.devflow.screens.StatefulDashboardScreen
import com.santiagorey.devflow.screens.StatefulLoginScreen
import com.santiagorey.devflow.ui.theme.DevFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevFlowTheme {
                var currentScreen by remember { mutableStateOf("login") }

                if (currentScreen == "login") {
                    StatefulLoginScreen(
                        onLoginClick = { _, _ ->
                            currentScreen = "dashboard"
                        }
                    )
                } else {
                    StatefulDashboardScreen(
                        onNavigateToLogin = {
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}
