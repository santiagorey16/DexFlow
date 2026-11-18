package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.ProjectTask
import com.santiagorey.devflow.projects.TaskStatus
import com.santiagorey.devflow.projects.currentPhaseIndex
import com.santiagorey.devflow.projects.formatDuration
import com.santiagorey.devflow.projects.taskPhases
import kotlinx.coroutines.delay

/** Acciones sobre las tareas (las ejecuta ProjectsViewModel desde MainActivity). */
class TaskActions(
    val onAdd: (title: String, phaseIndex: Int) -> Unit,
    val onAddMany: (List<ProjectTask>) -> Unit,
    val onSetStatus: (ProjectTask, TaskStatus) -> Unit,
    val onDelete: (taskId: String) -> Unit,
    val onStartTimer: (taskId: String) -> Unit,
    val onStopTimer: (taskId: String) -> Unit,
    val onLinkIssue: (taskId: String, issueNumber: Int) -> Unit,
    val onApplyIssueStates: (Map<Int, Boolean>) -> Unit
)

/**
 * Tareas del proyecto en dos vistas: lista por fase o tablero Kanban.
 * Incluye cronómetro por tarea, sugerencias de la IA e issues de GitHub.
 */
@Composable
internal fun TasksCard(project: Project, client: Client?, assistant: AssistantViewModel, actions: TaskActions) {
    var kanban by rememberSaveable { mutableStateOf(false) }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newPhase by rememberSaveable { mutableIntStateOf(currentPhaseIndex(project.progress).coerceAtMost(3)) }
    var phaseMenu by remember { mutableStateOf(false) }
    val canUseGitHub = project.repoFullName != null && assistant.canUseGitHub

    // Reloj para que el cronómetro avance en pantalla (cada 30 s, solo si hay alguno en marcha).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val anyRunning = project.tasks.any { it.timerStartedAt != null }
    LaunchedEffect(anyRunning) {
        while (anyRunning) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Encabezado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tareas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        if (project.tasks.isEmpty()) "Divide el trabajo por fases"
                        else "${project.tasks.count { it.done }}/${project.tasks.size} hechas · ⏱ ${formatDuration(project.trackedMs(now))}",
                        color = DevFlowColors.TextSecondary, fontSize = 11.sp
                    )
                }
                if (project.tasks.isNotEmpty()) {
                    FilterChipBox(text = if (kanban) "☰ Lista" else "▦ Kanban", selected = false, onClick = { kanban = !kanban })
                }
            }

            // Acciones con IA y GitHub
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (assistant.isConfigured) {
                    SmallAction(
                        label = "✨ Sugerir con IA",
                        color = DevFlowColors.Purple,
                        loading = assistant.isSuggestingTasks,
                        onClick = { assistant.suggestTasks(project, client) }
                    )
                }
                if (canUseGitHub && project.tasks.any { it.issueNumber != null }) {
                    SmallAction(
                        label = "↻ Sincronizar issues",
                        color = DevFlowColors.Cyan,
                        loading = assistant.isSyncingIssues,
                        onClick = { assistant.syncIssues(project, actions.onApplyIssueStates) }
                    )
                }
            }
            assistant.taskSuggestionError?.let { Text(it, color = DevFlowColors.Red, fontSize = 12.sp) }

            if (kanban) {
                KanbanBoard(project.tasks, now, actions)
            } else {
                taskPhases.forEachIndexed { phaseIndex, phase ->
                    val phaseTasks = project.tasks.filter { it.phaseIndex == phaseIndex }
                    if (phaseTasks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${phase.name.uppercase()} · ${phaseTasks.count { it.done }}/${phaseTasks.size}",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = phaseColor(phase.from))
                            )
                            phaseTasks.forEach { task ->
                                TaskRow(
                                    task = task,
                                    now = now,
                                    canCreateIssue = canUseGitHub,
                                    actions = actions,
                                    onCreateIssue = { assistant.createIssueForTask(project, task) { n -> actions.onLinkIssue(task.id, n) } }
                                )
                            }
                        }
                    }
                }
            }

            // Nueva tarea
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(120) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Nueva tarea…", color = DevFlowColors.TextDark, fontSize = 13.sp) },
                    colors = devFlowTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    Text(
                        taskPhases[newPhase].name.take(10) + " ▾",
                        color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { phaseMenu = true }.padding(6.dp)
                    )
                    DropdownMenu(expanded = phaseMenu, onDismissRequest = { phaseMenu = false }) {
                        taskPhases.forEachIndexed { index, phase ->
                            DropdownMenuItem(text = { Text(phase.name) }, onClick = { newPhase = index; phaseMenu = false })
                        }
                    }
                }
            }
            Button(
                onClick = {
                    actions.onAdd(newTitle, newPhase)
                    newTitle = ""
                },
                enabled = newTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) { Text("+ Agregar tarea", fontWeight = FontWeight.Bold) }
        }
    }

    assistant.taskSuggestions?.let { suggestions ->
        TaskSuggestionsDialog(
            suggestions = suggestions,
            onDismiss = { assistant.clearTaskSuggestions() },
            onAdd = { chosen ->
                actions.onAddMany(chosen)
                assistant.clearTaskSuggestions()
            }
        )
    }
}

/** Texto que funciona como botón pequeño; muestra una rueda mientras [loading]. */
@Composable
private fun SmallAction(label: String, color: Color, loading: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = !loading) { onClick() }.padding(vertical = 4.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = color)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

/** Una tarea: casilla de hecha, título, tiempo, cronómetro y menú (mover, crear issue, borrar). */
@Composable
private fun TaskRow(task: ProjectTask, now: Long, canCreateIssue: Boolean, actions: TaskActions, onCreateIssue: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = task.done,
            onCheckedChange = { actions.onSetStatus(task, if (task.done) TaskStatus.TODO else TaskStatus.DONE) },
            colors = CheckboxDefaults.colors(checkedColor = DevFlowColors.Cyan, uncheckedColor = DevFlowColors.TextSecondary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                color = if (task.done) DevFlowColors.TextSecondary else Color.White,
                fontSize = 13.sp,
                textDecoration = if (task.done) TextDecoration.LineThrough else null
            )
            TaskMeta(task, now)
        }
        TimerButton(task, actions)
        Box {
            Text("⋮", color = DevFlowColors.TextSecondary, fontSize = 16.sp, modifier = Modifier.clickable { menuOpen = true }.padding(8.dp))
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                TaskStatus.entries.filter { it != task.status }.forEach { status ->
                    DropdownMenuItem(text = { Text("Mover a: ${status.label}") }, onClick = { menuOpen = false; actions.onSetStatus(task, status) })
                }
                if (canCreateIssue && task.issueNumber == null) {
                    DropdownMenuItem(text = { Text("Crear issue en GitHub") }, onClick = { menuOpen = false; onCreateIssue() })
                }
                DropdownMenuItem(text = { Text("Eliminar", color = DevFlowColors.Red) }, onClick = { menuOpen = false; actions.onDelete(task.id) })
            }
        }
    }
}

/** Datos debajo de la tarea: tiempo registrado e issue de GitHub vinculado. */
@Composable
private fun TaskMeta(task: ProjectTask, now: Long) {
    val parts = listOfNotNull(
        task.issueNumber?.let { "#$it" },
        task.totalMs(now).takeIf { it >= 60_000 }?.let { "⏱ ${formatDuration(it)}" },
        if (task.status == TaskStatus.IN_PROGRESS && !task.done) "en curso" else null
    )
    if (parts.isNotEmpty()) {
        Text(parts.joinToString(" · "), color = if (task.timerStartedAt != null) DevFlowColors.Green else DevFlowColors.TextDark, fontSize = 10.sp)
    }
}

/** Botón ▶/■ para iniciar o detener el cronómetro de la tarea. */
@Composable
private fun TimerButton(task: ProjectTask, actions: TaskActions) {
    if (task.done) return
    val running = task.timerStartedAt != null
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (running) DevFlowColors.Green.copy(alpha = 0.2f) else DevFlowColors.Background)
            .clickable { if (running) actions.onStopTimer(task.id) else actions.onStartTimer(task.id) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(if (running) "■" else "▶", color = if (running) DevFlowColors.Green else DevFlowColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Vista Kanban: tres columnas (Por hacer, En curso, Hecho) con scroll horizontal. */
@Composable
private fun KanbanBoard(tasks: List<ProjectTask>, now: Long, actions: TaskActions) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TaskStatus.entries.forEach { status ->
            val columnTasks = tasks.filter { it.status == status }
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DevFlowColors.Background)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${status.label} (${columnTasks.size})", color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (columnTasks.isEmpty()) Text("—", color = DevFlowColors.TextDark, fontSize = 12.sp)
                columnTasks.forEach { task ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DevFlowColors.Card)
                            .border(1.dp, DevFlowColors.Border, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(task.title, color = Color.White, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(taskPhases[task.phaseIndex].name, color = DevFlowColors.TextDark, fontSize = 10.sp)
                        TaskMeta(task, now)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                            // Flechas para mover la tarea entre columnas
                            if (status != TaskStatus.TODO) {
                                Text("◀", color = DevFlowColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.clickable {
                                    actions.onSetStatus(task, TaskStatus.entries[status.ordinal - 1])
                                })
                            }
                            if (status != TaskStatus.DONE) {
                                Text("▶ ${TaskStatus.entries[status.ordinal + 1].label}", color = DevFlowColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                    actions.onSetStatus(task, TaskStatus.entries[status.ordinal + 1])
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Diálogo con las tareas que sugirió la IA: todas vienen marcadas y el usuario elige cuáles agregar. */
@Composable
private fun TaskSuggestionsDialog(
    suggestions: List<ProjectTask>,
    onDismiss: () -> Unit,
    onAdd: (List<ProjectTask>) -> Unit
) {
    val selected = remember(suggestions) { mutableStateListOf<String>().apply { addAll(suggestions.map { it.id }) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("Tareas sugeridas", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Elige las que quieres agregar:", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
                suggestions.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (task.id in selected) selected.remove(task.id) else selected.add(task.id) }
                    ) {
                        Checkbox(
                            checked = task.id in selected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = DevFlowColors.Cyan)
                        )
                        Column {
                            Text(task.title, color = Color.White, fontSize = 13.sp)
                            Text(taskPhases[task.phaseIndex].name, color = DevFlowColors.TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(suggestions.filter { it.id in selected }) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) { Text("Agregar ${selected.size}") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DevFlowColors.TextSecondary) }
        }
    )
}
