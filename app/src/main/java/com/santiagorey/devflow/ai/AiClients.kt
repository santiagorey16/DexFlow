package com.santiagorey.devflow.ai

import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Etiqueta para filtrar los mensajes de la IA en Logcat. */
private const val LOG_TAG = "DevFlowAI"

/** Máximo tiempo sin recibir datos en una respuesta con streaming. */
private const val STREAM_TIMEOUT_MS = 20_000

/**
 * Cliente común para las tres IAs: listar modelos (sirve para probar la clave) y conversar.
 * Las respuestas llegan en streaming: [onDelta] recibe cada trozo de texto apenas se genera.
 */
interface AiChatClient {
    /** Modelos de chat disponibles en la cuenta. También sirve para probar si la clave funciona. */
    suspend fun listModels(): List<AiModel>

    /**
     * Envía la conversación y va entregando la respuesta a pedazos en [onDelta].
     * Devuelve el texto completo al terminar.
     */
    suspend fun stream(
        model: String,
        system: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit
    ): String

    /** Respuesta completa, sin ir mostrando el texto (tareas sugeridas, informes…). */
    suspend fun send(model: String, system: String, history: List<ChatMessage>): String =
        stream(model, system, history) {}
}

/**
 * Se reutiliza un cliente por proveedor y clave: así las conexiones HTTPS quedan abiertas
 * entre mensajes y no se repite el saludo TLS en cada pregunta (respuestas más rápidas).
 */
private val clientCache = java.util.concurrent.ConcurrentHashMap<String, AiChatClient>()

/** Devuelve el cliente de la IA elegida (reutiliza el mismo si ya existe con esa clave). */
fun aiClientFor(provider: AiProvider, apiKey: String): AiChatClient =
    clientCache.getOrPut("${provider.name}:$apiKey") {
        when (provider) {
            AiProvider.CLAUDE -> ClaudeChatClient(apiKey)
            AiProvider.GEMINI -> GeminiChatClient(apiKey)
            AiProvider.OPENAI -> OpenAiChatClient(apiKey)
        }
    }

/** Olvida los clientes guardados (al desconectar la IA o cambiar de clave). */
fun clearAiClients() = clientCache.clear()

/** Las conversaciones solo envían los últimos mensajes: menos texto que leer = respuesta más rápida. */
internal const val MAX_HISTORY_MESSAGES = 16

/**
 * Últimos mensajes para la IA, empezando siempre por uno del usuario
 * (varias APIs fallan si la conversación empieza con la IA).
 */
internal fun List<ChatMessage>.recentForAi(): List<ChatMessage> =
    takeLast(MAX_HISTORY_MESSAGES).dropWhile { it.role != ChatMessage.Role.USER }

/** Modelo sugerido por defecto entre los disponibles en la cuenta. */
fun defaultModelFor(provider: AiProvider, models: List<AiModel>): AiModel? {
    val ids = models.map { it.id }
    val preferred = when (provider) {
        AiProvider.CLAUDE -> listOf("claude-opus-5-5")
        // En Gemini: el "flash" estable más nuevo (rápido y con capa gratuita).
        // Las versiones preview/experimentales suelen no tener cuota gratis.
        AiProvider.GEMINI -> ids.filter { isStableGemini(it) && "flash" in it && "lite" !in it }
            .sortedByDescending { geminiVersion(it) }
            .ifEmpty { ids.filter { isStableGemini(it) } }
        AiProvider.OPENAI -> ids.filter { it.startsWith("gpt-") }
    }
    val id = preferred.firstOrNull { it in ids } ?: ids.firstOrNull()
    return models.firstOrNull { it.id == id }
}

/** Palabras que indican un modelo de Gemini en prueba (sin capa gratis estable). */
private val GEMINI_UNSTABLE_MARKERS = listOf("preview", "exp", "omni", "thinking", "latest", "tuning")

/** Modelos Gemini estables (sin preview, experimental, ni variantes especiales). */
internal fun isStableGemini(id: String): Boolean =
    GEMINI_UNSTABLE_MARKERS.none { it in id } && GEMINI_STABLE_NAME.matches(id)

/** Nombre de un modelo estable: "gemini-<versión>-<flash|pro>[-lite]" (los nombres en clave no lo son). */
private val GEMINI_STABLE_NAME = Regex("""^gemini-\d+(\.\d+)?-(flash|pro)(-lite)?(-\d+)?$""")

/**
 * Ajuste de "razonamiento" para que Gemini responda rápido en un chat:
 * - Gemini 2.5 Flash: `thinkingBudget = 0` (sin razonamiento).
 * - Gemini 3 o posterior: `thinkingLevel = "low"` (razonamiento mínimo).
 * Devuelve null para modelos donde no se puede ajustar (p. ej. 2.5 Pro).
 */
internal fun geminiFastThinking(model: String): JSONObject? {
    val version = geminiVersion(model)
    return when {
        version >= 3.0 -> JSONObject().put("thinkingLevel", "low")
        version >= 2.5 && "flash" in model -> JSONObject().put("thinkingBudget", 0)
        else -> null
    }
}

/** Número de versión de un id tipo "gemini-2.5-flash" (0 si no tiene). */
internal fun geminiVersion(id: String): Double =
    Regex("gemini-(\\d+(?:\\.\\d+)?)").find(id)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

/** true si el mensaje lo escribió el usuario. */
private fun ChatMessage.isUser() = role == ChatMessage.Role.USER

// =====================================================
// CLAUDE (SDK oficial de Anthropic)
// =====================================================

/** Cliente de Claude con el SDK oficial de Anthropic. */
private class ClaudeChatClient(apiKey: String) : AiChatClient {

    private val client: AnthropicClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build()

    /** Modelos de Claude de la cuenta (el SDK pagina solo con autoPager). */
    override suspend fun listModels(): List<AiModel> = withContext(Dispatchers.IO) {
        claudeCall { client.models().list().autoPager().map { AiModel(it.id(), it.displayName()) } }
    }

    /**
     * Respuesta de Claude por streaming. Usa esfuerzo bajo (más rápido) y, en los modelos que
     * lo aceptan, respaldos automáticos si un filtro rechaza la petición.
     */
    override suspend fun stream(
        model: String,
        system: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        claudeCall {
            val builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_CLAUDE_TOKENS)
                .system(system)
            history.recentForAi().forEach { if (it.isUser()) builder.addUserMessage(it.promptText) else builder.addAssistantMessage(it.promptText) }
            val modern = supportsEffortAndFallbacks(model)
            // Esfuerzo bajo: para conversar es mucho más rápido y la calidad se mantiene.
            if (modern) builder.outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            val plain = builder.build()

            // Si un filtro de seguridad rechaza la petición, Anthropic la reintenta con otro modelo
            // en vez de devolver una respuesta vacía. Si la cuenta no tiene esa función, se repite sin ella.
            val streamResponse = if (modern) {
                val withFallbacks = plain.toBuilder()
                    .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    .build()
                try {
                    client.messages().createStreaming(withFallbacks)
                } catch (e: BadRequestException) {
                    val text = e.message.orEmpty().lowercase()
                    if ("fallback" !in text && "beta" !in text) throw e
                    Log.w(LOG_TAG, "Claude no aceptó fallbacks; se reintenta sin ellos: ${e.message}")
                    client.messages().createStreaming(plain)
                }
            } else {
                client.messages().createStreaming(plain)
            }

            val full = StringBuilder()
            var refused = false
            streamResponse.use { response ->
                response.stream().forEach { event ->
                    ensureActive()
                    event.contentBlockDelta().ifPresent { delta ->
                        delta.delta().text().ifPresent { piece ->
                            full.append(piece.text())
                            onDelta(piece.text())
                        }
                    }
                    event.messageDelta().ifPresent { md ->
                        if (md.delta().stopReason().orElse(null) == StopReason.REFUSAL) refused = true
                    }
                }
            }
            if (refused && full.isBlank()) throw AiException("Claude no pudo responder a esta petición. Prueba a reformularla.")
            full.toString().ifBlank { throw AiException("Claude no devolvió texto. Inténtalo de nuevo.") }
        }
    }

    /** Modelos actuales que aceptan `effort` y los reintentos automáticos (`fallbacks`). */
    private fun supportsEffortAndFallbacks(model: String) =
        model.startsWith("claude-opus-5") || model.startsWith("claude-sonnet-5-5") || model.startsWith("claude-fable-5")

    /** Traduce los errores del SDK de Anthropic a mensajes en español (clave inválida, sin saldo…). */
    private inline fun <T> claudeCall(block: () -> T): T =
        try {
            block()
        } catch (e: AiException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw AiException("La clave de Claude no es válida. Revísala o crea una nueva.", e)
        } catch (e: PermissionDeniedException) {
            throw AiException("Tu cuenta de Anthropic no tiene permiso para este modelo.", e)
        } catch (e: NotFoundException) {
            throw AiException("Ese modelo de Claude no está disponible en tu cuenta. Elige otro.", e)
        } catch (e: RateLimitException) {
            throw AiException("Claude recibió demasiadas peticiones. Espera un momento.", e)
        } catch (e: AnthropicServiceException) {
            Log.e(LOG_TAG, "Claude respondió HTTP ${e.statusCode()}: ${e.message}", e)
            val detail = e.message.orEmpty()
            val message = if (e.statusCode() == 402 || "credit balance" in detail.lowercase()) {
                // Las cuentas nuevas sin saldo reciben un 400 con este texto.
                "Tu cuenta de Anthropic no tiene saldo. Recárgala en console.anthropic.com → Billing."
            } else {
                "Claude respondió con un error (${e.statusCode()})."
            }
            throw AiException("$message\nDetalle: ${detail.take(220)}", e)
        } catch (e: AnthropicIoException) {
            Log.e(LOG_TAG, "Sin conexión con Claude", e)
            throw AiException("Sin conexión con Claude. Revisa tu internet.", e)
        }

    private companion object {
        const val MAX_CLAUDE_TOKENS = 16000L
    }
}

// =====================================================
// GEMINI (API REST de Google AI Studio)
// =====================================================

/** Cliente de Gemini por REST (API de Google AI Studio). */
private class GeminiChatClient(private val apiKey: String) : AiChatClient {

    // La clave va en un encabezado, nunca en la URL.
    private val headers = mapOf("x-goog-api-key" to apiKey)

    /** Modelos de la cuenta (se guardan para elegir respaldos sin volver a pedirlos). */
    @Volatile private var cachedModels: List<AiModel>? = null

    /** Respaldo que respondió hace poco (modelo, válido hasta). */
    @Volatile private var workingBackup: Pair<String, Long>? = null

    /** Modelos de Gemini que sirven para conversar, ordenados: estables y nuevos primero. */
    override suspend fun listModels(): List<AiModel> = withContext(Dispatchers.IO) {
        val json = providerCall("Gemini") { HttpJson.request("$BASE/models?pageSize=200", headers = headers) }
        val models = json.optJSONArray("models") ?: JSONArray()
        (0 until models.length())
            .mapNotNull { models.optJSONObject(it) }
            .filter { model ->
                val methods = model.optJSONArray("supportedGenerationMethods")
                methods != null && (0 until methods.length()).any { methods.optString(it) == "generateContent" }
            }
            .map { model ->
                val id = model.optString("name").removePrefix("models/")
                AiModel(id, model.optString("displayName", id))
            }
            .filter { model -> model.id.startsWith("gemini") && SPECIAL_PURPOSE.none { it in model.id } }
            // Primero los estables y más nuevos; las versiones preview al final.
            .sortedWith(
                compareByDescending<AiModel> { isStableGemini(it.id) }
                    .thenByDescending { geminiVersion(it.id) }
                    .thenBy { it.id }
            )
            .also { cachedModels = it }
    }

    /**
     * Respuesta de Gemini por streaming. Si el modelo está saturado (503) y aún no llegó texto,
     * prueba con otros modelos Flash de la cuenta y recuerda 10 minutos el que funcionó.
     */
    override suspend fun stream(
        model: String,
        system: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", JSONArray().apply {
                history.recentForAi().forEach { message ->
                    put(
                        JSONObject()
                            .put("role", if (message.isUser()) "user" else "model")
                            .put("parts", JSONArray().put(JSONObject().put("text", message.promptText)))
                    )
                }
            })
        // Sin "maxOutputTokens": cada modelo tiene su propio límite (varios Flash aceptan como
        // máximo 8192) y pedir más hace que Gemini rechace la petición.

        // Si el modelo elegido está saturado (503 o sin respuesta), se usa un modelo de respaldo de la
        // propia cuenta. Solo se cambia de modelo si todavía no llegó ningún texto.
        // Si hace poco un respaldo respondió porque el elegido estaba saturado, se empieza por él.
        val sticky = workingBackup?.takeIf { it.second > System.currentTimeMillis() }?.first
        val candidates = (listOfNotNull(sticky) + model + backupModels(model)).distinct()
        var firstError: AiException? = null
        for ((attempt, candidate) in candidates.withIndex()) {
            try {
                val text = streamOnce(candidate, body, onDelta)
                workingBackup = if (candidate != model) candidate to System.currentTimeMillis() + STICKY_BACKUP_MS else null
                if (candidate != model) {
                    val note = "\n\n_(Respondió $candidate porque $model estaba saturado en ese momento.)_"
                    onDelta(note)
                    return@withContext text + note
                }
                return@withContext text
            } catch (e: GeminiOverloaded) {
                if (firstError == null) firstError = e.error
                Log.w(LOG_TAG, "Gemini $candidate saturado (intento ${attempt + 1}); se prueba otra opción")
            } catch (e: AiException) {
                // Un respaldo que no está disponible en la cuenta: se prueba el siguiente.
                if (candidate == model) throw e
                Log.w(LOG_TAG, "Respaldo $candidate no disponible: ${e.message}")
            }
        }
        throw firstError ?: AiException("Gemini no está disponible ahora. Inténtalo más tarde.")
    }

    /**
     * Respaldos: hasta 2 Flash estables de la cuenta (del más nuevo al más antiguo) y un Flash-Lite.
     * Se eligen de la lista real porque Google retira modelos viejos para cuentas nuevas.
     */
    private suspend fun backupModels(current: String): List<String> {
        val models = cachedModels ?: runCatching { listModels() }.getOrNull().orEmpty()
        val flash = models.map { it.id }.filter { it != current && isStableGemini(it) && "flash" in it }
        // 2 Flash completos y, al final, un Flash-Lite (suele estar menos congestionado).
        return (flash.filterNot { "lite" in it }.take(2) + flash.filter { "lite" in it }.take(1)).distinct()
    }

    /** Señal interna: el modelo respondió 503/saturado y se puede probar otra opción. */
    private class GeminiOverloaded(val error: AiException) : Exception(error)

    /** Una petición con streaming a un modelo concreto. */
    private suspend fun streamOnce(model: String, body: JSONObject, onDelta: (String) -> Unit): String = withContext(Dispatchers.IO) {
        // Los Gemini "piensan" antes de responder y eso tarda; para conversar se reduce al mínimo
        // (2.5 usa "thinkingBudget", 3+ usa "thinkingLevel"). Si el modelo no lo acepta, se repite sin el ajuste.
        val fastBody = geminiFastThinking(model)?.let {
            JSONObject(body.toString()).put("generationConfig", JSONObject().put("thinkingConfig", it))
        }

        val full = StringBuilder()
        var blockReason: String? = null
        val started = System.currentTimeMillis()
        var firstTextAt = 0L
        var usage: JSONObject? = null
        try {
            providerCall("Gemini") {
            HttpJson.streamEventsWithFallback(this, "$BASE/models/$model:streamGenerateContent?alt=sse", headers, fastBody, body, "thinking") { data ->
                val json = JSONObject(data)
                json.optJSONObject("usageMetadata")?.let { usage = it }
                json.optJSONObject("promptFeedback")?.optString("blockReason")?.takeIf { it.isNotBlank() }?.let { blockReason = it }
                val parts = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    ?: return@streamEventsWithFallback
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.optBoolean("thought")) continue // razonamiento interno: no se muestra
                    val text = part.optString("text")
                    if (text.isNotEmpty()) {
                        if (firstTextAt == 0L) firstTextAt = System.currentTimeMillis()
                        full.append(text)
                        onDelta(text)
                    }
                }
            }
            }
        } catch (e: AiException) {
            // Saturado (503) o sin respuesta a tiempo, y sin texto todavía: se prueba otro modelo.
            val saturated = (e.cause as? HttpStatusException)?.code == 503 || e.cause is java.net.SocketTimeoutException
            if (saturated && full.isEmpty()) throw GeminiOverloaded(e)
            throw e
        }
        // Tiempos para diagnosticar respuestas lentas (Logcat, etiqueta DevFlowAI).
        Log.i(
            LOG_TAG,
            "Gemini $model: primer texto ${if (firstTextAt > 0) firstTextAt - started else -1} ms, " +
                "total ${System.currentTimeMillis() - started} ms, entrada=${usage?.optInt("promptTokenCount")} " +
                "razonamiento=${usage?.optInt("thoughtsTokenCount")} salida=${usage?.optInt("candidatesTokenCount")}"
        )
        full.toString().ifBlank {
            throw AiException(
                if (blockReason != null) "Gemini bloqueó la pregunta ($blockReason). Reformúlala."
                else "Gemini no devolvió texto. Inténtalo de nuevo o cambia de modelo."
            )
        }
    }

    private companion object {
        const val BASE = "https://generativelanguage.googleapis.com/v1beta"

        /** Modelos que no sirven para conversar (voz, imágenes, robótica, etc.). */
        /** Tiempo que se recuerda el respaldo que funcionó. */
        const val STICKY_BACKUP_MS = 10 * 60 * 1000L

        val SPECIAL_PURPOSE = listOf("tts", "image", "audio", "live", "embedding", "robotics", "computer-use", "veo", "imagen", "transcribe")

    }
}

// =====================================================
// CHATGPT (API REST de OpenAI)
// =====================================================

/** Cliente de ChatGPT por REST (API de OpenAI). */
private class OpenAiChatClient(apiKey: String) : AiChatClient {

    private val headers = mapOf("Authorization" to "Bearer $apiKey")

    /** Modelos GPT y de razonamiento (o1, o3…) que funcionan con /chat/completions. */
    override suspend fun listModels(): List<AiModel> = withContext(Dispatchers.IO) {
        val json = providerCall("ChatGPT") { HttpJson.request("$BASE/models", headers = headers) }
        val data = json.optJSONArray("data") ?: JSONArray()
        (0 until data.length())
            .mapNotNull { data.optJSONObject(it)?.optString("id") }
            .filter { id -> (id.startsWith("gpt-") || REASONING_MODEL.matches(id)) && EXCLUDED.none { it in id } }
            .sortedDescending()
            .map { AiModel(it, it) }
    }

    /**
     * Respuesta de ChatGPT por streaming. A los modelos de razonamiento les pide pensar poco
     * (reasoning_effort = low) para responder antes.
     */
    override suspend fun stream(
        model: String,
        system: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                history.recentForAi().forEach { message ->
                    put(JSONObject().put("role", if (message.isUser()) "user" else "assistant").put("content", message.promptText))
                }
            })

        // Los modelos de razonamiento (gpt-5, o3…) piensan menos con "low": responden mucho antes.
        val fastBody = if (model.startsWith("gpt-5") || REASONING_MODEL.matches(model)) {
            JSONObject(body.toString()).put("reasoning_effort", "low")
        } else {
            null
        }

        val full = StringBuilder()
        providerCall("ChatGPT") {
            HttpJson.streamEventsWithFallback(this, "$BASE/chat/completions", headers, fastBody, body, "reasoning") { data ->
                val text = JSONObject(data).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("delta")?.optString("content").orEmpty()
                if (text.isNotEmpty() && text != "null") {
                    full.append(text)
                    onDelta(text)
                }
            }
        }
        full.toString().ifBlank { throw AiException("ChatGPT no devolvió texto. Inténtalo de nuevo.") }
    }

    private companion object {
        const val BASE = "https://api.openai.com/v1"
        val REASONING_MODEL = Regex("^o\\d.*")

        // Modelos que no aceptan /chat/completions (voz, imágenes, búsqueda y los que solo
        // funcionan con la API "Responses", como los codex, pro y deep-research).
        val EXCLUDED = listOf(
            "audio", "realtime", "image", "tts", "transcribe", "embedding", "search", "moderation",
            "instruct", "codex", "-pro", "deep-research", "computer-use"
        )
    }
}

// =====================================================
// HTTP + errores comunes
// =====================================================

/** Respuesta HTTP con error (4xx o 5xx). Guarda el código y el cuerpo para explicar qué pasó. */
internal class HttpStatusException(val code: Int, val body: String) : IOException("HTTP $code")

/** Pequeño cliente HTTP con JSON y streaming SSE (sin dependencias extra). */
internal object HttpJson {

    /** Petición que devuelve un objeto JSON. */
    fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: JSONObject? = null
    ): JSONObject = JSONObject(requestText(url, method, headers, body?.toString()).ifBlank { "{}" })

    /** Petición que devuelve una lista JSON (p. ej. los repos de GitHub). */
    fun requestArray(url: String, headers: Map<String, String> = emptyMap()): JSONArray =
        JSONArray(requestText(url, "GET", headers, null).ifBlank { "[]" })

    /** Petición que devuelve el texto tal cual (archivos de un repo, diffs). */
    fun requestText(url: String, method: String, headers: Map<String, String>, body: String?): String {
        val connection = open(url, method, headers, body)
        try {
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Lee una respuesta "Server-Sent Events" línea por línea y entrega el contenido de cada
     * `data:` a [onData]. Se detiene si se cancela la corrutina (p. ej. "Nuevo chat").
     */
    fun streamEvents(
        scope: CoroutineScope,
        url: String,
        headers: Map<String, String>,
        body: JSONObject,
        onData: (String) -> Unit
    ) {
        val started = System.currentTimeMillis()
        // En streaming los datos llegan seguido: si en 20 s no llega nada, el servidor está atascado.
        val connection = open(url, "POST", headers + ("Accept" to "text/event-stream"), body.toString(), readTimeoutMs = STREAM_TIMEOUT_MS)
        Log.i(LOG_TAG, "Respuesta del servidor en ${System.currentTimeMillis() - started} ms (${url.substringBefore('?').substringAfterLast('/')})")
        try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    scope.ensureActive()
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty() || data == "[DONE]") continue
                    onData(data)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Intenta primero con [fastBody] (ajustes para responder más rápido). Si el modelo rechaza
     * ese ajuste (400 que menciona [rejectedHint]), repite con [body] normal.
     */
    fun streamEventsWithFallback(
        scope: CoroutineScope,
        url: String,
        headers: Map<String, String>,
        fastBody: JSONObject?,
        body: JSONObject,
        rejectedHint: String,
        onData: (String) -> Unit
    ) {
        if (fastBody == null) return streamEvents(scope, url, headers, body, onData)
        try {
            streamEvents(scope, url, headers, fastBody, onData)
        } catch (e: HttpStatusException) {
            if (e.code != 400 || rejectedHint !in e.body.lowercase()) throw e
            Log.w(LOG_TAG, "El modelo no aceptó el ajuste rápido ($rejectedHint); se repite sin él")
            streamEvents(scope, url, headers, body, onData)
        }
    }

    /** Abre la conexión y lanza [HttpStatusException] con el cuerpo del error si no es 2xx. */
    private fun open(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        readTimeoutMs: Int = 120_000
    ): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = readTimeoutMs
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            connection.disconnect()
            throw HttpStatusException(code, error)
        }
        return connection
    }
}

/**
 * Traduce errores HTTP de Gemini y ChatGPT a mensajes en español según el código
 * (400 clave inválida, 429 límite de uso, 503 saturado…).
 */
private inline fun <T> providerCall(name: String, block: () -> T): T =
    try {
        block()
    } catch (e: HttpStatusException) {
        Log.e(LOG_TAG, "$name respondió HTTP ${e.code}: ${e.body.take(2000)}")
        val lower = e.body.lowercase()
        val message = when (e.code) {
            400 -> if ("api key" in lower || "api_key" in lower) {
                "La clave de $name no es válida. Revísala o crea una nueva."
            } else {
                "$name rechazó la petición (400). Prueba con otro modelo."
            }
            401, 403 -> "La clave de $name no es válida o no tiene permisos."
            402 -> "Tu cuenta de $name no tiene saldo."
            404 -> "Ese modelo de $name no está disponible. Elige otro."
            429 -> if ("limit: 0" in lower || "free_tier" in lower || "billing" in lower || "quota" in lower) {
                "Este modelo no está incluido en tu plan de $name o se acabó la cuota. Toca \"Cambiar modelo\" y elige uno estable (por ejemplo un \"Flash\")."
            } else {
                "Llegaste al límite de uso de $name por ahora. Espera un minuto o cambia de modelo."
            }
            503 -> "$name está saturado en este momento (mucha demanda). Inténtalo en un minuto o toca \"Cambiar modelo\" y elige otro."
            in 500..599 -> "$name tiene problemas en este momento. Inténtalo más tarde."
            else -> "$name respondió con un error (${e.code})."
        }
        // Se agrega el motivo que da el proveedor: así el usuario (y el soporte) sabe qué pasó.
        val reason = providerErrorReason(e.body)
        throw AiException(if (reason != null) "$message\nDetalle: $reason" else message, e)
    } catch (e: java.net.SocketTimeoutException) {
        Log.w(LOG_TAG, "$name no respondió a tiempo", e)
        throw AiException("$name tardó demasiado en responder (puede estar saturado). Inténtalo de nuevo o cambia de modelo.", e)
    } catch (e: IOException) {
        Log.e(LOG_TAG, "Sin conexión con $name", e)
        throw AiException("Sin conexión con $name. Revisa tu internet.", e)
    }

/** Extrae el mensaje de error que devuelven Gemini y OpenAI ({"error": {"message": ...}}). */
internal fun providerErrorReason(body: String): String? =
    runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }
        .getOrNull()
        ?.lineSequence()?.firstOrNull()
        ?.take(220)
        ?.takeIf { it.isNotBlank() }
