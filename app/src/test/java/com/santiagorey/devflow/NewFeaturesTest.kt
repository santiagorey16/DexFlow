package com.santiagorey.devflow

import com.santiagorey.devflow.ai.ChatMessage
import com.santiagorey.devflow.ai.DailyChallenge
import com.santiagorey.devflow.ai.providerErrorReason
import com.santiagorey.devflow.projects.ProjectTask
import com.santiagorey.devflow.projects.TaskStatus
import com.santiagorey.devflow.projects.formatDuration
import com.santiagorey.devflow.projects.taskFromMap
import com.santiagorey.devflow.projects.toMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest {

    @Test
    fun taskTimer_accumulatesAndStopsWhenDone() {
        val start = 1_000_000L
        val running = ProjectTask(title = "Login", phaseIndex = 2).startTimer(start)
        assertEquals(TaskStatus.IN_PROGRESS, running.status)
        assertEquals(30 * 60_000L, running.totalMs(start + 30 * 60_000L))

        val stopped = running.stopTimer(start + 30 * 60_000L)
        assertNull(stopped.timerStartedAt)
        assertEquals(30 * 60_000L, stopped.trackedMs)

        // Terminar una tarea con el cronómetro en marcha lo detiene y guarda el tiempo.
        val done = stopped.startTimer(start + 40 * 60_000L).withStatus(TaskStatus.DONE, start + 50 * 60_000L)
        assertTrue(done.done)
        assertNull(done.timerStartedAt)
        assertEquals(40 * 60_000L, done.trackedMs)
    }

    @Test
    fun formatDuration_readable() {
        assertEquals("0 min", formatDuration(30_000))
        assertEquals("45 min", formatDuration(45 * 60_000L))
        assertEquals("2 h 5 min", formatDuration(125 * 60_000L))
    }

    @Test
    fun taskMapping_roundTripKeepsEverything() {
        val task = ProjectTask(
            title = "Pruebas E2E", phaseIndex = 3, inProgress = true,
            trackedMs = 90_000, timerStartedAt = 123L, issueNumber = 42
        )
        assertEquals(task, taskFromMap(task.toMap()))
        assertNull(taskFromMap(mapOf("phase" to 1))) // sin título no es una tarea válida
    }

    @Test
    fun dailyChallenge_parsesSections() {
        val reply = """
            **Invertir una cadena**
            Escribe una función que invierta un texto. Ejemplo: "hola" -> "aloh".
            PISTA: recorre la cadena desde el final.
            SOLUCIÓN:
            fun invertir(s: String) = s.reversed()
        """.trimIndent()
        val challenge = DailyChallenge.parse(reply)
        assertNotNull(challenge)
        assertEquals("Invertir una cadena", challenge!!.title)
        assertTrue("hola" in challenge.statement)
        assertEquals("recorre la cadena desde el final.", challenge.hint)
        assertTrue("reversed" in challenge.solution)
        assertNull(DailyChallenge.parse("Solo un texto sin marcas"))
    }

    @Test
    fun providerErrorReason_readsMessage() {
        val body = """{"error": {"code": 400, "message": "maxOutputTokens is out of range", "status": "INVALID_ARGUMENT"}}"""
        assertEquals("maxOutputTokens is out of range", providerErrorReason(body))
        assertNull(providerErrorReason("<html>error</html>"))
    }

    @Test
    fun chatMessage_attachmentGoesOnlyToTheAi() {
        val message = ChatMessage(ChatMessage.Role.USER, "Revisa este archivo", attachment = "fun main() {}")
        assertEquals("Revisa este archivo", message.text)
        assertTrue(message.promptText.endsWith("fun main() {}"))
        assertFalse(ChatMessage(ChatMessage.Role.USER, "Hola").promptText.contains("\n"))
    }
}
