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

/**
 * Única Activity de la app: todas las pantallas son composables que viven adentro.
 *
 * Se encarga de:
 * - Arrancar la app (splash, canal de notificaciones, recordatorios en segundo plano).
 * - Crear los 3 ViewModels y conectarlos entre sí (cuando cambia el usuario, avisa a los demás).
 * - Elegir la primera pantalla: Dashboard si hay sesión, Login si no.
 * - Calcular lo que comparten varias pantallas (avisos de la campana, coach del día, conexión).
 * - Mostrar [DevFlowNavHost], que tiene todas las pantallas.
 */
class MainActivity : ComponentActivity() {

    /**
     * Pantalla a abrir cuando se toca una notificación (p. ej. "notifications").
     * Es estado de Compose (`mutableStateOf`): al cambiar, el LaunchedEffect que lo lee navega solo.
     */
    private var pendingOpen by mutableStateOf<String?>(null)

    /** Llega aquí si la app YA estaba abierta y se toca una notificación. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpen = intent.getStringExtra(DevFlowNotifications.EXTRA_OPEN)
    }

    /** Lo primero que ejecuta Android al abrir la app. */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de inicio: fondo negro con el logo (debe ir antes de super.onCreate).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // La app dibuja de borde a borde (también detrás de la hora y la batería).
        enableEdgeToEdge()
        // Si la app estaba cerrada y se abrió desde una notificación, aquí viene a qué pantalla ir.
        pendingOpen = intent?.getStringExtra(DevFlowNotifications.EXTRA_OPEN)
        // Android 8+ exige un "canal" para mostrar notificaciones (si ya existe, no pasa nada).
        DevFlowNotifications.createChannel(this)
        // Revisión de entregas y CI cada 6 h, aunque la app esté cerrada (no se duplica: usa KEEP).
        ReminderWorker.schedule(this)

        // Desde aquí todo es Jetpack Compose.
        setContent {
            DevFlowTheme {
                // "Control remoto" de la navegación. remember = el mismo en cada redibujado
                // (si se creara uno nuevo, se perdería la pila de pantallas).
                val navController = rememberNavController()
                // viewModel() los crea la primera vez y devuelve los mismos después
                // (por ejemplo al girar el teléfono), así no se pierden los datos.
                val authViewModel: AuthViewModel = viewModel()
                val projectsViewModel: ProjectsViewModel = viewModel()
                val assistantViewModel: AssistantViewModel = viewModel()
                // El estado de sesión llega como StateFlow; collectAsState lo vuelve estado de Compose
                // para que esta parte se redibuje cuando alguien inicia o cierra sesión.
                val uiState by authViewModel.uiState.collectAsState()
                val context = LocalContext.current

                // Se calcula una sola vez: con sesión iniciada la app abre directo en el Dashboard.
                val startDestination = remember {
                    if (uiState.isAuthenticated) Routes.DASHBOARD else Routes.LOGIN
                }

                /**
                 * Navegación de la barra inferior (0 Dashboard … 4 Ajustes).
                 * La pila queda siempre corta: Dashboard → pestaña actual. Así "atrás" desde cualquier
                 * pestaña lleva al Dashboard, y desde el Dashboard sale de la app.
                 */
                fun navigateToTab(tabIndex: Int) {
                    val route = when (tabIndex) {
                        0 -> Routes.DASHBOARD
                        1 -> Routes.PROJECTS
                        2 -> Routes.GUIDES
                        3 -> Routes.CUSTOMERS
                        4 -> Routes.SETTINGS
                        else -> Routes.DASHBOARD
                    }
                    // Ya estás en esa pestaña: no hace nada.
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
                        popUpTo(Routes.DASHBOARD) { saveState = true } // saca lo de encima del Dashboard y guarda su estado
                        launchSingleTop = true                          // no abre dos copias de la misma pestaña
                        restoreState = true                             // recupera scroll y filtros si ya la visitaste
                    }
                }

                // ── Conectar los ViewModels ──
                // Los ViewModels no se conocen entre sí: MainActivity les avisa cuando cambia el usuario.
                // dataKey es el uid de Firebase (null sin sesión); el efecto se repite cada vez que cambia.

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

                // Mensajes de error o éxito del login y de Ajustes: se muestran una vez y se borran
                // (clearMessages) para que no vuelvan a salir al redibujar.
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

                // Cerrar sesión: también borra la copia de proyectos que usan los recordatorios,
                // para no avisar de proyectos de la cuenta anterior.
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

                // Número de avisos y acción de la campana. Llega a todas las pestañas con
                // CompositionLocalProvider (más abajo), sin pasarlo como parámetro pantalla por pantalla.
                val bellState = NotificationBellState(
                    count = alerts.size,
                    onClick = { navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } }
                )

                // ── Coach diario: consejo de la IA sobre el proyecto destacado ──
                val coachEnabled by rememberBooleanPreference(PREF_COACH_LIVE, true)
                // Proyecto destacado: el activo con la entrega más próxima; si ninguno tiene fecha, el primero.
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

                // true/false según haya internet; se actualiza solo (ver ConnectivityBanner.kt).
                val isOnline = rememberIsOnline()

                // Dibujo final: todas las pantallas (DevFlowNavHost) y, encima, la franja "sin conexión".
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

