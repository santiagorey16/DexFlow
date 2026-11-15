package com.santiagorey.devflow.github

import com.santiagorey.devflow.ai.AiException
import com.santiagorey.devflow.ai.HttpJson
import com.santiagorey.devflow.ai.HttpStatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class GitHubRepo(
    val fullName: String,
    val name: String,
    val description: String,
    val language: String?,
    val isPrivate: Boolean,
    val updatedAt: String,
    val htmlUrl: String
)

/** Última ejecución de CI (GitHub Actions) de un repositorio. */
data class CiRun(
    val repoFullName: String,
    val workflowName: String,
    val status: String,
    val conclusion: String?,
    val branch: String,
    val url: String
) {
    val failed: Boolean get() = conclusion in setOf("failure", "timed_out", "startup_failure")
}

data class GitHubCommit(val message: String, val author: String, val date: String)

data class GitHubIssue(val number: Int, val title: String, val isPullRequest: Boolean, val labels: List<String>)

/** Foto del estado de un repositorio, lista para mostrarla o pasársela a la IA. */
data class RepoSnapshot(
    val repo: GitHubRepo,
    val defaultBranch: String,
    val pushedAt: String,
    val stars: Int,
    val languages: Map<String, Long>,
    val recentCommits: List<GitHubCommit>,
    val openIssues: List<GitHubIssue>,
    val files: List<String>,
    val totalFiles: Int,
    /** Todas las rutas del repo (para la auditoría; `files` es la versión recortada para la IA). */
    val allPaths: List<String> = files,
    val readmeExcerpt: String?
) {
    val openPullRequests get() = openIssues.count { it.isPullRequest }
    val openIssueCount get() = openIssues.count { !it.isPullRequest }

    /** Texto con el contexto del repo para la IA. */
    fun toPromptContext(): String = buildString {
        appendLine("Repositorio: ${repo.fullName} (${if (repo.isPrivate) "privado" else "público"})")
        if (repo.description.isNotBlank()) appendLine("Descripción: ${repo.description}")
        appendLine("Rama principal: $defaultBranch · Último push: $pushedAt")
        if (languages.isNotEmpty()) {
            val total = languages.values.sum().coerceAtLeast(1)
            appendLine("Lenguajes: " + languages.entries.joinToString { "${it.key} ${it.value * 100 / total}%" })
        }
        appendLine()
        appendLine("Últimos commits (más reciente primero):")
        if (recentCommits.isEmpty()) appendLine("- (sin commits)")
        recentCommits.forEach { appendLine("- ${it.date.take(10)} · ${it.author}: ${it.message}") }
        appendLine()
        appendLine("Issues y PRs abiertos:")
        if (openIssues.isEmpty()) appendLine("- (ninguno)")
        openIssues.forEach {
            val kind = if (it.isPullRequest) "PR" else "Issue"
            val labels = if (it.labels.isNotEmpty()) " [${it.labels.joinToString()}]" else ""
            appendLine("- $kind #${it.number}: ${it.title}$labels")
        }
        appendLine()
        appendLine("Estructura de archivos ($totalFiles en total, se muestran ${files.size}):")
        files.forEach { appendLine("  $it") }
        if (readmeExcerpt != null) {
            appendLine()
            appendLine("README (inicio):")
            appendLine(readmeExcerpt)
        }
    }
}

/** Cliente mínimo de la API REST de GitHub usando el token del usuario. */
class GitHubApi(private val token: String) {

    suspend fun listRepos(): List<GitHubRepo> = withContext(Dispatchers.IO) {
        val array = call {
            HttpJson.requestArray(
                "$BASE/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member",
                headers()
            )
        }
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toRepo() }
    }

    suspend fun snapshot(fullName: String): RepoSnapshot = withContext(Dispatchers.IO) {
        val repoJson = call { HttpJson.request("$BASE/repos/$fullName", headers = headers()) }
        val repo = repoJson.toRepo()
        val branch = repoJson.optString("default_branch", "main")

        coroutineScope {
            val languages = async { optionalCall { languages(fullName) } ?: emptyMap() }
            val commits = async { optionalCall { commits(fullName) } ?: emptyList() }
            val issues = async { optionalCall { openIssues(fullName) } ?: emptyList() }
            val tree = async { optionalCall { tree(fullName, branch) } ?: emptyList<String>() }
            val readme = async { optionalCall { readme(fullName) } }

            RepoSnapshot(
                repo = repo,
                defaultBranch = branch,
                pushedAt = repoJson.optString("pushed_at").take(10),
                stars = repoJson.optInt("stargazers_count"),
                languages = languages.await(),
                recentCommits = commits.await(),
                openIssues = issues.await(),
                files = tree.await().take(MAX_FILES),
                totalFiles = tree.await().size,
                allPaths = tree.await(),
                readmeExcerpt = readme.await()
            )
        }
    }

    /** Contenido de un archivo del repo (rama principal). */
    suspend fun fileContent(fullName: String, path: String): String = withContext(Dispatchers.IO) {
        val encodedPath = path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        call {
            HttpJson.requestText(
                "$BASE/repos/$fullName/contents/$encodedPath", "GET",
                headers() + ("Accept" to "application/vnd.github.raw+json"), null
            )
        }
    }

    /** Cambios (diff) de un Pull Request. */
    suspend fun pullRequestDiff(fullName: String, number: Int): String = withContext(Dispatchers.IO) {
        call {
            HttpJson.requestText(
                "$BASE/repos/$fullName/pulls/$number", "GET",
                headers() + ("Accept" to "application/vnd.github.diff"), null
            )
        }
    }

    /** Crea un issue (desde una tarea) y devuelve su número. */
    suspend fun createIssue(fullName: String, title: String, body: String): Int = withContext(Dispatchers.IO) {
        val json = call {
            HttpJson.request(
                "$BASE/repos/$fullName/issues", "POST", headers(),
                JSONObject().put("title", title).put("body", body)
            )
        }
        json.optInt("number")
    }

    /** Estado de varios issues: número → cerrado. */
    suspend fun issueStates(fullName: String, numbers: Collection<Int>): Map<Int, Boolean> = withContext(Dispatchers.IO) {
        coroutineScope {
            numbers.map { number ->
                async {
                    optionalCall {
                        val json = HttpJson.request("$BASE/repos/$fullName/issues/$number", headers = headers())
                        number to (json.optString("state") == "closed")
                    }
                }
            }.mapNotNull { it.await() }.toMap()
        }
    }

    /** Última ejecución de GitHub Actions del repo (null si no usa Actions). */
    suspend fun latestWorkflowRun(fullName: String): CiRun? = withContext(Dispatchers.IO) {
        val json = call { HttpJson.request("$BASE/repos/$fullName/actions/runs?per_page=1", headers = headers()) }
        val run = json.optJSONArray("workflow_runs")?.optJSONObject(0) ?: return@withContext null
        CiRun(
            repoFullName = fullName,
            workflowName = run.optString("name"),
            status = run.optString("status"),
            conclusion = run.optString("conclusion").takeUnless { it.isBlank() || it == "null" },
            branch = run.optString("head_branch"),
            url = run.optString("html_url")
        )
    }

    private fun languages(fullName: String): Map<String, Long> {
        val json = HttpJson.request("$BASE/repos/$fullName/languages", headers = headers())
        return json.keys().asSequence().associateWith { json.optLong(it) }
            .entries.sortedByDescending { it.value }.associate { it.key to it.value }
    }

    private fun commits(fullName: String): List<GitHubCommit> {
        val array = HttpJson.requestArray("$BASE/repos/$fullName/commits?per_page=$MAX_COMMITS", headers())
        return (0 until array.length()).mapNotNull { i ->
            val commit = array.optJSONObject(i)?.optJSONObject("commit") ?: return@mapNotNull null
            val author = commit.optJSONObject("author")
            GitHubCommit(
                message = commit.optString("message").lineSequence().first().take(140),
                author = author?.optString("name").orEmpty(),
                date = author?.optString("date").orEmpty()
            )
        }
    }

    private fun openIssues(fullName: String): List<GitHubIssue> {
        val array = HttpJson.requestArray("$BASE/repos/$fullName/issues?state=open&per_page=$MAX_ISSUES", headers())
        return (0 until array.length()).mapNotNull { i ->
            val issue = array.optJSONObject(i) ?: return@mapNotNull null
            val labels = issue.optJSONArray("labels")
            GitHubIssue(
                number = issue.optInt("number"),
                title = issue.optString("title"),
                isPullRequest = issue.has("pull_request"),
                labels = labels?.let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") } } ?: emptyList()
            )
        }
    }

    private fun tree(fullName: String, branch: String): List<String> {
        val ref = URLEncoder.encode(branch, "UTF-8")
        val json = HttpJson.request("$BASE/repos/$fullName/git/trees/$ref?recursive=1", headers = headers())
        val items = json.optJSONArray("tree") ?: return emptyList()
        val paths = (0 until items.length())
            .mapNotNull { items.optJSONObject(it) }
            .filter { it.optString("type") == "blob" }
            .map { it.optString("path") }
            .filterNot { path -> IGNORED_DIRS.any { path.startsWith(it) || "/$it" in path } }
        return paths
    }

    private fun readme(fullName: String): String? {
        val headers = headers() + ("Accept" to "application/vnd.github.raw+json")
        val text = HttpJson.requestText("$BASE/repos/$fullName/readme", "GET", headers, null)
        return text.take(MAX_README_CHARS).takeIf { it.isNotBlank() }
    }

    private fun headers() = mapOf(
        "Authorization" to "Bearer $token",
        "Accept" to "application/vnd.github+json",
        "X-GitHub-Api-Version" to "2022-11-28",
        "User-Agent" to "DevFlow-Android"
    )

    private fun JSONObject.toRepo() = GitHubRepo(
        fullName = optString("full_name"),
        name = optString("name"),
        description = optString("description").takeUnless { it == "null" }.orEmpty(),
        language = optString("language").takeUnless { it.isBlank() || it == "null" },
        isPrivate = optBoolean("private"),
        updatedAt = optString("updated_at").take(10),
        htmlUrl = optString("html_url")
    )

    private inline fun <T> call(block: () -> T): T =
        try {
            block()
        } catch (e: HttpStatusException) {
            throw AiException(
                when (e.code) {
                    401 -> "El acceso a GitHub expiró. Vuelve a conectar tus repositorios."
                    403 -> if ("rate limit" in e.body.lowercase()) {
                        "Llegaste al límite de consultas de GitHub. Espera unos minutos."
                    } else {
                        "GitHub no permite leer este repositorio con tus permisos."
                    }
                    404 -> "No se encontró el repositorio (¿lo renombraron o es de otra cuenta?)."
                    else -> "GitHub respondió con un error (${e.code})."
                },
                e
            )
        } catch (e: IOException) {
            throw AiException("Sin conexión con GitHub. Revisa tu internet.", e)
        }

    /** Partes secundarias del repo: si fallan (p. ej. repo vacío), se omiten. */
    private inline fun <T> optionalCall(block: () -> T): T? = runCatching(block).getOrNull()

    private companion object {
        const val BASE = "https://api.github.com"
        const val MAX_COMMITS = 15
        const val MAX_ISSUES = 15
        // Menos rutas en el contexto = la IA responde antes (la auditoría usa la lista completa).
        const val MAX_FILES = 120
        const val MAX_README_CHARS = 2000
        val IGNORED_DIRS = listOf("node_modules/", "build/", ".gradle/", ".idea/", "dist/", "vendor/", ".git/")
    }
}
