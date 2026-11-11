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
    val createdAt: Long = System.currentTimeMillis()
) {
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
