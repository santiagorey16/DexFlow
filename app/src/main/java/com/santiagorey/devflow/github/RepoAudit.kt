package com.santiagorey.devflow.github

/** Un punto de la auditoría "Apego a Normas DevFlow". */
data class AuditCheck(
    val guideCode: String,
    val title: String,
    val passed: Boolean,
    val detail: String,
    val weight: Int,
    /** Cumplimiento de 0 a 1 (permite puntaje parcial). */
    val fulfillment: Double = if (passed) 1.0 else 0.0
)

/** Resultado de auditar un repo: los puntos revisados y métricas extra. */
data class RepoAudit(
    val repoFullName: String,
    val checks: List<AuditCheck>,
    val testFiles: Int,
    val conventionalCommitPercent: Int?,
    val securityPercent: Int
) {
    /** Puntaje total de 0 a 100. */
    val score: Int
        get() {
            val total = checks.sumOf { it.weight }.coerceAtLeast(1)
            return (checks.sumOf { it.weight * it.fulfillment } * 100 / total).toInt()
        }

    /** Resumen para pedirle a la IA cómo mejorar. */
    fun toPromptText(): String = buildString {
        appendLine("Auditoría DevFlow de $repoFullName: $score/100")
        checks.forEach { appendLine("- [${if (it.passed) "OK" else "FALTA"}] ${it.title}: ${it.detail}") }
    }
}

/**
 * Evalúa un repositorio contra las guías de DevFlow usando solo su estructura y sus commits
 * (sin descargar el código). Es una revisión rápida, no reemplaza un análisis de seguridad real.
 */
object RepoAuditor {

    private val conventionalCommit =
        Regex("^(feat|fix|chore|docs|style|refactor|perf|test|build|ci|revert)(\\([^)]+\\))?!?: .+")

    private val secretFileNames = setOf(
        ".env", "id_rsa", "id_dsa", "id_ed25519", "credentials.json", "local.properties",
        ".npmrc", ".pypirc", "secrets.json", "secrets.yml", "secrets.yaml"
    )
    private val secretExtensions = listOf(".pem", ".p12", ".pfx", ".jks", ".keystore", ".key")
    private val safeEnvSuffixes = listOf(".example", ".sample", ".template", ".dist")

    private val testMarkers = listOf("/test/", "/tests/", "/androidtest/", "/__tests__/", "/spec/")
    private val testFileRegex = Regex(".*([._-](test|spec)|^test_.*|Test|Tests)\\.[a-zA-Z0-9]+$")
    private val codeExtensions = setOf(
        "kt", "java", "js", "jsx", "ts", "tsx", "py", "go", "rb", "php", "cs", "swift", "dart", "rs", "c", "cpp", "vue"
    )

    /** Revisa secretos, .gitignore, tests, CI, Conventional Commits y README del repo. */
    fun audit(snapshot: RepoSnapshot): RepoAudit {
        val paths = snapshot.allPaths
        val names = paths.map { it.substringAfterLast('/') }

        // Seguridad: archivos con secretos que no deberían estar en el repositorio
        val secretFiles = paths.filter { path ->
            val name = path.substringAfterLast('/').lowercase()
            val isEnv = name == ".env" || (name.startsWith(".env.") && safeEnvSuffixes.none { name.endsWith(it) })
            isEnv || name in secretFileNames || secretExtensions.any { name.endsWith(it) } ||
                (name.startsWith("service-account") && name.endsWith(".json"))
        }
        val hasGitignore = ".gitignore" in names

        // Pruebas
        val codeFiles = paths.filter { it.substringAfterLast('.', "").lowercase() in codeExtensions }
        val testFiles = codeFiles.filter { path ->
            val lower = "/" + path.lowercase()
            testMarkers.any { it in lower } || testFileRegex.matches(path.substringAfterLast('/'))
        }
        val testRatio = if (codeFiles.isEmpty()) 0.0 else testFiles.size.toDouble() / codeFiles.size

        // CI/CD
        val ciFiles = paths.filter {
            it.startsWith(".github/workflows/") || it == ".gitlab-ci.yml" || it == "Jenkinsfile" ||
                it.startsWith(".circleci/") || it == "azure-pipelines.yml" || it == "bitbucket-pipelines.yml"
        }

        // Commits
        val commits = snapshot.recentCommits
        val conventional = commits.count { conventionalCommit.matches(it.message) }
        val conventionalPercent = if (commits.isEmpty()) null else conventional * 100 / commits.size

        // Documentación
        val hasReadme = snapshot.readmeExcerpt != null || names.any { it.lowercase().startsWith("readme") }

        val checks = listOf(
            AuditCheck(
                guideCode = "SEC-STD-002",
                title = "Sin secretos en el repositorio",
                passed = secretFiles.isEmpty(),
                detail = if (secretFiles.isEmpty()) {
                    "No se encontraron archivos .env, llaves ni credenciales."
                } else {
                    "Posibles secretos subidos: " + secretFiles.take(3).joinToString() +
                        (if (secretFiles.size > 3) " y ${secretFiles.size - 3} más" else "") +
                        ". Bórralos del historial y cambia esas claves."
                },
                weight = 25
            ),
            AuditCheck(
                guideCode = "SEC-STD-002",
                title = ".gitignore configurado",
                passed = hasGitignore,
                detail = if (hasGitignore) "El repositorio ignora archivos locales." else "Agrega un .gitignore para no subir builds, claves ni archivos del IDE.",
                weight = 10
            ),
            AuditCheck(
                guideCode = "QA-STD-003",
                title = "Pruebas automatizadas",
                passed = testRatio >= 0.15,
                detail = when {
                    codeFiles.isEmpty() -> "No se detectó código fuente todavía."
                    testFiles.isEmpty() -> "No se encontraron archivos de pruebas. Empieza con pruebas unitarias de la lógica principal."
                    else -> "${testFiles.size} archivos de prueba para ${codeFiles.size} de código (${(testRatio * 100).toInt()}%)."
                },
                weight = 20,
                fulfillment = (testRatio / 0.15).coerceIn(0.0, 1.0)
            ),
            AuditCheck(
                guideCode = "DPS-STD-004",
                title = "Integración continua (CI)",
                passed = ciFiles.isNotEmpty(),
                detail = if (ciFiles.isNotEmpty()) "Pipeline encontrado: ${ciFiles.first()}" else "Agrega un workflow de GitHub Actions que compile y ejecute las pruebas en cada push.",
                weight = 15
            ),
            AuditCheck(
                guideCode = "DPS-STD-004",
                title = "Conventional Commits",
                passed = (conventionalPercent ?: 0) >= 70,
                detail = when (conventionalPercent) {
                    null -> "El repositorio aún no tiene commits."
                    else -> "$conventional de ${commits.size} commits recientes siguen el formato (feat:, fix:, chore:…)."
                },
                weight = 20,
                fulfillment = (conventionalPercent ?: 0) / 100.0
            ),
            AuditCheck(
                guideCode = "DEV-STD-001",
                title = "README con documentación",
                passed = hasReadme,
                detail = if (hasReadme) "El proyecto tiene README." else "Agrega un README con cómo instalar, ejecutar y probar el proyecto.",
                weight = 10
            )
        )

        val securityChecks = checks.filter { it.guideCode == "SEC-STD-002" }
        val securityPercent = (securityChecks.sumOf { it.weight * it.fulfillment } * 100 /
            securityChecks.sumOf { it.weight }.coerceAtLeast(1)).toInt()

        return RepoAudit(
            repoFullName = snapshot.repo.fullName,
            checks = checks,
            testFiles = testFiles.size,
            conventionalCommitPercent = conventionalPercent,
            securityPercent = securityPercent
        )
    }
}
