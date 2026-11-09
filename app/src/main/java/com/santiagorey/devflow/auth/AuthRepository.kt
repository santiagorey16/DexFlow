package com.santiagorey.devflow.auth

import android.app.Activity
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
        val provider = OAuthProvider.newBuilder("github.com").apply {
            scopes = listOf("user:email")
        }

        // Check if there is a pending auth result
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener { result -> onResult(Result.success(result)) }
                .addOnFailureListener { exception -> onResult(Result.failure(exception)) }
        } else {
            auth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener { result -> onResult(Result.success(result)) }
                .addOnFailureListener { exception -> onResult(Result.failure(exception)) }
        }
    }

    fun logout() {
        auth.signOut()
    }
}
