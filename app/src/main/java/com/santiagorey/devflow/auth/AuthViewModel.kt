package com.santiagorey.devflow.auth

import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = repository.currentUser != null,
            currentUser = repository.currentUser
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _uiState.update {
            it.copy(
                isAuthenticated = user != null,
                currentUser = user
            )
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.user,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in loginWithEmail", e)
                val userFriendlyMessage = when (e) {
                    is FirebaseAuthInvalidUserException,
                    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
                    else -> "Revisa tu conexión e inténtalo nuevamente."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = userFriendlyMessage
                    )
                }
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.user,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in registerWithEmail", e)
                val userFriendlyMessage = when (e) {
                    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo."
                    is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico no es válido."
                    else -> "Revisa tu conexión e inténtalo nuevamente."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = userFriendlyMessage
                    )
                }
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
                Log.e("AuthViewModel", "Error in sendPasswordReset", e)
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.user,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in loginWithGoogle", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo iniciar sesión con Google."
                    )
                }
            }
        }
    }

    fun loginWithGitHub(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        repository.loginWithGitHub(activity) { result ->
            result.fold(
                onSuccess = { authResult ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = authResult.user,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    Log.e("AuthViewModel", "Error in loginWithGitHub", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se pudo iniciar sesión con GitHub."
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _uiState.update {
            AuthUiState(
                isAuthenticated = false,
                currentUser = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }
}
