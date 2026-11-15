package com.santiagorey.devflow.ai

/**
 * IAs que el usuario puede conectar con su propia clave ("trae tu propia IA").
 * Incluye el texto del tutorial paso a paso para conseguir la clave de cada una.
 */
enum class AiProvider(
    val displayName: String,
    val company: String,
    val tagline: String,
    val keyUrl: String,
    val keyPrefix: String,
    val isFreeTier: Boolean,
    val steps: List<String>
) {
    GEMINI(
        displayName = "Gemini",
        company = "Google",
        tagline = "Tiene capa gratuita: ideal para empezar sin pagar.",
        keyUrl = "https://aistudio.google.com/apikey",
        keyPrefix = "AIza",
        isFreeTier = true,
        steps = listOf(
            "Abre Google AI Studio con el botón de abajo e inicia sesión con tu cuenta de Google.",
            "Toca \"Create API key\" (Crear clave de API) y elige o crea un proyecto.",
            "Copia la clave que aparece (empieza por \"AIza\").",
            "Vuelve a DevFlow y pégala en el siguiente paso."
        )
    ),
    CLAUDE(
        displayName = "Claude",
        company = "Anthropic",
        tagline = "Muy bueno revisando y explicando código. Se paga por uso.",
        keyUrl = "https://console.anthropic.com/settings/keys",
        keyPrefix = "sk-ant-",
        isFreeTier = false,
        steps = listOf(
            "Abre la consola de Anthropic con el botón de abajo y crea una cuenta o inicia sesión.",
            "En \"Billing\" carga un saldo pequeño (la API se cobra por uso).",
            "En \"API Keys\" toca \"Create Key\", ponle un nombre como \"DevFlow\" y créala.",
            "Copia la clave (empieza por \"sk-ant-\"). Solo se muestra una vez.",
            "Vuelve a DevFlow y pégala en el siguiente paso."
        )
    ),
    OPENAI(
        displayName = "ChatGPT",
        company = "OpenAI",
        tagline = "Los modelos GPT de OpenAI. Se paga por uso.",
        keyUrl = "https://platform.openai.com/api-keys",
        keyPrefix = "sk-",
        isFreeTier = false,
        steps = listOf(
            "Abre la plataforma de OpenAI con el botón de abajo e inicia sesión.",
            "En \"Billing\" agrega un método de pago y un saldo pequeño.",
            "En \"API keys\" toca \"Create new secret key\" y ponle un nombre como \"DevFlow\".",
            "Copia la clave (empieza por \"sk-\"). Solo se muestra una vez.",
            "Vuelve a DevFlow y pégala en el siguiente paso."
        )
    );

    /** Aviso suave si la clave no tiene el formato esperado (no bloquea). */
    fun looksLikeValidKey(key: String): Boolean = key.trim().startsWith(keyPrefix)

    companion object {
        fun fromName(name: String?): AiProvider? = entries.firstOrNull { it.name == name }
    }
}

/** Modelo disponible en la cuenta del usuario. */
data class AiModel(val id: String, val displayName: String)

/** Mensaje del chat con el asistente. */
data class ChatMessage(
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Contenido extra para la IA (archivo, diff, error largo) que no se muestra completo en el chat. */
    val attachment: String? = null
) {
    enum class Role { USER, ASSISTANT }

    /** Texto que recibe la IA: la pregunta más el adjunto, si hay. */
    val promptText: String
        get() = if (attachment.isNullOrBlank()) text else "$text\n\n$attachment"
}

/** Error con un mensaje listo para mostrar al usuario. */
class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)
