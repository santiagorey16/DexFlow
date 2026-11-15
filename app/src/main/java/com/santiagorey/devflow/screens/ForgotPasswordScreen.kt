package com.santiagorey.devflow.screens

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme


@Composable
fun ForgotPasswordScreen(
    isLoading: Boolean,
    successMessage: String?,
    onSendResetClick: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {

    var email by remember {
        mutableStateOf("")
    }

    var emailError by remember {
        mutableStateOf<String?>(null)
    }

    val keyboardController =
        LocalSoftwareKeyboardController.current


    fun sendResetEmail() {

        emailError = null

        val cleanEmail =
            email.trim()

        if (cleanEmail.isBlank()) {

            emailError =
                "Ingresa tu correo electrónico."

            return
        }

        if (
            !Patterns.EMAIL_ADDRESS
                .matcher(cleanEmail)
                .matches()
        ) {

            emailError =
                "Ingresa un correo electrónico válido."

            return
        }

        keyboardController?.hide()

        onSendResetClick(cleanEmail)
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


            // =====================================================
            // LOGO
            // =====================================================

            DevFlowLogo(
                size = 82.dp
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // =====================================================
            // TÍTULO
            // =====================================================

            Text(
                text =
                    "Recuperar contraseña",

                style = TextStyle(
                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        25.sp,

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
                    "Ingresa el correo asociado a tu cuenta y te enviaremos un enlace para restablecer tu contraseña.",

                style = TextStyle(
                    fontSize =
                        13.sp,

                    color =
                        Color(0xFF94A3B8),

                    lineHeight =
                        19.sp
                ),

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =====================================================
            // CARD
            // =====================================================

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


                    // =================================================
                    // LABEL
                    // =================================================

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


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    // =================================================
                    // EMAIL
                    // =================================================

                    OutlinedTextField(
                        value =
                            email,

                        onValueChange = {

                            email = it

                            emailError = null
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled =
                            !isLoading,

                        singleLine =
                            true,

                        placeholder = {

                            Text(
                                text =
                                    "tu@correo.com",

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
                                    "Correo electrónico",

                                tint =
                                    Color(0xFF64748B),

                                modifier =
                                    Modifier.size(20.dp)
                            )
                        },

                        isError =
                            emailError != null,

                        supportingText = {

                            emailError?.let {

                                Text(
                                    text = it
                                )
                            }
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Email,

                                imeAction =
                                    ImeAction.Done
                            ),

                        keyboardActions =
                            KeyboardActions(
                                onDone = {

                                    sendResetEmail()
                                }
                            ),

                        shape =
                            RoundedCornerShape(12.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedContainerColor =
                                    Color(0xFF0A0D14),

                                unfocusedContainerColor =
                                    Color(0xFF0A0D14),

                                disabledContainerColor =
                                    Color(0xFF0A0D14),

                                focusedBorderColor =
                                    Color(0xFF635BFF),

                                unfocusedBorderColor =
                                    Color(0xFF1E293B),

                                focusedTextColor =
                                    Color.White,

                                unfocusedTextColor =
                                    Color.White,

                                cursorColor =
                                    Color(0xFF38BDF8),

                                errorBorderColor =
                                    Color(0xFFEF4444),

                                errorCursorColor =
                                    Color(0xFFEF4444),

                                errorSupportingTextColor =
                                    Color(0xFFF87171)
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )


                    // =================================================
                    // ENVIAR ENLACE
                    // =================================================

                    Button(
                        onClick = {

                            sendResetEmail()
                        },

                        enabled =
                            !isLoading,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),

                        shape =
                            RoundedCornerShape(12.dp),

                        colors =
                            ButtonDefaults.buttonColors(

                                containerColor =
                                    Color(0xFF635BFF),

                                disabledContainerColor =
                                    Color(0xFF3E3A80)
                            )
                    ) {

                        if (isLoading) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(22.dp),

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
                                        "Enviar enlace",

                                    style = TextStyle(
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
                                        Modifier.width(8.dp)
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
                                        Modifier.size(18.dp)
                                )
                            }
                        }
                    }


                    // =================================================
                    // MENSAJE DE ÉXITO
                    // =================================================

                    if (
                        !successMessage.isNullOrBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )


                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(12.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color(0xFF0B261E)
                                ),

                            border =
                                BorderStroke(
                                    1.dp,
                                    Color(0xFF14532D)
                                )
                        ) {

                            Text(
                                text =
                                    successMessage,

                                modifier =
                                    Modifier.padding(14.dp),

                                style = TextStyle(
                                    fontSize =
                                        13.sp,

                                    color =
                                        Color(0xFF86EFAC),

                                    lineHeight =
                                        18.sp
                                ),

                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )


                    // =================================================
                    // VOLVER LOGIN
                    // =================================================

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
                                "¿Recordaste tu contraseña? ",

                            style = TextStyle(
                                fontSize =
                                    13.sp,

                                color =
                                    Color(0xFF94A3B8)
                            )
                        )


                        Text(
                            text =
                                "Iniciar sesión",

                            style = TextStyle(
                                fontSize =
                                    13.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    Color(0xFF38BDF8),

                                textDecoration =
                                    TextDecoration.Underline
                            ),

                            modifier =
                                Modifier.clickable(
                                    enabled =
                                        !isLoading
                                ) {

                                    onNavigateToLogin()
                                }
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // =====================================================
            // INFO SEGURIDAD
            // =====================================================

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
                        1.dp,
                        Color(0xFF1E293B)
                    )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),

                    horizontalArrangement =
                        Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        painter =
                            painterResource(
                                id =
                                    R.drawable.ic_shield_check
                            ),

                        contentDescription =
                            "Seguridad",

                        tint =
                            Color(0xFF38BDF8),

                        modifier =
                            Modifier.size(18.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Text(
                        text =
                            "Nunca compartiremos tu contraseña por correo.",

                        style = TextStyle(
                            fontFamily =
                                FontFamily.Monospace,

                            fontSize =
                                10.sp,

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


// =====================================================
// LOGO
// =====================================================



// =====================================================
// PREVIEW
// =====================================================

@Preview(
    showBackground = true
)
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