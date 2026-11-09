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
    val currentUser: FirebaseUser? = null
)
