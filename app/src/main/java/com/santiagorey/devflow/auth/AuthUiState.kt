package com.santiagorey.devflow.auth

import com.google.firebase.auth.FirebaseUser

/**
 * UI State holding authentication status and message feedback.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: FirebaseUser? = null,
    /** Proveedores vinculados a la cuenta (p. ej. "github.com", "google.com", "password"). */
    val providerIds: Set<String> = emptySet(),
    /** Usuario de GitHub obtenido al iniciar sesión o vincular la cuenta. */
    val githubUsername: String? = null,
    /** Sesión de desarrollo (credenciales temporales, sin Firebase). */
    val isTempSession: Boolean = false,
    /** Proveedor que se está vinculando ahora mismo desde Ajustes. */
    val linkingProvider: String? = null
) {
    /** Identificador usado para separar los datos guardados de cada cuenta. */
    val dataKey: String?
        get() = currentUser?.uid ?: if (isTempSession) "temp_session" else null
}
