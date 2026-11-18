package com.santiagorey.devflow.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R

// Piezas comunes de Login, Registro y Recuperar contraseña (mismo estilo oscuro).

/** Colores de las pantallas de login, registro y recuperar contraseña. */
internal object AuthColors {
    val Background = Color(0xFF0B0E14)
    val Card = Color(0xFF111622)
    val Field = Color(0xFF0A0D14)
    val Border = Color(0xFF1E293B)
    val Primary = Color(0xFF635BFF)
    val PrimaryDisabled = Color(0xFF3E3A80)
    val Accent = Color(0xFF38BDF8)
    val TextMuted = Color(0xFF94A3B8)
    val Icon = Color(0xFF64748B)
    val Placeholder = Color(0xFF475569)
}

/** Fondo oscuro con la columna desplazable centrada. */
@Composable
internal fun AuthScaffold(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxSize(), color = AuthColors.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/** Título grande centrado ("Bienvenido a DevFlow", "Crear cuenta"…). */
@Composable
internal fun AuthTitle(text: String, fontSize: TextUnit) {
    Text(
        text = text,
        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = fontSize, color = Color.White),
        textAlign = TextAlign.Center
    )
}

/** Texto gris centrado debajo del título. */
@Composable
internal fun AuthSubtitle(text: String, lineHeight: TextUnit = 18.sp) {
    Text(
        text = text,
        style = TextStyle(fontSize = 13.sp, color = AuthColors.TextMuted, lineHeight = lineHeight),
        textAlign = TextAlign.Center
    )
}

/** Tarjeta principal del formulario. */
@Composable
internal fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuthColors.Card),
        border = BorderStroke(1.dp, AuthColors.Border)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/** Etiqueta en mayúsculas sobre cada campo. */
@Composable
internal fun AuthLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = AuthColors.TextMuted,
            letterSpacing = 0.5.sp
        )
    )
}

/** Colores del campo de texto: fondo oscuro, borde morado al enfocar y rojo si hay error. */
@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AuthColors.Field,
    unfocusedContainerColor = AuthColors.Field,
    disabledContainerColor = AuthColors.Field,
    focusedBorderColor = AuthColors.Primary,
    unfocusedBorderColor = AuthColors.Border,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AuthColors.Accent,
    errorBorderColor = Color(0xFFEF4444),
    errorCursorColor = Color(0xFFEF4444),
    errorSupportingTextColor = Color(0xFFF87171),
    disabledTextColor = AuthColors.Icon
)

/**
 * Campo de texto del formulario. Si [onToggleVisible] no es null, es un campo de contraseña
 * con el botón del ojo. Con [showErrors] = false no se reserva espacio para el error.
 */
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes iconRes: Int,
    iconDescription: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    showErrors: Boolean = true,
    passwordVisible: Boolean = false,
    onToggleVisible: (() -> Unit)? = null
) {
    val isPassword = onToggleVisible != null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = { Text(text = placeholder, color = AuthColors.Placeholder, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = iconDescription,
                tint = AuthColors.Icon,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onToggleVisible?.invoke() }) {
                    Icon(
                        painter = painterResource(id = if (passwordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off),
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = AuthColors.Icon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            null
        },
        supportingText = if (showErrors) {
            { error?.let { Text(it) } }
        } else {
            null
        },
        isError = error != null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(12.dp),
        colors = authTextFieldColors()
    )
}

/** Botón principal morado con flecha (o rueda de carga). */
@Composable
internal fun AuthPrimaryButton(text: String, isLoading: Boolean, height: Dp = 50.dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthColors.Primary,
            disabledContainerColor = AuthColors.PrimaryDisabled
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text = text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Botón secundario (GitHub, Google) con icono. */
@Composable
internal fun RowScope.AuthSocialButton(
    text: String,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AuthColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = AuthColors.Field)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White))
        }
    }
}

/** "¿No tienes cuenta? Crear cuenta" y similares. */
@Composable
internal fun AuthFooterLink(
    prompt: String,
    action: String,
    actionColor: Color = AuthColors.Accent,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = prompt, style = TextStyle(fontSize = 13.sp, color = AuthColors.TextMuted))
        Text(
            text = action,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = actionColor,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
        )
    }
}

/** Tarjeta pequeña con el escudo y un mensaje de seguridad. */
@Composable
internal fun AuthSecurityNote(
    text: String,
    iconDescription: String,
    fontSize: TextUnit,
    fontWeight: FontWeight? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AuthColors.Card),
        border = BorderStroke(1.dp, AuthColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shield_check),
                contentDescription = iconDescription,
                tint = AuthColors.Accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    color = AuthColors.TextMuted
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
