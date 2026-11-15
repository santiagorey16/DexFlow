package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.github.RepoAudit
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.TaskStatus
import com.santiagorey.devflow.projects.formatDuration
import com.santiagorey.devflow.projects.currentPhaseIndex
import com.santiagorey.devflow.projects.formatCreatedDate
import com.santiagorey.devflow.projects.formatDeliveryDate
import com.santiagorey.devflow.projects.projectPhases

/**
 * Detalle de un proyecto: resumen, avance, tareas por fase, repositorio y acciones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectOverviewScreen(
    project: Project,
    client: Client?,
    assistant: AssistantViewModel,
    onBackClick: () -> Unit,
    onOpenClient: (clientId: String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReviewWithAi: () -> Unit,
    onSaveProgress: (Int) -> Unit,
    taskActions: TaskActions
) {
    val strictMode by rememberBooleanPreference(PREF_STRICT_QUALITY, false)
    // Acción pendiente de confirmar con el checklist del modo estricto
    var pendingCompletion by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    /** Ejecuta [action]; si deja el proyecto al 100% y el modo estricto está activo, pide confirmación. */
    fun guardCompletion(reachesHundred: Boolean, action: () -> Unit) {
        if (reachesHundred && strictMode && !project.isCompleted) pendingCompletion = action else action()
    }

    val statusColor = phaseColor(project.progress)
    val audit = assistant.audit?.takeIf { it.repoFullName == project.repoFullName }

    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = { BackTopBar(title = project.name, onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Resumen ──────────────────────────────────────
            DevFlowCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(text = project.phase.name.uppercase(), color = statusColor)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${project.progress}%", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 18.sp)
                    }
                    LinearProgressIndicator(
                        progress = { project.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = DevFlowColors.Border
                    )
                    if (project.description.isNotBlank()) {
                        Text(project.description, color = DevFlowColors.TextSecondary, fontSize = 13.sp)
                    }
                    InfoLine("Entrega", formatDeliveryDate(project.deliveryDateMillis) +
                        if (!project.isCompleted && project.deliveryDateMillis != null) " (${daysLabel(project.daysToDelivery)})" else "",
                        valueColor = if (project.isOverdue) DevFlowColors.Red else Color.White)
                    InfoLine(
                        "Cliente", client?.name ?: "Sin cliente",
                        valueColor = if (client != null) DevFlowColors.Cyan else DevFlowColors.TextSecondary,
                        onClick = client?.let { c -> { onOpenClient(c.id) } }
                    )
                    if (project.budget.isNotBlank()) InfoLine("Presupuesto", "USD ${project.budget}")
                    InfoLine("Repositorio", project.repoFullName ?: "Sin vincular (desde el asistente IA)")
                    InfoLine("Creado", formatCreatedDate(project.createdAt))
                    if (project.tasks.any { it.totalMs() > 0 }) InfoLine("Tiempo", formatDuration(project.trackedMs()))
                    if (project.stack.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            project.stack.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DevFlowColors.DarkCard)
                                        .border(1.dp, DevFlowColors.Border, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text(tag, fontSize = 11.sp, color = Color.White) }
                            }
                        }
                    }
                }
            }

            // ── Acciones ─────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onReviewWithAi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Purple)
                ) { Text("🤖 Revisar con IA", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                ) { Text("✎ Editar", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
            }

            // ── Avance ───────────────────────────────────────
            if (project.tasks.isEmpty()) {
                ManualProgressCard(project) { value ->
                    guardCompletion(value >= 100) { onSaveProgress(value) }
                }
            }

            // ── Tareas por fase ──────────────────────────────
            TasksCard(
                project = project,
                client = client,
                assistant = assistant,
                // Marcar como hecha la última tarea pendiente pasa por el modo estricto.
                actions = TaskActions(
                    onAdd = taskActions.onAdd,
                    onAddMany = taskActions.onAddMany,
                    onSetStatus = { task, status ->
                        val completesProject = status == TaskStatus.DONE && !task.done && project.tasks.count { !it.done } == 1
                        guardCompletion(completesProject) { taskActions.onSetStatus(task, status) }
                    },
                    onDelete = taskActions.onDelete,
                    onStartTimer = taskActions.onStartTimer,
                    onStopTimer = taskActions.onStopTimer,
                    onLinkIssue = taskActions.onLinkIssue,
                    onApplyIssueStates = taskActions.onApplyIssueStates
                )
            )

            // ── Zona de peligro ──────────────────────────────
            Text(
                text = if (confirmDelete) "Toca de nuevo para eliminar el proyecto" else "Eliminar proyecto",
                color = DevFlowColors.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { if (confirmDelete) onDelete() else confirmDelete = true }
                    .padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    pendingCompletion?.let { action ->
        StrictModeDialog(
            audit = audit,
            onDismiss = { pendingCompletion = null },
            onConfirm = {
                pendingCompletion = null
                action()
            }
        )
    }
}

internal const val PREF_STRICT_QUALITY = "strict_quality"
internal const val PREF_COACH_LIVE = "coach_live"
internal const val PREF_QUALITY_ALERTS = "cicd_alerts"

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = Color.White, onClick: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, color = DevFlowColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(96.dp))
        Text(
            value, color = valueColor, fontSize = 12.sp,
            modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    }
}

@Composable
private fun ManualProgressCard(project: Project, onSave: (Int) -> Unit) {
    var progress by remember(project.id, project.progress) { mutableFloatStateOf(project.progress.toFloat()) }
    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Avance manual", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    "${progress.toInt()}% • ${projectPhases[currentPhaseIndex(progress.toInt())].name}",
                    color = DevFlowColors.BrightCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = progress,
                onValueChange = { progress = it },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = DevFlowColors.Cyan, activeTrackColor = DevFlowColors.Cyan, inactiveTrackColor = DevFlowColors.Border)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Consejo: agrega tareas y el avance se calculará solo.",
                    color = DevFlowColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { onSave(progress.toInt()) },
                    enabled = progress.toInt() != project.progress
                ) { Text("Guardar", color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
/** Checklist del "Modo Estricto de Calidad" antes de dar un proyecto por terminado. */
@Composable
private fun StrictModeDialog(audit: RepoAudit?, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val items = listOf(
        "Escribí pruebas para la funcionalidad principal",
        "El README explica cómo instalar y ejecutar el proyecto",
        "No hay claves ni secretos en el repositorio",
        "El cliente revisó y aprobó la entrega"
    )
    val checked = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("🛡 Modo estricto de calidad", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Antes de marcar el proyecto al 100%, confirma:", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
                items.forEachIndexed { index, text ->
                    // Toda la fila marca o desmarca (no solo el cuadrito).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (index in checked) checked.remove(index) else checked.add(index) }
                    ) {
                        Checkbox(
                            checked = index in checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = DevFlowColors.Cyan)
                        )
                        Text(text, color = Color.White, fontSize = 13.sp)
                    }
                }
                if (audit != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Auditoría del repo: ${audit.score}/100", color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    audit.checks.filterNot { it.passed }.forEach {
                        Text("✗ ${it.title}", color = DevFlowColors.Red, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = checked.size == items.size,
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Green)
            ) { Text("Marcar como terminado") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Todavía no", color = DevFlowColors.TextSecondary) }
        }
    )
}
