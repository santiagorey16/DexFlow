package com.santiagorey.devflow.ai

import com.santiagorey.devflow.github.RepoAudit
import com.santiagorey.devflow.github.RepoSnapshot
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.formatDeliveryDate
import com.santiagorey.devflow.projects.formatDuration
import com.santiagorey.devflow.projects.projectPhases

/**
 * Textos que DevFlow le envía a la IA. Son funciones puras (sin estado), así el
 * AssistantViewModel solo se encarga de cuándo pedir y qué hacer con la respuesta.
 */
internal object AssistantPrompts {

    /** Archivos o diffs más largos que esto se recortan para no superar el límite de la IA. */
    const val MAX_ATTACHMENT_CHARS = 60_000

    /** Instrucciones generales + datos del proyecto y de su repositorio. */
    fun system(mentorMode: MentorMode, project: Project?, client: Client?, snapshot: RepoSnapshot?): String = buildString {
        appendLine(
            "Eres el asistente de DevFlow, una app que acompaña a desarrolladores de software " +
                "(junior y senior) paso a paso en sus proyectos. Responde siempre en español, " +
                "con formato simple apto para una pantalla de teléfono: párrafos cortos, listas con " +
                "guiones y bloques de código solo cuando aporten."
        )
        appendLine()
        when (mentorMode) {
            MentorMode.JUNIOR -> appendLine(
                "El usuario es desarrollador junior: explica el porqué de cada recomendación, usa pasos " +
                    "numerados, define los términos técnicos la primera vez y sugiere qué estudiar para mejorar."
            )
            MentorMode.SENIOR -> appendLine(
                "El usuario es desarrollador senior: sé directo y técnico, ve al grano, menciona trade-offs " +
                    "y evita explicar lo básico."
            )
        }
        appendLine()
        appendLine(
            "Usa solo la información del proyecto y del repositorio que aparece abajo. Si algo no está ahí, " +
                "dilo en vez de inventar archivos, commits o resultados."
        )

        if (project != null) {
            appendLine()
            appendLine("## Proyecto en DevFlow")
            appendLine("Nombre: ${project.name}")
            if (project.description.isNotBlank()) appendLine("Descripción: ${project.description}")
            appendLine("Fase: ${project.phase.name} · Avance registrado: ${project.progress}%")
            if (project.stack.isNotEmpty()) appendLine("Stack: ${project.stack.joinToString()}")
            appendLine("Entrega: ${formatDeliveryDate(project.deliveryDateMillis)}" + if (project.isOverdue) " (VENCIDA)" else "")
            if (client != null) appendLine("Cliente: ${client.name}" + if (client.company.isNotBlank()) " (${client.company})" else "")
            if (project.tasks.isNotEmpty()) {
                appendLine("Tareas (${project.tasks.count { it.done }}/${project.tasks.size} completadas):")
                project.tasks.forEach { task ->
                    appendLine("- [${if (task.done) "x" else " "}] ${projectPhases[task.phaseIndex].name}: ${task.title}")
                }
            }
        }

        // El repo solo se incluye si corresponde al proyecto de esta conversación.
        snapshot?.takeIf { project != null && it.repo.fullName == project.repoFullName }?.let {
            appendLine()
            appendLine("## Repositorio de GitHub")
            append(it.toPromptContext())
        }
    }

    // ── Herramientas del chat ──────────────────────────────

    /**
     * Recorta textos muy largos. Devuelve el texto y una nota que se muestra en el chat
     * cuando se recortó.
     */
    fun limitAttachment(content: String): Pair<String, String> =
        if (content.length <= MAX_ATTACHMENT_CHARS) {
            content to ""
        } else {
            content.take(MAX_ATTACHMENT_CHARS) to
                " (es muy largo: se envían los primeros ${MAX_ATTACHMENT_CHARS / 1000} mil caracteres de ${content.length / 1000} mil)"
        }

    /** Pedido para revisar un archivo del repo. */
    fun reviewFile(path: String, note: String) =
        "Revisa el archivo `$path`$note: busca errores, malas prácticas, problemas de seguridad " +
            "y mejoras. Para cada punto indica la línea aproximada y muestra el código corregido."

    /** Pedido para hacer code review de un Pull Request. */
    fun reviewPullRequest(number: Int, title: String, note: String) =
        "Haz un code review del Pull Request #$number \"$title\"$note. Resume qué cambia, señala " +
            "errores, riesgos y malas prácticas, y termina con un veredicto: aprobar o pedir cambios."

    /** Pedido para explicar un error pegado por el usuario. */
    fun explainError(note: String) =
        "Explícame este error$note: qué significa, la causa más probable en mi proyecto y cómo solucionarlo paso a paso."

    const val EXPLAIN_SIMPLER =
        "Explícame tu respuesta anterior de forma más simple, como a alguien que está empezando a programar, " +
            "con una analogía y un ejemplo pequeño."

    // ── Respuestas sueltas (tareas, coach, informe, aprendizaje) ──

    /** Pide 8–12 tareas en JSON, sin repetir las que ya existen. */
    fun suggestTasks(project: Project): String {
        val existing = project.tasks.joinToString { it.title }.ifBlank { "ninguna" }
        return "Propón entre 8 y 12 tareas concretas y accionables para este proyecto, repartidas en las fases " +
            "0 = Planificación, 1 = Diseño, 2 = Desarrollo, 3 = QA & pruebas. No repitas estas tareas existentes: $existing. " +
            "Responde SOLO con un arreglo JSON, sin texto adicional, con este formato: " +
            "[{\"title\": \"texto corto en español\", \"phase\": 0}]"
    }

    const val COACH_TIP =
        "Dame UN solo consejo práctico para avanzar hoy en este proyecto, en máximo 2 frases y sin títulos ni listas."

    /** Resume los proyectos del cliente y pide un informe sin tecnicismos, listo para enviar. */
    fun clientReport(client: Client, projects: List<Project>): String {
        val summary = projects.joinToString("\n") { p ->
            val done = p.tasks.filter { it.done }.takeLast(5).joinToString { it.title }
            val pending = p.tasks.filterNot { it.done }.take(5).joinToString { it.title }
            val time = p.trackedMs().takeIf { it >= 60_000 }?.let { ", tiempo trabajado ${formatDuration(it)}" }.orEmpty()
            "- ${p.name}: ${p.progress}% (${p.phase.name}), entrega ${formatDeliveryDate(p.deliveryDateMillis)}$time" +
                (if (done.isNotBlank()) ". Hecho: $done" else "") +
                (if (pending.isNotBlank()) ". Pendiente: $pending" else "")
        }.ifBlank { "(sin proyectos asignados)" }
        return "Redacta un informe de avance breve y profesional para enviarle a mi cliente ${client.name}" +
            (if (client.company.isNotBlank()) " de ${client.company}" else "") +
            " por WhatsApp o correo. Usa un tono cordial, sin tecnicismos, en máximo 150 palabras, " +
            "con un saludo, el estado de cada proyecto, los próximos pasos y una despedida. " +
            "No uses formato Markdown. Responde SOLO con el texto del mensaje, listo para copiar y " +
            "enviar, sin introducciones, comentarios ni notas para mí. Proyectos:\n$summary"
    }

    /** Lo que el usuario está usando, para personalizar retos y ruta. */
    fun learningContext(projects: List<Project>, mentorMode: MentorMode, snapshot: RepoSnapshot?, audit: RepoAudit?): String {
        val stack = projects.flatMap { it.stack }.distinct().ifEmpty { listOf("desarrollo de software en general") }
        val languages = snapshot?.languages?.keys?.take(4).orEmpty()
        val gaps = audit?.checks?.filterNot { it.passed }?.map { it.title }.orEmpty()
        return buildString {
            append("Nivel: ${mentorMode.label}. Stack de sus proyectos: ${stack.joinToString()}.")
            if (languages.isNotEmpty()) append(" Lenguajes de su repositorio: ${languages.joinToString()}.")
            if (gaps.isNotEmpty()) append(" Puntos débiles detectados en la auditoría de su repo: ${gaps.joinToString()}.")
        }
    }

    /** Pide un reto con las marcas PISTA: y SOLUCIÓN: para poder separarlo después. */
    fun dailyChallenge(context: String) =
        "$context\nPropón UN reto de programación corto (15-30 minutos) adecuado a su nivel y stack. " +
            "Formato exacto: primera línea el título; luego el enunciado con un ejemplo de entrada y salida; " +
            "luego una línea que diga solo ${DailyChallenge.HINT_MARK} seguida de una pista; " +
            "luego una línea que diga solo ${DailyChallenge.SOLUTION_MARK} seguida de la solución con código."

    /** Pide una ruta de aprendizaje de 4 semanas. */
    fun learningPath(context: String) =
        "$context\nArma una ruta de aprendizaje de 4 semanas para mejorar como desarrollador. " +
            "Para cada semana: un objetivo, 3 temas concretos, una práctica aplicable a su proyecto y un recurso " +
            "gratuito (documentación oficial o curso conocido). Sé breve: usa títulos '## Semana N' y listas con guiones."
}
