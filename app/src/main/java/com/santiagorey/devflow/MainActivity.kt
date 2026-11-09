package com.santiagorey.devflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.santiagorey.devflow.auth.AuthViewModel
import com.santiagorey.devflow.screens.ForgotPasswordScreen
import com.santiagorey.devflow.screens.RegisterScreen
import com.santiagorey.devflow.screens.StatefulDashboardScreen
import com.santiagorey.devflow.screens.StatefulLoginScreen
import com.santiagorey.devflow.ui.theme.DevFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DevFlowTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val uiState by authViewModel.uiState.collectAsState()
                val context = LocalContext.current
                val activity = this@MainActivity

                // Toast Feedback for error/success messages
                LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
                    uiState.errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        authViewModel.clearMessages()
                    }
                    uiState.successMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        authViewModel.clearMessages()
                    }
                }

                // Automatic Auth Navigation Handling
                LaunchedEffect(uiState.isAuthenticated) {
                    if (uiState.isAuthenticated) {
                        if (navController.currentDestination?.route != "dashboard") {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    } else {
                        if (navController.currentDestination?.route == "dashboard") {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }
                }

                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "dashboard" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("login") {
                        StatefulLoginScreen(
                            isLoading = uiState.isLoading,
                            onLoginClick = { email, password ->
                                authViewModel.loginWithEmail(email, password)
                            },
                            onGoogleClick = { idToken ->
                                authViewModel.loginWithGoogle(idToken)
                            },
                            onGitHubClick = {
                                authViewModel.loginWithGitHub(activity)
                            },
                            onForgotPasswordClick = {
                                navController.navigate("forgot_password")
                            },
                            onCreateAccountClick = {
                                navController.navigate("register")
                            }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            isLoading = uiState.isLoading,
                            onRegisterClick = { name, email, password, confirm ->
                                authViewModel.registerWithEmail(name, email, password, confirm)
                            },
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("forgot_password") {
                        ForgotPasswordScreen(
                            isLoading = uiState.isLoading,
                            successMessage = uiState.successMessage,
                            onSendResetClick = { email ->
                                authViewModel.sendPasswordReset(email)
                            },
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("forgot_password") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        StatefulDashboardScreen(
                            currentUser = uiState.currentUser,
                            onLogoutClick = {
                                authViewModel.logout()
                            },
                            onNavigateToLogin = {
                                authViewModel.logout()
                            }
                        )
                    }
                }
            }
        }
    }
}
