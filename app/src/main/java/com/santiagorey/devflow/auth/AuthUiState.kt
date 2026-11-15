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
    /** Hay un token de GitHub guardado: el asistente puede leer los repositorios. */
    val githubRepoAccess: Boolean = false,
    /** Proveedor que se está vinculando ahora mismo desde Ajustes. */
    val linkingProvider: String? = null,
    /** Se está guardando un cambio de perfil (nombre o contraseña). */
    val isUpdatingProfile: Boolean = false,
    /**
     * Cambia cada vez que se edita el perfil. FirebaseUser es el mismo objeto aunque cambie
     * su nombre, así que sin este contador la pantalla no se enteraría del cambio.
     */
    val profileVersion: Int = 0
) {
    /** Identificador usado para separar los datos guardados de cada cuenta. */
    val dataKey: String?
        get() = currentUser?.uid
}
