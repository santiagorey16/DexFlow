package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.github.RepoAudit
import com.santiagorey.devflow.projects.Project

/**
 * Tarjeta "Apego a Normas DevFlow": audita un repositorio de GitHub contra las guías
 * (secretos, .gitignore, pruebas, CI, Conventional Commits y README).
 */
@Composable
fun RepoAuditCard(
    assistant: AssistantViewModel,
    projects: List<Project>,
    githubLinked: Boolean,
    githubRepoAccess: Boolean,
    linkingGitHub: Boolean,
    onConnectGitHubRepos: () -> Unit,
    onReviewWithAi: (projectId: String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    // Por defecto se audita el repo del primer proyecto que tenga uno vinculado.
    val defaultRepo = projects.firstNotNullOfOrNull { it.repoFullName }
    LaunchedEffect(githubRepoAccess, defaultRepo) {
        if (githubRepoAccess && assistant.auditRepoName == null && defaultRepo != null) {
            assistant.auditRepo(defaultRepo)
        }
    }

    val audit = assistant.audit
    val repoName = assistant.auditRepoName
    val linkedProject = repoName?.let { name -> projects.firstOrNull { it.repoFullName == name } }

    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUDITORÍA DE REPOSITORIOS", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.Cyan))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Apego a Normas DevFlow", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White))
                    Text(
                        text = when {
                            !githubRepoAccess && githubLinked -> "Tu GitHub está conectado, pero falta darle permiso a DevFlow para leer tus repositorios."
                            !githubRepoAccess -> "Conecta GitHub para evaluar tus repositorios contra estas guías."
                            repoName == null -> "Elige un repositorio para evaluarlo."
                            else -> repoName
                        },
                        style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                ScoreCircle(audit?.score, loading = assistant.isAuditing)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuditMetric(Modifier.weight(1f), "Seguridad", audit?.let { "${it.securityPercent}%" }, audit?.securityPercent)
                AuditMetric(Modifier.weight(1f), "Pruebas", audit?.let { "${it.testFiles}" }, audit?.let { if (it.testFiles > 0) 100 else 0 })
                AuditMetric(Modifier.weight(1f), "Commits", audit?.conventionalCommitPercent?.let { "$it%" }, audit?.conventionalCommitPercent)
            }

            assistant.auditError?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = DevFlowColors.Red, fontSize = 12.sp)
            }

            if (audit != null) {
                Spacer(modifier = Modifier.height(14.dp))
                AuditChecklist(audit)
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                !githubRepoAccess -> PrimaryActionButton(
                    text = when {
                        linkingGitHub -> "Conectando…"
                        githubLinked -> "Dar acceso a mis repositorios"
                        else -> "Conectar GitHub"
                    },
                    enabled = !linkingGitHub,
                    onClick = onConnectGitHubRepos
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryActionButton(
                        text = if (repoName == null) "Elegir repositorio" else "Auditar otro repositorio",
                        enabled = !assistant.isAuditing,
                        onClick = {
                            assistant.loadRepos()
                            showPicker = true
                        }
                    )
                    if (audit != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "↻ Volver a auditar", color = DevFlowColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { assistant.auditRepo(audit.repoFullName, force = true) }.padding(vertical = 4.dp)
                            )
                            if (linkedProject != null) {
                                Text(
                                    "🤖 Mejorar con IA", color = DevFlowColors.Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onReviewWithAi(linkedProject.id) }.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        RepoPickerDialog(
            assistant = assistant,
            currentRepo = repoName,
            title = "Auditar repositorio",
            allowUnlink = false,
            onDismiss = { showPicker = false },
            onPick = { fullName ->
                showPicker = false
                if (fullName != null) assistant.auditRepo(fullName, force = true)
            }
        )
    }
}

private fun scoreColor(value: Int?): Color = when {
    value == null -> DevFlowColors.TextSecondary
    value >= 80 -> DevFlowColors.Green
    value >= 50 -> Color(0xFFFBBF24)
    else -> DevFlowColors.Red
}

@Composable
private fun ScoreCircle(score: Int?, loading: Boolean) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(DevFlowColors.Background)
            .border(2.dp, if (score == null) DevFlowColors.Border else scoreColor(score), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = DevFlowColors.Cyan)
        } else {
            Text(score?.toString() ?: "--", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scoreColor(score)))
        }
    }
}

@Composable
private fun AuditMetric(modifier: Modifier, label: String, value: String?, percent: Int?) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DevFlowColors.Background)
            .border(1.dp, DevFlowColors.Border, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(label, style = TextStyle(fontSize = 10.sp, color = DevFlowColors.TextSecondary))
        Text(value ?: "--", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = scoreColor(percent)))
    }
}

@Composable
private fun AuditChecklist(audit: RepoAudit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        audit.checks.forEach { check ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    if (check.passed) "✓" else "✗",
                    color = if (check.passed) DevFlowColors.Green else DevFlowColors.Red,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(check.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(check.guideCode, color = DevFlowColors.TextDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(check.detail, color = DevFlowColors.TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
        Text(
            "Revisión rápida basada en la estructura y los commits del repo; no reemplaza un análisis de seguridad completo.",
            color = DevFlowColors.TextDark, fontSize = 10.sp
        )
    }
}

@Composable
private fun PrimaryActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(42.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White), maxLines = 1)
    }
}
