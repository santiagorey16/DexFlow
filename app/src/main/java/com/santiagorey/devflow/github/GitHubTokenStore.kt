package com.santiagorey.devflow.github

import android.content.Context
import com.santiagorey.devflow.ai.SecureStore

/** Token de GitHub de cada cuenta, cifrado en el teléfono (nunca se sube a Firestore). */
class GitHubTokenStore(context: Context) {
    private val store = SecureStore(context)

    fun get(uid: String): String? = store.getString(key(uid))
    fun save(uid: String, token: String) = store.putString(key(uid), token)
    fun clear(uid: String) = store.remove(key(uid))

    private fun key(uid: String) = "github_token_$uid"
}
