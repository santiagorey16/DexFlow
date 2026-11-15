package com.santiagorey.devflow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.ai.AssistantViewModel
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project

/** Informe de avance para el cliente, redactado por la IA y listo para enviar. */
@Composable
fun ClientReportCard(
    assistant: AssistantViewModel,
    client: Client,
    projects: List<Project>,
    onConnectAi: () -> Unit
) {
    val context = LocalContext.current
    // Al salir de la ficha se descarta el informe (es de este cliente).
    DisposableEffect(client.id) { onDispose { assistant.clearClientReport() } }

    DevFlowCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📨", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Informe de avance con IA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "La IA redacta un resumen de los proyectos de ${client.name} para enviárselo.",
                        color = DevFlowColors.TextSecondary, fontSize = 11.sp
                    )
                }
            }

            val report = assistant.clientReport
            when {
                !assistant.isConfigured -> TextButton(onClick = onConnectAi) {
                    Text("Conectar mi IA para usar esta función", color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold)
                }
                assistant.isGeneratingReport -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DevFlowColors.Purple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redactando informe…", color = DevFlowColors.TextSecondary, fontSize = 12.sp)
                }
                report == null -> Button(
                    onClick = { assistant.generateClientReport(client, projects) },
                    enabled = projects.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Purple)
                ) {
                    Text(if (projects.isEmpty()) "Asigna un proyecto para generar el informe" else "✨ Generar informe", fontWeight = FontWeight.Bold)
                }
                else -> {
                    SelectionContainer {
                        Text(
                            report,
                            color = Color.White, fontSize = 13.sp, lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DevFlowColors.Background)
                                .padding(12.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (client.phone.isNotBlank()) {
                            Button(
                                onClick = { ContactActions.whatsAppMessage(context, client.phone, report) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) { Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                        if (client.email.isNotBlank()) {
                            Button(
                                onClick = { ContactActions.emailMessage(context, client.email, "Informe de avance", report) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
                            ) { Text("Correo", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                        Button(
                            onClick = { ContactActions.share(context, report) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Border)
                        ) { Text("Compartir", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }
                    TextButton(onClick = { assistant.generateClientReport(client, projects) }) {
                        Text("↻ Generar otra versión", color = DevFlowColors.Cyan, fontSize = 12.sp)
                    }
                }
            }
            assistant.clientReportError?.let { Text(it, color = DevFlowColors.Red, fontSize = 12.sp) }
        }
    }
}
