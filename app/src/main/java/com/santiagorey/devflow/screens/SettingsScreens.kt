package com.santiagorey.devflow.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.R
import com.santiagorey.devflow.auth.PROVIDER_GITHUB
import com.santiagorey.devflow.auth.PROVIDER_GOOGLE
import com.santiagorey.devflow.ui.theme.DevFlowTheme

private const val XP_PER_LEVEL = 500

private val levelTitles = listOf(
    "Junior en formación",
    "Desarrollador Junior",
    "Semi-Senior en formación",
    "Desarrollador Mid-Level",
    "Desarrollador Senior"
)

@Composable
fun SettingsScreens(
    currentUser: FirebaseUser? = null,
    providerIds: Set<String> = emptySet(),
    githubUsername: String? = null,
    linkingProvider: String? = null,
    isTempSession: Boolean = false,
    experience: Int = 0,
    projectCount: Int = 0,
    completedCount: Int = 0,
    clientCount: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onConnectGitHub: () -> Unit = {},
    onConnectGoogle: () -> Unit = {},
    onDisconnect: (providerId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Preferencias
    var autoFormat by rememberSaveable { mutableStateOf(true) }
    var cicdAlerts by rememberSaveable { mutableStateOf(true) }
    var coachLive by rememberSaveable { mutableStateOf(true) }
    var strictQuality by rememberSaveable { mutableStateOf(false) }
    var selectedEditor by rememberSaveable { mutableStateOf("VS Code") }

    // Nivel calculado a partir de la experiencia real
    val level = experience / XP_PER_LEVEL + 1
    val xpInLevel = experience % XP_PER_LEVEL
    val levelTitle = levelTitles[(level - 1).coerceAtMost(levelTitles.lastIndex)]

    val connectedCount = listOf(PROVIDER_GITHUB, PROVIDER_GOOGLE).count { it in providerIds }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DevFlowColors.Background,
        bottomBar = {
            DevFlowBottomBar(
                selectedTab = 4, // Pestaña Ajustes
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopHeaderBar(
                currentUser = currentUser,
                onLogoutClick = onLogoutClick,
                section = "AJUSTES"
            )

            Column {
                Text(
                    text = "Ajustes",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                )
                Text(
                    text = "Perfil, preferencias e integraciones",
                    style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary)
                )
            }

            // Perfil
            DevFlowCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            photoUrl = currentUser?.photoUrl?.toString(),
                            displayName = currentUser?.displayName ?: currentUser?.email,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.friendlyName(),
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUser?.email ?: if (isTempSession) "Sesión temporal de desarrollo" else "",
                                style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (githubUsername != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("@$githubUsername", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.Cyan))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Experiencia (dos líneas para que no se comprima en el teléfono)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NIVEL $level",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$xpInLevel / $XP_PER_LEVEL XP",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.Cyan),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = levelTitle,
                        style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { xpInLevel / XP_PER_LEVEL.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = DevFlowColors.Cyan,
                        trackColor = DevFlowColors.Border
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (experience == 0) {
                            "Gana XP creando proyectos (+100), agregando clientes (+50) y completando entregas (+200)."
                        } else {
                            "Te faltan ${XP_PER_LEVEL - xpInLevel} XP para el nivel ${level + 1}."
                        },
                        style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("INSIGNIAS & LOGROS", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.TextSecondary))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgeItem(modifier = Modifier.weight(1f), iconRes = R.drawable.ic_folder, title = "Primer proyecto", subtitle = "$projectCount / 1", unlocked = projectCount >= 1)
                        BadgeItem(modifier = Modifier.weight(1f), iconRes = R.drawable.ic_users_group, title = "3 clientes", subtitle = "$clientCount / 3", unlocked = clientCount >= 3)
                        BadgeItem(modifier = Modifier.weight(1f), iconRes = R.drawable.ic_shield_check, title = "Primera entrega", subtitle = "$completedCount / 1", unlocked = completedCount >= 1)
                    }
                }
            }

            // Preferencias de Desarrollo
            SectionHeader(title = "</> Preferencias de Desarrollo")
            DevFlowCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EDITOR DE CÓDIGO PREDETERMINADO", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DevFlowColors.TextSecondary))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cursor", "VS Code", "WebStorm").forEach { editor ->
                            val isSel = editor == selectedEditor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DevFlowColors.Primary else DevFlowColors.Background)
                                    .border(1.dp, if (isSel) DevFlowColors.Cyan else DevFlowColors.Border, RoundedCornerShape(8.dp))
                                    .clickable { selectedEditor = editor }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(editor, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White), maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ToggleSettingRow(
                        iconRes = R.drawable.ic_settings,
                        title = "Auto-Formato & Linter al Guardar",
                        subtitle = "ESLint + Prettier en pre-commit hooks",
                        checked = autoFormat,
                        onCheckedChange = { autoFormat = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ToggleSettingRow(
                        iconRes = R.drawable.ic_bell,
                        title = "Alertas CI/CD & Vulnerabilidades",
                        subtitle = "Notificaciones en fallo de tests o CVEs",
                        checked = cicdAlerts,
                        onCheckedChange = { cicdAlerts = it }
                    )
                }
            }

            // Integraciones de Desarrollo
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚙ Integraciones",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (connectedCount == 0) "NINGUNA ACTIVA" else "$connectedCount ACTIVA${if (connectedCount > 1) "S" else ""}",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevFlowColors.Cyan)
                )
            }
            DevFlowCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    IntegrationRow(
                        iconRes = R.drawable.ic_github,
                        title = "GitHub",
                        subtitle = if (PROVIDER_GITHUB in providerIds) {
                            githubUsername?.let { "Conectado como @$it" } ?: "Cuenta conectada"
                        } else {
                            "Inicia sesión con GitHub para vincular tus repositorios"
                        },
                        connected = PROVIDER_GITHUB in providerIds,
                        loading = linkingProvider == PROVIDER_GITHUB,
                        onConnect = onConnectGitHub,
                        onDisconnect = { onDisconnect(PROVIDER_GITHUB) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    IntegrationRow(
                        iconRes = R.drawable.ic_google,
                        iconTint = Color.Unspecified,
                        title = "Google",
                        subtitle = if (PROVIDER_GOOGLE in providerIds) {
                            currentUser?.providerData?.firstOrNull { it.providerId == PROVIDER_GOOGLE }?.email ?: "Cuenta conectada"
                        } else {
                            "Inicia sesión con tu cuenta de Google"
                        },
                        connected = PROVIDER_GOOGLE in providerIds,
                        loading = linkingProvider == PROVIDER_GOOGLE,
                        onConnect = onConnectGoogle,
                        onDisconnect = { onDisconnect(PROVIDER_GOOGLE) }
                    )
                    if (isTempSession) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Estás en la sesión temporal: inicia sesión con una cuenta real para conectar integraciones.",
                            style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary)
                        )
                    }
                }
            }

            // Normativas & Asistente Junior
            SectionHeader(title = "🎓 Normativas & Asistente Junior")
            DevFlowCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("DevFlow Coach en Vivo", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White))
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DevFlowColors.Purple.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("PEDAGÓGICO", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = DevFlowColors.Purple))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Muestra consejos de buenas prácticas mientras trabajas.", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary))
                        }
                        Switch(
                            checked = coachLive,
                            onCheckedChange = { coachLive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DevFlowColors.Purple)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ToggleSettingRow(
                        iconRes = R.drawable.ic_shield_check,
                        title = "Modo Estricto de Calidad",
                        subtitle = "Pide tests o documentación antes de dar una tarea por completada.",
                        checked = strictQuality,
                        onCheckedChange = { strictQuality = it }
                    )
                }
            }

            // Cuenta
            SectionHeader(title = "🛡 Cuenta")
            DevFlowCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
                            onLogoutClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF450A0A)),
                        border = BorderStroke(1.dp, Color(0xFF991B1B))
                    ) {
                        Text("Cerrar sesión", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF87171)))
                    }
                }
            }

            Text(
                text = "DevFlow v1.0 • Developer Edition",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.TextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    )
}

@Composable
private fun BadgeItem(modifier: Modifier, iconRes: Int, title: String, subtitle: String, unlocked: Boolean) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DevFlowColors.Background)
            .border(1.dp, if (unlocked) DevFlowColors.Cyan else DevFlowColors.Border, RoundedCornerShape(10.dp))
            .padding(10.dp)
            .alpha(if (unlocked) 1f else 0.45f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = if (unlocked) DevFlowColors.Cyan else DevFlowColors.TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White), textAlign = TextAlign.Center, maxLines = 2)
            Text(subtitle, style = TextStyle(fontSize = 9.sp, color = DevFlowColors.TextSecondary), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ToggleSettingRow(iconRes: Int, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White))
                Text(subtitle, style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DevFlowColors.Cyan)
        )
    }
}

@Composable
private fun IntegrationRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    connected: Boolean,
    loading: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    iconTint: Color = Color.White
) {
    var confirmDisconnect by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DevFlowColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White))
                if (connected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("● CONECTADO", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = DevFlowColors.Green))
                }
            }
            Text(subtitle, style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))

        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = DevFlowColors.Cyan
            )

            connected -> OutlinedButton(
                onClick = {
                    if (confirmDisconnect) {
                        confirmDisconnect = false
                        onDisconnect()
                    } else {
                        confirmDisconnect = true
                    }
                },
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DevFlowColors.Border)
            ) {
                Text(
                    text = if (confirmDisconnect) "¿Seguro?" else "Desconectar",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (confirmDisconnect) DevFlowColors.Red else DevFlowColors.TextSecondary),
                    maxLines = 1
                )
            }

            else -> Button(
                onClick = onConnect,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) {
                Text("Conectar", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreensPreview() {
    DevFlowTheme {
        SettingsScreens()
    }
}
