package com.santiagorey.devflow.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.R

// Componentes, colores y utilidades compartidos por todas las pantallas de DevFlow.

/** Paleta de colores fija de DevFlow (tema oscuro). Todas las pantallas usan estos colores. */
internal object DevFlowColors {

    val Background = Color(0xFF0B0E14)
    val Card = Color(0xFF111622)
    val DarkCard = Color(0xFF0A0D14)
    val Border = Color(0xFF1E293B)

    val Primary = Color(0xFF635BFF)
    val Cyan = Color(0xFF38BDF8)
    val BrightCyan = Color(0xFF00E5FF)
    val Purple = Color(0xFFA855F7)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)

    val TextSecondary = Color(0xFF94A3B8)
    val TextDark = Color(0xFF64748B)
}

/** Color asociado a la fase de un proyecto. */
internal fun phaseColor(progress: Int): Color =
    when {
        progress >= 100 -> DevFlowColors.Green
        progress >= 80 -> DevFlowColors.BrightCyan
        progress >= 40 -> DevFlowColors.Cyan
        progress >= 20 -> DevFlowColors.Purple
        else -> DevFlowColors.TextSecondary
    }

/** Nombre a mostrar del usuario (nombre, o la parte local del correo). */
internal fun FirebaseUser?.friendlyName(): String =
    this?.displayName?.takeIf { it.isNotBlank() }
        ?: this?.email?.substringBefore('@')
        ?: "Developer"

/** Texto amigable para los días que faltan a una entrega: "hoy", "mañana", "en 5 días", "vencido". */
internal fun daysLabel(days: Long?): String =
    when {
        days == null -> "sin fecha"
        days < 0 -> "vencido"
        days == 0L -> "hoy"
        days == 1L -> "mañana"
        else -> "en $days días"
    }

// =====================================================
// LOGO REAL DE DEVFLOW
// =====================================================

@Composable
/** Logo de DevFlow (el mismo en todas las pantallas; las esquinas crecen con el tamaño). */
fun DevFlowLogo(
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.devflow_logo),
        contentDescription = "Logo de DevFlow",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (size >= 80.dp) 18.dp else 12.dp))
    )
}

/** Tarjeta con un número destacado (título, valor grande y subtítulo). */
@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {

    DevFlowCard(
        modifier = modifier.height(95.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),

            verticalArrangement = Arrangement.SpaceBetween
        ) {

            SmallLabel(
                text = title.uppercase()
            )

            Row(
                verticalAlignment = Alignment.Bottom
            ) {

                Text(
                    text = value,
                    fontSize = 22.sp,
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = DevFlowColors.TextSecondary
                )
            }
        }
    }
}

/** Buscador con lupa. El texto lo guarda quien lo usa (state hoisting). */
@Composable
fun SearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholder: String = "Buscar cliente, empresa o teléfono..."
) {

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = DevFlowColors.TextDark,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null,
                tint = DevFlowColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = devFlowTextFieldColors()
    )
}

/** Etiqueta pequeña de color con un punto, p. ej. "● En curso". */
@Composable
fun StatusChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "● $text",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = color),
            maxLines = 1
        )
    }
}

/** Tarjeta para cuando no hay nada que mostrar, con un botón de acción opcional. */
@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    DevFlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = DevFlowColors.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

/** Barra superior con flecha de volver (apunta a la izquierda). */
@Composable
fun BackTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        color = DevFlowColors.Card,
        border = BorderStroke(1.dp, DevFlowColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            DevFlowLogo(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Campo de texto con etiqueta flotante y el estilo de DevFlow (formularios de proyecto y cliente). */
@Composable
fun DevFlowTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = devFlowTextFieldColors()
    )
}

@Composable
/** Colores de los campos de texto de DevFlow. [filled] = con fondo oscuro (formularios en tarjetas). */
internal fun devFlowTextFieldColors(filled: Boolean = false) =
    OutlinedTextFieldDefaults.colors(

        focusedContainerColor = if (filled) DevFlowColors.DarkCard else Color.Transparent,
        unfocusedContainerColor = if (filled) DevFlowColors.DarkCard else Color.Transparent,

        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,

        focusedBorderColor = DevFlowColors.Primary,
        unfocusedBorderColor = DevFlowColors.Border,

        focusedLabelColor = DevFlowColors.Cyan,
        unfocusedLabelColor = DevFlowColors.TextSecondary,

        cursorColor = DevFlowColors.Cyan
    )

/**
 * Tarjeta base de DevFlow: fondo oscuro, borde y esquinas redondeadas.
 * [content] es un slot: quien la usa pone lo que quiera adentro.
 */
@Composable
fun DevFlowCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = DevFlowColors.Card
        ),

        border = BorderStroke(
            1.dp,
            DevFlowColors.Border
        )
    ) {

        Column(
            content = content
        )
    }
}

/** Etiqueta pequeña en mayúsculas y letra monoespaciada. */
@Composable
fun SmallLabel(
    text: String,
    color: Color = DevFlowColors.TextSecondary
) {

    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
}

/**
 * Barra de navegación inferior con las 5 pestañas.
 * [selectedTab] marca la activa y [onTabSelected] avisa cuál se tocó (MainActivity.navigateToTab).
 */
@Composable
fun DevFlowBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    val tabs = listOf(
        R.drawable.ic_dashboard to "Dashboard",
        R.drawable.ic_projects to "Proyectos",
        R.drawable.ic_shield_check to "Guías",
        R.drawable.ic_users_group to "Clientes",
        R.drawable.ic_settings to "Ajustes"
    )

    Surface(
        color = DevFlowColors.DarkCard,

        border = BorderStroke(
            1.dp,
            DevFlowColors.Border
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),

            horizontalArrangement = Arrangement.SpaceAround,

            verticalAlignment = Alignment.CenterVertically
        ) {

            tabs.forEachIndexed { index, item ->

                val selected =
                    selectedTab == index

                val itemColor =
                    if (selected) {
                        DevFlowColors.Cyan
                    } else {
                        DevFlowColors.TextSecondary
                    }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            onTabSelected(index)
                        },

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        painter = painterResource(id = item.first),
                        contentDescription = null,
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = item.second,
                        color = itemColor,
                        fontSize = 10.sp,

                        fontWeight =
                            if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                    )
                }
            }
        }
    }
}
