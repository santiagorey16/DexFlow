package com.santiagorey.devflow.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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


internal object DevFlowColors {

    val Background = Color(0xFF0B0E14)
    val Card = Color(0xFF111622)
    val DarkCard = Color(0xFF0A0D14)
    val Border = Color(0xFF1E293B)

    val Primary = Color(0xFF635BFF)
    val Cyan = Color(0xFF38BDF8)
    val BrightCyan = Color(0xFF00E5FF)
    val Purple = Color(0xFFA855F7)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)

    val TextSecondary = Color(0xFF94A3B8)
    val TextDark = Color(0xFF64748B)
}

/** Color asociado a la fase de un proyecto. */
internal fun phaseColor(progress: Int): Color =
    when {
        progress >= 100 -> DevFlowColors.Green
        progress >= 80 -> DevFlowColors.BrightCyan
        progress >= 40 -> DevFlowColors.Cyan
        progress >= 20 -> DevFlowColors.Purple
        else -> DevFlowColors.TextSecondary
    }

/** Nombre a mostrar del usuario (nombre, o la parte local del correo). */
internal fun FirebaseUser?.friendlyName(): String =
    this?.displayName?.takeIf { it.isNotBlank() }
        ?: this?.email?.substringBefore('@')
        ?: "Developer"


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
    modifier: Modifier = Modifier
) {
    var showTipCard by remember { mutableStateOf(true) }

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
                        onOpen = { onTabSelected(1) }
                    )
                }
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
                        onClick = { onTabSelected(1) }
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

            // 8. Buenas Prácticas Junior Banner
            if (showTipCard) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2937)),
                        border = BorderStroke(1.dp, Color(0xFF155E75))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("BUENAS PRÁCTICAS JUNIOR", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DevFlowColors.Cyan))
                                }
                                Text(
                                    text = "✕",
                                    style = TextStyle(color = DevFlowColors.TextSecondary, fontSize = 14.sp),
                                    modifier = Modifier.clickable { showTipCard = false }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Define los requisitos y la fecha de entrega antes de programar: reduce errores y reprocesos.",
                                style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary, lineHeight = 16.sp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

internal fun daysLabel(days: Long?): String =
    when {
        days == null -> "sin fecha"
        days < 0 -> "vencido"
        days == 0L -> "hoy"
        days == 1L -> "mañana"
        else -> "en $days días"
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


// =====================================================
// LOGO REAL DE DEVFLOW
// =====================================================

@Composable
fun DevFlowLogo(
    size: Dp = 52.dp
) {

    Image(
        painter = painterResource(
            id = R.drawable.devflow_logo
        ),

        contentDescription = "Logo de DevFlow",

        contentScale = ContentScale.Fit,

        modifier = Modifier
            .size(size)
            .clip(
                RoundedCornerShape(12.dp)
            )
    )
}


// =====================================================
// AVATAR DE PERFIL
// =====================================================

@Composable
fun ProfileAvatar(
    photoUrl: String? = null,
    displayName: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val initials = remember(displayName) {

        if (!displayName.isNullOrBlank()) {

            val parts = displayName
                .trim()
                .split(" ")
                .filter { it.isNotBlank() }

            if (parts.size >= 2) {

                "${parts.first().take(1)}${parts.last().take(1)}"
                    .uppercase()

            } else {

                displayName
                    .take(2)
                    .uppercase()
            }

        } else {

            "SR"
        }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        DevFlowColors.Primary,
                        DevFlowColors.Purple
                    )
                )
            )
            .border(
                width = 1.5.dp,

                brush = Brush.linearGradient(
                    listOf(
                        DevFlowColors.Cyan,
                        DevFlowColors.Primary
                    )
                ),

                shape = CircleShape
            )
            .clickable {
                onClick()
            },

        contentAlignment = Alignment.Center
    ) {

        if (!photoUrl.isNullOrBlank()) {

            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

        } else {

            Text(
                text = initials,

                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            )
        }
    }
}


@Composable
fun TopHeaderBar(
    currentUser: FirebaseUser? = null,
    onLogoutClick: () -> Unit = {},
    section: String = "DASHBOARD"
) {

    var showMenu by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            DevFlowLogo(
                size = 54.dp
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Row {

                    Text(
                        text = "Dev",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Flow",
                        color = DevFlowColors.Cyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                SmallLabel(
                    text = section
                )
            }
        }

        Box {

            ProfileAvatar(
                photoUrl = currentUser
                    ?.photoUrl
                    ?.toString(),

                displayName = currentUser?.displayName
                    ?: currentUser?.email,

                onClick = {
                    showMenu = true
                }
            )

            DropdownMenu(
                expanded = showMenu,

                onDismissRequest = {
                    showMenu = false
                }
            ) {

                DropdownMenuItem(
                    text = {

                        Column {

                            Text(
                                text = currentUser?.displayName
                                    ?: "Usuario DevFlow",

                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )

                            currentUser?.email?.let { email ->

                                Text(
                                    text = email,

                                    style = TextStyle(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    },

                    onClick = {
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = {

                        Text(
                            text = "Cerrar sesión",
                            color = Color.Red
                        )
                    },

                    onClick = {

                        showMenu = false

                        onLogoutClick()
                    }
                )
            }
        }
    }
}



@Composable
fun HeaderSection(
    clientCount: Int,
    projectCount: Int,
    onAddClientClick: () -> Unit
) {

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Clientes",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Gestiona tus clientes y sus proyectos",
                    fontSize = 12.sp,
                    color = DevFlowColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddClientClick,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DevFlowColors.Primary
                )
            ) {
                Text(
                    text = "+ Agregar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Clientes",
                value = clientCount.toString(),
                subtitle = "registrados"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Proyectos",
                value = projectCount.toString(),
                subtitle = "asignados"
            )
        }
    }
}


@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {

    DevFlowCard(
        modifier = modifier.height(95.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),

            verticalArrangement = Arrangement.SpaceBetween
        ) {

            SmallLabel(
                text = title.uppercase()
            )

            Row(
                verticalAlignment = Alignment.Bottom
            ) {

                Text(
                    text = value,
                    fontSize = 22.sp,
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = DevFlowColors.TextSecondary
                )
            }
        }
    }
}



@Composable
fun SearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholder: String = "Buscar cliente, empresa o teléfono..."
) {

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = DevFlowColors.TextDark,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null,
                tint = DevFlowColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = devFlowTextFieldColors()
    )
}


@Composable
fun EmptyClientsCard(
    searchActive: Boolean
) {

    DevFlowCard {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 25.dp,
                    vertical = 32.dp
                ),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (searchActive) {
                    "No encontramos clientes"
                } else {
                    "No tienes Clientes por el momento"
                },

                fontSize = 19.sp,

                fontWeight = FontWeight.Bold,

                color = Color.White,

                textAlign = TextAlign.Center
            )

            if (searchActive) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Prueba buscando con otro nombre.",
                    color = DevFlowColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Composable
fun ClientCard(
    client: Client,
    projectCount: Int,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {

    DevFlowCard(
        modifier = Modifier.clickable { onClick() }
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                ClientAvatar(name = client.name)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = client.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = listOf(client.company, client.phone)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                            .ifBlank { "Sin datos de contacto" },
                        color = DevFlowColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (client.phone.isNotBlank()) {
                    ContactIconButton(iconRes = R.drawable.ic_phone, description = "Llamar", onClick = onCall)
                    Spacer(modifier = Modifier.width(6.dp))
                    ContactIconButton(iconRes = R.drawable.ic_chat, description = "Escribir", onClick = onMessage)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DevFlowColors.DarkCard)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (projectCount) {
                        0 -> "Sin proyectos asignados"
                        1 -> "1 proyecto asignado"
                        else -> "$projectCount proyectos asignados"
                    },
                    color = if (projectCount == 0) DevFlowColors.TextSecondary else Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Ver ficha →",
                    color = DevFlowColors.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun ContactIconButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    tint: Color = DevFlowColors.Cyan
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DevFlowColors.DarkCard)
            .border(1.dp, DevFlowColors.Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}


@Composable
fun StatusChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "● $text",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = color),
            maxLines = 1
        )
    }
}


@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    DevFlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = DevFlowColors.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}


/** Barra superior con flecha de volver (apunta a la izquierda). */
@Composable
fun BackTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        color = DevFlowColors.Card,
        border = BorderStroke(1.dp, DevFlowColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            DevFlowLogo(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun DevFlowTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = devFlowTextFieldColors()
    )
}


@Composable
fun ClientAvatar(
    name: String
) {

    val initials = remember(name) {

        val parts = name
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        when {

            parts.size >= 2 ->
                "${parts.first().take(1)}${parts.last().take(1)}"
                    .uppercase()

            parts.isNotEmpty() ->
                parts.first()
                    .take(2)
                    .uppercase()

            else ->
                "CL"
        }
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        DevFlowColors.Cyan,
                        DevFlowColors.Primary
                    )
                )
            ),

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
internal fun devFlowTextFieldColors() =
    OutlinedTextFieldDefaults.colors(

        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,

        focusedBorderColor = DevFlowColors.Primary,
        unfocusedBorderColor = DevFlowColors.Border,

        focusedLabelColor = DevFlowColors.Cyan,
        unfocusedLabelColor = DevFlowColors.TextSecondary,

        cursorColor = DevFlowColors.Cyan
    )


@Composable
fun DevFlowCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = DevFlowColors.Card
        ),

        border = BorderStroke(
            1.dp,
            DevFlowColors.Border
        )
    ) {

        Column(
            content = content
        )
    }
}


@Composable
fun SmallLabel(
    text: String,
    color: Color = DevFlowColors.TextSecondary
) {

    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
}



@Composable
fun DevFlowBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    val tabs = listOf(
        "▦" to "Dashboard",
        "◇" to "Proyectos",
        "▤" to "Guías",
        "♙" to "Clientes",
        "☷" to "Ajustes"
    )

    Surface(
        color = DevFlowColors.DarkCard,

        border = BorderStroke(
            1.dp,
            DevFlowColors.Border
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),

            horizontalArrangement = Arrangement.SpaceAround,

            verticalAlignment = Alignment.CenterVertically
        ) {

            tabs.forEachIndexed { index, item ->

                val selected =
                    selectedTab == index

                val itemColor =
                    if (selected) {
                        DevFlowColors.Cyan
                    } else {
                        DevFlowColors.TextSecondary
                    }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            onTabSelected(index)
                        },

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = item.first,
                        color = itemColor,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = item.second,
                        color = itemColor,
                        fontSize = 10.sp,

                        fontWeight =
                            if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                    )
                }
            }
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
