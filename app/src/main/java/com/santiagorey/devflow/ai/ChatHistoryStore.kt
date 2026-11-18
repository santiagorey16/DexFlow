package com.santiagorey.devflow.ai

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Historial del asistente por proyecto ("general" para preguntas sin proyecto).
 *
 * - Cuenta real: Firestore en `users/{uid}/chats/{chatId}` (se ve en cualquier teléfono).
 * - Sesión temporal: solo en el teléfono.
 *
 * Se guardan los últimos [MAX_MESSAGES] mensajes para no crecer sin límite.
 */
class ChatHistoryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("devflow_chats", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Lee el historial: de Firestore con cuenta o del teléfono sin ella. Si falla, devuelve una lista vacía. */
    suspend fun load(account: String, uid: String?, chatId: String): List<ChatMessage> =
        safely("cargar", emptyList()) {
            if (uid != null) {
                val doc = chatDoc(uid, chatId).get().await()
                (doc.get("messages") as? List<*>)?.mapNotNull { it.toChatMessage() }.orEmpty()
            } else {
                prefs.getString(localKey(account, chatId), null)?.let(::parseLocal).orEmpty()
            }
        }

    /** Guarda los últimos [MAX_MESSAGES] mensajes del chat. */
    suspend fun save(account: String, uid: String?, chatId: String, messages: List<ChatMessage>) {
        val recent = messages.takeLast(MAX_MESSAGES)
        safely("guardar", Unit) {
            if (uid != null) {
                chatDoc(uid, chatId).set(
                    mapOf(
                        "messages" to recent.map { mapOf("role" to it.role.name, "text" to it.text, "ts" to it.timestamp) },
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                val array = JSONArray()
                recent.forEach { array.put(JSONObject().put("role", it.role.name).put("text", it.text).put("ts", it.timestamp)) }
                prefs.edit { putString(localKey(account, chatId), array.toString()) }
            }
        }
    }

    /** Borra el historial del chat ("Nuevo chat"). */
    suspend fun clear(account: String, uid: String?, chatId: String) {
        safely("borrar", Unit) {
            if (uid != null) chatDoc(uid, chatId).delete().await()
            else prefs.edit { remove(localKey(account, chatId)) }
        }
    }

    /**
     * Ejecuta [block] y, si falla, lo registra y devuelve [fallback]. Las cancelaciones
     * (cambiar de proyecto, "Nuevo chat") no son errores: se dejan pasar sin registrarlas.
     */
    private suspend fun <T> safely(action: String, fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo $action el historial", e)
            fallback
        }

    /** Documento users/{uid}/chats/{chatId} en Firestore. */
    private fun chatDoc(uid: String, chatId: String) =
        firestore.collection("users").document(uid).collection("chats").document(chatId)

    /** Clave del historial guardado en el teléfono. */
    private fun localKey(account: String, chatId: String) = "chat_${account}_$chatId"

    /** JSON guardado en el teléfono → mensajes. */
    private fun parseLocal(raw: String): List<ChatMessage> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val role = ChatMessage.Role.entries.firstOrNull { it.name == item.optString("role") } ?: return@mapNotNull null
            ChatMessage(role, item.optString("text"), item.optLong("ts"))
        }
    }

    /** Mapa leído de Firestore → mensaje (null si está incompleto). */
    private fun Any?.toChatMessage(): ChatMessage? {
        val map = this as? Map<*, *> ?: return null
        val role = ChatMessage.Role.entries.firstOrNull { it.name == map["role"] } ?: return null
        val text = map["text"] as? String ?: return null
        return ChatMessage(role, text, (map["ts"] as? Number)?.toLong() ?: 0L)
    }

    companion object {
        const val GENERAL_CHAT = "general"
        private const val MAX_MESSAGES = 40
        private const val TAG = "DevFlowAI"
    }
}
