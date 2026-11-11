package com.santiagorey.devflow.auth

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun registerWithEmail(name: String, email: String, password: String): AuthResult {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.let { user ->
            val profileUpdates = userProfileChangeRequest {
                displayName = name.trim()
            }
            user.updateProfile(profileUpdates).await()
        }
        return result
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun loginWithGoogle(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential).await()
    }

    fun loginWithGitHub(activity: Activity, onResult: (Result<AuthResult>) -> Unit) {
        // Si el flujo web quedó pendiente (la actividad se recreó), se retoma.
        val pendingResultTask = auth.pendingAuthResult
        val task = pendingResultTask
            ?: auth.startActivityForSignInWithProvider(activity, gitHubProvider())
        task.deliverTo(onResult)
    }

    // =====================================================
    // VINCULAR CUENTAS (Ajustes → Integraciones)
    // =====================================================

    fun linkGitHub(activity: Activity, onResult: (Result<AuthResult>) -> Unit) {
        val user = auth.currentUser
            ?: return onResult(Result.failure(IllegalStateException("No hay sesión activa")))
        val task = auth.pendingAuthResult
            ?: user.startActivityForLinkWithProvider(activity, gitHubProvider())
        task.deliverTo(onResult)
    }

    suspend fun linkGoogle(idToken: String): AuthResult {
        val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return user.linkWithCredential(credential).await()
    }

    suspend fun unlink(providerId: String): FirebaseUser? {
        val user = auth.currentUser ?: return null
        return user.unlink(providerId).await().user
    }

    suspend fun reloadUser(): FirebaseUser? {
        auth.currentUser?.reload()?.await()
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

    private fun gitHubProvider() =
        OAuthProvider.newBuilder("github.com").apply {
            scopes = listOf("user:email", "read:user")
        }.build()

    private fun Task<AuthResult>.deliverTo(onResult: (Result<AuthResult>) -> Unit) {
        addOnSuccessListener { result -> onResult(Result.success(result)) }
        addOnFailureListener { exception -> onResult(Result.failure(exception)) }
    }
}

/** Proveedores reales vinculados al usuario (sin la entrada interna "firebase"). */
fun FirebaseUser?.linkedProviderIds(): Set<String> =
    this?.providerData
        ?.map { it.providerId }
        ?.filter { it != "firebase" }
        ?.toSet()
        ?: emptySet()
