package com.santiagorey.devflow.notifications

import com.santiagorey.devflow.github.CiRun
import com.santiagorey.devflow.github.RepoAudit
import com.santiagorey.devflow.projects.Project

/** Aviso que aparece en la campana del encabezado. */
data class DevFlowAlert(
    val id: String,
    val severity: Severity,
    val title: String,
    val detail: String,
    val projectId: String? = null,
    val url: String? = null
) {
    /** HIGH = rojo (vencido, CI fallando). MEDIUM = amarillo (entrega cercana). */
    enum class Severity { HIGH, MEDIUM }
}

/**
 * Calcula los avisos actuales. No se guardan: cuando el problema se resuelve
 * (se entrega el proyecto, se arregla el CI…), el aviso desaparece solo.
 */
fun buildAlerts(
    projects: List<Project>,
    ciRuns: Collection<CiRun>,
    audit: RepoAudit?,
    qualityAlertsEnabled: Boolean
): List<DevFlowAlert> {
    val alerts = mutableListOf<DevFlowAlert>()

    projects.filter { !it.isCompleted }.forEach { project ->
        val days = project.daysToDelivery ?: return@forEach
        when {
            days < 0 -> alerts += DevFlowAlert(
                id = "overdue_${project.id}",
                severity = DevFlowAlert.Severity.HIGH,
                title = "Entrega vencida: ${project.name}",
                detail = "Venció hace ${-days} día${if (days == -1L) "" else "s"} · avance ${project.progress}%",
                projectId = project.id
            )
            days <= 2 -> alerts += DevFlowAlert(
                id = "soon_${project.id}",
                severity = DevFlowAlert.Severity.MEDIUM,
                title = "Entrega ${if (days == 0L) "hoy" else if (days == 1L) "mañana" else "en 2 días"}: ${project.name}",
                detail = "Avance actual ${project.progress}%" +
                    project.tasks.count { !it.done }.let { if (it > 0) " · $it tareas pendientes" else "" },
                projectId = project.id
            )
        }
    }

    if (qualityAlertsEnabled) {
        ciRuns.filter { it.failed }.forEach { run ->
            alerts += DevFlowAlert(
                id = "ci_${run.repoFullName}",
                severity = DevFlowAlert.Severity.HIGH,
                title = "Falla el CI en ${run.repoFullName}",
                detail = "\"${run.workflowName}\" falló en la rama ${run.branch}. Toca para ver el detalle en GitHub.",
                projectId = projects.firstOrNull { it.repoFullName == run.repoFullName }?.id,
                url = run.url
            )
        }
        val secrets = audit?.checks?.firstOrNull { it.title == "Sin secretos en el repositorio" && !it.passed }
        if (audit != null && secrets != null) {
            alerts += DevFlowAlert(
                id = "secrets_${audit.repoFullName}",
                severity = DevFlowAlert.Severity.HIGH,
                title = "Posibles secretos en ${audit.repoFullName}",
                detail = secrets.detail,
                projectId = projects.firstOrNull { it.repoFullName == audit.repoFullName }?.id
            )
        }
    }

    return alerts.sortedBy { it.severity.ordinal }
}
