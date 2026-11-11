package com.santiagorey.devflow.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.projects.Client
import com.santiagorey.devflow.projects.Project
import com.santiagorey.devflow.projects.formatDeliveryDate
import com.santiagorey.devflow.projects.initialProgressFor
import com.santiagorey.devflow.ui.theme.DevFlowTheme
import java.time.LocalDate
import java.time.ZoneOffset

private val stackOptions = listOf("Frontend Web", "API / Backend", "Base de Datos", "App Móvil")

/**
 * Asistente "Nuevo Proyecto" (Paso 1 a 5). Todos los campos empiezan vacíos.
 *
 * @param preselectedClientId cliente ya elegido (cuando se llega desde la ficha de un cliente).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    clients: List<Client> = emptyList(),
    preselectedClientId: String? = null,
    newlyCreatedClientId: String? = null,
    onBackClick: () -> Unit = {},
    onAddClientClick: () -> Unit = {},
    onCreateProject: (Project) -> Unit = {}
) {
    val context = LocalContext.current

    // Form states (vacíos por defecto)
    var projectName by rememberSaveable { mutableStateOf("") }
    var projectDescription by rememberSaveable { mutableStateOf("") }
    var selectedClientId by rememberSaveable { mutableStateOf(preselectedClientId) }
    var selectedStartingPoint by rememberSaveable { mutableIntStateOf(0) } // 0: Análisis, 1: Diseño, 2: Código
    var selectedStack by rememberSaveable { mutableStateOf(listOf<String>()) }
    var deliveryDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var budget by rememberSaveable { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var clientMenuOpen by remember { mutableStateOf(false) }

    // Cliente recién creado desde "+ Nuevo": se selecciona automáticamente
    LaunchedEffect(newlyCreatedClientId) {
        if (newlyCreatedClientId != null) selectedClientId = newlyCreatedClientId
    }

    val selectedClient = clients.firstOrNull { it.id == selectedClientId }

    // Pasos completados para la barra de progreso superior
    val completedSteps = listOf(
        projectName.isNotBlank(),
        selectedClient != null,
        true, // el punto de partida siempre tiene un valor
        selectedStack.isNotEmpty(),
        deliveryDateMillis != null || budget.isNotBlank()
    ).count { it }

    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = {
            BackTopBar(title = "Nuevo Proyecto", onBackClick = onBackClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Header Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURACIÓN ASISTIDA",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DevFlowColors.Cyan)
                )
                Text(
                    text = "$completedSteps de 5 listos",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.TextSecondary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index < completedSteps) DevFlowColors.Cyan else DevFlowColors.Border)
                    )
                }
            }

            // Paso 1: Información básica
            WizardStepCard(iconRes = R.drawable.ic_folder, step = "PASO 1", title = "Información básica") {
                FieldLabel("NOMBRE DEL PROYECTO *")
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Ej. App de reservas", color = DevFlowColors.TextDark, fontSize = 13.sp) },
                    colors = wizardFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FieldLabel("DESCRIPCIÓN BREVE")
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("¿Qué se va a construir?", color = DevFlowColors.TextDark, fontSize = 13.sp) },
                    colors = wizardFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Paso 2: Cliente
            WizardStepCard(
                iconRes = R.drawable.ic_users_group,
                step = "PASO 2",
                title = "Cliente o Stakeholder",
                action = "+ Nuevo",
                onAction = onAddClientClick
            ) {
                Box {
                    PickerField(
                        text = selectedClient?.let { c ->
                            if (c.company.isNotBlank()) "${c.name} (${c.company})" else c.name
                        } ?: "",
                        placeholder = if (clients.isEmpty()) "No tienes clientes aún" else "Selecciona un cliente",
                        iconRes = R.drawable.ic_users_group,
                        onClick = {
                            if (clients.isEmpty()) onAddClientClick() else clientMenuOpen = true
                        }
                    )

                    DropdownMenu(
                        expanded = clientMenuOpen,
                        onDismissRequest = { clientMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin cliente", color = DevFlowColors.TextSecondary) },
                            onClick = {
                                selectedClientId = null
                                clientMenuOpen = false
                            }
                        )
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(client.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        if (client.company.isNotBlank()) {
                                            Text(client.company, color = DevFlowColors.TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedClientId = client.id
                                    clientMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(id = R.drawable.ic_shield_check), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (clients.isEmpty()) "Toca \"+ Nuevo\" para registrar a tu primer cliente." else "Podrás llamarlo o escribirle desde su ficha.",
                        style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary)
                    )
                }
            }

            // Paso 3: Punto de partida
            WizardStepCard(iconRes = R.drawable.ic_tasks, step = "PASO 3", title = "Punto de partida", accent = DevFlowColors.Purple) {
                StartingPointOption(
                    selected = selectedStartingPoint == 0,
                    title = "Análisis de requisitos",
                    description = "Levantamiento de especificaciones técnicas, user stories y criterios de aceptación.",
                    badge = "Sugerido",
                    onClick = { selectedStartingPoint = 0 }
                )
                Spacer(modifier = Modifier.height(8.dp))
                StartingPointOption(
                    selected = selectedStartingPoint == 1,
                    title = "Diseño y Prototipado",
                    description = "Wireframes, arquitectura de información y tokens de estilo.",
                    onClick = { selectedStartingPoint = 1 }
                )
                Spacer(modifier = Modifier.height(8.dp))
                StartingPointOption(
                    selected = selectedStartingPoint == 2,
                    title = "Inicio directo en Código",
                    description = "Creación del repositorio, scaffolding y configuración de CI/CD.",
                    onClick = { selectedStartingPoint = 2 }
                )
            }

            // Paso 4: Stack tecnológico
            WizardStepCard(iconRes = R.drawable.ic_projects, step = "PASO 4", title = "Stack tecnológico") {
                Text("Selecciona las capas del proyecto (puedes elegir varias):", fontSize = 12.sp, color = DevFlowColors.TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stackOptions.forEach { tech ->
                        val isSelected = tech in selectedStack
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF0284C7) else DevFlowColors.DarkCard)
                                .border(1.dp, if (isSelected) DevFlowColors.Cyan else DevFlowColors.Border, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedStack = if (isSelected) selectedStack - tech else selectedStack + tech
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isSelected) "$tech ✓" else tech,
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            )
                        }
                    }
                }
            }

            // Paso 5: Entrega y Costos
            WizardStepCard(iconRes = R.drawable.ic_calendar, step = "PASO 5 (OPCIONAL)", title = "Entrega y Costos") {
                FieldLabel("FECHA DE ENTREGA")
                PickerField(
                    text = deliveryDateMillis?.let { formatDeliveryDate(it) } ?: "",
                    placeholder = "Elegir en el calendario",
                    iconRes = R.drawable.ic_calendar,
                    onClick = { showDatePicker = true }
                )
                if (deliveryDateMillis != null) {
                    Text(
                        text = "Quitar fecha",
                        style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary),
                        modifier = Modifier
                            .clickable { deliveryDateMillis = null }
                            .padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FieldLabel("PRESUPUESTO (USD)")
                OutlinedTextField(
                    value = budget,
                    onValueChange = { input -> budget = input.filter { it.isDigit() || it == '.' }.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("0", color = DevFlowColors.TextDark, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = wizardFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (projectName.isBlank()) {
                        Toast.makeText(context, "El nombre del proyecto es obligatorio.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val project = Project(
                        name = projectName.trim(),
                        description = projectDescription.trim(),
                        clientId = selectedClientId,
                        startingPoint = selectedStartingPoint,
                        stack = selectedStack,
                        deliveryDateMillis = deliveryDateMillis,
                        budget = budget.trim(),
                        progress = initialProgressFor(selectedStartingPoint)
                    )
                    onCreateProject(project)
                    Toast.makeText(context, "¡Proyecto '${project.name}' creado!", Toast.LENGTH_SHORT).show()
                },
                enabled = projectName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DevFlowColors.BrightCyan,
                    disabledContainerColor = DevFlowColors.Border
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Crear proyecto",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (projectName.isNotBlank()) Color.Black else DevFlowColors.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        tint = if (projectName.isNotBlank()) Color.Black else DevFlowColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Cancelar y volver",
                style = TextStyle(fontSize = 13.sp, color = DevFlowColors.TextSecondary, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackClick() }
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val todayUtcMillis = remember {
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deliveryDateMillis,
            yearRange = LocalDate.now().year..(LocalDate.now().year + 10),
            selectableDates = object : SelectableDates {
                // Solo se permiten fechas desde hoy en adelante
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year >= LocalDate.now().year
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        deliveryDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Aceptar", color = DevFlowColors.Cyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = DevFlowColors.TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = DevFlowColors.Card)
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false, // obliga a usar el calendario (sin escribir la fecha)
                colors = DatePickerDefaults.colors(
                    containerColor = DevFlowColors.Card,
                    titleContentColor = DevFlowColors.TextSecondary,
                    headlineContentColor = Color.White,
                    weekdayContentColor = DevFlowColors.TextSecondary,
                    subheadContentColor = DevFlowColors.TextSecondary,
                    navigationContentColor = Color.White,
                    yearContentColor = Color.White,
                    currentYearContentColor = DevFlowColors.Cyan,
                    selectedYearContainerColor = DevFlowColors.Primary,
                    dayContentColor = Color.White,
                    disabledDayContentColor = DevFlowColors.TextDark,
                    selectedDayContainerColor = DevFlowColors.Primary,
                    todayContentColor = DevFlowColors.Cyan,
                    todayDateBorderColor = DevFlowColors.Cyan
                )
            )
        }
    }
}

@Composable
private fun WizardStepCard(
    iconRes: Int,
    step: String,
    title: String,
    accent: Color = DevFlowColors.Cyan,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DevFlowColors.Card),
        border = BorderStroke(1.dp, DevFlowColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DevFlowColors.Border),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(step, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = accent))
                    Text(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (action != null) {
                    Text(
                        text = action,
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DevFlowColors.Cyan),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAction() }
                            .padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevFlowColors.TextSecondary))
    Spacer(modifier = Modifier.height(4.dp))
}

/** Campo de solo lectura que abre un selector al tocarlo (no se puede escribir). */
@Composable
private fun PickerField(
    text: String,
    placeholder: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, color = DevFlowColors.TextDark, fontSize = 13.sp) },
            trailingIcon = {
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(18.dp))
            },
            colors = wizardFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )
        // Capa transparente: captura el toque para que no aparezca el teclado.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
        )
    }
}

@Composable
private fun StartingPointOption(
    selected: Boolean,
    title: String,
    description: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) DevFlowColors.DarkCard else Color.Transparent)
            .border(1.dp, if (selected) DevFlowColors.Primary else DevFlowColors.Border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = DevFlowColors.Cyan)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
                if (badge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DevFlowColors.Border)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, fontSize = 10.sp, color = DevFlowColors.TextSecondary)
                    }
                }
            }
            Text(description, fontSize = 11.sp, color = DevFlowColors.TextSecondary)
        }
    }
}

@Composable
private fun wizardFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DevFlowColors.DarkCard,
    unfocusedContainerColor = DevFlowColors.DarkCard,
    focusedBorderColor = DevFlowColors.Primary,
    unfocusedBorderColor = DevFlowColors.Border,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = DevFlowColors.Cyan
)

@Preview(showBackground = true)
@Composable
fun ProjectDetailPreview() {
    DevFlowTheme {
        ProjectDetailScreen()
    }
}
