package com.santiagorey.devflow

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.auth.AuthUiState
import com.santiagorey.devflow.auth.AuthViewModel
import com.santiagorey.devflow.auth.PROVIDER_GITHUB
import com.santiagorey.devflow.notifications.DevFlowAlert
import com.santiagorey.devflow.projects.ProjectsViewModel
import com.santiagorey.devflow.screens.AiSetupScreen
import com.santiagorey.devflow.screens.AssistantScreen
import com.santiagorey.devflow.screens.ClientDetailScreen
import com.santiagorey.devflow.screens.ClientReportCard
import com.santiagorey.devflow.screens.CustomersScreen
import com.santiagorey.devflow.screens.DashboardScreen
import com.santiagorey.devflow.screens.ForgotPasswordScreen
import com.santiagorey.devflow.screens.GuidesScreens
import com.santiagorey.devflow.screens.LearningCard
import com.santiagorey.devflow.screens.NotificationsScreen
import com.santiagorey.devflow.screens.PrivacyScreen
import com.santiagorey.devflow.screens.ProjectDetailScreen
import com.santiagorey.devflow.screens.ProjectOverviewScreen
import com.santiagorey.devflow.screens.ProyectScreens
import com.santiagorey.devflow.screens.RegisterScreen
import com.santiagorey.devflow.screens.RepoAuditCard
import com.santiagorey.devflow.screens.SettingsScreens
import com.santiagorey.devflow.screens.StatefulLoginScreen
import com.santiagorey.devflow.screens.TaskActions
import com.santiagorey.devflow.screens.launchGoogleSignIn

/**
 * Todas las pantallas de la app y cómo se navega entre ellas.
 *
 * Cada `composable(ruta) { ... }` dice qué pantalla se dibuja en esa ruta y le pasa sus datos
 * (desde los ViewModels) y sus acciones (lambdas como onBackClick). Las pantallas no conocen
 * al navController: avisan "tocaron atrás" y aquí se decide a dónde ir.
 *
 * @param startDestination primera pantalla: "login" sin sesión o "dashboard" con sesión.
 */
@Composable
internal fun DevFlowNavHost(
    navController: NavHostController,
    startDestination: String,
    uiState: AuthUiState,
    authViewModel: AuthViewModel,
    projectsViewModel: ProjectsViewModel,
    assistantViewModel: AssistantViewModel,
    activity: Activity,
    alerts: List<DevFlowAlert>,
    qualityAlertsEnabled: Boolean,
    navigateToTab: (Int) -> Unit,
    logout: () -> Unit,
    openAiSetup: () -> Unit
) {
    val context = LocalContext.current
    // Para lanzar el selector de cuentas de Google al tocar un botón (en Ajustes).
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Fuera de sesión ──
        // Login: los datos van al AuthViewModel; si sale bien, MainActivity navega sola al Dashboard.
        composable(Routes.LOGIN) {
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
                onTabSelected = navigateToTab,
                onLogoutClick = logout,
                onNewProject = { navController.navigate(Routes.projectNew()) },
                onNewClient = { navController.navigate(Routes.clientNew()) },
                aiConnectedLabel = assistantViewModel.provider
                    ?.takeIf { assistantViewModel.isConfigured }?.displayName,
                onOpenAssistant = { id -> navController.navigate(Routes.assistant(id)) },
                onOpenProject = { id -> navController.navigate(Routes.projectDetail(id)) },
                coachTip = assistantViewModel.coachTip,
                coachLoading = assistantViewModel.isLoadingCoach
            )
        }

        composable(Routes.PROJECTS) {
            ProyectScreens(
                currentUser = uiState.currentUser,
                projects = projectsViewModel.projects,
                clients = projectsViewModel.clients,
                onTabSelected = navigateToTab,
                onLogoutClick = logout,
                onNewProjectClick = { navController.navigate(Routes.projectNew()) },
                onOpenProject = { id -> navController.navigate(Routes.projectDetail(id)) },
                onOpenAssistant = { navController.navigate(Routes.assistant()) },
                onOpenClient = { id -> navController.navigate(Routes.clientDetail(id)) }
            )
        }

        // Detalle del proyecto: resumen, avance, tareas y acciones
        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
            val project = projectsViewModel.getProject(projectId)
            if (project == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ProjectOverviewScreen(
                    project = project,
                    client = project.clientId?.let { projectsViewModel.getClient(it) },
                    assistant = assistantViewModel,
                    onBackClick = { navController.popBackStack() },
                    onOpenClient = { id -> navController.navigate(Routes.clientDetail(id)) },
                    onEdit = { navController.navigate(Routes.projectEdit(project.id)) },
                    onDelete = {
                        navController.popBackStack()
                        projectsViewModel.deleteProject(project.id)
                    },
                    onReviewWithAi = { navController.navigate(Routes.assistant(project.id)) },
                    onSaveProgress = { value -> projectsViewModel.updateProgress(project.id, value) },
                    taskActions = TaskActions(
                        onAdd = { title, phase -> projectsViewModel.addTask(project.id, title, phase) },
                        onAddMany = { tasks -> projectsViewModel.addTasks(project.id, tasks) },
                        onSetStatus = { task, status -> projectsViewModel.setTaskStatus(project.id, task.id, status) },
                        onDelete = { taskId -> projectsViewModel.deleteTask(project.id, taskId) },
                        onStartTimer = { taskId -> projectsViewModel.startTaskTimer(project.id, taskId) },
                        onStopTimer = { taskId -> projectsViewModel.stopTaskTimer(project.id, taskId) },
                        onLinkIssue = { taskId, number -> projectsViewModel.linkTaskIssue(project.id, taskId, number) },
                        onApplyIssueStates = { states -> projectsViewModel.applyIssueStates(project.id, states) }
                    )
                )
            }
        }

        composable(Routes.GUIDES) {
            GuidesScreens(
                currentUser = uiState.currentUser,
                onTabSelected = navigateToTab,
                onLogoutClick = logout,
                onOpenSettings = { navigateToTab(4) },
                onAskAi = { prompt -> navController.navigate(Routes.assistant(prompt = prompt)) },
                learningCard = {
                    LearningCard(
                        assistant = assistantViewModel,
                        projects = projectsViewModel.projects,
                        onConnectAi = openAiSetup,
                        onAskAi = { prompt -> navController.navigate(Routes.assistant(prompt = prompt)) }
                    )
                },
                auditCard = {
                    RepoAuditCard(
                        assistant = assistantViewModel,
                        projects = projectsViewModel.projects,
                        githubLinked = PROVIDER_GITHUB in uiState.providerIds,
                        githubRepoAccess = uiState.githubRepoAccess,
                        linkingGitHub = uiState.linkingProvider == PROVIDER_GITHUB,
                        onConnectGitHubRepos = { authViewModel.connectGitHubRepos(activity) },
                        onReviewWithAi = { id -> navController.navigate(Routes.assistant(id)) }
                    )
                }
            )
        }

        composable(Routes.CUSTOMERS) {
            CustomersScreen(
                currentUser = uiState.currentUser,
                clients = projectsViewModel.clients,
                projects = projectsViewModel.projects,
                onTabSelected = navigateToTab,
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
                experience = projectsViewModel.experience,
                projectCount = projectsViewModel.projects.size,
                completedCount = projectsViewModel.projects.count { it.isCompleted },
                clientCount = projectsViewModel.clients.size,
                onTabSelected = navigateToTab,
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
                onDisconnect = authViewModel::unlinkProvider,
                isProfileBusy = uiState.isUpdatingProfile,
                onUpdateName = authViewModel::updateDisplayName,
                onChangePassword = authViewModel::changePassword,
                mentorMode = assistantViewModel.mentorMode,
                onMentorModeChange = assistantViewModel::updateMentorMode,
                aiStatusLabel = assistantViewModel.provider
                    ?.takeIf { assistantViewModel.isConfigured }
                    ?.let { "${it.displayName} · ${assistantViewModel.modelId}" },
                githubRepoAccess = uiState.githubRepoAccess,
                onOpenAiSetup = openAiSetup,
                onOpenAssistant = { navController.navigate(Routes.assistant()) },
                onDisconnectAi = assistantViewModel::disconnectAi,
                onConnectGitHubRepos = { authViewModel.connectGitHubRepos(activity) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onDeleteAccount = {
                    authViewModel.deleteAccount {
                        // También se borran las claves de IA guardadas en el teléfono.
                        assistantViewModel.disconnectAi()
                    }
                }
            )
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBackClick = { navController.popBackStack() })
        }

        // Tutorial: conectar la IA de confianza del usuario
        composable(Routes.AI_SETUP) {
            AiSetupScreen(
                setup = assistantViewModel.setup,
                onBackClick = { navController.popBackStack() },
                onTestKey = assistantViewModel::testKey,
                onSelectModel = assistantViewModel::selectModel,
                onFinish = {
                    assistantViewModel.finishSetup()
                    Toast.makeText(context, "¡IA conectada! Ya puedes usar el asistente.", Toast.LENGTH_SHORT).show()
                    // Si se llegó desde el asistente se vuelve a él; si no, se abre.
                    val cameFromAssistant = navController.previousBackStackEntry
                        ?.destination?.route == Routes.ASSISTANT
                    if (cameFromAssistant) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.assistant()) {
                            popUpTo(Routes.AI_SETUP) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Asistente IA (opcionalmente enfocado en un proyecto)
        composable(
            route = Routes.ASSISTANT,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prompt") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            AssistantScreen(
                initialPrompt = backStackEntry.arguments?.getString("prompt"),
                assistant = assistantViewModel,
                projects = projectsViewModel.projects,
                clients = projectsViewModel.clients,
                initialProjectId = backStackEntry.arguments?.getString("projectId"),
                linkingGitHub = uiState.linkingProvider == PROVIDER_GITHUB,
                hasGitHubAccess = uiState.githubRepoAccess,
                onBackClick = { navController.popBackStack() },
                onOpenSetup = openAiSetup,
                onConnectGitHub = { authViewModel.connectGitHubRepos(activity) },
                onLinkRepo = projectsViewModel::linkRepo
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

        // Editar un proyecto existente (mismo asistente, con los datos cargados)
        composable(
            route = Routes.PROJECT_EDIT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
            val project = projectsViewModel.getProject(projectId)
            val newClientId by backStackEntry.savedStateHandle
                .getStateFlow<String?>(Routes.RESULT_NEW_CLIENT_ID, null)
                .collectAsState()

            if (project == null) {
                // El proyecto fue eliminado o no existe
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ProjectDetailScreen(
                    clients = projectsViewModel.clients,
                    project = project,
                    newlyCreatedClientId = newClientId,
                    onBackClick = { navController.popBackStack() },
                    onAddClientClick = { navController.navigate(Routes.clientNew(fromProject = true)) },
                    onSaveProject = { updated ->
                        projectsViewModel.updateProject(updated)
                        navController.popBackStack()
                    }
                )
            }
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
                    },
                    reportSection = {
                        ClientReportCard(
                            assistant = assistantViewModel,
                            client = client,
                            projects = projectsViewModel.projectsForClient(client.id),
                            onConnectAi = openAiSetup
                        )
                    }
                )
            }
        }

        // Avisos de la campana
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                alerts = alerts,
                qualityAlertsEnabled = qualityAlertsEnabled,
                onBackClick = { navController.popBackStack() },
                onAlertClick = { alert ->
                    when {
                        alert.url != null -> runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alert.url)))
                        }
                        alert.projectId != null -> navController.navigate(Routes.projectDetail(alert.projectId))
                    }
                }
            )
        }
    }
}
