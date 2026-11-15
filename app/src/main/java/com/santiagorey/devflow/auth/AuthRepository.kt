package com.santiagorey.devflow.auth

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
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

    /**
     * GitHub ya está vinculado: se vuelve a autorizar para obtener un token nuevo
     * con permiso de lectura de repositorios (los tokens no se guardan en Firebase).
     */
    fun reauthorizeGitHub(activity: Activity, onResult: (Result<AuthResult>) -> Unit) {
        val user = auth.currentUser
            ?: return onResult(Result.failure(IllegalStateException("No hay sesión activa")))
        val task = auth.pendingAuthResult
            ?: user.startActivityForReauthenticateWithProvider(activity, gitHubProvider())
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

    // =====================================================
    // PERFIL (Ajustes → Cuenta)
    // =====================================================

    suspend fun updateDisplayName(name: String): FirebaseUser {
        val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa")
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        return user
    }

    /** Firebase exige un inicio de sesión reciente: se re-autentica con la contraseña actual. */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa")
        val email = user.email ?: throw IllegalStateException("La cuenta no tiene correo")
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        user.updatePassword(newPassword).await()
    }

    // =====================================================
    // ELIMINAR CUENTA (requisito de Google Play)
    // =====================================================

    /** Firebase solo permite borrar la cuenta si se inició sesión hace poco. */
    fun hasRecentLogin(): Boolean {
        val lastSignIn = auth.currentUser?.metadata?.lastSignInTimestamp ?: return false
        return System.currentTimeMillis() - lastSignIn < RECENT_LOGIN_MS
    }

    /** Borra todos los datos del usuario en Firestore y después la cuenta. */
    suspend fun deleteAccountAndData() {
        val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa")
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(user.uid)
        for (name in USER_COLLECTIONS) {
            val docs = userDoc.collection(name).get().await().documents
            // Firestore admite hasta 500 operaciones por lote.
            docs.chunked(400).forEach { chunk ->
                db.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
            }
        }
        userDoc.delete().await()
        user.delete().await()
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
            // "repo" permite que el asistente lea tus repositorios (también los privados).
            scopes = listOf("user:email", "read:user", "repo")
        }.build()

    private fun Task<AuthResult>.deliverTo(onResult: (Result<AuthResult>) -> Unit) {
        addOnSuccessListener { result -> onResult(Result.success(result)) }
        addOnFailureListener { exception -> onResult(Result.failure(exception)) }
    }
}

/** Token de GitHub que entrega el inicio de sesión / vinculación (null si no es GitHub). */
fun AuthResult.gitHubAccessToken(): String? =
    (credential as? OAuthCredential)
        ?.takeIf { it.provider == "github.com" }
        ?.accessToken

/** Proveedores reales vinculados al usuario (sin la entrada interna "firebase"). */
fun FirebaseUser?.linkedProviderIds(): Set<String> =
    this?.providerData
        ?.map { it.providerId }
        ?.filter { it != "firebase" }
        ?.toSet()
        ?: emptySet()

private const val RECENT_LOGIN_MS = 5 * 60 * 1000L

/** Colecciones de cada usuario en Firestore (users/{uid}/...). */
internal val USER_COLLECTIONS = listOf("projects", "clients", "activity", "chats")
