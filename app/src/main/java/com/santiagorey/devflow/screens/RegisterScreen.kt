package com.santiagorey.devflow.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme


/**
 * Pantalla de crear cuenta: nombre, correo, contraseña y confirmación.
 * Valida cada campo y muestra el error debajo del campo que falló.
 * Si todo está bien, llama a [onRegisterClick] (el AuthViewModel crea la cuenta en Firebase).
 */
@Composable
fun RegisterScreen(
    isLoading: Boolean,
    onRegisterClick: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val focusEmail = remember { FocusRequester() }
    val focusPassword = remember { FocusRequester() }
    val focusConfirm = remember { FocusRequester() }

    val keyboardController = LocalSoftwareKeyboardController.current

    /**
     * Revisa los 4 campos, guarda un mensaje de error por cada uno que falle
     * y solo si todos están bien oculta el teclado y registra.
     */
    fun validateAndRegister() {
        nameError = if (name.isBlank()) "Ingresa tu nombre." else null
        emailError = when {
            email.isBlank() -> "Ingresa tu correo electrónico."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Ingresa un correo válido."
            else -> null
        }
        passwordError = when {
            password.isBlank() -> "Ingresa una contraseña."
            password.length < 8 -> "La contraseña debe tener mínimo 8 caracteres."
            else -> null
        }
        confirmPasswordError = when {
            confirmPassword.isBlank() -> "Confirma tu contraseña."
            password != confirmPassword -> "Las contraseñas no coinciden."
            else -> null
        }

        val valid = listOf(nameError, emailError, passwordError, confirmPasswordError).all { it == null }
        if (valid) {
            keyboardController?.hide()
            onRegisterClick(name.trim(), email.trim(), password, confirmPassword)
        }
    }

    AuthScaffold(modifier = modifier) {
        DevFlowLogo(size = 82.dp)
        Spacer(modifier = Modifier.height(18.dp))
        AuthTitle(text = "Crear cuenta en DevFlow", fontSize = 25.sp)
        Spacer(modifier = Modifier.height(6.dp))
        AuthSubtitle(text = "Crea tu perfil y comienza a gestionar tus proyectos de software.")
        Spacer(modifier = Modifier.height(26.dp))

        AuthCard {
            AuthLabel(text = "NOMBRE COMPLETO")
            Spacer(modifier = Modifier.height(7.dp))
            AuthTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                placeholder = "Tu nombre completo",
                iconRes = R.drawable.ic_users_group,
                iconDescription = "Nombre",
                error = nameError,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusEmail.requestFocus() })
            )

            Spacer(modifier = Modifier.height(10.dp))
            AuthLabel(text = "CORREO ELECTRÓNICO")
            Spacer(modifier = Modifier.height(7.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "tu@correo.com",
                iconRes = R.drawable.ic_email,
                iconDescription = "Correo",
                modifier = Modifier.focusRequester(focusEmail),
                error = emailError,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusPassword.requestFocus() })
            )

            Spacer(modifier = Modifier.height(10.dp))
            AuthLabel(text = "CONTRASEÑA")
            Spacer(modifier = Modifier.height(7.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                placeholder = "Mínimo 8 caracteres",
                iconRes = R.drawable.ic_lock,
                iconDescription = "Contraseña",
                modifier = Modifier.focusRequester(focusPassword),
                error = passwordError,
                enabled = !isLoading,
                passwordVisible = passwordVisible,
                onToggleVisible = { passwordVisible = !passwordVisible },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusConfirm.requestFocus() })
            )

            Spacer(modifier = Modifier.height(10.dp))
            AuthLabel(text = "CONFIRMAR CONTRASEÑA")
            Spacer(modifier = Modifier.height(7.dp))
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                placeholder = "Repite tu contraseña",
                iconRes = R.drawable.ic_lock,
                iconDescription = "Confirmar contraseña",
                modifier = Modifier.focusRequester(focusConfirm),
                error = confirmPasswordError,
                enabled = !isLoading,
                passwordVisible = confirmPasswordVisible,
                onToggleVisible = { confirmPasswordVisible = !confirmPasswordVisible },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { validateAndRegister() })
            )

            Spacer(modifier = Modifier.height(22.dp))
            AuthPrimaryButton(text = "Crear cuenta", isLoading = isLoading) { validateAndRegister() }

            Spacer(modifier = Modifier.height(22.dp))
            AuthFooterLink(
                prompt = "¿Ya tienes una cuenta? ",
                action = "Inicia sesión",
                enabled = !isLoading,
                onClick = onNavigateToLogin
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "DEVFLOW • SOFTWARE PROJECT MANAGEMENT",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AuthColors.Placeholder,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}


@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    DevFlowTheme {
        RegisterScreen(
            isLoading = false,
            onRegisterClick = { _, _, _, _ -> },
            onNavigateToLogin = {}
        )
    }
}
