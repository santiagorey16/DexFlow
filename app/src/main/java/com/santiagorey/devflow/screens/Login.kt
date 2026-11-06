package com.santiagorey.devflow.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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

/**
 * Stateful wrapper component that manages the UI state for [LoginScreen].
 */
@Composable
fun StatefulLoginScreen(
    onForgotPasswordClick: () -> Unit = {},
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onGitHubClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {}
) {
    val context = LocalContext.current
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
        onForgotPasswordClick = {
            onForgotPasswordClick()
            Toast.makeText(context, "Solicitud de contraseña enviada", Toast.LENGTH_SHORT).show()
        },
        onLoginClick = {
            onLoginClick(email, password)
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Iniciando sesión con $email", Toast.LENGTH_SHORT).show()
            }
        },
        onGitHubClick = {
            onGitHubClick()
            Toast.makeText(context, "Iniciando sesión con GitHub...", Toast.LENGTH_SHORT).show()
        },
        onGoogleClick = {
            onGoogleClick()
            Toast.makeText(context, "Iniciando sesión con Google...", Toast.LENGTH_SHORT).show()
        },
        onCreateAccountClick = {
            onCreateAccountClick()
            Toast.makeText(context, "Redirigiendo a registro gratis...", Toast.LENGTH_SHORT).show()
        }
    )
}

/**
 * Stateless LoginScreen composable adhering to Compose state hoisting principles.
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0B0E14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Logo
            DevFlowLogo(
                size = 84.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Version Badge
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0F2937))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF155E75),
                        shape = CircleShape
                    )
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
                    text = "DEVELOPER EDITION",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Title
            Text(
                text = "Bienvenido a DevFlow",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Gestiona el ciclo de vida de tu software desde la\nidea hasta el despliegue y mantenimiento",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111622)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF1E293B)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Email Label Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CORREO ELECTRÓNICO",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "dev@identity",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email Input Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "alex.dev@ejemplo.com",
                                color = Color(0xFF475569),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_email),
                                contentDescription = "Email",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0D14),
                            unfocusedContainerColor = Color(0xFF0A0D14),
                            focusedBorderColor = Color(0xFF635BFF),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Password Label Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONTRASEÑA",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.clickable { onForgotPasswordClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Password Input Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "••••••••••••",
                                color = Color(0xFF475569),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock),
                                contentDescription = "Password",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                                Icon(
                                    painter = painterResource(id = if (passwordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off),
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0D14),
                            unfocusedContainerColor = Color(0xFF0A0D14),
                            focusedBorderColor = Color(0xFF635BFF),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Iniciar Sesion Button
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF635BFF)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Iniciar sesión",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_forward),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider with "o continúa con"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "o continúa con",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Social Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // GitHub Button
                        OutlinedButton(
                            onClick = onGitHubClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF0A0D14)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_github),
                                    contentDescription = "GitHub",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // Google Button
                        OutlinedButton(
                            onClick = onGoogleClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF0A0D14)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = "Google",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Create Account Prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿No tienes cuenta? ",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                        Text(
                            text = "Crear cuenta gratis",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textDecoration = TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable { onCreateAccountClick() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Security Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111622)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF1E293B)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield_check),
                        contentDescription = "Shield",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cifrado de extremo a extremo & sincronización Git",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Custom Logo component that replicates Image 2 with precision.
 */
@Composable
fun DevFlowLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFF38BDF8),
                spotColor = Color(0xFFA855F7)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF060911)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                        Color(0xFF635BFF).copy(alpha = 0.3f),
                        Color(0xFFA855F7).copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.toPx() - 16.dp.toPx()
            val h = size.toPx() - 16.dp.toPx()

            // Main D shape path
            val dPath = Path().apply {
                val left = w * 0.28f
                val top = h * 0.12f
                val right = w * 0.74f
                val bottom = h * 0.62f
                val rx = w * 0.22f

                moveTo(left, top)
                lineTo(right - rx, top)
                cubicTo(
                    right, top,
                    right, bottom,
                    right - rx, bottom
                )
                lineTo(left, bottom)
                close()
            }

            // D Outer Gradient
            drawPath(
                path = dPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF3B82F6),
                        Color(0xFFA855F7)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Orbital Swoosh Ring
            val orbitPath = Path().apply {
                moveTo(w * 0.10f, h * 0.60f)
                cubicTo(
                    w * 0.15f, h * 0.35f,
                    w * 0.88f, h * 0.25f,
                    w * 0.90f, h * 0.42f
                )
                cubicTo(
                    w * 0.92f, h * 0.58f,
                    w * 0.25f, h * 0.72f,
                    w * 0.10f, h * 0.60f
                )
            }
            drawPath(
                path = orbitPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7))
                ),
                style = Stroke(width = w * 0.05f)
            )

            // Code symbol </ >
            val codePath = Path().apply {
                // <
                moveTo(w * 0.38f, h * 0.33f)
                lineTo(w * 0.32f, h * 0.37f)
                lineTo(w * 0.38f, h * 0.41f)

                // /
                moveTo(w * 0.49f, h * 0.31f)
                lineTo(w * 0.43f, h * 0.43f)

                // >
                moveTo(w * 0.54f, h * 0.33f)
                lineTo(w * 0.60f, h * 0.37f)
                lineTo(w * 0.54f, h * 0.41f)
            }
            drawPath(
                path = codePath,
                color = Color.White,
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // DevFlow Text & Line at bottom of logo box
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Dev",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                )
                Text(
                    text = "Flow",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7))
                        )
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7))
                        )
                    )
            )
        }
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
