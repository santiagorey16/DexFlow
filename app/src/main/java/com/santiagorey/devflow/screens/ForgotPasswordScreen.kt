package com.santiagorey.devflow.screens

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme


/**
 * Pantalla "Recuperar contraseña": pide el correo y Firebase envía un enlace para cambiarla.
 * [successMessage] llega del AuthViewModel cuando el correo se envió.
 */
@Composable
fun ForgotPasswordScreen(
    isLoading: Boolean,
    successMessage: String?,
    onSendResetClick: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    /** Valida el correo (vacío o con formato inválido) y, si está bien, pide el enlace. */
    fun sendResetEmail() {
        val cleanEmail = email.trim()
        emailError = when {
            cleanEmail.isBlank() -> "Ingresa tu correo electrónico."
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "Ingresa un correo electrónico válido."
            else -> null
        }
        if (emailError != null) return
        keyboardController?.hide()
        onSendResetClick(cleanEmail)
    }

    AuthScaffold(modifier = modifier) {
        DevFlowLogo(size = 82.dp)
        Spacer(modifier = Modifier.height(20.dp))
        AuthTitle(text = "Recuperar contraseña", fontSize = 25.sp)
        Spacer(modifier = Modifier.height(8.dp))
        AuthSubtitle(
            text = "Ingresa el correo asociado a tu cuenta y te enviaremos un enlace para restablecer tu contraseña.",
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        AuthCard {
            AuthLabel(text = "CORREO ELECTRÓNICO")
            Spacer(modifier = Modifier.height(8.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "tu@correo.com",
                iconRes = R.drawable.ic_email,
                iconDescription = "Correo electrónico",
                error = emailError,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { sendResetEmail() })
            )

            Spacer(modifier = Modifier.height(22.dp))
            AuthPrimaryButton(text = "Enviar enlace", isLoading = isLoading) { sendResetEmail() }

            if (!successMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B261E)),
                    border = BorderStroke(1.dp, Color(0xFF14532D))
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(14.dp),
                        style = TextStyle(fontSize = 13.sp, color = Color(0xFF86EFAC), lineHeight = 18.sp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            AuthFooterLink(
                prompt = "¿Recordaste tu contraseña? ",
                action = "Iniciar sesión",
                enabled = !isLoading,
                onClick = onNavigateToLogin
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        AuthSecurityNote(
            text = "Nunca compartiremos tu contraseña por correo.",
            iconDescription = "Seguridad",
            fontSize = 10.sp
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    DevFlowTheme {
        ForgotPasswordScreen(
            isLoading = false,
            successMessage = null,
            onSendResetClick = {},
            onNavigateToLogin = {}
        )
    }
}
