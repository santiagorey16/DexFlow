package com.santiagorey.devflow.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * Login "con estado": guarda lo que escribes (correo, contraseña, ver contraseña)
 * valida que no esté vacío y abre el selector de Google.
 * Le pasa todo a [LoginScreen], que solo dibuja (patrón state hoisting).
 */
@Composable
fun StatefulLoginScreen(
    isLoading: Boolean = false,
    onForgotPasswordClick: () -> Unit = {},
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onGitHubClick: () -> Unit = {},
    onGoogleClick: (String) -> Unit = {},
    onCreateAccountClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LoginScreen(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        passwordVisible = passwordVisible,
        onPasswordVisibleChange = { passwordVisible = it },
        isLoading = isLoading,
        onForgotPasswordClick = onForgotPasswordClick,
        onLoginClick = {
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            } else {
                onLoginClick(email, password)
            }
        },
        onGitHubClick = onGitHubClick,
        onGoogleClick = {
            launchGoogleSignIn(
                context = context,
                coroutineScope = coroutineScope,
                onSuccess = { idToken -> onGoogleClick(idToken) },
                onError = { errorMsg -> Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show() }
            )
        },
        onCreateAccountClick = onCreateAccountClick
    )
}


/**
 * Abre el selector de cuentas de Google y devuelve el ID token para Firebase.
 *
 * Requisitos en Firebase Console:
 *  1. Authentication → Sign-in method → habilitar "Google".
 *  2. Configuración del proyecto → agregar la huella SHA-1 de la app.
 *  3. Volver a descargar google-services.json (debe incluir "oauth_client").
 */
fun launchGoogleSignIn(
    context: Context,
    coroutineScope: CoroutineScope,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val credentialManager = CredentialManager.create(context)

    // Generado por el plugin google-services a partir de google-services.json
    val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val webClientId = if (webClientIdRes != 0) context.getString(webClientIdRes) else ""

    if (webClientId.isBlank()) {
        onError(
            "Google Sign-In no está configurado: habilita Google en Firebase, agrega tu SHA-1 y vuelve a descargar google-services.json."
        )
        return
    }

    // "Sign in with Google": siempre muestra el selector de cuentas del dispositivo.
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    coroutineScope.launch {
        try {
            val credential = credentialManager.getCredential(request = request, context = context).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                onSuccess(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                onError("Respuesta no válida de Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i("GoogleSignIn", "Cancelado por el usuario")
        } catch (e: NoCredentialException) {
            Log.e("GoogleSignIn", "Sin cuentas disponibles", e)
            onError("No hay cuentas de Google en este dispositivo. Agrega una en Ajustes del teléfono.")
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error en Google Sign-In", e)
            onError("No se pudo iniciar sesión con Google.")
        }
    }
}


/**
 * Pantalla de inicio de sesión SIN estado: recibe los valores y avisa con lambdas.
 * Así se puede ver en el @Preview sin ejecutar la app.
 * Al abrirse pone el cursor en el campo de correo (LaunchedEffect + FocusRequester).
 */
@Composable
fun LoginScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGitHubClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusRequesterEmail = remember { FocusRequester() }
    val focusRequesterPassword = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequesterEmail.requestFocus() }

    AuthScaffold(modifier = modifier) {
        DevFlowLogo(size = 82.dp)
        Spacer(modifier = Modifier.height(20.dp))
        TaglinePill()
        Spacer(modifier = Modifier.height(16.dp))
        AuthTitle(text = "Bienvenido a DevFlow", fontSize = 26.sp)
        Spacer(modifier = Modifier.height(8.dp))
        AuthSubtitle(text = "Gestiona el ciclo de vida de tu software desde la\nidea hasta el despliegue y mantenimiento")
        Spacer(modifier = Modifier.height(28.dp))

        AuthCard {
            AuthLabel(text = "CORREO ELECTRÓNICO")
            Spacer(modifier = Modifier.height(8.dp))
            AuthTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = "tu@correo.com",
                iconRes = R.drawable.ic_email,
                iconDescription = "Email",
                modifier = Modifier.focusRequester(focusRequesterEmail),
                showErrors = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusRequesterPassword.requestFocus() })
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuthLabel(text = "CONTRASEÑA")
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AuthColors.TextMuted),
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AuthTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "••••••••••••",
                iconRes = R.drawable.ic_lock,
                iconDescription = "Password",
                modifier = Modifier.focusRequester(focusRequesterPassword),
                showErrors = false,
                passwordVisible = passwordVisible,
                onToggleVisible = { onPasswordVisibleChange(!passwordVisible) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    onLoginClick()
                })
            )

            Spacer(modifier = Modifier.height(22.dp))
            AuthPrimaryButton(text = "Iniciar sesión", isLoading = isLoading, height = 48.dp) {
                keyboardController?.hide()
                onLoginClick()
            }

            Spacer(modifier = Modifier.height(20.dp))
            OrContinueWithDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthSocialButton(text = "GitHub", iconRes = R.drawable.ic_github, iconTint = Color.White, onClick = onGitHubClick)
                AuthSocialButton(text = "Google", iconRes = R.drawable.ic_google, iconTint = Color.Unspecified, onClick = onGoogleClick)
            }

            Spacer(modifier = Modifier.height(22.dp))
            AuthFooterLink(
                prompt = "¿No tienes cuenta? ",
                action = "Crear cuenta gratis",
                actionColor = Color.White,
                onClick = onCreateAccountClick
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        AuthSecurityNote(
            text = "Cifrado de extremo a extremo & sincronización Git",
            iconDescription = "Shield",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Pastilla "PROYECTOS • GITHUB • IA" con el punto verde. */
@Composable
private fun TaglinePill() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF0F2937))
            .border(width = 1.dp, color = Color(0xFF155E75), shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "PROYECTOS • GITHUB • IA",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = AuthColors.Accent
            )
        )
    }
}

/** Separador "──── o continúa con ────" antes de los botones de GitHub y Google. */
@Composable
private fun OrContinueWithDivider() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthColors.Border)
        Text(
            text = "o continúa con",
            modifier = Modifier.padding(horizontal = 12.dp),
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AuthColors.Icon)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthColors.Border)
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    DevFlowTheme {
        LoginScreen(
            email = "Santiagorey@ejemplo.com",
            onEmailChange = {},
            password = "123",
            onPasswordChange = {},
            passwordVisible = false,
            onPasswordVisibleChange = {},
            onForgotPasswordClick = {},
            onLoginClick = {},
            onGitHubClick = {},
            onGoogleClick = {},
            onCreateAccountClick = {}
        )
    }
}
