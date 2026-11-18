package com.santiagorey.devflow.ai

import com.santiagorey.devflow.projects.ProjectTask
import org.json.JSONArray

// Modos, atajos y estado del tutorial del asistente.

/** Cómo responde el asistente: con explicaciones paso a paso o directo al grano. */
enum class MentorMode(val label: String, val description: String) {
    JUNIOR("Junior", "Explica paso a paso, con el porqué de cada cosa"),
    SENIOR("Senior", "Respuestas directas y técnicas");

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: JUNIOR
    }
}

/**
 * Extrae las tareas del JSON que devuelve la IA. Tolera texto alrededor del arreglo
 * o bloques ```json, que algunos modelos agregan aunque se les pida no hacerlo.
 */
internal fun parseTaskSuggestions(reply: String): List<ProjectTask> {
    val start = reply.indexOf('[')
    val end = reply.lastIndexOf(']')
    if (start == -1 || end <= start) return emptyList()
    val array = runCatching { JSONArray(reply.substring(start, end + 1)) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        val item = array.optJSONObject(i) ?: return@mapNotNull null
        val title = item.optString("title").trim()
        if (title.isEmpty()) return@mapNotNull null
        ProjectTask(title = title.take(120), phaseIndex = item.optInt("phase", 0).coerceIn(0, 3))
    }
}

/** Atajos del asistente; algunos necesitan un repositorio vinculado. */
enum class QuickAction(val label: String, val needsRepo: Boolean, val prompt: String) {
    REVIEW_REPO(
        "Revisar avance del repo", true,
        "Revisa el estado de mi repositorio: resume qué avancé en los últimos commits, qué parece " +
            "faltar según los issues, la estructura y el README, señala riesgos o malas prácticas que veas " +
            "y dame los 3 próximos pasos concretos."
    ),
    ESTIMATE_PROGRESS(
        "Estimar % de avance", true,
        "Con la información del repositorio, estima el porcentaje de avance real del proyecto y en qué fase " +
            "está (Planificación, Diseño, Desarrollo o QA). Compáralo con el avance que tengo registrado y explica por qué."
    ),
    REVIEW_COMMITS(
        "Revisar mis commits", true,
        "Revisa los mensajes de mis últimos commits: ¿son claros? Sugiere cómo mejorarlos siguiendo " +
            "Conventional Commits, con ejemplos reescritos."
    ),
    NEXT_STEP(
        "¿Qué hago ahora?", false,
        "Según la fase, el avance y la fecha de entrega del proyecto, ¿cuál debería ser mi siguiente paso? " +
            "Dame un plan corto para esta semana."
    ),
    EXPLAIN_STACK(
        "Explícame el stack", false,
        "Explícame las tecnologías del stack de este proyecto: para qué sirve cada una, cómo se conectan " +
            "entre sí y qué debería aprender primero."
    )
}

/** Estado del tutorial "Conectar tu IA". */
data class AiSetupState(
    val isTesting: Boolean = false,
    val error: String? = null,
    val models: List<AiModel> = emptyList(),
    val selectedModelId: String? = null,
    val testedProvider: AiProvider? = null
)

