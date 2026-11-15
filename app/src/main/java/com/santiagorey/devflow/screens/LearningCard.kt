package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.projects.Project

/** Aprendizaje en Guías: reto del día y ruta de aprendizaje personalizada por la IA. */
@Composable
fun LearningCard(
    assistant: AssistantViewModel,
    projects: List<Project>,
    onConnectAi: () -> Unit,
    onAskAi: (prompt: String) -> Unit
) {
    var showHint by rememberSaveable { mutableStateOf(false) }
    var showSolution by rememberSaveable { mutableStateOf(false) }
    var pathExpanded by rememberSaveable { mutableStateOf(false) }

    // El reto del día se genera solo (una vez al día y queda guardado).
    LaunchedEffect(assistant.isConfigured) {
        if (assistant.isConfigured) assistant.loadDailyChallenge(projects)
    }

    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("APRENDE CON TU IA", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.Purple))

            if (!assistant.isConfigured) {
                Text(
                    "Conecta tu IA para recibir un reto de programación cada día y una ruta de aprendizaje según tu stack.",
                    color = DevFlowColors.TextSecondary, fontSize = 12.sp
                )
                TextButton(onClick = onConnectAi) { Text("Conectar mi IA", color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold) }
                return@Column
            }

            // ── Reto del día ──
            Text("🎯 Reto del día", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val challenge = assistant.dailyChallenge
            when {
                assistant.isLoadingChallenge -> Loading("Preparando tu reto…")
                challenge != null -> {
                    Text(challenge.title, color = DevFlowColors.BrightCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    SelectionContainer { Text(challenge.statement, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LinkText(if (showHint) "Ocultar pista" else "💡 Ver pista") { showHint = !showHint }
                        LinkText(if (showSolution) "Ocultar solución" else "✅ Ver solución") { showSolution = !showSolution }
                    }
                    if (showHint) Block(challenge.hint)
                    if (showSolution) Block(challenge.solution, monospace = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LinkText("🤖 Pedir ayuda a la IA", DevFlowColors.Purple) {
                            onAskAi("Estoy resolviendo este reto y necesito una guía sin que me des la solución completa:\n\n${challenge.title}\n${challenge.statement}")
                        }
                        LinkText("↻ Otro reto") { assistant.loadDailyChallenge(projects, force = true) }
                    }
                }
                else -> Button(
                    onClick = { assistant.loadDailyChallenge(projects, force = true) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                ) { Text("Generar reto") }
            }

            Spacer(modifier = Modifier.size(4.dp))

            // ── Ruta de aprendizaje ──
            Text("🗺 Mi ruta de aprendizaje (4 semanas)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Según el stack de tus proyectos, los lenguajes de tu repo y lo que falló en la auditoría.",
                color = DevFlowColors.TextSecondary, fontSize = 11.sp
            )
            val path = assistant.learningPath
            when {
                assistant.isLoadingPath -> Loading("Armando tu ruta…")
                path != null -> {
                    Block(if (pathExpanded) path else path.lines().take(8).joinToString("\n") + "\n…")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LinkText(if (pathExpanded) "Ver menos" else "Ver ruta completa") { pathExpanded = !pathExpanded }
                        LinkText("↻ Regenerar") { assistant.loadLearningPath(projects, force = true) }
                    }
                }
                else -> Button(
                    onClick = { assistant.loadLearningPath(projects) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Purple)
                ) { Text("Crear mi ruta") }
            }

            assistant.learningError?.let { Text(it, color = DevFlowColors.Red, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun Loading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = DevFlowColors.Purple)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = DevFlowColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun LinkText(text: String, color: Color = DevFlowColors.Cyan, onClick: () -> Unit) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClick() }.padding(vertical = 4.dp))
}

@Composable
private fun Block(text: String, monospace: Boolean = false) {
    SelectionContainer {
        Text(
            text,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DevFlowColors.Background)
                .padding(10.dp)
        )
    }
}
