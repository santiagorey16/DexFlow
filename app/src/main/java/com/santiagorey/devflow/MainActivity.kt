package com.santiagorey.devflow

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
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
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.auth.AuthViewModel
import com.santiagorey.devflow.auth.PROVIDER_GITHUB
import com.santiagorey.devflow.projects.ProjectsViewModel
import com.santiagorey.devflow.notifications.AlertCache
import com.santiagorey.devflow.notifications.DevFlowNotifications
import com.santiagorey.devflow.notifications.ReminderWorker
import com.santiagorey.devflow.notifications.buildAlerts
import com.santiagorey.devflow.screens.AiSetupScreen
import com.santiagorey.devflow.screens.LocalNotificationBell
import com.santiagorey.devflow.screens.NotificationBellState
import com.santiagorey.devflow.screens.NotificationsScreen
import com.santiagorey.devflow.screens.OfflineBanner
import com.santiagorey.devflow.screens.rememberIsOnline
import com.santiagorey.devflow.screens.PREF_COACH_LIVE
import com.santiagorey.devflow.screens.PREF_QUALITY_ALERTS
import com.santiagorey.devflow.screens.rememberBooleanPreference
import com.santiagorey.devflow.screens.AssistantScreen
import com.santiagorey.devflow.screens.ClientDetailScreen
import com.santiagorey.devflow.screens.ClientReportCard
import com.santiagorey.devflow.screens.CustomersScreen
import com.santiagorey.devflow.screens.DashboardScreen
import com.santiagorey.devflow.screens.ForgotPasswordScreen
import com.santiagorey.devflow.screens.GuidesScreens
import com.santiagorey.devflow.screens.LearningCard
import com.santiagorey.devflow.screens.ProjectDetailScreen
import com.santiagorey.devflow.screens.ProjectOverviewScreen
import com.santiagorey.devflow.screens.PrivacyScreen
import com.santiagorey.devflow.screens.ProyectScreens
import com.santiagorey.devflow.screens.RegisterScreen
import com.santiagorey.devflow.screens.RepoAuditCard
import com.santiagorey.devflow.screens.SettingsScreens
import com.santiagorey.devflow.screens.StatefulLoginScreen
import com.santiagorey.devflow.screens.TaskActions
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

    const val PROJECT_DETAIL = "project/{projectId}"
    fun projectDetail(projectId: String) = "project/$projectId"

    const val PROJECT_EDIT = "project_edit/{projectId}"
    fun projectEdit(projectId: String) = "project_edit/$projectId"

    const val CLIENT_NEW = "client_new?fromProject={fromProject}"
    fun clientNew(fromProject: Boolean = false) = "client_new?fromProject=$fromProject"

    const val CLIENT_DETAIL = "client_detail/{clientId}"
    fun clientDetail(clientId: String) = "client_detail/$clientId"

    const val AI_SETUP = "ai_setup"
    const val NOTIFICATIONS = "notifications"
    const val PRIVACY = "privacy"

    const val ASSISTANT = "assistant?projectId={projectId}&prompt={prompt}"
    fun assistant(projectId: String? = null, prompt: String? = null): String {
        val params = listOfNotNull(
            projectId?.let { "projectId=$it" },
            prompt?.let { "prompt=${Uri.encode(it)}" }
        )
        return if (params.isEmpty()) "assistant" else "assistant?" + params.joinToString("&")
    }

    val authRoutes = setOf(LOGIN, REGISTER, FORGOT_PASSWORD)

    /** Clave para devolver al asistente de proyecto el cliente recién creado. */
    const val RESULT_NEW_CLIENT_ID = "new_client_id"
}

class MainActivity : ComponentActivity() {

    /** Pantalla a abrir cuando se toca una notificación (p. ej. "notifications"). */
    private var pendingOpen by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpen = intent.getStringExtra(DevFlowNotifications.EXTRA_OPEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de inicio: fondo negro con el logo (debe ir antes de super.onCreate).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingOpen = intent?.getStringExtra(DevFlowNotifications.EXTRA_OPEN)
        DevFlowNotifications.createChannel(this)
        ReminderWorker.schedule(this)

        setContent {
            DevFlowTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val projectsViewModel: ProjectsViewModel = viewModel()
                val assistantViewModel: AssistantViewModel = viewModel()
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
                    if (navController.currentDestination?.route == route) return

                    if (route == Routes.DASHBOARD) {
                        // El Dashboard es la raíz: se vuelve a él sacando lo que haya encima.
                        // (Navegar hacia él con restoreState restauraba la pestaña anterior
                        // y el usuario quedaba atrapado, p. ej. después de crear un proyecto.)
                        val popped = navController.popBackStack(
                            Routes.DASHBOARD,
                            inclusive = false,
                            saveState = true
                        )
                        if (!popped) navController.resetTo(Routes.DASHBOARD)
                        return
                    }

                    navController.navigate(route) {
                        popUpTo(Routes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                // Carga los proyectos/clientes de la cuenta activa
                // (se sincronizan con Firestore)
                LaunchedEffect(uiState.dataKey) {
                    projectsViewModel.setUser(
                        key = uiState.dataKey,
                        remote = uiState.currentUser != null
                    )
                }

                // IA conectada y token de GitHub de la cuenta activa
                LaunchedEffect(uiState.dataKey, uiState.githubRepoAccess) {
                    assistantViewModel.setAccount(key = uiState.dataKey, uid = uiState.currentUser?.uid)
                }

                // Avisa si falla la sincronización con la nube
                LaunchedEffect(projectsViewModel.syncError) {
                    projectsViewModel.syncError?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        projectsViewModel.clearSyncError()
                    }
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

                val logout: () -> Unit = {
                    AlertCache.clear(context)
                    authViewModel.logout()
                }

                // ── Notificaciones push: permiso (Android 13+), datos para el trabajo en segundo plano ──
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(uiState.isAuthenticated) {
                    if (uiState.isAuthenticated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !DevFlowNotifications.canNotify(context)
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // Al tocar una notificación se abre la lista de avisos.
                LaunchedEffect(pendingOpen, uiState.isAuthenticated) {
                    if (pendingOpen == DevFlowNotifications.OPEN_NOTIFICATIONS && uiState.isAuthenticated) {
                        navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true }
                        pendingOpen = null
                    }
                }

                // El tutorial de IA siempre empieza desde el paso 1.
                val openAiSetup: () -> Unit = {
                    assistantViewModel.startSetup()
                    navController.navigate(Routes.AI_SETUP)
                }

                // ── Avisos de la campana (entregas, CI y seguridad) ──
                val qualityAlertsEnabled by rememberBooleanPreference(PREF_QUALITY_ALERTS, true)
                // derivedStateOf: solo se recalcula cuando cambian los proyectos (no en cada redibujado).
                val linkedRepos by remember { derivedStateOf { projectsViewModel.projects.mapNotNull { it.repoFullName }.toSet() } }
                LaunchedEffect(linkedRepos, uiState.githubRepoAccess, qualityAlertsEnabled) {
                    if (qualityAlertsEnabled && uiState.githubRepoAccess) {
                        assistantViewModel.refreshCiStatuses(linkedRepos)
                    }
                }
                val alerts by remember {
                    derivedStateOf {
                        buildAlerts(
                            projects = projectsViewModel.projects,
                            ciRuns = assistantViewModel.ciRuns.values,
                            audit = assistantViewModel.audit,
                            qualityAlertsEnabled = qualityAlertsEnabled
                        )
                    }
                }
                // Copia mínima para que el recordatorio en segundo plano funcione con la app cerrada
                LaunchedEffect(projectsViewModel.projects.toList(), qualityAlertsEnabled, uiState.isAuthenticated) {
                    if (uiState.isAuthenticated) {
                        AlertCache.save(context, uiState.currentUser?.uid, qualityAlertsEnabled, projectsViewModel.projects.toList())
                    }
                }

                val bellState = NotificationBellState(
                    count = alerts.size,
                    onClick = { navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } }
                )

                // ── Coach diario: consejo de la IA sobre el proyecto destacado ──
                val coachEnabled by rememberBooleanPreference(PREF_COACH_LIVE, true)
                val focusProject by remember {
                    derivedStateOf {
                        projectsViewModel.projects
                            .filter { !it.isCompleted }
                            .let { active -> active.filter { (it.daysToDelivery ?: -1) >= 0 }.minByOrNull { it.daysToDelivery ?: Long.MAX_VALUE } ?: active.firstOrNull() }
                    }
                }
                LaunchedEffect(coachEnabled, assistantViewModel.isConfigured, focusProject?.id) {
                    if (coachEnabled) {
                        assistantViewModel.loadCoachTip(
                            focusProject,
                            focusProject?.clientId?.let { projectsViewModel.getClient(it) }
                        )
                    }
                }

                val isOnline = rememberIsOnline()

                CompositionLocalProvider(LocalNotificationBell provides bellState) {
                Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
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
                            onTabSelected = ::navigateToTab,
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
                            onTabSelected = ::navigateToTab,
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
                            onTabSelected = ::navigateToTab,
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
                // Franja "sin conexión" sobre cualquier pantalla
                OfflineBanner(online = isOnline)
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
