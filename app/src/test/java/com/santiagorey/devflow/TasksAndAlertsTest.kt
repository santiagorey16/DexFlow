package com.santiagorey.devflow

import com.santiagorey.devflow.ai.parseTaskSuggestions
import com.santiagorey.devflow.github.CiRun
import com.santiagorey.devflow.notifications.DevFlowAlert
import com.santiagorey.devflow.notifications.buildAlerts
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.ProjectTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TasksAndAlertsTest {

    private fun utcMillis(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun progressFromTasks_countsDone() {
        val project = Project(
            name = "A",
            tasks = listOf(
                ProjectTask(title = "1", phaseIndex = 0, done = true),
                ProjectTask(title = "2", phaseIndex = 1, done = true),
                ProjectTask(title = "3", phaseIndex = 2),
                ProjectTask(title = "4", phaseIndex = 3)
            )
        )
        assertEquals(50, project.progressFromTasks)
        assertNull(Project(name = "B").progressFromTasks)
    }

    @Test
    fun parseTaskSuggestions_toleratesCodeFencesAndBadPhases() {
        val reply = """
            Claro, aquí tienes:
            ```json
            [{"title": "Definir requisitos", "phase": 0}, {"title": "Pruebas E2E", "phase": 9}, {"title": "  "}]
            ```
        """.trimIndent()
        val tasks = parseTaskSuggestions(reply)
        assertEquals(2, tasks.size)
        assertEquals("Definir requisitos", tasks[0].title)
        assertEquals(3, tasks[1].phaseIndex) // fase fuera de rango → se ajusta a QA
        assertTrue(parseTaskSuggestions("sin json").isEmpty())
    }

    @Test
    fun alerts_overdueSoonAndCi() {
        val overdue = Project(id = "p1", name = "Vencido", deliveryDateMillis = utcMillis(LocalDate.now().minusDays(3)))
        val soon = Project(id = "p2", name = "Pronto", deliveryDateMillis = utcMillis(LocalDate.now().plusDays(1)), repoFullName = "ana/app")
        val done = Project(id = "p3", name = "Listo", deliveryDateMillis = utcMillis(LocalDate.now().minusDays(1)), progress = 100)
        val ci = CiRun("ana/app", "Android CI", "completed", "failure", "main", "https://github.com/ana/app/actions/runs/1")

        val withQuality = buildAlerts(listOf(overdue, soon, done), listOf(ci), audit = null, qualityAlertsEnabled = true)
        assertEquals(3, withQuality.size)
        assertEquals(DevFlowAlert.Severity.HIGH, withQuality.first().severity)
        assertTrue(withQuality.any { it.id == "ci_ana/app" && it.projectId == "p2" })
        assertTrue(withQuality.none { it.projectId == "p3" })

        val withoutQuality = buildAlerts(listOf(overdue, soon), listOf(ci), audit = null, qualityAlertsEnabled = false)
        assertEquals(2, withoutQuality.size)
    }
}
