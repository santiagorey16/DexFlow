package com.santiagorey.devflow

import com.santiagorey.devflow.ai.AiModel
import com.santiagorey.devflow.ai.AiProvider
import com.santiagorey.devflow.ai.defaultModelFor
import com.santiagorey.devflow.github.GitHubCommit
import com.santiagorey.devflow.github.GitHubIssue
import com.santiagorey.devflow.github.GitHubRepo
import com.santiagorey.devflow.github.RepoAuditor
import com.santiagorey.devflow.github.RepoSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantTest {

    @Test
    fun keyPrefix_hintsWrongProvider() {
        assertTrue(AiProvider.GEMINI.looksLikeValidKey("AIzaSyExample"))
        assertTrue(AiProvider.CLAUDE.looksLikeValidKey("  sk-ant-api03-example "))
        assertFalse(AiProvider.CLAUDE.looksLikeValidKey("AIzaSyExample"))
        assertTrue(AiProvider.OPENAI.looksLikeValidKey("sk-proj-example"))
    }

    @Test
    fun defaultModel_claudePrefersOpus55() {
        val models = listOf(AiModel("claude-haiku-5-5", "Haiku"), AiModel("claude-opus-5-5", "Opus"))
        assertEquals("claude-opus-5-5", defaultModelFor(AiProvider.CLAUDE, models)?.id)
    }

    @Test
    fun defaultModel_geminiPrefersFlashOverLite() {
        val models = listOf(
            AiModel("gemini-3.5-pro", "Pro"),
            AiModel("gemini-3.5-flash-lite", "Lite"),
            AiModel("gemini-3.5-flash", "Flash")
        )
        assertEquals("gemini-3.5-flash", defaultModelFor(AiProvider.GEMINI, models)?.id)
    }

    @Test
    fun defaultModel_geminiSkipsPreviewModels() {
        // Caso real: el preview quedaba como recomendado y no tiene cuota gratis.
        val models = listOf(
            AiModel("gemini-omni-flash-preview-11-2026", "Omni Flash Preview"),
            AiModel("gemini-3.0-flash-preview", "3.0 Flash Preview"),
            AiModel("gemini-2.5-flash", "2.5 Flash"),
            AiModel("gemini-2.0-flash", "2.0 Flash")
        )
        assertEquals("gemini-2.5-flash", defaultModelFor(AiProvider.GEMINI, models)?.id)
    }

    @Test
    fun gemini_codenamesAndAudioModelsAreNotStable() {
        assertTrue(com.santiagorey.devflow.ai.isStableGemini("gemini-3.8-flash"))
        assertTrue(com.santiagorey.devflow.ai.isStableGemini("gemini-3.5-flash-lite"))
        assertFalse(com.santiagorey.devflow.ai.isStableGemini("gemini-4-argon"))
        assertFalse(com.santiagorey.devflow.ai.isStableGemini("gemini-3.0-flash-preview"))
        // Gemini 3+ baja el razonamiento con thinkingLevel; 2.5 Flash con thinkingBudget.
        assertEquals("low", com.santiagorey.devflow.ai.geminiFastThinking("gemini-3.8-flash")?.optString("thinkingLevel"))
    }

    @Test
    fun reviewableFiles_skipBinaries() {
        assertTrue(com.santiagorey.devflow.screens.isReviewableFile("app/src/Main.kt"))
        assertTrue(com.santiagorey.devflow.screens.isReviewableFile("notebooks/CQ7.ipynb"))
        assertTrue(com.santiagorey.devflow.screens.isReviewableFile("Dockerfile"))
        assertFalse(com.santiagorey.devflow.screens.isReviewableFile("Captura de pantalla.png"))
        assertFalse(com.santiagorey.devflow.screens.isReviewableFile("docs/CQ2.docx"))
    }

    @Test
    fun audit_detectsSecretsTestsCiAndCommits() {
        val snapshot = RepoSnapshot(
            repo = GitHubRepo("ana/app", "app", "", "Kotlin", false, "2026-11-10", ""),
            defaultBranch = "main",
            pushedAt = "2026-11-10",
            stars = 0,
            languages = emptyMap(),
            recentCommits = listOf(
                GitHubCommit("feat: login", "Ana", ""),
                GitHubCommit("fix(auth): token", "Ana", ""),
                GitHubCommit("cambios varios", "Ana", "")
            ),
            openIssues = emptyList(),
            files = emptyList(),
            totalFiles = 5,
            readmeExcerpt = null,
            allPaths = listOf(
                ".env",
                ".env.example",
                "app/src/main/java/Main.kt",
                "app/src/main/java/Repo.kt",
                "app/src/test/java/MainTest.kt"
            )
        )

        val audit = RepoAuditor.audit(snapshot)
        val byTitle = audit.checks.associateBy { it.title }

        assertFalse(byTitle.getValue("Sin secretos en el repositorio").passed)
        assertTrue(".env" in byTitle.getValue("Sin secretos en el repositorio").detail)
        assertFalse(".env.example" in byTitle.getValue("Sin secretos en el repositorio").detail)
        assertFalse(byTitle.getValue(".gitignore configurado").passed)
        assertTrue(byTitle.getValue("Pruebas automatizadas").passed)
        assertFalse(byTitle.getValue("Integración continua (CI)").passed)
        assertEquals(66, audit.conventionalCommitPercent)
        assertEquals(1, audit.testFiles)
        assertEquals(0, audit.securityPercent)
        assertTrue(audit.score in 1..99)
    }

    @Test
    fun defaultModel_fallsBackToFirstOrNull() {
        val models = listOf(AiModel("o-something", "O"))
        assertEquals("o-something", defaultModelFor(AiProvider.OPENAI, models)?.id)
        assertNull(defaultModelFor(AiProvider.OPENAI, emptyList()))
    }

    @Test
    fun repoSnapshot_promptContextIncludesKeyFacts() {
        val snapshot = RepoSnapshot(
            repo = GitHubRepo("ana/devflow", "devflow", "App de prueba", "Kotlin", true, "2026-11-10", "https://github.com/ana/devflow"),
            defaultBranch = "main",
            pushedAt = "2026-11-10",
            stars = 0,
            languages = mapOf("Kotlin" to 900L, "HTML" to 100L),
            recentCommits = listOf(GitHubCommit("feat: login con GitHub", "Ana", "2026-11-10T12:00:00Z")),
            openIssues = listOf(
                GitHubIssue(3, "Falla el registro", false, listOf("bug")),
                GitHubIssue(4, "Agregar Firestore", true, emptyList())
            ),
            files = listOf("app/src/main/MainActivity.kt"),
            totalFiles = 1,
            readmeExcerpt = "# DevFlow"
        )

        val text = snapshot.toPromptContext()
        assertTrue("privado" in text)
        assertTrue("Kotlin 90%" in text)
        assertTrue("feat: login con GitHub" in text)
        assertTrue("Issue #3: Falla el registro [bug]" in text)
        assertTrue("PR #4: Agregar Firestore" in text)
        assertEquals(1, snapshot.openIssueCount)
        assertEquals(1, snapshot.openPullRequests)
    }
}
