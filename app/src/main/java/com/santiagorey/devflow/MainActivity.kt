package com.santiagorey.devflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.santiagorey.devflow.auth.AuthViewModel
import com.santiagorey.devflow.projects.ProjectsViewModel
import com.santiagorey.devflow.screens.ClientDetailScreen
import com.santiagorey.devflow.screens.CustomersScreen
import com.santiagorey.devflow.screens.DashboardScreen
import com.santiagorey.devflow.screens.ForgotPasswordScreen
import com.santiagorey.devflow.screens.GuidesScreens
import com.santiagorey.devflow.screens.ProjectDetailScreen
import com.santiagorey.devflow.screens.ProyectScreens
import com.santiagorey.devflow.screens.RegisterScreen
import com.santiagorey.devflow.screens.SettingsScreens
import com.santiagorey.devflow.screens.StatefulLoginScreen
import com.santiagorey.devflow.screens.isTempLogin
import com.santiagorey.devflow.screens.launchGoogleSignIn
import com.santiagorey.devflow.ui.theme.DevFlowTheme

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val GUIDES = "guides"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"

    const val PROJECT_NEW = "project_new?clientId={clientId}"
    fun projectNew(clientId: String? = null) =
        if (clientId == null) "project_new" else "project_new?clientId=$clientId"

    const val CLIENT_NEW = "client_new?fromProject={fromProject}"
    fun clientNew(fromProject: Boolean = false) = "client_new?fromProject=$fromProject"

    const val CLIENT_DETAIL = "client_detail/{clientId}"
    fun clientDetail(clientId: String) = "client_detail/$clientId"

    val authRoutes = setOf(LOGIN, REGISTER, FORGOT_PASSWORD)

    /** Clave para devolver al asistente de proyecto el cliente recién creado. */
    const val RESULT_NEW_CLIENT_ID = "new_client_id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DevFlowTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val projectsViewModel: ProjectsViewModel = viewModel()
                val uiState by authViewModel.uiState.collectAsState()
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val activity = this@MainActivity

                // Se calcula una sola vez: con sesión iniciada la app abre directo en el Dashboard.
                val startDestination = remember {
                    if (uiState.isAuthenticated) Routes.DASHBOARD else Routes.LOGIN
                }

                fun navigateToTab(tabIndex: Int) {
                    val route = when (tabIndex) {
                        0 -> Routes.DASHBOARD
                        1 -> Routes.PROJECTS
                        2 -> Routes.GUIDES
                        3 -> Routes.CUSTOMERS
                        4 -> Routes.SETTINGS
                        else -> Routes.DASHBOARD
                    }
                    if (navController.currentDestination?.route != route) {
                        navController.navigate(route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                // Carga los proyectos/clientes de la cuenta activa
                LaunchedEffect(uiState.dataKey) {
                    projectsViewModel.setUser(uiState.dataKey)
                }

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

                // Al iniciar sesión el Dashboard pasa a ser la raíz (el login sale de la pila);
                // al cerrar sesión se vuelve al login limpiando todo.
                LaunchedEffect(uiState.isAuthenticated) {
                    val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect
                    if (uiState.isAuthenticated && currentRoute in Routes.authRoutes) {
                        navController.resetTo(Routes.DASHBOARD)
                    } else if (!uiState.isAuthenticated && currentRoute !in Routes.authRoutes) {
                        navController.resetTo(Routes.LOGIN)
                    }
                }

                val logout: () -> Unit = { authViewModel.logout() }

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(Routes.LOGIN) {
                        StatefulLoginScreen(
                            isLoading = uiState.isLoading,
                            onLoginClick = { email, password ->
                                if (isTempLogin(email, password)) {
                                    authViewModel.loginTemp()
                                } else {
                                    authViewModel.loginWithEmail(email, password)
                                }
                            },
                            onGoogleClick = { idToken ->
                                authViewModel.loginWithGoogle(idToken)
                            },
                            onGitHubClick = {
                                authViewModel.loginWithGitHub(activity)
                            },
                            onForgotPasswordClick = {
                                navController.navigate(Routes.FORGOT_PASSWORD)
                            },
                            onCreateAccountClick = {
                                navController.navigate(Routes.REGISTER)
                            }
                        )
                    }

                    composable(Routes.REGISTER) {
                        RegisterScreen(
                            isLoading = uiState.isLoading,
                            onRegisterClick = { name, email, password, confirm ->
                                authViewModel.registerWithEmail(name, email, password, confirm)
                            },
                            onNavigateToLogin = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.REGISTER) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.FORGOT_PASSWORD) {
                        ForgotPasswordScreen(
                            isLoading = uiState.isLoading,
                            successMessage = uiState.successMessage,
                            onSendResetClick = { email ->
                                authViewModel.sendPasswordReset(email)
                            },
                            onNavigateToLogin = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            currentUser = uiState.currentUser,
                            projects = projectsViewModel.projects,
                            clients = projectsViewModel.clients,
                            activity = projectsViewModel.activity,
                            onTabSelected = ::navigateToTab,
                            onLogoutClick = logout,
                            onNewProject = { navController.navigate(Routes.projectNew()) },
                            onNewClient = { navController.navigate(Routes.clientNew()) }
                        )
                    }

                    composable(Routes.PROJECTS) {
                        ProyectScreens(
                            currentUser = uiState.currentUser,
                            projects = projectsViewModel.projects,
                            clients = projectsViewModel.clients,
                            onTabSelected = ::navigateToTab,
                            onLogoutClick = logout,
                            onNavigateToProjectDetail = { navController.navigate(Routes.projectNew()) },
                            onUpdateProgress = projectsViewModel::updateProgress,
                            onDeleteProject = projectsViewModel::deleteProject,
                            onOpenClient = { id -> navController.navigate(Routes.clientDetail(id)) }
                        )
                    }

                    composable(Routes.GUIDES) {
                        GuidesScreens(
                            currentUser = uiState.currentUser,
                            onTabSelected = ::navigateToTab,
                            onLogoutClick = logout,
                            onOpenSettings = { navigateToTab(4) }
                        )
                    }

                    composable(Routes.CUSTOMERS) {
                        CustomersScreen(
                            currentUser = uiState.currentUser,
                            clients = projectsViewModel.clients,
                            projects = projectsViewModel.projects,
                            onTabSelected = ::navigateToTab,
                            onLogoutClick = logout,
                            onClientClick = { id -> navController.navigate(Routes.clientDetail(id)) },
                            onAddClientClick = { navController.navigate(Routes.clientNew()) }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreens(
                            currentUser = uiState.currentUser,
                            providerIds = uiState.providerIds,
                            githubUsername = uiState.githubUsername,
                            linkingProvider = uiState.linkingProvider,
                            isTempSession = uiState.isTempSession,
                            experience = projectsViewModel.experience,
                            projectCount = projectsViewModel.projects.size,
                            completedCount = projectsViewModel.projects.count { it.isCompleted },
                            clientCount = projectsViewModel.clients.size,
                            onTabSelected = ::navigateToTab,
                            onLogoutClick = logout,
                            onConnectGitHub = { authViewModel.linkGitHub(activity) },
                            onConnectGoogle = {
                                launchGoogleSignIn(
                                    context = context,
                                    coroutineScope = coroutineScope,
                                    onSuccess = authViewModel::linkGoogle,
                                    onError = authViewModel::showError
                                )
                            },
                            onDisconnect = authViewModel::unlinkProvider
                        )
                    }

                    // Asistente de nuevo proyecto (opcionalmente con un cliente ya elegido)
                    composable(
                        route = Routes.PROJECT_NEW,
                        arguments = listOf(
                            navArgument("clientId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val preselectedClientId = backStackEntry.arguments?.getString("clientId")
                        val newClientId by backStackEntry.savedStateHandle
                            .getStateFlow<String?>(Routes.RESULT_NEW_CLIENT_ID, null)
                            .collectAsState()

                        ProjectDetailScreen(
                            clients = projectsViewModel.clients,
                            preselectedClientId = preselectedClientId,
                            newlyCreatedClientId = newClientId,
                            onBackClick = { navController.popBackStack() },
                            onAddClientClick = { navController.navigate(Routes.clientNew(fromProject = true)) },
                            onCreateProject = { project ->
                                projectsViewModel.addProject(project)
                                if (preselectedClientId != null) {
                                    // Venía desde la ficha del cliente: se vuelve a ella.
                                    navController.popBackStack()
                                } else {
                                    navController.navigate(Routes.PROJECTS) {
                                        popUpTo(Routes.DASHBOARD)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    // Formulario de nuevo cliente
                    composable(
                        route = Routes.CLIENT_NEW,
                        arguments = listOf(
                            navArgument("fromProject") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val fromProject = backStackEntry.arguments?.getBoolean("fromProject") ?: false
                        ClientDetailScreen(
                            client = null,
                            onBackClick = { navController.popBackStack() },
                            onSaveClient = { client ->
                                val saved = projectsViewModel.addClient(client)
                                if (fromProject) {
                                    // Se devuelve el cliente creado al asistente de proyecto.
                                    navController.previousBackStackEntry?.savedStateHandle
                                        ?.set(Routes.RESULT_NEW_CLIENT_ID, saved.id)
                                    navController.popBackStack()
                                } else {
                                    navController.navigate(Routes.clientDetail(saved.id)) {
                                        popUpTo(Routes.CLIENT_NEW) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    // Ficha del cliente: llamar, escribir y asignar proyectos
                    composable(
                        route = Routes.CLIENT_DETAIL,
                        arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val clientId = backStackEntry.arguments?.getString("clientId").orEmpty()
                        val client = projectsViewModel.clients.firstOrNull { it.id == clientId }

                        if (client == null) {
                            // El cliente fue eliminado o no existe
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            ClientDetailScreen(
                                client = client,
                                projects = projectsViewModel.projects,
                                onBackClick = { navController.popBackStack() },
                                onSaveClient = projectsViewModel::updateClient,
                                onDeleteClient = { id ->
                                    navController.popBackStack()
                                    projectsViewModel.deleteClient(id)
                                },
                                onAssignProject = projectsViewModel::assignClientToProject,
                                onNewProjectForClient = { id ->
                                    navController.navigate(Routes.projectNew(clientId = id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Navega a [route] dejándola como única pantalla en la pila (no se puede volver atrás). */
private fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
