package com.santiagorey.devflow.screens

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme


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


    fun validateAndRegister() {

        nameError = null
        emailError = null
        passwordError = null
        confirmPasswordError = null

        var valid = true

        if (name.isBlank()) {
            nameError = "Ingresa tu nombre."
            valid = false
        }

        if (email.isBlank()) {
            emailError = "Ingresa tu correo electrónico."
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = "Ingresa un correo válido."
            valid = false
        }

        if (password.isBlank()) {
            passwordError = "Ingresa una contraseña."
            valid = false
        } else if (password.length < 8) {
            passwordError = "La contraseña debe tener mínimo 8 caracteres."
            valid = false
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Confirma tu contraseña."
            valid = false
        } else if (password != confirmPassword) {
            confirmPasswordError = "Las contraseñas no coinciden."
            valid = false
        }

        if (valid) {
            keyboardController?.hide()

            onRegisterClick(
                name.trim(),
                email.trim(),
                password,
                confirmPassword
            )
        }
    }


    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0B0E14)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // =====================================================
            // LOGO DEVFLOW
            // =====================================================

            RegisterDevFlowLogo(
                size = 82.dp
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =====================================================
            // TÍTULO
            // =====================================================

            Text(
                text = "Crear cuenta en DevFlow",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            Text(
                text = "Crea tu perfil y comienza a gestionar tus proyectos de software.",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )


            Spacer(
                modifier = Modifier.height(26.dp)
            )


            // =====================================================
            // CARD REGISTRO
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111622)
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFF1E293B)
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    // =================================================
                    // NOMBRE
                    // =================================================

                    RegisterLabel(
                        text = "NOMBRE COMPLETO"
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Santiago Rey",
                                color = Color(0xFF475569),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_users_group
                                ),
                                contentDescription = "Nombre",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        supportingText = {
                            nameError?.let {
                                Text(it)
                            }
                        },
                        isError = nameError != null,
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusEmail.requestFocus()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = registerTextFieldColors()
                    )


                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    // =================================================
                    // EMAIL
                    // =================================================

                    RegisterLabel(
                        text = "CORREO ELECTRÓNICO"
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusEmail),
                        placeholder = {
                            Text(
                                text = "santiago@ejemplo.com",
                                color = Color(0xFF475569),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_email
                                ),
                                contentDescription = "Correo",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        supportingText = {
                            emailError?.let {
                                Text(it)
                            }
                        },
                        isError = emailError != null,
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusPassword.requestFocus()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = registerTextFieldColors()
                    )


                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    // =================================================
                    // CONTRASEÑA
                    // =================================================

                    RegisterLabel(
                        text = "CONTRASEÑA"
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusPassword),
                        placeholder = {
                            Text(
                                text = "Mínimo 8 caracteres",
                                color = Color(0xFF475569),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_lock
                                ),
                                contentDescription = "Contraseña",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (passwordVisible) {
                                            R.drawable.ic_eye
                                        } else {
                                            R.drawable.ic_eye_off
                                        }
                                    ),
                                    contentDescription = if (passwordVisible) {
                                        "Ocultar contraseña"
                                    } else {
                                        "Mostrar contraseña"
                                    },
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = {
                            passwordError?.let {
                                Text(it)
                            }
                        },
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusConfirm.requestFocus()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = registerTextFieldColors()
                    )


                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    // =================================================
                    // CONFIRMAR CONTRASEÑA
                    // =================================================

                    RegisterLabel(
                        text = "CONFIRMAR CONTRASEÑA"
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusConfirm),
                        placeholder = {
                            Text(
                                text = "Repite tu contraseña",
                                color = Color(0xFF475569),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_lock
                                ),
                                contentDescription = "Confirmar contraseña",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    confirmPasswordVisible =
                                        !confirmPasswordVisible
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (confirmPasswordVisible) {
                                            R.drawable.ic_eye
                                        } else {
                                            R.drawable.ic_eye_off
                                        }
                                    ),
                                    contentDescription =
                                        if (confirmPasswordVisible) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        },
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = {
                            confirmPasswordError?.let {
                                Text(it)
                            }
                        },
                        isError = confirmPasswordError != null,
                        visualTransformation =
                            if (confirmPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                validateAndRegister()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = registerTextFieldColors()
                    )


                    Spacer(
                        modifier = Modifier.height(22.dp)
                    )


                    // =================================================
                    // BOTÓN REGISTRO
                    // =================================================

                    Button(
                        onClick = {
                            validateAndRegister()
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF635BFF),
                            disabledContainerColor = Color(0xFF3E3A80)
                        )
                    ) {

                        if (isLoading) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                        } else {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {

                                Text(
                                    text = "Crear cuenta",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                )


                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )


                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_arrow_forward
                                    ),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(22.dp)
                    )


                    // =================================================
                    // VOLVER AL LOGIN
                    // =================================================

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "¿Ya tienes una cuenta? ",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )


                        Text(
                            text = "Inicia sesión",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                textDecoration = TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable(
                                enabled = !isLoading
                            ) {
                                onNavigateToLogin()
                            }
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(24.dp)
            )


            Text(
                text = "DEVFLOW • SOFTWARE PROJECT MANAGEMENT",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF475569),
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}


// =====================================================
// LABEL REUTILIZABLE
// =====================================================

@Composable
private fun RegisterLabel(
    text: String
) {

    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
        )
    )
}


// =====================================================
// COLORES DE CAMPOS
// =====================================================

@Composable
private fun registerTextFieldColors() =
    OutlinedTextFieldDefaults.colors(

        focusedContainerColor = Color(0xFF0A0D14),
        unfocusedContainerColor = Color(0xFF0A0D14),
        disabledContainerColor = Color(0xFF0A0D14),

        focusedBorderColor = Color(0xFF635BFF),
        unfocusedBorderColor = Color(0xFF1E293B),

        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,

        cursorColor = Color(0xFF38BDF8),

        errorBorderColor = Color(0xFFEF4444),
        errorCursorColor = Color(0xFFEF4444),
        errorSupportingTextColor = Color(0xFFF87171),

        disabledTextColor = Color(0xFF64748B)
    )


// =====================================================
// LOGO DEL REGISTRO
// =====================================================

@Composable
private fun RegisterDevFlowLogo(
    modifier: Modifier = Modifier,
    size: Dp = 82.dp
) {

    Image(
        painter = painterResource(
            id = R.drawable.devflow_logo
        ),
        contentDescription = "Logo de DevFlow",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
    )
}


// =====================================================
// PREVIEW
// =====================================================

@Preview(
    showBackground = true
)
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