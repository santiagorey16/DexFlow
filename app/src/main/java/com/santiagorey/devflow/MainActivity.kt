package com.santiagorey.devflow

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.auth.AuthViewModel
import com.santiagorey.devflow.projects.ProjectsViewModel
import com.santiagorey.devflow.notifications.AlertCache
import com.santiagorey.devflow.notifications.DevFlowNotifications
import com.santiagorey.devflow.notifications.ReminderWorker
import com.santiagorey.devflow.notifications.buildAlerts
import com.santiagorey.devflow.screens.LocalNotificationBell
import com.santiagorey.devflow.screens.NotificationBellState
import com.santiagorey.devflow.screens.OfflineBanner
import com.santiagorey.devflow.screens.rememberIsOnline
import com.santiagorey.devflow.screens.PREF_COACH_LIVE
import com.santiagorey.devflow.screens.PREF_QUALITY_ALERTS
import com.santiagorey.devflow.screens.rememberBooleanPreference
import com.santiagorey.devflow.ui.theme.DevFlowTheme

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
                        DevFlowNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            uiState = uiState,
                            authViewModel = authViewModel,
                            projectsViewModel = projectsViewModel,
                            assistantViewModel = assistantViewModel,
                            activity = this@MainActivity,
                            alerts = alerts,
                            qualityAlertsEnabled = qualityAlertsEnabled,
                            navigateToTab = ::navigateToTab,
                            logout = logout,
                            openAiSetup = openAiSetup
                        )
                        // Franja "sin conexión" sobre cualquier pantalla
                        OfflineBanner(online = isOnline)
                    }
                }
            }
        }
    }
}

