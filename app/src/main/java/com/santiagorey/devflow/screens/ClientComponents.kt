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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.projects.Client

// Tarjetas, avatar y botones de contacto de los clientes.

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

