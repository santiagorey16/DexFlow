package com.santiagorey.devflow.projects

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/**
 * Proyecto creado por el usuario desde el asistente "Nuevo Proyecto".
 *
 * @param deliveryDateMillis fecha de entrega elegida en el calendario
 *        (milisegundos UTC a medianoche, tal como los entrega el DatePicker).
 */
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val clientId: String? = null,
    val startingPoint: Int = 0,
    val stack: List<String> = emptyList(),
    val deliveryDateMillis: Long? = null,
    val budget: String = "",
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** Repositorio de GitHub vinculado ("usuario/repo"), para que la IA revise el avance. */
    val repoFullName: String? = null,
    /** Tareas del proyecto; si hay tareas, el avance se calcula con ellas. */
    val tasks: List<ProjectTask> = emptyList()
) {
    /** Tiempo total registrado en las tareas del proyecto. */
    fun trackedMs(now: Long = System.currentTimeMillis()): Long = tasks.sumOf { it.totalMs(now) }

    /** Avance según las tareas completadas (null si el proyecto no tiene tareas). */
    val progressFromTasks: Int?
        get() = if (tasks.isEmpty()) null else tasks.count { it.done } * 100 / tasks.size

    val phase: ProjectPhase
        get() = projectPhases[currentPhaseIndex(progress)]

    val isCompleted: Boolean
        get() = progress >= 100

    val deliveryDate: LocalDate?
        get() = deliveryDateMillis?.let { millisToLocalDate(it) }

    /** Días que faltan para la entrega (negativo si ya pasó). */
    val daysToDelivery: Long?
        get() = deliveryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    val isOverdue: Boolean
        get() = !isCompleted && (daysToDelivery ?: 0) < 0
}

/**
 * Tarea dentro de una fase del proyecto.
 * @param phaseIndex 0 Planificación, 1 Diseño, 2 Desarrollo, 3 QA & pruebas.
 */
data class ProjectTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val phaseIndex: Int,
    val done: Boolean = false,
    /** En curso (columna del medio del Kanban). */
    val inProgress: Boolean = false,
    /** Tiempo registrado (ms), sin contar el cronómetro en marcha. */
    val trackedMs: Long = 0L,
    /** Momento en que se inició el cronómetro (null si está detenido). */
    val timerStartedAt: Long? = null,
    /** Issue de GitHub vinculado (se cierra la tarea cuando se cierra el issue). */
    val issueNumber: Int? = null
) {
    val status: TaskStatus
        get() = when {
            done -> TaskStatus.DONE
            inProgress || timerStartedAt != null -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.TODO
        }

    /** Tiempo total, incluido el cronómetro si está en marcha. */
    fun totalMs(now: Long = System.currentTimeMillis()): Long =
        trackedMs + (timerStartedAt?.let { (now - it).coerceAtLeast(0) } ?: 0L)

    fun withStatus(newStatus: TaskStatus, now: Long = System.currentTimeMillis()): ProjectTask {
        // Al terminar una tarea se detiene su cronómetro.
        val stopped = if (newStatus == TaskStatus.DONE) stopTimer(now) else this
        return stopped.copy(done = newStatus == TaskStatus.DONE, inProgress = newStatus == TaskStatus.IN_PROGRESS)
    }

    fun startTimer(now: Long = System.currentTimeMillis()): ProjectTask =
        if (timerStartedAt != null) this else copy(timerStartedAt = now, inProgress = true, done = false)

    fun stopTimer(now: Long = System.currentTimeMillis()): ProjectTask =
        if (timerStartedAt == null) this else copy(trackedMs = totalMs(now), timerStartedAt = null)
}

enum class TaskStatus(val label: String) { TODO("Por hacer"), IN_PROGRESS("En curso"), DONE("Hecho") }

/** "1 h 25 min", "12 min" o "0 min". */
fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    return when {
        hours > 0 -> "$hours h ${minutes % 60} min"
        else -> "$minutes min"
    }
}

/** Fases en las que se pueden crear tareas (todas menos "Completado"). */
val taskPhases: List<ProjectPhase>
    get() = projectPhases.take(4)

data class Client(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ActivityEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActivityType { PROJECT, CLIENT, PROGRESS, COMPLETED }

data class ProjectPhase(
    val name: String,
    val description: String,
    val from: Int,
    val to: Int
)

val projectPhases = listOf(
    ProjectPhase("Planificación", "Levantamiento de requisitos y alcance", 0, 20),
    ProjectPhase("Diseño", "Arquitectura, UI/UX y prototipos", 20, 40),
    ProjectPhase("Desarrollo", "Implementación de funcionalidades", 40, 80),
    ProjectPhase("QA & pruebas", "Pruebas, corrección de errores y validación", 80, 100),
    ProjectPhase("Completado", "Entregado y en mantenimiento", 100, 100)
)

fun currentPhaseIndex(progress: Int): Int =
    when {
        progress >= 100 -> 4
        progress >= 80 -> 3
        progress >= 40 -> 2
        progress >= 20 -> 1
        else -> 0
    }

/** Progreso inicial según el punto de partida elegido en el asistente. */
fun initialProgressFor(startingPoint: Int): Int =
    when (startingPoint) {
        1 -> 20 // Diseño
        2 -> 40 // Código
        else -> 0 // Análisis
    }

private val spanishLocale = Locale.forLanguageTag("es-ES")

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", spanishLocale)

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun formatDeliveryDate(millis: Long?): String =
    millis?.let { millisToLocalDate(it).format(shortDateFormatter) } ?: "Sin fecha"

fun formatCreatedDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
        .toLocalDate().format(shortDateFormatter)

fun formatTodayLong(): String {
    val text = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanishLocale))
    return text.replaceFirstChar { it.titlecase(spanishLocale) }
}

fun formatRelativeTime(timestamp: Long): String {
    val diffMinutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        diffMinutes < 1 -> "Justo ahora"
        diffMinutes < 60 -> "Hace $diffMinutes min"
        diffMinutes < 60 * 24 -> "Hace ${diffMinutes / 60} h"
        diffMinutes < 60 * 24 * 2 -> "Ayer"
        else -> "Hace ${diffMinutes / (60 * 24)} días"
    }
}
