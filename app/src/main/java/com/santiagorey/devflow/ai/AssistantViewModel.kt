package com.santiagorey.devflow.ai

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.santiagorey.devflow.github.CiRun
import com.santiagorey.devflow.github.GitHubApi
import com.santiagorey.devflow.github.GitHubRepo
import com.santiagorey.devflow.github.GitHubTokenStore
import com.santiagorey.devflow.github.RepoAudit
import com.santiagorey.devflow.github.RepoAuditor
import com.santiagorey.devflow.github.RepoSnapshot
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.ProjectTask
import com.santiagorey.devflow.projects.projectPhases
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Asistente de IA de DevFlow ("trae tu propia IA").
 *
 * Cada usuario conecta su proveedor (Gemini, Claude o ChatGPT) con su propia clave, que se
 * guarda cifrada en el teléfono. Las peticiones van directo del teléfono al proveedor: DevFlow
 * no tiene servidor propio ni ve las claves.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SecureStore(application)
    private val gitHubTokens = GitHubTokenStore(application)

    /** Cuenta activa (uid de Firebase): cada cuenta tiene su propia IA conectada. */
    private var accountKey: String? = null
    private var firebaseUid: String? = null

    var provider by mutableStateOf<AiProvider?>(null)
        private set
    var modelId by mutableStateOf<String?>(null)
        private set
    var isConfigured by mutableStateOf(false)
        private set
    var mentorMode by mutableStateOf(MentorMode.fromName(store.getPlain(KEY_MENTOR)))
        private set

    var setup by mutableStateOf(AiSetupState())
        private set

    // Chat
    val messages = mutableStateListOf<ChatMessage>()
    var isSending by mutableStateOf(false)
        private set
    var chatError by mutableStateOf<String?>(null)
        private set
    private var sendJob: Job? = null

    // Contexto del proyecto y su repositorio
    var activeProjectId by mutableStateOf<String?>(null)
        private set
    var snapshot by mutableStateOf<RepoSnapshot?>(null)
        private set
    var isLoadingRepo by mutableStateOf(false)
        private set
    var repoError by mutableStateOf<String?>(null)
        private set

    // Lista de repos para vincular
    var repos by mutableStateOf<List<GitHubRepo>>(emptyList())
        private set
    var isLoadingRepos by mutableStateOf(false)
        private set
    var reposError by mutableStateOf<String?>(null)
        private set

    // =====================================================
    // CUENTA
    // =====================================================

    /**
     * @param key uid de la cuenta (null sin sesión).
     * @param uid uid de Firebase (para el token de GitHub).
     */
    fun setAccount(key: String?, uid: String?) {
        firebaseUid = uid
        if (key == accountKey) return
        accountKey = key
        provider = key?.let { AiProvider.fromName(store.getPlain(providerKey(it))) }
        modelId = key?.let { store.getPlain(modelKey(it)) }
        isConfigured = provider != null && modelId != null && currentApiKey() != null
        resetChat()
        activeProjectId = null
        snapshot = null
        repos = emptyList()
        audit = null
        auditRepoName = null
        auditError = null
    }

    val hasGitHubAccess: Boolean
        get() = firebaseUid?.let { gitHubTokens.get(it) } != null

    // =====================================================
    // TUTORIAL: CONECTAR TU IA
    // =====================================================

    fun startSetup() {
        setup = AiSetupState()
    }

    /** Prueba la clave pidiendo la lista de modelos; si funciona, la guarda cifrada. */
    fun testKey(provider: AiProvider, apiKey: String) {
        val account = accountKey ?: return
        val key = apiKey.trim()
        if (key.isEmpty()) {
            setup = setup.copy(error = "Pega tu clave para continuar.")
            return
        }
        setup = setup.copy(isTesting = true, error = null)
        viewModelScope.launch {
            try {
                val models = aiClientFor(provider, key).listModels()
                if (models.isEmpty()) throw AiException("Tu cuenta de ${provider.displayName} no tiene modelos de chat disponibles.")
                store.putString(apiKeyKey(account, provider), key)
                setup = AiSetupState(
                    models = models,
                    selectedModelId = defaultModelFor(provider, models)?.id,
                    testedProvider = provider
                )
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                setup = setup.copy(isTesting = false, error = e.message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                setup = setup.copy(isTesting = false, error = "No se pudo probar la clave: ${e.localizedMessage}")
            }
        }
    }

    fun selectModel(id: String) {
        setup = setup.copy(selectedModelId = id)
    }

    fun finishSetup() {
        val account = accountKey ?: return
        val tested = setup.testedProvider ?: return
        val model = setup.selectedModelId ?: return
        store.putPlain(providerKey(account), tested.name)
        store.putPlain(modelKey(account), model)
        provider = tested
        modelId = model
        isConfigured = true
        resetChat()
    }

    /** Borra la clave de la IA conectada en este teléfono. */
    fun disconnectAi() {
        val account = accountKey ?: return
        clearAiClients()
        AiProvider.entries.forEach { store.remove(apiKeyKey(account, it)) }
        store.putPlain(providerKey(account), null)
        store.putPlain(modelKey(account), null)
        provider = null
        modelId = null
        isConfigured = false
        resetChat()
    }

    // =====================================================
    // CAMBIAR DE MODELO (sin repetir el tutorial)
    // =====================================================

    var availableModels by mutableStateOf<List<AiModel>>(emptyList())
        private set
    var isLoadingModels by mutableStateOf(false)
        private set
    var modelsError by mutableStateOf<String?>(null)
        private set

    fun loadModels() {
        val currentProvider = provider ?: return
        val apiKey = currentApiKey() ?: return
        isLoadingModels = true
        modelsError = null
        viewModelScope.launch {
            try {
                availableModels = aiClientFor(currentProvider, apiKey).listModels()
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                modelsError = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                modelsError = "No se pudieron cargar los modelos."
            } finally {
                isLoadingModels = false
            }
        }
    }

    fun changeModel(id: String) {
        val account = accountKey ?: return
        store.putPlain(modelKey(account), id)
        modelId = id
        chatError = null
    }

    fun updateMentorMode(mode: MentorMode) {
        mentorMode = mode
        store.putPlain(KEY_MENTOR, mode.name)
    }

    // =====================================================
    // CONTEXTO: PROYECTO Y REPOSITORIO
    // =====================================================

    /** Cambia el proyecto del que se habla y carga su historial guardado. */
    fun selectProject(project: Project?) {
        if (project?.id == activeProjectId && historyLoaded) {
            // Mismo proyecto: solo recarga el repo si se vinculó, cambió o recién hay acceso a GitHub.
            if (project != null && project.repoFullName != loadedRepoName) loadSnapshot(project.repoFullName)
            return
        }
        activeProjectId = project?.id
        stopAndClearChat()
        loadHistory()
        loadSnapshot(project?.repoFullName)
    }

    fun refreshSnapshot(project: Project?) = loadSnapshot(project?.repoFullName)

    /** Repo que se está leyendo o ya se leyó (evita lecturas duplicadas). */
    private var loadedRepoName: String? = null

    private fun loadSnapshot(fullName: String?) {
        snapshot = null
        repoError = null
        loadedRepoName = null
        if (fullName == null) return
        val token = gitHubToken() ?: run {
            repoError = "Conecta tus repositorios de GitHub para que la IA pueda revisarlo."
            return
        }
        loadedRepoName = fullName
        isLoadingRepo = true
        viewModelScope.launch {
            try {
                snapshot = GitHubApi(token).snapshot(fullName)
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                repoError = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                repoError = "No se pudo leer el repositorio."
            } finally {
                isLoadingRepo = false
            }
        }
    }

    fun loadRepos() {
        val token = gitHubToken() ?: run {
            reposError = "Primero conecta tus repositorios de GitHub."
            return
        }
        isLoadingRepos = true
        reposError = null
        viewModelScope.launch {
            try {
                repos = GitHubApi(token).listRepos()
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                reposError = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                reposError = "No se pudieron cargar tus repositorios."
            } finally {
                isLoadingRepos = false
            }
        }
    }

    // =====================================================
    // AUDITORÍA DE REPOSITORIOS (pantalla Guías)
    // =====================================================

    var audit by mutableStateOf<RepoAudit?>(null)
        private set
    var auditRepoName by mutableStateOf<String?>(null)
        private set
    var isAuditing by mutableStateOf(false)
        private set
    var auditError by mutableStateOf<String?>(null)
        private set

    fun auditRepo(fullName: String, force: Boolean = false) {
        if (!force && fullName == auditRepoName && (audit != null || isAuditing)) return
        val token = gitHubToken() ?: run {
            auditError = "Primero da acceso a tus repositorios de GitHub."
            return
        }
        auditRepoName = fullName
        audit = null
        auditError = null
        isAuditing = true
        viewModelScope.launch {
            try {
                val result = RepoAuditor.audit(GitHubApi(token).snapshot(fullName))
                if (auditRepoName == fullName) audit = result
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                auditError = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                auditError = "No se pudo auditar el repositorio."
            } finally {
                isAuditing = false
            }
        }
    }

    // =====================================================
    // ESTADO DE CI (alertas de la campana)
    // =====================================================

    var ciRuns by mutableStateOf<Map<String, CiRun>>(emptyMap())
        private set
    private var ciCheckedAt = 0L
    private var ciCheckedRepos = emptySet<String>()

    /** Consulta GitHub Actions de los repos vinculados (como mucho cada 10 minutos). */
    fun refreshCiStatuses(repos: Set<String>) {
        val token = gitHubToken() ?: return
        val now = System.currentTimeMillis()
        if (repos.isEmpty()) {
            ciRuns = emptyMap()
            return
        }
        if (repos == ciCheckedRepos && now - ciCheckedAt < CI_REFRESH_MS) return
        ciCheckedRepos = repos
        ciCheckedAt = now
        viewModelScope.launch {
            val api = GitHubApi(token)
            val results = repos.mapNotNull { repo -> runCatching { api.latestWorkflowRun(repo) }.getOrNull() }
            ciRuns = results.associateBy { it.repoFullName }
        }
    }

    // =====================================================
    // TAREAS ↔ ISSUES DE GITHUB
    // =====================================================

    var isSyncingIssues by mutableStateOf(false)
        private set

    /** Crea un issue en el repo del proyecto a partir de una tarea. */
    fun createIssueForTask(project: Project, task: ProjectTask, onCreated: (Int) -> Unit) {
        val repo = project.repoFullName ?: return
        val token = gitHubToken() ?: return
        viewModelScope.launch {
            try {
                val number = GitHubApi(token).createIssue(
                    repo,
                    title = task.title,
                    body = "Tarea de la fase **${projectPhases[task.phaseIndex].name}** del proyecto \"${project.name}\".\n\n_Creado desde DevFlow._"
                )
                onCreated(number)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo crear el issue", e)
                taskSuggestionError = (e as? AiException)?.message ?: "No se pudo crear el issue en GitHub."
            }
        }
    }

    /** Consulta en GitHub qué issues vinculados se cerraron. */
    fun syncIssues(project: Project, onResult: (Map<Int, Boolean>) -> Unit) {
        val repo = project.repoFullName ?: return
        val token = gitHubToken() ?: return
        val numbers = project.tasks.mapNotNull { it.issueNumber }
        if (numbers.isEmpty() || isSyncingIssues) return
        isSyncingIssues = true
        viewModelScope.launch {
            try {
                onResult(GitHubApi(token).issueStates(repo, numbers))
            } finally {
                isSyncingIssues = false
            }
        }
    }

    val canUseGitHub: Boolean get() = gitHubToken() != null

    private fun gitHubToken(): String? = firebaseUid?.let { gitHubTokens.get(it) }

    // =====================================================
    // CHAT
    // =====================================================

    private val chatHistory = ChatHistoryStore(application)
    private var historyLoaded = false
    private var historyJob: Job? = null

    private val chatId: String get() = activeProjectId ?: ChatHistoryStore.GENERAL_CHAT

    /**
     * Envía una pregunta y muestra la respuesta mientras la IA la escribe (streaming).
     *
     * @param attachment contenido extra que se le pasa a la IA pero no se muestra completo en
     *        el chat (un archivo, un diff o un error largo).
     */
    fun send(text: String, project: Project?, client: Client?, attachment: String? = null) {
        val prompt = text.trim()
        if (prompt.isEmpty() || isSending) return
        val currentProvider = provider ?: return
        val model = modelId ?: return
        val apiKey = currentApiKey() ?: run {
            chatError = "No encontramos tu clave. Vuelve a conectar tu IA."
            return
        }

        messages.add(ChatMessage(ChatMessage.Role.USER, prompt, attachment = attachment))
        chatError = null
        isSending = true
        val system = buildSystemPrompt(project, client)
        val history = messages.toList()

        // Mensaje de la IA que se va llenando a medida que llega el texto.
        val reply = ChatMessage(ChatMessage.Role.ASSISTANT, "")
        messages.add(reply)
        val replyIndex = messages.lastIndex
        val received = StringBuilder()

        sendJob = viewModelScope.launch {
            try {
                var lastUiUpdate = 0L
                aiClientFor(currentProvider, apiKey).stream(model, system, history) { piece ->
                    received.append(piece)
                    // La pantalla se actualiza como mucho cada 60 ms (no por cada trozo): el chat
                    // se ve fluido sin redibujar cientos de veces. El texto llega en un hilo de red,
                    // así que se publica en el hilo principal.
                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdate >= UI_UPDATE_MS) {
                        lastUiUpdate = now
                        val snapshotText = received.toString()
                        viewModelScope.launch(Dispatchers.Main) {
                            if (replyIndex < messages.size) messages[replyIndex] = reply.copy(text = snapshotText)
                        }
                    }
                }
                if (replyIndex < messages.size) messages[replyIndex] = reply.copy(text = received.toString())
                saveHistory()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is AiException) Log.w(TAG, "Error de IA: ${e.message}", e) else Log.e(TAG, "Error inesperado en el chat", e)
                if (received.isBlank()) {
                    // Sin respuesta: se quita el mensaje vacío para poder reintentar.
                    if (replyIndex < messages.size) messages.removeAt(replyIndex)
                } else {
                    messages[replyIndex] = reply.copy(text = "$received\n\n_(respuesta incompleta)_")
                }
                chatError = (e as? AiException)?.message ?: "Algo salió mal: ${e.localizedMessage}"
            } finally {
                isSending = false
            }
        }
    }

    // ── Herramientas: revisar archivo / PR y explicar errores ──
    var isPreparingReview by mutableStateOf(false)
        private set

    /** Lee un archivo del repo y pide a la IA una revisión de código. */
    fun reviewFile(path: String, project: Project?, client: Client?) {
        val repo = snapshot?.repo?.fullName ?: return
        val token = gitHubToken() ?: return
        prepareAndSend(project, client) {
            val content = GitHubApi(token).fileContent(repo, path)
            val (body, note) = AssistantPrompts.limitAttachment(content)
            ReviewRequest(
                text = AssistantPrompts.reviewFile(path, note),
                attachment = "Archivo `$path`:\n```\n$body\n```"
            )
        }
    }

    /** Lee los cambios de un Pull Request y pide a la IA una revisión como lo haría un senior. */
    fun reviewPullRequest(number: Int, title: String, project: Project?, client: Client?) {
        val repo = snapshot?.repo?.fullName ?: return
        val token = gitHubToken() ?: return
        prepareAndSend(project, client) {
            val diff = GitHubApi(token).pullRequestDiff(repo, number)
            val (body, note) = AssistantPrompts.limitAttachment(diff)
            ReviewRequest(
                text = AssistantPrompts.reviewPullRequest(number, title, note),
                attachment = "Diff del PR #$number:\n```diff\n$body\n```"
            )
        }
    }

    /** Explica un error o stacktrace pegado por el usuario. */
    fun explainError(errorText: String, project: Project?, client: Client?) {
        val clean = errorText.trim()
        if (clean.isEmpty()) return
        val (body, note) = AssistantPrompts.limitAttachment(clean)
        send(
            AssistantPrompts.explainError(note),
            project, client,
            attachment = "Error:\n```\n$body\n```"
        )
    }

    private class ReviewRequest(val text: String, val attachment: String)

    private fun prepareAndSend(project: Project?, client: Client?, build: suspend () -> ReviewRequest) {
        if (isPreparingReview || isSending) return
        isPreparingReview = true
        chatError = null
        viewModelScope.launch {
            try {
                val request = build()
                send(request.text, project, client, request.attachment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo preparar la revisión", e)
                chatError = (e as? AiException)?.message ?: "No se pudo leer el contenido de GitHub."
            } finally {
                isPreparingReview = false
            }
        }
    }

    /** Pide a la IA que repita su última respuesta de forma más sencilla. */
    fun explainSimpler(project: Project?, client: Client?) =
        send(AssistantPrompts.EXPLAIN_SIMPLER, project, client)

    /** Reintenta la última pregunta que falló. */
    fun retry(project: Project?, client: Client?) {
        val last = messages.lastOrNull() ?: return
        if (last.role != ChatMessage.Role.USER) return
        messages.removeAt(messages.lastIndex)
        send(last.text, project, client, last.attachment)
    }

    /** Detiene la respuesta en curso (el texto recibido hasta ahora se conserva). */
    fun stopGenerating() {
        sendJob?.cancel()
        isSending = false
        saveHistory()
    }

    /** "Nuevo chat": borra la conversación de este proyecto (también la guardada). */
    fun newChat() {
        stopAndClearChat()
        val account = accountKey ?: return
        val id = chatId
        viewModelScope.launch { chatHistory.clear(account, firebaseUid, id) }
    }

    fun resetChat() = stopAndClearChat()

    /** Carga el historial guardado al abrir el asistente (si aún no se cargó). */
    fun ensureHistoryLoaded() {
        if (!historyLoaded && historyJob?.isActive != true) loadHistory()
    }

    private fun stopAndClearChat() {
        sendJob?.cancel()
        historyJob?.cancel()
        isSending = false
        messages.clear()
        chatError = null
        historyLoaded = false
    }

    private fun loadHistory() {
        val account = accountKey ?: return
        val id = chatId
        historyJob = viewModelScope.launch {
            val saved = chatHistory.load(account, firebaseUid, id)
            if (id == chatId && messages.isEmpty()) messages.addAll(saved)
            historyLoaded = true
        }
    }

    private fun saveHistory() {
        val account = accountKey ?: return
        val id = chatId
        val toSave = messages.filter { it.text.isNotBlank() }
        viewModelScope.launch { chatHistory.save(account, firebaseUid, id, toSave) }
    }

    private fun buildSystemPrompt(project: Project?, client: Client?): String =
        AssistantPrompts.system(mentorMode, project, client, snapshot)

    // =====================================================
    // IA DE UNA SOLA RESPUESTA (tareas, coach, informe)
    // =====================================================

    /** Pide una respuesta suelta (sin tocar el chat). */
    private suspend fun askOnce(prompt: String, project: Project?, client: Client?): String {
        val currentProvider = provider ?: throw AiException("Conecta tu IA en Ajustes para usar esta función.")
        val model = modelId ?: throw AiException("Elige un modelo en Ajustes.")
        val apiKey = currentApiKey() ?: throw AiException("No encontramos tu clave. Vuelve a conectar tu IA.")
        val system = buildSystemPrompt(project, client)
        return aiClientFor(currentProvider, apiKey).send(model, system, listOf(ChatMessage(ChatMessage.Role.USER, prompt)))
    }

    // ── Sugerir tareas ─────────────────────────────────────
    var taskSuggestions by mutableStateOf<List<ProjectTask>?>(null)
        private set
    var isSuggestingTasks by mutableStateOf(false)
        private set
    var taskSuggestionError by mutableStateOf<String?>(null)
        private set

    fun suggestTasks(project: Project, client: Client?) {
        if (isSuggestingTasks) return
        isSuggestingTasks = true
        taskSuggestionError = null
        taskSuggestions = null
        viewModelScope.launch {
            try {
                val reply = askOnce(AssistantPrompts.suggestTasks(project), project, client)
                val tasks = parseTaskSuggestions(reply)
                if (tasks.isEmpty()) throw AiException("La IA no devolvió tareas válidas. Inténtalo de nuevo.")
                taskSuggestions = tasks
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                taskSuggestionError = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                taskSuggestionError = "No se pudieron generar tareas."
            } finally {
                isSuggestingTasks = false
            }
        }
    }

    fun clearTaskSuggestions() {
        taskSuggestions = null
        taskSuggestionError = null
    }

    // ── Coach del día ──────────────────────────────────────
    var coachTip by mutableStateOf<String?>(null)
        private set
    var isLoadingCoach by mutableStateOf(false)
        private set

    /** Un consejo por proyecto y por día (se guarda para no gastar consultas). */
    fun loadCoachTip(project: Project?, client: Client?) {
        val account = accountKey ?: return
        if (!isConfigured || project == null) {
            coachTip = null
            return
        }
        val cacheKey = "coach_${account}_${project.id}_${java.time.LocalDate.now()}"
        store.getPlain(cacheKey)?.let {
            coachTip = it
            return
        }
        if (isLoadingCoach) return
        isLoadingCoach = true
        viewModelScope.launch {
            try {
                val tip = askOnce(AssistantPrompts.COACH_TIP, project, client).trim()
                store.putPlain(cacheKey, tip)
                coachTip = tip
            } catch (_: Exception) {
                coachTip = null
            } finally {
                isLoadingCoach = false
            }
        }
    }

    // ── Informe para el cliente ────────────────────────────
    var clientReport by mutableStateOf<String?>(null)
        private set
    var isGeneratingReport by mutableStateOf(false)
        private set
    var clientReportError by mutableStateOf<String?>(null)
        private set

    fun generateClientReport(client: Client, projects: List<Project>) {
        if (isGeneratingReport) return
        isGeneratingReport = true
        clientReportError = null
        clientReport = null
        viewModelScope.launch {
            try {
                clientReport = askOnce(AssistantPrompts.clientReport(client, projects), null, null).trim()
            } catch (e: AiException) {
                Log.w(TAG, "Error de IA/GitHub: ${e.message}", e)
                clientReportError = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el asistente", e)
                clientReportError = "No se pudo generar el informe."
            } finally {
                isGeneratingReport = false
            }
        }
    }

    fun clearClientReport() {
        clientReport = null
        clientReportError = null
    }

    // ── Aprendizaje: reto del día y ruta personalizada ─────
    var dailyChallenge by mutableStateOf<DailyChallenge?>(null)
        private set
    var learningPath by mutableStateOf<String?>(null)
        private set
    var isLoadingChallenge by mutableStateOf(false)
        private set
    var isLoadingPath by mutableStateOf(false)
        private set
    var learningError by mutableStateOf<String?>(null)
        private set

    /** Lo que el usuario está usando, para personalizar retos y ruta. */
    private fun learningContext(projects: List<Project>): String =
        AssistantPrompts.learningContext(projects, mentorMode, snapshot, audit)

    /** Un reto de programación por día (se guarda para no gastar consultas). */
    fun loadDailyChallenge(projects: List<Project>, force: Boolean = false) {
        val account = accountKey ?: return
        if (!isConfigured || isLoadingChallenge) return
        val cacheKey = "challenge_${account}_${java.time.LocalDate.now()}"
        if (!force) store.getPlain(cacheKey)?.let { cached ->
            dailyChallenge = DailyChallenge.parse(cached)
            if (dailyChallenge != null) return
        }
        isLoadingChallenge = true
        learningError = null
        viewModelScope.launch {
            try {
                val reply = askOnce(AssistantPrompts.dailyChallenge(learningContext(projects)), null, null)
                val parsed = DailyChallenge.parse(reply) ?: throw AiException("La IA no devolvió un reto válido. Inténtalo de nuevo.")
                store.putPlain(cacheKey, reply)
                dailyChallenge = parsed
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo generar el reto", e)
                learningError = (e as? AiException)?.message ?: "No se pudo generar el reto."
            } finally {
                isLoadingChallenge = false
            }
        }
    }

    /** Ruta de aprendizaje de 4 semanas según el stack y los puntos débiles (se guarda por semana). */
    fun loadLearningPath(projects: List<Project>, force: Boolean = false) {
        val account = accountKey ?: return
        if (!isConfigured || isLoadingPath) return
        val week = java.time.LocalDate.now().let { "${it.year}-${it.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)}" }
        val cacheKey = "path_${account}_$week"
        if (!force) store.getPlain(cacheKey)?.let {
            learningPath = it
            return
        }
        isLoadingPath = true
        learningError = null
        viewModelScope.launch {
            try {
                val reply = askOnce(AssistantPrompts.learningPath(learningContext(projects)), null, null).trim()
                store.putPlain(cacheKey, reply)
                learningPath = reply
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo generar la ruta", e)
                learningError = (e as? AiException)?.message ?: "No se pudo generar la ruta de aprendizaje."
            } finally {
                isLoadingPath = false
            }
        }
    }

    // =====================================================
    // CLAVES
    // =====================================================

    private fun currentApiKey(): String? {
        val account = accountKey ?: return null
        val p = provider ?: return null
        return store.getString(apiKeyKey(account, p))
    }

    private fun providerKey(account: String) = "ai_provider_$account"
    private fun modelKey(account: String) = "ai_model_$account"
    private fun apiKeyKey(account: String, provider: AiProvider) = "ai_key_${provider.name}_$account"

    private companion object {
        const val TAG = "DevFlowAI"
        const val KEY_MENTOR = "ai_mentor_mode"
        const val CI_REFRESH_MS = 10 * 60 * 1000L
        const val UI_UPDATE_MS = 60L
    }
}
