package com.santiagorey.devflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.santiagorey.devflow.screens.StatefulLoginScreen
import com.santiagorey.devflow.ui.theme.DevFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevFlowTheme {
                StatefulLoginScreen()
            }
        }
    }
}
