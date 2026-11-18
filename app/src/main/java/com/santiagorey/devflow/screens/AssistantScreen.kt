package com.santiagorey.devflow.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.ai.ChatMessage
import com.santiagorey.devflow.ai.MentorMode
import com.santiagorey.devflow.ai.QuickAction
import com.santiagorey.devflow.github.RepoSnapshot
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project

/**
 * Asistente de IA: conversa sobre un proyecto y revisa su repositorio de GitHub.
 */
@Composable
fun AssistantScreen(
    assistant: AssistantViewModel,
    projects: List<Project>,
    clients: List<Client>,
    initialProjectId: String?,
    initialPrompt: String? = null,
    linkingGitHub: Boolean,
    hasGitHubAccess: Boolean,
    onBackClick: () -> Unit,
    onOpenSetup: () -> Unit,
    onConnectGitHub: () -> Unit,
    onLinkRepo: (projectId: String, repoFullName: String?) -> Unit
) {
    // Historial guardado de la conversación
    LaunchedEffect(Unit) { assistant.ensureHistoryLoaded() }

    // Proyecto inicial (al llegar desde "Revisar con IA")
    LaunchedEffect(initialProjectId) {
        if (initialProjectId != null) assistant.selectProject(projects.firstOrNull { it.id == initialProjectId })
    }

    val project = projects.firstOrNull { it.id == assistant.activeProjectId }
    val client = project?.clientId?.let { id -> clients.firstOrNull { it.id == id } }

    // Si se vincula o cambia el repo del proyecto, se vuelve a leer.
    LaunchedEffect(project?.repoFullName, hasGitHubAccess) {
        if (project != null) assistant.selectProject(project)
    }

    var input by rememberSaveable { mutableStateOf(initialPrompt.orEmpty()) }
    var showRepoPicker by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf<AssistantTool?>(null) }
    val openModelPicker = {
        assistant.loadModels()
        showModelPicker = true
    }
    val listState = rememberLazyListState()

    LaunchedEffect(assistant.messages.size, assistant.isSending) {
        val count = assistant.messages.size + if (assistant.isSending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = {
            AssistantTopBar(
                subtitle = assistant.provider?.let { "${it.displayName} · ${assistant.modelId} ▾" } ?: "Sin IA conectada",
                onSubtitleClick = if (assistant.isConfigured) openModelPicker else null,
                onBackClick = onBackClick,
                onSettingsClick = onOpenSetup,
                onNewChat = { assistant.newChat() },
                canReset = assistant.messages.isNotEmpty()
            )
        },
        bottomBar = {
            if (assistant.isConfigured) {
                Column {
                    ToolsRow(
                        hasRepo = assistant.snapshot != null,
                        hasPullRequests = (assistant.snapshot?.openPullRequests ?: 0) > 0,
                        enabled = !assistant.isSending && !assistant.isPreparingReview,
                        preparing = assistant.isPreparingReview,
                        onTool = { activeTool = it }
                    )
                    ChatInput(
                        value = input,
                        onValueChange = { input = it },
                        sending = assistant.isSending,
                        onSend = {
                            assistant.send(input, project, client)
                            input = ""
                        },
                        onStop = { assistant.stopGenerating() }
                    )
                }
            }
        }
    ) { padding ->
        if (!assistant.isConfigured) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmptyStateCard(
                    title = "Conecta tu IA de confianza",
                    subtitle = "El asistente revisa tus proyectos y repositorios de GitHub y te guía paso a paso. " +
                        "Funciona con Gemini (gratis), Claude o ChatGPT usando tu propia cuenta.",
                    actionLabel = "Empezar tutorial",
                    onAction = onOpenSetup
                )
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ContextCard(
                    projects = projects,
                    project = project,
                    snapshot = assistant.snapshot,
                    isLoadingRepo = assistant.isLoadingRepo,
                    repoError = assistant.repoError,
                    hasGitHubAccess = hasGitHubAccess,
                    linkingGitHub = linkingGitHub,
                    mentorMode = assistant.mentorMode,
                    onSelectProject = { assistant.selectProject(it) },
                    onConnectGitHub = onConnectGitHub,
                    onPickRepo = {
                        assistant.loadRepos()
                        showRepoPicker = true
                    },
                    onRefreshRepo = { assistant.refreshSnapshot(project) },
                    onModeChange = assistant::updateMentorMode
                )
            }

            if (assistant.messages.isEmpty()) {
                item {
                    QuickActions(
                        hasRepo = assistant.snapshot != null,
                        enabled = !assistant.isSending,
                        onAction = { action -> assistant.send(action.prompt, project, client) }
                    )
                }
            }

            val lastAssistantIndex = assistant.messages.indexOfLast { it.role == ChatMessage.Role.ASSISTANT }
            itemsIndexed(assistant.messages, key = { index, m -> "$index-${m.timestamp}-${m.role}" }) { index, message ->
                MessageBubble(
                    message = message,
                    showActions = index == lastAssistantIndex && !assistant.isSending && message.text.isNotBlank(),
                    onExplainSimpler = { assistant.explainSimpler(project, client) }
                )
            }

            if (assistant.isSending && assistant.messages.lastOrNull()?.text.isNullOrEmpty()) {
                item { ThinkingBubble(assistant.provider?.displayName ?: "La IA") }
            }

            assistant.chatError?.let { error ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DevFlowColors.Red.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Text(error, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                        Row {
                            TextButton(onClick = { assistant.retry(project, client) }) {
                                Text("Reintentar", color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = openModelPicker) {
                                Text("Cambiar modelo", color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    activeTool?.let { tool ->
        ToolDialog(
            tool = tool,
            snapshot = assistant.snapshot,
            onDismiss = { activeTool = null },
            onReviewFile = { path ->
                activeTool = null
                assistant.reviewFile(path, project, client)
            },
            onReviewPullRequest = { number, title ->
                activeTool = null
                assistant.reviewPullRequest(number, title, project, client)
            },
            onExplainError = { text ->
                activeTool = null
                assistant.explainError(text, project, client)
            }
        )
    }

    if (showModelPicker) {
        ModelPickerDialog(
            assistant = assistant,
            onDismiss = { showModelPicker = false },
            onPick = { id ->
                showModelPicker = false
                assistant.changeModel(id)
            }
        )
    }

    if (showRepoPicker && project != null) {
        RepoPickerDialog(
            assistant = assistant,
            currentRepo = project.repoFullName,
            onDismiss = { showRepoPicker = false },
            onPick = { fullName ->
                showRepoPicker = false
                onLinkRepo(project.id, fullName)
            }
        )
    }
}

/** Barra superior del asistente: volver, IA y modelo en uso (al tocarlo se cambia), menú. */
@Composable
private fun AssistantTopBar(
    subtitle: String,
    onSubtitleClick: (() -> Unit)?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewChat: () -> Unit,
    canReset: Boolean
) {
    androidx.compose.material3.Surface(
        color = DevFlowColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, DevFlowColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("🤖 Asistente IA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    subtitle,
                    color = if (onSubtitleClick != null) DevFlowColors.Cyan else DevFlowColors.TextSecondary,
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = if (onSubtitleClick != null) Modifier.clickable { onSubtitleClick() } else Modifier
                )
            }
            if (canReset) {
                TextButton(onClick = onNewChat) { Text("Nuevo chat", color = DevFlowColors.Cyan, fontSize = 12.sp) }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = "Configurar IA", tint = DevFlowColors.TextSecondary)
            }
        }
    }
}

/** Tarjeta de contexto: de qué proyecto se habla y el estado de su repositorio de GitHub. */
@Composable
private fun ContextCard(
    projects: List<Project>,
    project: Project?,
    snapshot: RepoSnapshot?,
    isLoadingRepo: Boolean,
    repoError: String?,
    hasGitHubAccess: Boolean,
    linkingGitHub: Boolean,
    mentorMode: MentorMode,
    onSelectProject: (Project?) -> Unit,
    onConnectGitHub: () -> Unit,
    onPickRepo: () -> Unit,
    onRefreshRepo: () -> Unit,
    onModeChange: (MentorMode) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    DevFlowCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Proyecto
            SmallCaption("PROYECTO")
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DevFlowColors.DarkCard)
                        .border(1.dp, DevFlowColors.Border, RoundedCornerShape(10.dp))
                        .clickable { menuOpen = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_folder), null, tint = DevFlowColors.Cyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        project?.let { "${it.name} · ${it.progress}%" } ?: "Pregunta general (sin proyecto)",
                        color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("▾", color = DevFlowColors.TextSecondary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Pregunta general (sin proyecto)", color = DevFlowColors.TextSecondary) },
                        onClick = { menuOpen = false; onSelectProject(null) }
                    )
                    projects.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name, color = Color.White) },
                            onClick = { menuOpen = false; onSelectProject(p) }
                        )
                    }
                }
            }

            // Repositorio
            if (project != null) {
                SmallCaption("REPOSITORIO DE GITHUB")
                when {
                    !hasGitHubAccess -> ActionRow(
                        text = "Conecta GitHub para que la IA revise el código de este proyecto.",
                        action = if (linkingGitHub) "Conectando…" else "Conectar GitHub",
                        enabled = !linkingGitHub,
                        onClick = onConnectGitHub
                    )
                    project.repoFullName == null -> ActionRow(
                        text = "Este proyecto aún no tiene repositorio vinculado.",
                        action = "Vincular repo",
                        onClick = onPickRepo
                    )
                    isLoadingRepo -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DevFlowColors.Cyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leyendo ${project.repoFullName}…", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
                    }
                    repoError != null -> ActionRow(text = repoError, action = "Reintentar", onClick = onRefreshRepo)
                    snapshot != null -> RepoSummary(snapshot, onChange = onPickRepo, onRefresh = onRefreshRepo)
                }
            }

            // Modo de mentoría
            SmallCaption("MODO DEL ASISTENTE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MentorMode.entries.forEach { mode ->
                    FilterChipBox(
                        text = mode.label,
                        selected = mode == mentorMode,
                        onClick = { onModeChange(mode) }
                    )
                }
            }
            Text(mentorMode.description, color = DevFlowColors.TextSecondary, fontSize = 11.sp)
        }
    }
}

/** Resumen del repo leído: lenguajes, commits, issues y PRs abiertos. */
@Composable
private fun RepoSummary(snapshot: RepoSnapshot, onChange: () -> Unit, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DevFlowColors.DarkCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_github), null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(snapshot.repo.fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (snapshot.repo.isPrivate) StatusChip(text = "PRIVADO", color = DevFlowColors.Purple)
        }
        Text(
            "${snapshot.recentCommits.size} commits recientes · ${snapshot.openIssueCount} issues · ${snapshot.openPullRequests} PRs abiertos",
            color = DevFlowColors.TextSecondary, fontSize = 11.sp
        )
        val topLanguages = snapshot.languages.keys.take(3)
        Text(
            "Último push: ${snapshot.pushedAt.ifBlank { "—" }}" + if (topLanguages.isNotEmpty()) " · ${topLanguages.joinToString()}" else "",
            color = DevFlowColors.TextSecondary, fontSize = 11.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("↻ Actualizar", color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRefresh() }.padding(vertical = 4.dp))
            Text("Cambiar repo", color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onChange() }.padding(vertical = 4.dp))
        }
    }
}

/** Fila con un texto y un enlace de acción a la derecha. */
@Composable
private fun ActionRow(text: String, action: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = DevFlowColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
        ) {
            Text(action, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/** Texto pequeño gris de ayuda. */
@Composable
private fun SmallCaption(text: String) {
    Text(text, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevFlowColors.TextSecondary, fontWeight = FontWeight.Bold))
}

/** Atajos de preguntas frecuentes (QuickAction). Los que necesitan repo se desactivan si no hay. */
@Composable
private fun QuickActions(hasRepo: Boolean, enabled: Boolean, onAction: (QuickAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Prueba con:", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAction.entries.filter { !it.needsRepo || hasRepo }.forEach { action ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DevFlowColors.Card)
                        .border(1.dp, DevFlowColors.Cyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable(enabled = enabled) { onAction(action) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(action.label, color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Burbuja de un mensaje del chat: los tuyos a la derecha y los de la IA a la izquierda
 * con Markdown simple y botón "Explícamelo más simple".
 */
@Composable
private fun MessageBubble(message: ChatMessage, showActions: Boolean, onExplainSimpler: () -> Unit) {
    val isUser = message.role == ChatMessage.Role.USER
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        if (message.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.85f else 1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isUser) DevFlowColors.Primary else DevFlowColors.Card)
                    .then(if (isUser) Modifier else Modifier.border(1.dp, DevFlowColors.Border, RoundedCornerShape(14.dp)))
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = if (isUser) AnnotatedString(message.text) else renderSimpleMarkdown(message.text),
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        if (isUser && message.attachment != null) {
            Text("📎 Se adjuntó contenido para la IA", color = DevFlowColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, end = 4.dp))
        }
        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp, start = 4.dp)) {
                Text(
                    "🧒 Explícalo más simple", color = DevFlowColors.Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onExplainSimpler() }.padding(vertical = 4.dp)
                )
                Text(
                    "Copiar", color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Respuesta", message.text))
                            Toast.makeText(context, "Respuesta copiada", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

/** Burbuja "La IA está pensando…" mientras llega la respuesta. */
@Composable
private fun ThinkingBubble(name: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DevFlowColors.Card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = DevFlowColors.Cyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text("$name está pensando…", color = DevFlowColors.TextSecondary, fontSize = 13.sp)
    }
}

/** Herramientas del asistente que necesitan un dato extra (archivo, PR o texto del error). */
internal enum class AssistantTool { REVIEW_FILE, REVIEW_PR, EXPLAIN_ERROR }

/** Herramientas del chat: revisar un archivo, revisar un PR o explicar un error. */
@Composable
private fun ToolsRow(hasRepo: Boolean, hasPullRequests: Boolean, enabled: Boolean, preparing: Boolean, onTool: (AssistantTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DevFlowColors.Background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (preparing) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = DevFlowColors.Cyan)
            Text("Leyendo GitHub…", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
        }
        ToolChip("🐞 Explicar un error", enabled) { onTool(AssistantTool.EXPLAIN_ERROR) }
        if (hasRepo) ToolChip("📄 Revisar archivo", enabled) { onTool(AssistantTool.REVIEW_FILE) }
        if (hasPullRequests) ToolChip("🔀 Revisar PR", enabled) { onTool(AssistantTool.REVIEW_PR) }
    }
}

/** Chip de una herramienta. */
@Composable
private fun ToolChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DevFlowColors.Card)
            .border(1.dp, DevFlowColors.Border, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (enabled) Color.White else DevFlowColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Caja para escribir: botón Enviar, o Detener mientras la IA responde. */
@Composable
private fun ChatInput(value: String, onValueChange: (String) -> Unit, sending: Boolean, onSend: () -> Unit, onStop: () -> Unit) {
    androidx.compose.material3.Surface(
        color = DevFlowColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, DevFlowColors.Border),
        modifier = Modifier.imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).heightIn(max = 140.dp),
                placeholder = { Text("Pregunta sobre tu proyecto…", color = DevFlowColors.TextDark, fontSize = 14.sp) },
                colors = devFlowTextFieldColors(),
                shape = RoundedCornerShape(20.dp),
                maxLines = 5
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Mientras la IA escribe, el botón sirve para detenerla.
            val canSend = value.isNotBlank() && !sending
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            sending -> DevFlowColors.Red
                            canSend -> DevFlowColors.BrightCyan
                            else -> DevFlowColors.Border
                        }
                    )
                    .clickable(enabled = sending || canSend) { if (sending) onStop() else onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                } else {
                    Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = "Enviar", tint = if (canSend) Color.Black else DevFlowColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

