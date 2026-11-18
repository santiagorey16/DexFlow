package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.github.RepoSnapshot

// Diálogos del asistente (modelo, herramientas, repositorio) y utilidades de texto.

/** Diálogo para cambiar el modelo de la IA sin repetir el tutorial (lista en LazyColumn). */
@Composable
internal fun ModelPickerDialog(
    assistant: AssistantViewModel,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("Cambiar modelo", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            when {
                assistant.isLoadingModels -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DevFlowColors.Cyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargando modelos…", color = DevFlowColors.TextSecondary, fontSize = 13.sp)
                }
                assistant.modelsError != null -> Text(assistant.modelsError.orEmpty(), color = DevFlowColors.Red, fontSize = 13.sp)
                else -> LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(assistant.availableModels, key = { it.id }) { model ->
                        val selected = model.id == assistant.modelId
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) DevFlowColors.DarkCard else Color.Transparent)
                                .clickable { onPick(model.id) }
                                .padding(10.dp)
                        ) {
                            Text(
                                (if (selected) "✓ " else "") + model.displayName,
                                color = if (selected) DevFlowColors.Cyan else Color.White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            if (model.displayName != model.id) {
                                Text(model.id, color = DevFlowColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = DevFlowColors.TextSecondary) }
        }
    )
}

/** Diálogo de cada herramienta: elegir un archivo del repo, un PR, o pegar un error. */
@Composable
internal fun ToolDialog(
    tool: AssistantTool,
    snapshot: RepoSnapshot?,
    onDismiss: () -> Unit,
    onReviewFile: (String) -> Unit,
    onReviewPullRequest: (Int, String) -> Unit,
    onExplainError: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val title = when (tool) {
        AssistantTool.REVIEW_FILE -> "Revisar un archivo"
        AssistantTool.REVIEW_PR -> "Revisar un Pull Request"
        AssistantTool.EXPLAIN_ERROR -> "Explicar un error"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tool) {
                    AssistantTool.EXPLAIN_ERROR -> {
                        Text("Pega el mensaje de error o el stacktrace (Logcat, consola, etc.):", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp),
                            placeholder = { Text("java.lang.NullPointerException: …", color = DevFlowColors.TextDark, fontSize = 12.sp) },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                            colors = devFlowTextFieldColors()
                        )
                    }
                    AssistantTool.REVIEW_FILE -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            placeholder = { Text("Buscar archivo…", color = DevFlowColors.TextDark) },
                            colors = devFlowTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Solo archivos de texto o código: imágenes, PDF u Office no se pueden revisar como texto.
                        val files = snapshot?.allPaths.orEmpty()
                            .filter { isReviewableFile(it) }
                            .filter { text.isBlank() || it.contains(text, ignoreCase = true) }
                            .take(80)
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(files, key = { it }) { path ->
                                Text(
                                    path, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth().clickable { onReviewFile(path) }.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    AssistantTool.REVIEW_PR -> {
                        val prs = snapshot?.openIssues.orEmpty().filter { it.isPullRequest }
                        if (prs.isEmpty()) Text("No hay Pull Requests abiertos.", color = DevFlowColors.TextSecondary, fontSize = 13.sp)
                        prs.forEach { pr ->
                            Text(
                                "#${pr.number} · ${pr.title}", color = Color.White, fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().clickable { onReviewPullRequest(pr.number, pr.title) }.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (tool == AssistantTool.EXPLAIN_ERROR) {
                Button(
                    onClick = { onExplainError(text) },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                ) { Text("Explicar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DevFlowColors.TextSecondary) }
        }
    )
}

/** Diálogo para vincular un repositorio de GitHub al proyecto, con buscador. */
@Composable
internal fun RepoPickerDialog(
    assistant: AssistantViewModel,
    currentRepo: String?,
    title: String = "Vincular repositorio",
    allowUnlink: Boolean = true,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = assistant.repos.filter { query.isBlank() || it.fullName.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Buscar…", color = DevFlowColors.TextDark) },
                    colors = devFlowTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                when {
                    assistant.isLoadingRepos -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DevFlowColors.Cyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargando tus repositorios…", color = DevFlowColors.TextSecondary, fontSize = 13.sp)
                    }
                    assistant.reposError != null -> Text(assistant.reposError.orEmpty(), color = DevFlowColors.Red, fontSize = 13.sp)
                    filtered.isEmpty() -> Text("No hay repositorios que coincidan.", color = DevFlowColors.TextSecondary, fontSize = 13.sp)
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(filtered, key = { it.fullName }) { repo ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (repo.fullName == currentRepo) DevFlowColors.DarkCard else Color.Transparent)
                                    .clickable { onPick(repo.fullName) }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(repo.fullName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (repo.isPrivate) Text("🔒", fontSize = 11.sp)
                                }
                                Text(
                                    listOfNotNull(repo.language, "act. ${repo.updatedAt}").joinToString(" · "),
                                    color = DevFlowColors.TextSecondary, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = DevFlowColors.TextSecondary) }
        },
        dismissButton = {
            if (currentRepo != null && allowUnlink) {
                TextButton(onClick = { onPick(null) }) { Text("Desvincular", color = DevFlowColors.Red) }
            }
        }
    )
}

/** Markdown mínimo para las respuestas: **negrita**, `código`, bloques ``` y títulos con #. */
internal fun renderSimpleMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var inCodeBlock = false
    text.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append("\n")
        if (rawLine.trimStart().startsWith("```")) {
            inCodeBlock = !inCodeBlock
            return@forEachIndexed
        }
        if (inCodeBlock) {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, background = Color(0xFF0A0D14), color = Color(0xFFE2E8F0))) {
                append(rawLine)
            }
            return@forEachIndexed
        }
        val heading = rawLine.trimStart().takeWhile { it == '#' }.length
        val line = if (heading > 0) rawLine.trimStart().drop(heading).trimStart() else rawLine
        if (heading > 0) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DevFlowColors.BrightCyan)) { appendInline(line) }
        } else {
            appendInline(line.replaceFirst(Regex("^(\\s*)[*-] "), "$1• "))
        }
    }
}

/** Aplica **negrita** y `código` dentro de una línea. */
private fun AnnotatedString.Builder.appendInline(line: String) {
    var i = 0
    while (i < line.length) {
        when {
            line.startsWith("**", i) && line.indexOf("**", i + 2) > i -> {
                val end = line.indexOf("**", i + 2)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.substring(i + 2, end)) }
                i = end + 2
            }
            line[i] == '`' && line.indexOf('`', i + 1) > i -> {
                val end = line.indexOf('`', i + 1)
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF1E293B), color = DevFlowColors.BrightCyan)) {
                    append(line.substring(i + 1, end))
                }
                i = end + 1
            }
            else -> {
                append(line[i])
                i++
            }
        }
    }
}

/** Extensiones binarias (imágenes, documentos, compilados) que no tiene sentido enviar a la IA como texto. */
private val BINARY_EXTENSIONS = setOf(
    "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "svgz", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "zip", "rar", "7z", "gz", "tar", "jar", "aar", "apk", "aab", "class", "so", "dll", "exe", "bin", "mp3", "mp4",
    "wav", "mov", "avi", "ttf", "otf", "woff", "woff2", "keystore", "jks", "db", "sqlite"
)

/** true si el archivo es de texto o código y se puede pasar a la IA para revisarlo. */
internal fun isReviewableFile(path: String): Boolean =
    path.substringAfterLast('/').substringAfterLast('.', "").lowercase() !in BINARY_EXTENSIONS

