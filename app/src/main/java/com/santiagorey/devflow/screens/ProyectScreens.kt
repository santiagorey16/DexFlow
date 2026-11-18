package com.santiagorey.devflow.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.currentPhaseIndex
import com.santiagorey.devflow.projects.formatCreatedDate
import com.santiagorey.devflow.projects.formatDeliveryDate
import com.santiagorey.devflow.projects.projectPhases
import com.santiagorey.devflow.ui.theme.DevFlowTheme

/**
 * Pestaña Proyectos: lista con buscador y filtro por fase.
 * La búsqueda y el filtro usan rememberSaveable (no se pierden al girar el teléfono)
 * y la lista se ordena por fecha de entrega (los que no tienen fecha van al final).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProyectScreens(
    currentUser: FirebaseUser? = null,
    projects: List<Project> = emptyList(),
    clients: List<Client> = emptyList(),
    onTabSelected: (Int) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNewProjectClick: () -> Unit = {},
    onOpenProject: (projectId: String) -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenClient: (clientId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // -1 = Todos; 0..4 = índice de fase
    var selectedPhase by rememberSaveable { mutableIntStateOf(-1) }

    /** Busca el cliente asignado a un proyecto (o null si no tiene). */
    fun clientOf(project: Project) = clients.firstOrNull { it.id == project.clientId }

    val filteredProjects = projects
        .filter { selectedPhase == -1 || currentPhaseIndex(it.progress) == selectedPhase }
        .filter { project ->
            searchQuery.isBlank() ||
                project.name.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true) ||
                project.stack.any { it.contains(searchQuery, ignoreCase = true) } ||
                (clientOf(project)?.name?.contains(searchQuery, ignoreCase = true) ?: false)
        }
        // Ordenados por fecha de entrega (los que no tienen fecha al final)
        .sortedWith(compareBy<Project, Long?>(nullsLast(naturalOrder())) { it.deliveryDateMillis })

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DevFlowColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAssistant,
                containerColor = DevFlowColors.Purple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text("🤖", fontSize = 22.sp)
            }
        },
        bottomBar = {
            DevFlowBottomBar(
                selectedTab = 1, // Pestaña Proyectos
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopHeaderBar(
                currentUser = currentUser,
                onLogoutClick = onLogoutClick,
                section = "PROYECTOS"
            )

            // Título + botón compacto (no se comprime en pantallas pequeñas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Proyectos",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White),
                        maxLines = 1
                    )
                    Text(
                        text = when (projects.size) {
                            0 -> "Sin proyectos"
                            1 -> "1 proyecto"
                            else -> "${projects.size} proyectos"
                        },
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.Cyan)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onNewProjectClick,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                ) {
                    Text(
                        text = "+ Nuevo",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White),
                        maxLines = 1
                    )
                }
            }

            if (projects.isEmpty()) {
                EmptyStateCard(
                    title = "Aún no tienes proyectos",
                    subtitle = "Crea tu primer proyecto con el asistente y aparecerá aquí con su avance y fecha de entrega.",
                    actionLabel = "+ Crear proyecto",
                    onAction = onNewProjectClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                return@Column
            }

            SearchSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                placeholder = "Buscar por proyecto, cliente o tecnología..."
            )

            // Filtros por fase con conteos reales
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipBox(
                    text = "Todos (${projects.size})",
                    selected = selectedPhase == -1,
                    onClick = { selectedPhase = -1 }
                )
                projectPhases.forEachIndexed { index, phase ->
                    val count = projects.count { currentPhaseIndex(it.progress) == index }
                    FilterChipBox(
                        text = "${phase.name} ($count)",
                        selected = selectedPhase == index,
                        onClick = { selectedPhase = index }
                    )
                }
            }

            Text(
                text = "Ordenado por fecha de entrega",
                style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary)
            )

            if (filteredProjects.isEmpty()) {
                EmptyStateCard(
                    title = "No hay coincidencias",
                    subtitle = "Prueba con otra búsqueda o cambia el filtro."
                )
            }

            filteredProjects.forEach { project ->
                val client = clientOf(project)
                ProjectCardItem(
                    project = project,
                    clientName = client?.name,
                    onClientClick = client?.let { c -> { onOpenClient(c.id) } },
                    onManageClick = { onOpenProject(project.id) }
                )
            }

            // Resumen calculado con los proyectos reales
            val active = projects.filter { !it.isCompleted }
            if (active.isNotEmpty()) {
                val onTime = active.count { !it.isOverdue }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DevFlowColors.DarkCard),
                    border = BorderStroke(1.dp, DevFlowColors.Border)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DevFlowColors.Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_tasks), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (onTime == active.size) "Ritmo de entrega al día" else "Hay entregas vencidas",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            )
                            Text(
                                text = "$onTime de ${active.size} proyectos activos dentro del cronograma",
                                style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

/** Chip de filtro (p. ej. una fase). Se resalta cuando [selected] es true. */
@Composable
internal fun FilterChipBox(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DevFlowColors.Primary else DevFlowColors.DarkCard)
            .border(1.dp, if (selected) DevFlowColors.Cyan else DevFlowColors.Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White))
    }
}

/** Tarjeta de un proyecto en la lista: fase, avance, entrega, stack y cliente. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCardItem(
    project: Project,
    clientName: String?,
    onClientClick: (() -> Unit)?,
    onManageClick: () -> Unit
) {
    val statusColor = phaseColor(project.progress)
    DevFlowCard(modifier = Modifier.clickable { onManageClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = clientName ?: "Sin cliente asignado",
                        style = TextStyle(fontSize = 12.sp, color = if (onClientClick != null) DevFlowColors.Cyan else DevFlowColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (onClientClick != null) Modifier.clickable { onClientClick() } else Modifier
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(text = project.phase.name.uppercase(), color = statusColor)
            }

            if (project.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(project.description, style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DevFlowColors.Background)
                    .padding(10.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progreso general", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary))
                        Text("${project.progress}%", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = statusColor))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { project.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = DevFlowColors.Border
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Entrega: ${formatDeliveryDate(project.deliveryDateMillis)}" +
                            if (!project.isCompleted && project.deliveryDateMillis != null) " (${daysLabel(project.daysToDelivery)})" else "",
                        style = TextStyle(fontSize = 10.sp, color = if (project.isOverdue) DevFlowColors.Red else DevFlowColors.TextSecondary)
                    )
                }
            }

            if (project.stack.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    project.stack.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DevFlowColors.DarkCard)
                                .border(1.dp, DevFlowColors.Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(tag, style = TextStyle(fontSize = 11.sp, color = Color.White))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Creado: ${formatCreatedDate(project.createdAt)}" +
                        if (project.budget.isNotBlank()) " • USD ${project.budget}" else "",
                    style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DevFlowColors.Background)
                        .border(1.dp, DevFlowColors.Border, RoundedCornerShape(8.dp))
                        .clickable { onManageClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (project.tasks.isEmpty()) "Ver detalle" else "${project.tasks.count { it.done }}/${project.tasks.size} tareas", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_forward), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ProyectScreensPreview() {
    DevFlowTheme {
        ProyectScreens()
    }
}
