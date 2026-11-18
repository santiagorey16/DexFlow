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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.github.GitHubTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Identificadores que usa Firebase para cada forma de iniciar sesión.
const val PROVIDER_GITHUB = "github.com"
/** Ver PROVIDER_GITHUB. */
const val PROVIDER_GOOGLE = "google.com"
/** Ver PROVIDER_GITHUB. */
const val PROVIDER_PASSWORD = "password"

/**
 * Todo lo de la cuenta: iniciar sesión (correo, Google, GitHub), registrarse, recuperar
 * contraseña, vincular o desvincular proveedores, editar el perfil y borrar la cuenta.
 *
 * Las pantallas leen [uiState] y llaman a estas funciones; nunca hablan con Firebase directamente.
 * El trabajo con Firebase lo hace [AuthRepository]; aquí se decide qué mostrar (cargando, errores…).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    /** Única clase que habla con FirebaseAuth. */
    private val repository = AuthRepository()

    /** Datos pequeños de la cuenta en el teléfono (p. ej. el usuario de GitHub). */
    private val prefs = application.getSharedPreferences("devflow_auth", Context.MODE_PRIVATE)

    /** Token de GitHub de cada cuenta, cifrado en el teléfono. */
    private val gitHubTokens = GitHubTokenStore(application)

    // Patrón "estado privado que se puede cambiar + estado público de solo lectura":
    // solo este ViewModel cambia _uiState; las pantallas leen uiState con collectAsState().
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Firebase llama a este listener cada vez que alguien inicia o cierra sesión.
     * Actualiza el estado y, gracias a eso, MainActivity navega sola al Dashboard o al Login.
     */
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _uiState.update {
            it.copy(
                isAuthenticated = user != null,
                currentUser = user,
                providerIds = user.linkedProviderIds(),
                githubUsername = user?.let { u -> storedGithubUsername(u.uid) },
                githubRepoAccess = hasRepoAccess(user)
            )
        }
    }

    // Se ejecuta una vez, al crear el ViewModel.
    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        // Limpieza de versiones anteriores que tenían una sesión temporal de pruebas.
        prefs.edit { remove(LEGACY_TEMP_SESSION_KEY) }
        // Refresca los proveedores vinculados por si cambiaron en otro dispositivo.
        if (repository.currentUser != null) {
            viewModelScope.launch {
                runCatching { repository.reloadUser() }.onSuccess { refreshUser(it) }
            }
        }
    }

    /** El ViewModel se destruye: se quita el listener para no dejarlo escuchando (fuga de memoria). */
    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    /**
     * Estado al abrir la app. Firebase recuerda la sesión en el teléfono aunque se cierre la app:
     * si hay usuario guardado, isAuthenticated = true y MainActivity empieza en el Dashboard;
     * si no, empieza en el Login.
     */
    private fun initialState(): AuthUiState {
        val user = repository.currentUser
        return AuthUiState(
            isAuthenticated = user != null,
            currentUser = user,
            providerIds = user.linkedProviderIds(),
            githubUsername = user?.let { storedGithubUsername(it.uid) },
            githubRepoAccess = hasRepoAccess(user)
        )
    }

    // =====================================================
    // INICIO DE SESIÓN
    // =====================================================

    /**
     * Inicia sesión con correo y contraseña.
     * Muestra la carga (isLoading), y si falla traduce el error de Firebase a un mensaje en español.
     */
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

    /** Crea una cuenta con correo. Valida los datos otra vez antes de llamar a Firebase. */
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

    /** Envía el correo para cambiar la contraseña. */
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

    /** Inicia sesión con el ID token que entregó el selector de cuentas de Google. */
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

    /** Inicia sesión con GitHub (abre el navegador; por eso necesita la Activity). */
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

    /** Agrega GitHub a la cuenta actual desde Ajustes → Integraciones. */
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

    /**
     * Da acceso de lectura a los repositorios para el asistente de IA.
     * Si GitHub no está vinculado lo vincula; si ya lo está, pide autorizar de nuevo
     * para obtener un token con el permiso "repo".
     */
    fun connectGitHubRepos(activity: Activity) {
        if (!canLink()) return
        if (PROVIDER_GITHUB !in _uiState.value.providerIds) {
            linkGitHub(activity)
            return
        }
        _uiState.update { it.copy(linkingProvider = PROVIDER_GITHUB, errorMessage = null) }
        repository.reauthorizeGitHub(activity) { result ->
            result.fold(
                onSuccess = { authResult ->
                    rememberGithubUsername(authResult)
                    refreshUser(authResult.user)
                    val ok = hasRepoAccess(authResult.user)
                    _uiState.update {
                        it.copy(
                            linkingProvider = null,
                            successMessage = if (ok) "Repositorios de GitHub conectados." else null,
                            errorMessage = if (ok) null else "GitHub no entregó acceso a los repositorios. Inténtalo de nuevo."
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Error in connectGitHubRepos", e)
                    _uiState.update {
                        it.copy(linkingProvider = null, errorMessage = friendlyLinkError(e, "GitHub"))
                    }
                }
            )
        }
    }

    /** Agrega Google a la cuenta actual. */
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

    /**
     * Quita un método de inicio de sesión. No deja quitar el último (quedarías sin forma de entrar).
     * Si es GitHub, también borra su token guardado.
     */
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
                    user?.uid?.let { uid ->
                        prefs.edit { remove(githubKey(uid)) }
                        gitHubTokens.clear(uid)
                    }
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

    // =====================================================
    // PERFIL
    // =====================================================

    /** Cambia el nombre visible. Sube profileVersion para que la pantalla se entere del cambio. */
    fun updateDisplayName(name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre no puede estar vacío.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProfile = true, errorMessage = null) }
            try {
                val user = repository.updateDisplayName(name)
                _uiState.update {
                    it.copy(
                        isUpdatingProfile = false,
                        currentUser = user,
                        profileVersion = it.profileVersion + 1,
                        successMessage = "Nombre actualizado."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in updateDisplayName", e)
                _uiState.update {
                    it.copy(isUpdatingProfile = false, errorMessage = "No se pudo actualizar el nombre. Revisa tu conexión.")
                }
            }
        }
    }

    /** Cambia la contraseña verificando primero la actual. */
    fun changePassword(currentPassword: String, newPassword: String) {
        if (newPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener mínimo 8 caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProfile = true, errorMessage = null) }
            try {
                repository.changePassword(currentPassword, newPassword)
                _uiState.update {
                    it.copy(isUpdatingProfile = false, successMessage = "Contraseña actualizada.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in changePassword", e)
                val message = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "La contraseña actual no es correcta."
                    is FirebaseAuthWeakPasswordException -> "La nueva contraseña es muy débil."
                    else -> "No se pudo cambiar la contraseña. Revisa tu conexión."
                }
                _uiState.update { it.copy(isUpdatingProfile = false, errorMessage = message) }
            }
        }
    }

    /**
     * Elimina la cuenta y todos sus datos. [onDeleted] limpia lo guardado en el teléfono
     * (claves de IA, caché) antes de volver al login.
     */
    fun deleteAccount(onDeleted: (uid: String) -> Unit) {
        val uid = repository.currentUser?.uid ?: return
        if (!repository.hasRecentLogin()) {
            _uiState.update {
                it.copy(errorMessage = "Por seguridad, cierra sesión, vuelve a entrar y elimina la cuenta en los 5 minutos siguientes.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProfile = true, errorMessage = null) }
            try {
                repository.deleteAccountAndData()
                gitHubTokens.clear(uid)
                prefs.edit { remove(githubKey(uid)) }
                onDeleted(uid)
                _uiState.update { AuthUiState(successMessage = "Tu cuenta y tus datos fueron eliminados.") }
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                _uiState.update {
                    it.copy(isUpdatingProfile = false, errorMessage = "Por seguridad, vuelve a iniciar sesión y repite la eliminación.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in deleteAccount", e)
                _uiState.update {
                    it.copy(isUpdatingProfile = false, errorMessage = "No se pudo eliminar la cuenta. Revisa tu conexión e inténtalo de nuevo.")
                }
            }
        }
    }

    /** Cierra sesión. El AuthStateListener actualiza el estado y MainActivity vuelve al Login. */
    fun logout() {
        repository.logout()
        _uiState.update { AuthUiState(isAuthenticated = false, currentUser = null) }
    }

    /** Borra los mensajes ya mostrados (para que no salgan otra vez). */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /** Muestra un error que viene de fuera (p. ej. el selector de Google). */
    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    // =====================================================
    // HELPERS
    // =====================================================

    /**
     * Común a todos los logins que salieron bien: guarda el usuario de GitHub (si aplica)
     * y su token cifrado, y actualiza el estado.
     */
    private fun onSignedIn(result: AuthResult) {
        rememberGithubUsername(result)
        val user = result.user
        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                currentUser = user,
                providerIds = user.linkedProviderIds(),
                githubUsername = user?.let { u -> storedGithubUsername(u.uid) },
                githubRepoAccess = hasRepoAccess(user),
                errorMessage = null
            )
        }
    }

    /** Vuelve a leer los proveedores vinculados y si hay acceso a los repos. */
    private fun refreshUser(user: FirebaseUser?) {
        val current = user ?: repository.currentUser
        _uiState.update {
            it.copy(
                currentUser = current,
                providerIds = current.linkedProviderIds(),
                githubUsername = current?.let { u -> storedGithubUsername(u.uid) },
                githubRepoAccess = hasRepoAccess(current)
            )
        }
    }

    /** Solo se pueden conectar integraciones con una sesión iniciada; si no hay, muestra un error y devuelve false. */
    private fun canLink(): Boolean {
        if (repository.currentUser == null) {
            _uiState.update {
                it.copy(errorMessage = "Inicia sesión con una cuenta real (no la temporal) para conectar integraciones.")
            }
            return false
        }
        return true
    }

    /** true si hay un token de GitHub guardado para este usuario (el asistente puede leer repos). */
    private fun hasRepoAccess(user: FirebaseUser?): Boolean =
        user != null && gitHubTokens.get(user.uid) != null

    /** Guarda el usuario de GitHub (para mostrarlo en Ajustes) y su token cifrado. */
    private fun rememberGithubUsername(result: AuthResult) {
        // Token para leer repositorios (solo viene al iniciar sesión / vincular con GitHub).
        val token = result.gitHubAccessToken()
        val tokenUid = result.user?.uid
        if (token != null && tokenUid != null) gitHubTokens.save(tokenUid, token)

        val info = result.additionalUserInfo ?: return
        if (info.providerId != PROVIDER_GITHUB) return
        val username = info.username ?: return
        val uid = result.user?.uid ?: return
        prefs.edit { putString(githubKey(uid), username) }
    }

    /** Usuario de GitHub guardado de esta cuenta. */
    private fun storedGithubUsername(uid: String): String? = prefs.getString(githubKey(uid), null)

    /** Clave en SharedPreferences del usuario de GitHub de cada cuenta. */
    private fun githubKey(uid: String) = "github_username_$uid"

    /** Traduce los errores de Google o GitHub al iniciar sesión (cuenta ya existe, cancelado…). */
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

    /** Traduce los errores al vincular (esa cuenta ya está en otro usuario, cancelado…). */
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
        const val LEGACY_TEMP_SESSION_KEY = "temp_session"
    }
}
