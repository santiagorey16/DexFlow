package com.santiagorey.devflow

import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.currentPhaseIndex
import com.santiagorey.devflow.projects.experienceFor
import com.santiagorey.devflow.projects.initialProgressFor
import com.santiagorey.devflow.screens.daysLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DevFlowModelsTest {

    private fun utcMillis(date: LocalDate) =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun phaseIndex_respectsBoundaries() {
        assertEquals(0, currentPhaseIndex(0))
        assertEquals(0, currentPhaseIndex(19))
        assertEquals(1, currentPhaseIndex(20))
        assertEquals(2, currentPhaseIndex(40))
        assertEquals(2, currentPhaseIndex(79))
        assertEquals(3, currentPhaseIndex(80))
        assertEquals(4, currentPhaseIndex(100))
    }

    @Test
    fun initialProgress_dependsOnStartingPoint() {
        assertEquals(0, initialProgressFor(0))
        assertEquals(20, initialProgressFor(1))
        assertEquals(40, initialProgressFor(2))
    }

    @Test
    fun project_isOverdue_onlyWhenPastDueAndNotCompleted() {
        val yesterday = utcMillis(LocalDate.now().minusDays(1))
        val tomorrow = utcMillis(LocalDate.now().plusDays(1))

        assertTrue(Project(name = "A", deliveryDateMillis = yesterday, progress = 50).isOverdue)
        assertFalse(Project(name = "B", deliveryDateMillis = yesterday, progress = 100).isOverdue)
        assertFalse(Project(name = "C", deliveryDateMillis = tomorrow, progress = 10).isOverdue)
        assertFalse(Project(name = "D", deliveryDateMillis = null).isOverdue)
    }

    @Test
    fun project_daysToDelivery() {
        val inThreeDays = utcMillis(LocalDate.now().plusDays(3))
        assertEquals(3L, Project(name = "A", deliveryDateMillis = inThreeDays).daysToDelivery)
    }

    @Test
    fun experience_countsProjectsClientsAndCompleted() {
        val projects = listOf(
            Project(name = "A", progress = 100),
            Project(name = "B", progress = 30)
        )
        val clients = listOf(Client(name = "X"))
        // 2 proyectos (200) + 1 cliente (50) + 1 completado (200)
        assertEquals(450, experienceFor(projects, clients))
    }

    @Test
    fun daysLabel_readableText() {
        assertEquals("sin fecha", daysLabel(null))
        assertEquals("vencido", daysLabel(-2))
        assertEquals("hoy", daysLabel(0))
        assertEquals("mañana", daysLabel(1))
        assertEquals("en 5 días", daysLabel(5))
    }
}
