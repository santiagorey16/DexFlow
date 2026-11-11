package com.santiagorey.devflow.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val PROVIDER_GITHUB = "github.com"
const val PROVIDER_GOOGLE = "google.com"

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val prefs = application.getSharedPreferences("devflow_auth", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _uiState.update {
            it.copy(
                isAuthenticated = user != null || it.isTempSession,
                currentUser = user,
                providerIds = user.linkedProviderIds(),
                githubUsername = user?.let { u -> storedGithubUsername(u.uid) }
            )
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        // Refresca los proveedores vinculados por si cambiaron en otro dispositivo.
        if (repository.currentUser != null) {
            viewModelScope.launch {
                runCatching { repository.reloadUser() }.onSuccess { refreshUser(it) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    private fun initialState(): AuthUiState {
        val user = repository.currentUser
        val tempSession = user == null && prefs.getBoolean(KEY_TEMP_SESSION, false)
        return AuthUiState(
            isAuthenticated = user != null || tempSession,
            currentUser = user,
            providerIds = user.linkedProviderIds(),
            githubUsername = user?.let { storedGithubUsername(it.uid) },
            isTempSession = tempSession
        )
    }

    // =====================================================
    // INICIO DE SESIÓN
    // =====================================================

    /** Sesión local de desarrollo (credenciales temporales). Se conserva al reiniciar la app. */
    fun loginTemp() {
        prefs.edit { putBoolean(KEY_TEMP_SESSION, true) }
        _uiState.update { it.copy(isTempSession = true, isAuthenticated = true) }
    }

    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingrese usuario y contraseña") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.loginWithEmail(email, password)
                onSignedIn(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in loginWithEmail", e)
                val userFriendlyMessage = when (e) {
                    is FirebaseAuthInvalidUserException,
                    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
                    else -> "Revisa tu conexión e inténtalo nuevamente."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = userFriendlyMessage) }
            }
        }
    }

    fun registerWithEmail(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre es obligatorio.") }
            return
        }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un correo electrónico válido.") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener mínimo 8 caracteres.") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.registerWithEmail(name, email, password)
                onSignedIn(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in registerWithEmail", e)
                val userFriendlyMessage = when (e) {
                    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo."
                    is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico no es válido."
                    else -> "Revisa tu conexión e inténtalo nuevamente."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = userFriendlyMessage) }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un correo electrónico válido.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.sendPasswordReset(email)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Te enviamos un enlace para restablecer tu contraseña.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendPasswordReset", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo enviar el correo. Revisa que el correo esté registrado."
                    )
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.loginWithGoogle(idToken)
                onSignedIn(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in loginWithGoogle", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = friendlyProviderError(e, "Google"))
                }
            }
        }
    }

    fun loginWithGitHub(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        repository.loginWithGitHub(activity) { result ->
            result.fold(
                onSuccess = { authResult -> onSignedIn(authResult) },
                onFailure = { e ->
                    Log.e(TAG, "Error in loginWithGitHub", e)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyProviderError(e, "GitHub"))
                    }
                }
            )
        }
    }

    // =====================================================
    // INTEGRACIONES (vincular / desvincular)
    // =====================================================

    fun linkGitHub(activity: Activity) {
        if (!canLink()) return
        _uiState.update { it.copy(linkingProvider = PROVIDER_GITHUB, errorMessage = null) }
        repository.linkGitHub(activity) { result ->
            result.fold(
                onSuccess = { authResult ->
                    rememberGithubUsername(authResult)
                    refreshUser(authResult.user)
                    _uiState.update {
                        it.copy(linkingProvider = null, successMessage = "GitHub conectado correctamente.")
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Error in linkGitHub", e)
                    _uiState.update {
                        it.copy(linkingProvider = null, errorMessage = friendlyLinkError(e, "GitHub"))
                    }
                }
            )
        }
    }

    fun linkGoogle(idToken: String) {
        if (!canLink()) return
        viewModelScope.launch {
            _uiState.update { it.copy(linkingProvider = PROVIDER_GOOGLE, errorMessage = null) }
            try {
                val result = repository.linkGoogle(idToken)
                refreshUser(result.user)
                _uiState.update {
                    it.copy(linkingProvider = null, successMessage = "Google conectado correctamente.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in linkGoogle", e)
                _uiState.update {
                    it.copy(linkingProvider = null, errorMessage = friendlyLinkError(e, "Google"))
                }
            }
        }
    }

    fun unlinkProvider(providerId: String) {
        val state = _uiState.value
        if (state.providerIds.size <= 1) {
            _uiState.update {
                it.copy(errorMessage = "No puedes desconectar tu único método de inicio de sesión.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(linkingProvider = providerId) }
            try {
                val user = repository.unlink(providerId)
                if (providerId == PROVIDER_GITHUB) {
                    user?.uid?.let { uid -> prefs.edit { remove(githubKey(uid)) } }
                }
                refreshUser(user)
                _uiState.update { it.copy(linkingProvider = null, successMessage = "Integración desconectada.") }
            } catch (e: Exception) {
                Log.e(TAG, "Error in unlinkProvider", e)
                _uiState.update {
                    it.copy(linkingProvider = null, errorMessage = "No se pudo desconectar. Inténtalo de nuevo.")
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        prefs.edit { putBoolean(KEY_TEMP_SESSION, false) }
        _uiState.update { AuthUiState(isAuthenticated = false, currentUser = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun onSignedIn(result: AuthResult) {
        rememberGithubUsername(result)
        prefs.edit { putBoolean(KEY_TEMP_SESSION, false) }
        val user = result.user
        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                isTempSession = false,
                currentUser = user,
                providerIds = user.linkedProviderIds(),
                githubUsername = user?.let { u -> storedGithubUsername(u.uid) },
                errorMessage = null
            )
        }
    }

    private fun refreshUser(user: FirebaseUser?) {
        val current = user ?: repository.currentUser
        _uiState.update {
            it.copy(
                currentUser = current,
                providerIds = current.linkedProviderIds(),
                githubUsername = current?.let { u -> storedGithubUsername(u.uid) }
            )
        }
    }

    private fun canLink(): Boolean {
        if (repository.currentUser == null) {
            _uiState.update {
                it.copy(errorMessage = "Inicia sesión con una cuenta real (no la temporal) para conectar integraciones.")
            }
            return false
        }
        return true
    }

    private fun rememberGithubUsername(result: AuthResult) {
        val info = result.additionalUserInfo ?: return
        if (info.providerId != PROVIDER_GITHUB) return
        val username = info.username ?: return
        val uid = result.user?.uid ?: return
        prefs.edit { putString(githubKey(uid), username) }
    }

    private fun storedGithubUsername(uid: String): String? = prefs.getString(githubKey(uid), null)

    private fun githubKey(uid: String) = "github_username_$uid"

    private fun friendlyProviderError(e: Throwable, provider: String): String =
        when {
            e is FirebaseAuthUserCollisionException ->
                "Ya existe una cuenta con ese correo. Entra con tu método original y conecta $provider desde Ajustes."
            e is FirebaseAuthException && e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
                "El inicio de sesión con $provider no está habilitado en Firebase."
            e is FirebaseAuthException && e.errorCode == "ERROR_WEB_CONTEXT_CANCELED" ->
                "Inicio de sesión con $provider cancelado."
            else -> "No se pudo iniciar sesión con $provider: ${errorDetail(e)}"
        }

    private fun friendlyLinkError(e: Throwable, provider: String): String =
        when {
            e is FirebaseAuthUserCollisionException ->
                "Esa cuenta de $provider ya está vinculada a otro usuario de DevFlow."
            e is FirebaseAuthException && e.errorCode == "ERROR_PROVIDER_ALREADY_LINKED" ->
                "$provider ya estaba conectado a tu cuenta."
            e is FirebaseAuthException && e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
                "La conexión con $provider no está habilitada en Firebase."
            e is FirebaseAuthException && e.errorCode == "ERROR_WEB_CONTEXT_CANCELED" ->
                "Conexión con $provider cancelada."
            else -> "No se pudo conectar con $provider: ${errorDetail(e)}"
        }

    /** Código y mensaje reales del error, para poder diagnosticar fallos poco comunes. */
    private fun errorDetail(e: Throwable): String {
        val code = (e as? FirebaseAuthException)?.errorCode
        val message = e.localizedMessage ?: e.javaClass.simpleName
        return if (code != null) "$code — $message" else message
    }

    private companion object {
        const val TAG = "AuthViewModel"
        const val KEY_TEMP_SESSION = "temp_session"
    }
}
