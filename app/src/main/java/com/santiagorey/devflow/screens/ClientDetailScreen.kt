package com.santiagorey.devflow.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.formatCreatedDate
import com.santiagorey.devflow.projects.formatDeliveryDate
import com.santiagorey.devflow.ui.theme.DevFlowTheme

/**
 * Ficha de cliente (vinculada a la pestaña Clientes).
 *
 * - client == null  → formulario para agregar un cliente nuevo.
 * - client != null  → ficha con acciones para llamar / escribir y sus proyectos,
 *                     con opción de asignarlo a un proyecto existente o crear uno nuevo.
 */
@Composable
fun ClientDetailScreen(
    client: Client?,
    projects: List<Project> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveClient: (Client) -> Unit = {},
    onDeleteClient: (clientId: String) -> Unit = {},
    onAssignProject: (projectId: String, clientId: String?) -> Unit = { _, _ -> },
    onNewProjectForClient: (clientId: String) -> Unit = {},
    reportSection: @Composable () -> Unit = {}
) {
    var isEditing by rememberSaveable(client?.id) { mutableStateOf(client == null) }

    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = {
            BackTopBar(
                title = when {
                    client == null -> "Nuevo Cliente"
                    isEditing -> "Editar Cliente"
                    else -> "Ficha del Cliente"
                },
                onBackClick = {
                    if (client != null && isEditing) isEditing = false else onBackClick()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (client == null || isEditing) {
                ClientForm(
                    initial = client,
                    onSave = { saved ->
                        onSaveClient(saved)
                        if (client != null) isEditing = false
                    },
                    onCancel = { if (client != null) isEditing = false else onBackClick() }
                )
            } else {
                ClientProfile(
                    client = client,
                    projects = projects,
                    onEdit = { isEditing = true },
                    onDelete = { onDeleteClient(client.id) },
                    onAssignProject = onAssignProject,
                    onNewProject = { onNewProjectForClient(client.id) },
                    reportSection = reportSection
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// =====================================================
// FORMULARIO
// =====================================================

/** Formulario de cliente (nombre, teléfono, correo, empresa y notas) para crear o editar. */
@Composable
private fun ClientForm(
    initial: Client?,
    onSave: (Client) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(initial?.phone ?: "") }
    var email by rememberSaveable { mutableStateOf(initial?.email ?: "") }
    var company by rememberSaveable { mutableStateOf(initial?.company ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }

    val canSave = name.isNotBlank() && phone.isNotBlank()

    DevFlowCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallLabel(text = "DATOS DE CONTACTO", color = DevFlowColors.Cyan)

            DevFlowTextField(value = name, label = "Nombre del cliente *", onValueChange = { name = it })

            DevFlowTextField(
                value = phone,
                label = "Número de teléfono *",
                onValueChange = { input -> phone = input.filter { it.isDigit() || it in "+ -()" }.take(20) },
                keyboardType = KeyboardType.Phone
            )
            Text(
                text = "Incluye el código de país (ej. +57) para poder escribirle por WhatsApp.",
                style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary)
            )

            DevFlowTextField(
                value = email,
                label = "Correo (opcional)",
                onValueChange = { email = it.trim() },
                keyboardType = KeyboardType.Email
            )

            DevFlowTextField(value = company, label = "Empresa (opcional)", onValueChange = { company = it })

            DevFlowTextField(
                value = notes,
                label = "Notas (opcional)",
                onValueChange = { notes = it },
                singleLine = false,
                minLines = 2
            )
        }
    }

    Button(
        onClick = {
            if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "El correo no es válido.", Toast.LENGTH_SHORT).show()
                return@Button
            }
            val saved = (initial ?: Client(name = "")).copy(
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                company = company.trim(),
                notes = notes.trim()
            )
            onSave(saved)
        },
        enabled = canSave,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DevFlowColors.Primary,
            disabledContainerColor = DevFlowColors.Border
        )
    ) {
        Text(
            text = if (initial == null) "Guardar cliente" else "Guardar cambios",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (canSave) Color.White else DevFlowColors.TextSecondary
        )
    }

    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text("Cancelar", color = DevFlowColors.TextSecondary)
    }
}

// =====================================================
// FICHA
// =====================================================

/** Ficha del cliente: datos, botones de contacto, sus proyectos, informe con IA y eliminar. */
@Composable
private fun ClientProfile(
    client: Client,
    projects: List<Project>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssignProject: (projectId: String, clientId: String?) -> Unit,
    onNewProject: () -> Unit,
    reportSection: @Composable () -> Unit
) {
    val context = LocalContext.current
    var showAssignDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val clientProjects = projects.filter { it.clientId == client.id }

    // Cabecera
    DevFlowCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClientAvatar(name = client.name)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (client.company.isNotBlank()) {
                        Text(client.company, style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary))
                    }
                    Text("Cliente desde ${formatCreatedDate(client.createdAt)}", style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextDark))
                }
                Text(
                    text = "Editar",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DevFlowColors.Cyan),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onEdit() }
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            InfoRow(iconRes = R.drawable.ic_phone, text = client.phone.ifBlank { "Sin número" })
            if (client.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                InfoRow(iconRes = R.drawable.ic_email, text = client.email)
            }
            if (client.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(client.notes, style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary, lineHeight = 16.sp))
            }
        }
    }

    // Acciones de contacto
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContactActionButton(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_phone,
            label = "Llamar",
            onClick = { ContactActions.call(context, client.phone) }
        )
        ContactActionButton(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_chat,
            label = "SMS",
            onClick = { ContactActions.sms(context, client.phone) }
        )
        ContactActionButton(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_chat,
            label = "WhatsApp",
            onClick = { ContactActions.whatsApp(context, client.phone) }
        )
    }
    if (client.email.isNotBlank()) {
        ContactActionButton(
            modifier = Modifier.fillMaxWidth(),
            iconRes = R.drawable.ic_email,
            label = "Enviar correo",
            onClick = { ContactActions.email(context, client.email) }
        )
    }

    // Informe de avance redactado por la IA
    reportSection()

    // Proyectos del cliente
    Text(
        text = "Proyectos (${clientProjects.size})",
        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    )

    if (clientProjects.isEmpty()) {
        Text(
            text = "Este cliente aún no tiene proyectos. Asígnalo a uno existente o crea uno nuevo.",
            style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary)
        )
    } else {
        clientProjects.forEach { project ->
            ClientProjectRow(
                project = project,
                onRemove = { onAssignProject(project.id, null) }
            )
        }
    }

    Button(
        onClick = onNewProject,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
    ) {
        Text("+ Crear proyecto nuevo", fontWeight = FontWeight.Bold, color = Color.White)
    }

    OutlinedButton(
        onClick = {
            if (projects.any { it.clientId != client.id }) {
                showAssignDialog = true
            } else {
                Toast.makeText(context, "No hay otros proyectos creados para asignar.", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DevFlowColors.Border)
    ) {
        Text("Agregar a proyecto existente", fontWeight = FontWeight.Bold, color = DevFlowColors.Cyan)
    }

    Text(
        text = if (confirmDelete) "Toca de nuevo para eliminar el cliente" else "Eliminar cliente",
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DevFlowColors.Red),
        modifier = Modifier
            .clickable { if (confirmDelete) onDelete() else confirmDelete = true }
            .padding(vertical = 8.dp)
    )

    if (showAssignDialog) {
        AssignProjectDialog(
            projects = projects.filter { it.clientId != client.id },
            onDismiss = { showAssignDialog = false },
            onSelect = { project ->
                onAssignProject(project.id, client.id)
                showAssignDialog = false
                Toast.makeText(context, "${client.name} asignado a ${project.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/** Fila con icono y un dato del cliente (teléfono, correo…). */
@Composable
private fun InfoRow(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = TextStyle(fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace))
    }
}

/** Botón de contacto (Llamar, WhatsApp, Correo) con icono y texto. */
@Composable
private fun ContactActionButton(
    modifier: Modifier,
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DevFlowColors.Card)
            .border(1.dp, DevFlowColors.Border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1)
    }
}

/** Proyecto asignado al cliente, con opción de quitarlo. */
@Composable
private fun ClientProjectRow(project: Project, onRemove: () -> Unit) {
    val color = phaseColor(project.progress)
    DevFlowCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.name,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(text = project.phase.name.uppercase(), color = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = DevFlowColors.Border
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${project.progress}% • Entrega: ${formatDeliveryDate(project.deliveryDateMillis)}",
                    style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Quitar",
                    style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable { onRemove() }
                        .padding(4.dp)
                )
            }
        }
    }
}

/** Diálogo para asignar al cliente uno de los proyectos existentes. */
@Composable
private fun AssignProjectDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSelect: (Project) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("Elegir proyecto", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projects.forEach { project ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DevFlowColors.DarkCard)
                            .border(1.dp, DevFlowColors.Border, RoundedCornerShape(10.dp))
                            .clickable { onSelect(project) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (project.clientId != null) "Ya tiene otro cliente (se reemplazará)" else "Sin cliente asignado",
                                color = DevFlowColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DevFlowColors.TextSecondary)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ClientDetailPreview() {
    DevFlowTheme {
        ClientDetailScreen(client = Client(name = "Cliente de ejemplo", phone = "+57 300 000 0000"))
    }
}
