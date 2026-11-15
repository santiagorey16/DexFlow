package com.santiagorey.devflow.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.R
import com.santiagorey.devflow.projects.ActivityEntry
import com.santiagorey.devflow.projects.ActivityType
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.formatDeliveryDate
import com.santiagorey.devflow.projects.formatRelativeTime
import com.santiagorey.devflow.projects.formatTodayLong
import com.santiagorey.devflow.projects.projectPhases
import com.santiagorey.devflow.ui.theme.DevFlowTheme




@Composable
fun DashboardScreen(
    currentUser: FirebaseUser? = null,
    projects: List<Project> = emptyList(),
    clients: List<Client> = emptyList(),
    activity: List<ActivityEntry> = emptyList(),
    onTabSelected: (Int) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNewProject: () -> Unit = {},
    onNewClient: () -> Unit = {},
    aiConnectedLabel: String? = null,
    onOpenAssistant: (projectId: String?) -> Unit = {},
    onOpenProject: (projectId: String) -> Unit = {},
    coachTip: String? = null,
    coachLoading: Boolean = false,
    modifier: Modifier = Modifier
) {

    val coachEnabled by rememberBooleanPreference(PREF_COACH_LIVE, true)

    val activeProjects = projects.filter { !it.isCompleted }
    val completedCount = projects.size - activeProjects.size
    val dueThisWeek = activeProjects.filter { (it.daysToDelivery ?: -1) in 0..7 }
    val overdue = activeProjects.filter { it.isOverdue }
    val nextDelivery = activeProjects
        .filter { (it.daysToDelivery ?: -1) >= 0 }
        .minByOrNull { it.daysToDelivery ?: Long.MAX_VALUE }

    // Proyecto destacado: el de entrega más próxima o, si no, el más reciente.
    val focusProject = nextDelivery ?: activeProjects.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DevFlowColors.Background,
        bottomBar = {
            DevFlowBottomBar(
                selectedTab = 0,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(5.dp)) }

            // 1. Top Bar Header
            item {
                TopHeaderBar(
                    currentUser = currentUser,
                    onLogoutClick = onLogoutClick,
                    section = "DASHBOARD"
                )
            }

            // 2. Greeting Header
            item {
                Column {
                    Text(
                        text = "Hola, ${currentUser.friendlyName()}",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 ${formatTodayLong()} • " + when (dueThisWeek.size) {
                            0 -> "Sin entregas esta semana"
                            1 -> "Tienes 1 entrega próxima"
                            else -> "Tienes ${dueThisWeek.size} entregas próximas"
                        },
                        style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary)
                    )
                }
            }

            // 3. Continuar trabajando / Primer proyecto
            item {
                if (focusProject == null) {
                    WelcomeCard(
                        hasProjects = projects.isNotEmpty(),
                        onNewProject = onNewProject,
                        onNewClient = onNewClient
                    )
                } else {
                    val clientName = clients.firstOrNull { it.id == focusProject.clientId }?.name
                    ContinueWorkingCard(
                        project = focusProject,
                        clientName = clientName,
                        onOpen = { onOpenProject(focusProject.id) }
                    )
                }
            }

            // 3a. Aviso de entregas vencidas
            if (overdue.isNotEmpty()) {
                item {
                    OverdueBanner(
                        count = overdue.size,
                        firstName = overdue.first().name,
                        onClick = { onOpenProject(overdue.first().id) }
                    )
                }
            }

            // 3a. Coach con IA o tip del día
            item {
                CoachOrTipCard(
                    coachEnabled = coachEnabled && aiConnectedLabel != null && focusProject != null,
                    coachTip = coachTip,
                    coachLoading = coachLoading,
                    projectName = focusProject?.name
                )
            }

            // 3b. Asistente IA
            item {
                AssistantEntryCard(
                    connectedLabel = aiConnectedLabel,
                    projectName = focusProject?.name,
                    onClick = { onOpenAssistant(focusProject?.id) }
                )
            }

            // 4. Métricas reales
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_folder,
                            title = "PROYECTOS",
                            value = "${activeProjects.size} activos",
                            subtitle = "$completedCount completados"
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_tasks,
                            title = "PENDIENTES",
                            badgeText = if (overdue.isNotEmpty()) "${overdue.size} atras." else null,
                            badgeColor = DevFlowColors.Red,
                            value = "${activeProjects.size} abiertos",
                            subtitle = if (overdue.isEmpty()) "Todo al día" else "${overdue.size} con entrega vencida"
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_calendar,
                            title = "ENTREGAS",
                            hasDot = dueThisWeek.isNotEmpty(),
                            value = "${dueThisWeek.size} esta sem.",
                            subtitle = nextDelivery?.let { "${it.name}: ${daysLabel(it.daysToDelivery)}" }
                                ?: "Sin entregas próximas"
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_users_group,
                            title = "CLIENTES",
                            value = "${clients.size} registrados",
                            subtitle = "${projects.count { it.clientId != null }} proyectos asignados"
                        )
                    }
                }
            }

            // 5. Avance general (solo si hay proyectos)
            if (projects.isNotEmpty()) {
                item {
                    OverallProgressCard(projects = projects)
                }
            }

            // 6. Proyectos activos
            if (activeProjects.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Proyectos Activos", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White))
                        Text(
                            text = "Ver todos (${projects.size}) →",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.Cyan),
                            modifier = Modifier.clickable { onTabSelected(1) }
                        )
                    }
                }

                items(activeProjects.take(3), key = { it.id }) { project ->
                    DashboardProjectItem(
                        project = project,
                        clientName = clients.firstOrNull { it.id == project.clientId }?.name,
                        onClick = { onOpenProject(project.id) }
                    )
                }
            }

            // 7. Actividad reciente
            item {
                DevFlowCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = R.drawable.ic_tasks), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actividad Reciente", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activity.isEmpty()) {
                            Text(
                                text = "Aún no hay actividad. Cuando crees proyectos o agregues clientes aparecerán aquí.",
                                style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary, lineHeight = 16.sp)
                            )
                        } else {
                            activity.take(5).forEachIndexed { index, entry ->
                                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                                ActivityRow(
                                    iconRes = when (entry.type) {
                                        ActivityType.PROJECT -> R.drawable.ic_folder
                                        ActivityType.CLIENT -> R.drawable.ic_users_group
                                        ActivityType.PROGRESS -> R.drawable.ic_tasks
                                        ActivityType.COMPLETED -> R.drawable.ic_shield_check
                                    },
                                    title = entry.text,
                                    time = formatRelativeTime(entry.timestamp)
                                )
                            }
                        }
                    }
                }
            }


            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}


@Composable
private fun WelcomeCard(
    hasProjects: Boolean,
    onNewProject: () -> Unit,
    onNewClient: () -> Unit
) {
    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.ic_tasks), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EMPIEZA AQUÍ", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DevFlowColors.Cyan))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (hasProjects) "¡Todos tus proyectos están completados!" else "Aún no tienes proyectos",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Crea un proyecto para ver aquí tus estadísticas, entregas y pendientes.",
                style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary, lineHeight = 16.sp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onNewProject,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) {
                Text("+ Crear proyecto", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNewClient,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DevFlowColors.Border)
            ) {
                Text("Agregar cliente", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DevFlowColors.Cyan))
            }
        }
    }
}

@Composable
private fun ContinueWorkingCard(
    project: Project,
    clientName: String?,
    onOpen: () -> Unit
) {
    val color = phaseColor(project.progress)
    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(painter = painterResource(id = R.drawable.ic_tasks), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONTINUAR TRABAJANDO", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DevFlowColors.Cyan), maxLines = 1)
                }
                StatusChip(text = project.phase.name.uppercase(), color = color)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(project.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(clientName ?: "Sin cliente asignado", style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary))

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entrega: ${formatDeliveryDate(project.deliveryDateMillis)}", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary))
                Text("${project.progress}%", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color))
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = DevFlowColors.Border
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ir al proyecto", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(painter = painterResource(id = R.drawable.ic_arrow_forward), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OverallProgressCard(projects: List<Project>) {
    val average = projects.map { it.progress }.average().toInt()
    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("RENDIMIENTO", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.TextSecondary))
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Avance general", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White), modifier = Modifier.weight(1f))
                Text("$average%", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DevFlowColors.Cyan))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { average / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = DevFlowColors.Cyan,
                trackColor = DevFlowColors.Border
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("PROYECTOS POR FASE", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.TextSecondary))
            Spacer(modifier = Modifier.height(4.dp))
            projectPhases.forEachIndexed { index, phase ->
                val count = projects.count { com.santiagorey.devflow.projects.currentPhaseIndex(it.progress) == index }
                PhaseCountRow(label = phase.name, count = count, total = projects.size)
            }
        }
    }
}

@Composable
private fun PhaseCountRow(label: String, count: Int, total: Int) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White))
            Text("$count", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.Cyan))
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else count / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = DevFlowColors.Primary,
            trackColor = DevFlowColors.Border
        )
    }
}

@Composable
private fun DashboardMetricCard(
    iconRes: Int,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = DevFlowColors.Cyan,
    hasDot: Boolean = false
) {
    DevFlowCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DevFlowColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
                }

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = badgeColor, fontWeight = FontWeight.Bold))
                    }
                } else if (hasDot) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DevFlowColors.Cyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(title, style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.TextSecondary))
            Text(value, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DashboardProjectItem(
    project: Project,
    clientName: String?,
    onClick: () -> Unit
) {
    val color = phaseColor(project.progress)
    DevFlowCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DevFlowColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_folder), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(project.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(clientName ?: "Sin cliente", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(text = project.phase.name.uppercase(), color = color)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${project.progress}% completado", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White))
                Text("📅 ${formatDeliveryDate(project.deliveryDateMillis)}", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary))
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = DevFlowColors.Border
            )
        }
    }
}

/** Consejos generales que rotan cada día (cuando no hay coach con IA). */
private val dailyTips = listOf(
    "Define los requisitos y la fecha de entrega antes de programar: reduce errores y reprocesos.",
    "Haz commits pequeños y frecuentes con mensajes claros (feat:, fix:, docs:…).",
    "Escribe primero la prueba de la lógica más importante: te da confianza para cambiar el código después.",
    "Nunca subas claves ni archivos .env al repositorio; usa variables de entorno o un gestor de secretos.",
    "Antes de pedir ayuda, escribe en una frase qué esperabas y qué pasó: muchas veces encuentras el error ahí.",
    "Divide las tareas grandes en partes de menos de un día: así ves el avance real.",
    "Actualiza el README cada vez que cambie cómo se instala o ejecuta el proyecto.",
    "Revisa tu propio código como si fuera de otra persona antes de hacer el merge."
)

@Composable
private fun OverdueBanner(count: Int, firstName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DevFlowColors.Red.copy(alpha = 0.12f))
            .border(1.dp, DevFlowColors.Red.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠", fontSize = 18.sp, color = DevFlowColors.Red)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (count == 1) "1 proyecto con entrega vencida" else "$count proyectos con entrega vencida",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            )
            Text(
                if (count == 1) "\"$firstName\" · toca para revisarlo" else "Empieza por \"$firstName\"",
                style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(painter = painterResource(id = R.drawable.ic_arrow_forward), contentDescription = null, tint = DevFlowColors.Red, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun CoachOrTipCard(coachEnabled: Boolean, coachTip: String?, coachLoading: Boolean, projectName: String?) {
    val today = java.time.LocalDate.now()
    var dismissedDay by rememberStringPreference("tip_dismissed_day", "")
    // El tip general se puede cerrar por hoy; el coach siempre se muestra si está activo.
    if (!coachEnabled && dismissedDay == today.toString()) return

    val isCoach = coachEnabled && (coachTip != null || coachLoading)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCoach) Color(0xFF1E1535) else Color(0xFF0F2937)),
        border = BorderStroke(1.dp, if (isCoach) DevFlowColors.Purple.copy(alpha = 0.6f) else Color(0xFF155E75))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isCoach) "🤖" else "💡", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isCoach) "COACH DEL DÍA" + (projectName?.let { " · ${it.uppercase()}" } ?: "") else "TIP DEL DÍA",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isCoach) DevFlowColors.Purple else DevFlowColors.Cyan),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!isCoach) {
                    Text(
                        text = "✕",
                        style = TextStyle(color = DevFlowColors.TextSecondary, fontSize = 14.sp),
                        modifier = Modifier.clickable { dismissedDay = today.toString() }.padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            when {
                isCoach && coachLoading && coachTip == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = DevFlowColors.Purple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preparando tu consejo…", style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary))
                }
                else -> Text(
                    text = if (isCoach) coachTip.orEmpty() else dailyTips[today.dayOfYear % dailyTips.size],
                    style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary, lineHeight = 17.sp)
                )
            }
        }
    }
}

@Composable
private fun AssistantEntryCard(connectedLabel: String?, projectName: String?, onClick: () -> Unit) {
    DevFlowCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DevFlowColors.Purple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Asistente IA", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White))
                Text(
                    text = when {
                        connectedLabel == null -> "Conecta tu IA de confianza y deja que revise tus repos."
                        projectName != null -> "Revisa el avance de \"$projectName\" con $connectedLabel"
                        else -> "Pregúntale a $connectedLabel sobre tu desarrollo"
                    },
                    style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(painter = painterResource(id = R.drawable.ic_arrow_forward), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ActivityRow(iconRes: Int, title: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DevFlowColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 12.sp, color = Color.White, lineHeight = 16.sp))
            Text(time, style = TextStyle(fontSize = 10.sp, color = DevFlowColors.TextSecondary))
        }
    }
}




@Preview(
    showBackground = true
)
@Composable
fun DashboardPreview() {

    DevFlowTheme {

        DashboardScreen()
    }
}
