package com.santiagorey.devflow.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.R
import com.santiagorey.devflow.projects.Client

// Componentes, colores y utilidades compartidos por todas las pantallas de DevFlow.

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


// =====================================================
// AVATAR DE PERFIL
// =====================================================

@Composable
fun ProfileAvatar(
    photoUrl: String? = null,
    displayName: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val initials = remember(displayName) {

        if (!displayName.isNullOrBlank()) {

            val parts = displayName
                .trim()
                .split(" ")
                .filter { it.isNotBlank() }

            if (parts.size >= 2) {

                "${parts.first().take(1)}${parts.last().take(1)}"
                    .uppercase()

            } else {

                displayName
                    .take(2)
                    .uppercase()
            }

        } else {

            "SR"
        }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        DevFlowColors.Primary,
                        DevFlowColors.Purple
                    )
                )
            )
            .border(
                width = 1.5.dp,

                brush = Brush.linearGradient(
                    listOf(
                        DevFlowColors.Cyan,
                        DevFlowColors.Primary
                    )
                ),

                shape = CircleShape
            )
            .clickable {
                onClick()
            },

        contentAlignment = Alignment.Center
    ) {

        if (!photoUrl.isNullOrBlank()) {

            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

        } else {

            Text(
                text = initials,

                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            )
        }
    }
}


@Composable
fun TopHeaderBar(
    currentUser: FirebaseUser? = null,
    onLogoutClick: () -> Unit = {},
    section: String = "DASHBOARD"
) {

    var showMenu by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            DevFlowLogo(
                size = 54.dp
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Row {

                    Text(
                        text = "Dev",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Flow",
                        color = DevFlowColors.Cyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                SmallLabel(
                    text = section
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {

        // Campana de notificaciones (avisos de entregas, CI y seguridad)
        val bell = LocalNotificationBell.current
        if (bell.onClick != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { bell.onClick.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bell),
                    contentDescription = "Notificaciones",
                    tint = if (bell.count > 0) Color.White else DevFlowColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                if (bell.count > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(DevFlowColors.Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (bell.count > 9) "9+" else bell.count.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box {

            ProfileAvatar(
                photoUrl = currentUser
                    ?.photoUrl
                    ?.toString(),

                displayName = currentUser?.displayName
                    ?: currentUser?.email,

                onClick = {
                    showMenu = true
                }
            )

            DropdownMenu(
                expanded = showMenu,

                onDismissRequest = {
                    showMenu = false
                }
            ) {

                DropdownMenuItem(
                    text = {

                        Column {

                            Text(
                                text = currentUser?.displayName
                                    ?: "Usuario DevFlow",

                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )

                            currentUser?.email?.let { email ->

                                Text(
                                    text = email,

                                    style = TextStyle(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    },

                    onClick = {
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = {

                        Text(
                            text = "Cerrar sesión",
                            color = Color.Red
                        )
                    },

                    onClick = {

                        showMenu = false

                        onLogoutClick()
                    }
                )
            }
        }
        }
    }
}

/** Estado de la campana del encabezado (lo provee MainActivity para todas las pestañas). */
data class NotificationBellState(
    val count: Int = 0,
    val onClick: (() -> Unit)? = null
)

val LocalNotificationBell = compositionLocalOf { NotificationBellState() }



@Composable
fun HeaderSection(
    clientCount: Int,
    projectCount: Int,
    onAddClientClick: () -> Unit
) {

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Clientes",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Gestiona tus clientes y sus proyectos",
                    fontSize = 12.sp,
                    color = DevFlowColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddClientClick,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DevFlowColors.Primary
                )
            ) {
                Text(
                    text = "+ Agregar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Clientes",
                value = clientCount.toString(),
                subtitle = "registrados"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Proyectos",
                value = projectCount.toString(),
                subtitle = "asignados"
            )
        }
    }
}


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


@Composable
fun EmptyClientsCard(
    searchActive: Boolean
) {

    DevFlowCard {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 25.dp,
                    vertical = 32.dp
                ),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (searchActive) {
                    "No encontramos clientes"
                } else {
                    "No tienes Clientes por el momento"
                },

                fontSize = 19.sp,

                fontWeight = FontWeight.Bold,

                color = Color.White,

                textAlign = TextAlign.Center
            )

            if (searchActive) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Prueba buscando con otro nombre.",
                    color = DevFlowColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Composable
fun ClientCard(
    client: Client,
    projectCount: Int,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {

    DevFlowCard(
        modifier = Modifier.clickable { onClick() }
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                ClientAvatar(name = client.name)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = client.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = listOf(client.company, client.phone)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                            .ifBlank { "Sin datos de contacto" },
                        color = DevFlowColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (client.phone.isNotBlank()) {
                    ContactIconButton(iconRes = R.drawable.ic_phone, description = "Llamar", onClick = onCall)
                    Spacer(modifier = Modifier.width(6.dp))
                    ContactIconButton(iconRes = R.drawable.ic_chat, description = "Escribir", onClick = onMessage)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DevFlowColors.DarkCard)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (projectCount) {
                        0 -> "Sin proyectos asignados"
                        1 -> "1 proyecto asignado"
                        else -> "$projectCount proyectos asignados"
                    },
                    color = if (projectCount == 0) DevFlowColors.TextSecondary else Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Ver ficha →",
                    color = DevFlowColors.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun ContactIconButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    tint: Color = DevFlowColors.Cyan
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DevFlowColors.DarkCard)
            .border(1.dp, DevFlowColors.Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}


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
fun ClientAvatar(
    name: String
) {

    val initials = remember(name) {

        val parts = name
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        when {

            parts.size >= 2 ->
                "${parts.first().take(1)}${parts.last().take(1)}"
                    .uppercase()

            parts.isNotEmpty() ->
                parts.first()
                    .take(2)
                    .uppercase()

            else ->
                "CL"
        }
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        DevFlowColors.Cyan,
                        DevFlowColors.Primary
                    )
                )
            ),

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
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
