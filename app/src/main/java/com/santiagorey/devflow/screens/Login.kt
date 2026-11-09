package com.santiagorey.devflow.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


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

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    LoginScreen(
        email = email,

        onEmailChange = {
            email = it
        },

        password = password,

        onPasswordChange = {
            password = it
        },

        passwordVisible = passwordVisible,

        onPasswordVisibleChange = {
            passwordVisible = it
        },

        isLoading = isLoading,

        onForgotPasswordClick = onForgotPasswordClick,

        onLoginClick = {

            if (
                email.isBlank() ||
                password.isBlank()
            ) {

                Toast.makeText(
                    context,
                    "Por favor ingrese usuario y contraseña",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                onLoginClick(
                    email,
                    password
                )
            }
        },

        onGitHubClick = onGitHubClick,

        onGoogleClick = {

            launchGoogleSignIn(
                context = context,
                coroutineScope = coroutineScope,

                onSuccess = { idToken ->

                    onGoogleClick(idToken)
                },

                onError = { errorMsg ->

                    Toast.makeText(
                        context,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        },

        onCreateAccountClick = onCreateAccountClick
    )
}


fun launchGoogleSignIn(
    context: Context,
    coroutineScope: CoroutineScope,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {

    val credentialManager =
        CredentialManager.create(context)

    val webClientIdRes =
        context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )

    val webClientId =
        if (webClientIdRes != 0) {

            context.getString(
                webClientIdRes
            )

        } else {

            ""
        }

    if (webClientId.isBlank()) {

        onError(
            "Para activar Google Sign-In, descarga e instala google-services.json desde Firebase en app/"
        )

        return
    }

    val googleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

    val request =
        GetCredentialRequest.Builder()
            .addCredentialOption(
                googleIdOption
            )
            .build()

    coroutineScope.launch {

        try {

            val result =
                credentialManager.getCredential(
                    request = request,
                    context = context
                )

            val credential =
                result.credential

            if (
                credential is CustomCredential &&
                credential.type ==
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(
                        credential.data
                    )

                onSuccess(
                    googleIdTokenCredential.idToken
                )

            } else {

                onError(
                    "Respuesta no válida de Google Sign-In."
                )
            }

        } catch (
            e: GetCredentialCancellationException
        ) {

            Log.i(
                "GoogleSignIn",
                "Cancelado por el usuario"
            )

        } catch (
            e: Exception
        ) {

            Log.e(
                "GoogleSignIn",
                "Error en Google Sign-In",
                e
            )

            onError(
                "No se pudo iniciar sesión con Google."
            )
        }
    }
}


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

    val focusRequesterEmail =
        remember {
            FocusRequester()
        }

    val focusRequesterPassword =
        remember {
            FocusRequester()
        }

    val keyboardController =
        LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {

        focusRequesterEmail
            .requestFocus()
    }

    Surface(
        modifier =
            modifier.fillMaxSize(),

        color =
            Color(0xFF0B0E14)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(
                        RoundedCornerShape(18.dp)
                    )
                    .background(
                        Color(0xFF635BFF)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "</>",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Color(0xFF0F2937)
                    )
                    .border(
                        width = 1.dp,
                        color =
                            Color(0xFF155E75),
                        shape = CircleShape
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF10B981)
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text =
                        "DEVELOPER EDITION",

                    style = TextStyle(
                        fontFamily =
                            FontFamily.Monospace,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 11.sp,

                        color =
                            Color(0xFF38BDF8)
                    )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "Bienvenido a DevFlow",

                style = TextStyle(
                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 26.sp,

                    color =
                        Color.White
                ),

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Gestiona el ciclo de vida de tu software desde la\nidea hasta el despliegue y mantenimiento",

                style = TextStyle(
                    fontSize = 13.sp,

                    color =
                        Color(0xFF94A3B8),

                    lineHeight =
                        18.sp
                ),

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF111622)
                    ),

                border =
                    BorderStroke(
                        width = 1.dp,

                        color =
                            Color(0xFF1E293B)
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "CORREO ELECTRÓNICO",

                            style = TextStyle(
                                fontFamily =
                                    FontFamily.Monospace,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    11.sp,

                                color =
                                    Color(0xFF94A3B8),

                                letterSpacing =
                                    0.5.sp
                            )
                        )

                        Text(
                            text =
                                "dev@identity",

                            style =
                                TextStyle(
                                    fontFamily =
                                        FontFamily.Monospace,

                                    fontSize =
                                        11.sp,

                                    color =
                                        Color(0xFF64748B)
                                )
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = email,

                        onValueChange =
                            onEmailChange,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(
                                    focusRequesterEmail
                                ),

                        placeholder = {

                            Text(
                                text =
                                    "alex.dev@ejemplo.com",

                                color =
                                    Color(0xFF475569),

                                fontSize =
                                    14.sp
                            )
                        },

                        leadingIcon = {

                            Icon(
                                painter =
                                    painterResource(
                                        id =
                                            R.drawable.ic_email
                                    ),

                                contentDescription =
                                    "Email",

                                tint =
                                    Color(0xFF64748B),

                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )
                        },

                        singleLine = true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Email,

                                imeAction =
                                    ImeAction.Next
                            ),

                        keyboardActions =
                            KeyboardActions(
                                onNext = {

                                    focusRequesterPassword
                                        .requestFocus()
                                }
                            ),

                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedContainerColor =
                                        Color(
                                            0xFF0A0D14
                                        ),

                                    unfocusedContainerColor =
                                        Color(
                                            0xFF0A0D14
                                        ),

                                    focusedBorderColor =
                                        Color(
                                            0xFF635BFF
                                        ),

                                    unfocusedBorderColor =
                                        Color(
                                            0xFF1E293B
                                        ),

                                    focusedTextColor =
                                        Color.White,

                                    unfocusedTextColor =
                                        Color.White
                                )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "CONTRASEÑA",

                            style =
                                TextStyle(
                                    fontFamily =
                                        FontFamily.Monospace,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        11.sp,

                                    color =
                                        Color(0xFF94A3B8),

                                    letterSpacing =
                                        0.5.sp
                                )
                        )

                        Text(
                            text =
                                "¿Olvidaste tu contraseña?",

                            style =
                                TextStyle(
                                    fontSize =
                                        12.sp,

                                    fontWeight =
                                        FontWeight.Medium,

                                    color =
                                        Color(0xFF94A3B8)
                                ),

                            modifier =
                                Modifier.clickable {

                                    onForgotPasswordClick()
                                }
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = password,

                        onValueChange =
                            onPasswordChange,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(
                                    focusRequesterPassword
                                ),

                        placeholder = {

                            Text(
                                text =
                                    "••••••••••••",

                                color =
                                    Color(0xFF475569),

                                fontSize =
                                    14.sp
                            )
                        },

                        leadingIcon = {

                            Icon(
                                painter =
                                    painterResource(
                                        id =
                                            R.drawable.ic_lock
                                    ),

                                contentDescription =
                                    "Password",

                                tint =
                                    Color(0xFF64748B),

                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )
                        },

                        trailingIcon = {

                            IconButton(
                                onClick = {

                                    onPasswordVisibleChange(
                                        !passwordVisible
                                    )
                                }
                            ) {

                                Icon(
                                    painter =
                                        painterResource(
                                            id =
                                                if (
                                                    passwordVisible
                                                ) {

                                                    R.drawable.ic_eye

                                                } else {

                                                    R.drawable.ic_eye_off
                                                }
                                        ),

                                    contentDescription =
                                        if (
                                            passwordVisible
                                        ) {

                                            "Ocultar contraseña"

                                        } else {

                                            "Mostrar contraseña"
                                        },

                                    tint =
                                        Color(0xFF64748B),

                                    modifier =
                                        Modifier.size(
                                            20.dp
                                        )
                                )
                            }
                        },

                        visualTransformation =
                            if (
                                passwordVisible
                            ) {

                                VisualTransformation.None

                            } else {

                                PasswordVisualTransformation()
                            },

                        singleLine = true,

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Password,

                                imeAction =
                                    ImeAction.Done
                            ),

                        keyboardActions =
                            KeyboardActions(
                                onDone = {

                                    keyboardController
                                        ?.hide()

                                    onLoginClick()
                                }
                            ),

                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedContainerColor =
                                        Color(
                                            0xFF0A0D14
                                        ),

                                    unfocusedContainerColor =
                                        Color(
                                            0xFF0A0D14
                                        ),

                                    focusedBorderColor =
                                        Color(
                                            0xFF635BFF
                                        ),

                                    unfocusedBorderColor =
                                        Color(
                                            0xFF1E293B
                                        ),

                                    focusedTextColor =
                                        Color.White,

                                    unfocusedTextColor =
                                        Color.White
                                )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )

                    Button(
                        onClick = {

                            keyboardController
                                ?.hide()

                            onLoginClick()
                        },

                        enabled =
                            !isLoading,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),

                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            0xFF635BFF
                                        )
                                )
                    ) {

                        if (
                            isLoading
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        22.dp
                                    ),

                                color =
                                    Color.White,

                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.Center
                            ) {

                                Text(
                                    text =
                                        "Iniciar sesión",

                                    style =
                                        TextStyle(
                                            fontWeight =
                                                FontWeight.Bold,

                                            fontSize =
                                                15.sp,

                                            color =
                                                Color.White
                                        )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            8.dp
                                        )
                                )

                                Icon(
                                    painter =
                                        painterResource(
                                            id =
                                                R.drawable.ic_arrow_forward
                                        ),

                                    contentDescription =
                                        null,

                                    tint =
                                        Color.White,

                                    modifier =
                                        Modifier.size(
                                            18.dp
                                        )
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        HorizontalDivider(
                            modifier =
                                Modifier.weight(1f),

                            color =
                                Color(0xFF1E293B)
                        )

                        Text(
                            text =
                                "o continúa con",

                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        12.dp
                                ),

                            style =
                                TextStyle(
                                    fontFamily =
                                        FontFamily.Monospace,

                                    fontSize =
                                        11.sp,

                                    color =
                                        Color(0xFF64748B)
                                )
                        )

                        HorizontalDivider(
                            modifier =
                                Modifier.weight(1f),

                            color =
                                Color(0xFF1E293B)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {

                        OutlinedButton(
                            onClick =
                                onGitHubClick,

                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),

                            shape =
                                RoundedCornerShape(
                                    12.dp
                                ),

                            border =
                                BorderStroke(
                                    1.dp,

                                    Color(
                                        0xFF1E293B
                                    )
                                ),

                            colors =
                                ButtonDefaults
                                    .outlinedButtonColors(
                                        containerColor =
                                            Color(
                                                0xFF0A0D14
                                            )
                                    )
                        ) {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.Center
                            ) {

                                Icon(
                                    painter =
                                        painterResource(
                                            id =
                                                R.drawable.ic_github
                                        ),

                                    contentDescription =
                                        "GitHub",

                                    tint =
                                        Color.White,

                                    modifier =
                                        Modifier.size(
                                            18.dp
                                        )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            8.dp
                                        )
                                )

                                Text(
                                    text =
                                        "GitHub",

                                    style =
                                        TextStyle(
                                            fontWeight =
                                                FontWeight.Bold,

                                            fontSize =
                                                14.sp,

                                            color =
                                                Color.White
                                        )
                                )
                            }
                        }

                        OutlinedButton(
                            onClick =
                                onGoogleClick,

                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),

                            shape =
                                RoundedCornerShape(
                                    12.dp
                                ),

                            border =
                                BorderStroke(
                                    1.dp,

                                    Color(
                                        0xFF1E293B
                                    )
                                ),

                            colors =
                                ButtonDefaults
                                    .outlinedButtonColors(
                                        containerColor =
                                            Color(
                                                0xFF0A0D14
                                            )
                                    )
                        ) {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.Center
                            ) {

                                Icon(
                                    painter =
                                        painterResource(
                                            id =
                                                R.drawable.ic_google
                                        ),

                                    contentDescription =
                                        "Google",

                                    tint =
                                        Color.Unspecified,

                                    modifier =
                                        Modifier.size(
                                            18.dp
                                        )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            8.dp
                                        )
                                )

                                Text(
                                    text =
                                        "Google",

                                    style =
                                        TextStyle(
                                            fontWeight =
                                                FontWeight.Bold,

                                            fontSize =
                                                14.sp,

                                            color =
                                                Color.White
                                        )
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "¿No tienes cuenta? ",

                            style =
                                TextStyle(
                                    fontSize =
                                        13.sp,

                                    color =
                                        Color(0xFF94A3B8)
                                )
                        )

                        Text(
                            text =
                                "Crear cuenta gratis",

                            style =
                                TextStyle(
                                    fontSize =
                                        13.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        Color.White,

                                    textDecoration =
                                        TextDecoration.Underline
                                ),

                            modifier =
                                Modifier.clickable {

                                    onCreateAccountClick()
                                }
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(14.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF111622)
                    ),

                border =
                    BorderStroke(
                        width = 1.dp,

                        color =
                            Color(0xFF1E293B)
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    16.dp,

                                vertical =
                                    14.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    Icon(
                        painter =
                            painterResource(
                                id =
                                    R.drawable.ic_shield_check
                            ),

                        contentDescription =
                            "Shield",

                        tint =
                            Color(0xFF38BDF8),

                        modifier =
                            Modifier.size(
                                18.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )

                    Text(
                        text =
                            "Cifrado de extremo a extremo & sincronización Git",

                        style =
                            TextStyle(
                                fontFamily =
                                    FontFamily.Monospace,

                                fontWeight =
                                    FontWeight.Medium,

                                fontSize =
                                    11.sp,

                                color =
                                    Color(0xFF94A3B8)
                            ),

                        textAlign =
                            TextAlign.Center
                    )
                }
            }
        }
    }
}


@Preview(
    showBackground = true
)
@Composable
fun LoginScreenPreview() {

    DevFlowTheme {

        LoginScreen(
            email =
                "Santiagorey@ejemplo.com",

            onEmailChange = {},

            password =
                "123",

            onPasswordChange = {},

            passwordVisible =
                false,

            onPasswordVisibleChange =
                {},

            onForgotPasswordClick =
                {},

            onLoginClick =
                {},

            onGitHubClick =
                {},

            onGoogleClick =
                {},

            onCreateAccountClick =
                {}
        )
    }
}