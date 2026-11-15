package com.santiagorey.devflow.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Secciones de la política de privacidad (el mismo texto está en PRIVACY.md para Play Store). */
internal val privacySections = listOf(
    "Qué datos guardamos" to
        "Tu nombre y correo de inicio de sesión (Firebase Authentication), y los proyectos, clientes, tareas, actividad e historial del asistente que creas en la app. Se guardan en Google Firebase (Firestore) asociados a tu cuenta.",
    "Datos que se quedan en tu teléfono" to
        "Las claves de la IA que conectes y el permiso de GitHub se guardan cifrados con el Android Keystore, solo en tu dispositivo. Nunca se envían a servidores de DevFlow y no se incluyen en copias de seguridad.",
    "Inteligencia artificial" to
        "Cuando usas el asistente, el texto de tu pregunta y el contexto del proyecto (y del repositorio, si lo vinculaste) se envían directamente desde tu teléfono al proveedor que elegiste: Google (Gemini), Anthropic (Claude) u OpenAI (ChatGPT), bajo tu propia cuenta y sus políticas.",
    "GitHub" to
        "Si conectas GitHub, la app lee la información de los repositorios que elijas (commits, issues, archivos) y puede crear issues cuando tú lo pidas. Puedes desconectarlo en Ajustes cuando quieras.",
    "Datos de tus clientes" to
        "Los datos de contacto de tus clientes los ingresas tú y solo se usan para llamarles o escribirles desde la app. Eres responsable de tener su autorización.",
    "Con quién compartimos datos" to
        "No vendemos ni compartimos tus datos con terceros con fines comerciales. Solo se procesan en Firebase y en los servicios que tú conectas (IA y GitHub).",
    "Eliminar tus datos" to
        "En Ajustes → Privacidad y cuenta → Eliminar mi cuenta se borran tu cuenta y todos tus datos de Firestore de forma permanente. Las claves guardadas en el teléfono se eliminan también.",
    "Contacto" to
        "Si tienes dudas sobre tus datos, escríbenos desde la ficha de DevFlow en Google Play."
)

@Composable
fun PrivacyScreen(onBackClick: () -> Unit) {
    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = { BackTopBar(title = "Política de privacidad", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Esta política explica qué datos usa DevFlow y cómo los protegemos.",
                color = DevFlowColors.TextSecondary, fontSize = 13.sp
            )
            privacySections.forEach { (title, body) ->
                DevFlowCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(body, color = DevFlowColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}
