package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.R

// Encabezado de las pestañas: avatar, barra superior con la campana y resumen.

// =====================================================
// AVATAR DE PERFIL
// =====================================================

/** Círculo con la foto de perfil (se descarga con Coil: AsyncImage) o, si no hay foto, las iniciales. */
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

/**
 * Encabezado de las pestañas: logo, sección actual, campana de avisos y menú del perfil.
 * La campana la lee de [LocalNotificationBell], que provee MainActivity.
 */
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

/**
 * "Ambiente" de Compose con el estado de la campana: cualquier pantalla lo lee con
 * LocalNotificationBell.current sin recibirlo como parámetro.
 */
val LocalNotificationBell = compositionLocalOf { NotificationBellState() }

/** Encabezado de la pestaña Clientes: título, cuántos clientes y proyectos hay y botón para agregar. */
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

