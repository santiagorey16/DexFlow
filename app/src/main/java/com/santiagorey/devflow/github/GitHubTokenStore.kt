package com.santiagorey.devflow.github

import android.content.Context
import com.santiagorey.devflow.ai.SecureStore

/** Token de GitHub de cada cuenta, cifrado en el teléfono (nunca se sube a Firestore). */
class GitHubTokenStore(context: Context) {
    private val store = SecureStore(context)

    /** Token de GitHub de esa cuenta (null si no conectó sus repos). */
    fun get(uid: String): String? = store.getString(key(uid))
    /** Guarda el token cifrado. */
    fun save(uid: String, token: String) = store.putString(key(uid), token)
    /** Borra el token (al desvincular GitHub o borrar la cuenta). */
    fun clear(uid: String) = store.remove(key(uid))

    /** Clave del token de cada cuenta. */
    private fun key(uid: String) = "github_token_$uid"
}
