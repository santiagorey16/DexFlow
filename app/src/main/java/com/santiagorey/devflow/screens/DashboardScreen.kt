package com.santiagorey.devflow.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme
import java.util.UUID

// Model for Client / Stakeholder
data class ClientItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String,
    val company: String,
    val category: String, // "Sponsor Clave", "Cliente VIP", "Startup", "Finance"
    val project: String,
    val progress: Int, // 0 - 100
    val statusText: String, // "En desarrollo", "QA & UAT", "En retraso", "Facturación & KPIs"
    val tipCategory: String, // "TIP DE COMUNICACIÓN", "SEMESTRE CRÍTICO", "CADENCIA DE TRABAJO", "ROL EN EL PIPELINE"
    val tipNote: String,
    val contactChannel: String // e.g. "Hito: Sprint Review (28 Feb)"
)

/**
 * Stateful Dashboard wrapper that manages initial client data and user actions.
 */
@Composable
fun StatefulDashboardScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    var clientsList by remember {
        mutableStateOf(
            listOf(
                ClientItem(
                    name = "Carlos Méndez",
                    role = "Dir. Operaciones",
                    company = "FitLife Gym",
                    category = "Sponsor Clave",
                    project = "Tech Fitness Tracker",
                    progress = 65,
                    statusText = "En desarrollo",
                    tipCategory = "TIP DE COMUNICACIÓN",
                    tipNote = "\"Prefiere llamadas breves por la tarde (16:00 CET). Valida con avances funcionales antes que diapositivas.\"",
                    contactChannel = "Hito: Sprint Review (28 Feb)"
                ),
                ClientItem(
                    name = "Marta Ruiz",
                    role = "PO & Gerente General",
                    company = "Urbania Propiedades",
                    category = "Cliente VIP",
                    project = "Portal Web Inmobiliaria",
                    progress = 85,
                    statusText = "QA & UAT",
                    tipCategory = "SEMESTRE CRÍTICO",
                    tipNote = "\"Tiene revisión de avances de la entrega y visualización en la tabla. Requiere informe de seguridad GDPR firmado.\"",
                    contactChannel = "Canal: Preferencia direct (Slack)"
                ),
                ClientItem(
                    name = "Lucas Vega",
                    role = "Founder & Tech Lead",
                    company = "GreenBite Org",
                    category = "Startup",
                    project = "App Delivery Veggie",
                    progress = 42,
                    statusText = "En retraso",
                    tipCategory = "CADENCIA DE TRABAJO",
                    tipNote = "\"Sincronización ágil cada martes. Comunicación informal por Discord.\"",
                    contactChannel = "Sync: Martes 10:00 CET"
                ),
                ClientItem(
                    name = "Lucía Morales",
                    role = "Finanzas & Cobros",
                    company = "FitLife Gym",
                    category = "Finance",
                    project = "Tech Fitness Tracker",
                    progress = 100,
                    statusText = "Facturación & KPIs",
                    tipCategory = "ROL EN EL PIPELINE",
                    tipNote = "\"Valida directamente la integración contable de Stripe Billing.\"",
                    contactChannel = "Último contacto: Hace 3 días"
                )
            )
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    DashboardScreen(
        clients = clientsList,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onAddClientClick = { showAddDialog = true },
        onDeleteClient = { clientId ->
            clientsList = clientsList.filterNot { it.id == clientId }
        },
        onNavigateToLogin = onNavigateToLogin
    )

    // Add Client Dialog Modal
    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onAddClient = { newClient ->
                clientsList = clientsList + newClient
                showAddDialog = false
            }
        )
    }
}

/**
 * Stateless Dashboard Screen UI
 */
@Composable
fun DashboardScreen(
    clients: List<ClientItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClientClick: () -> Unit,
    onDeleteClient: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Filtered list by search query
    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) {
            clients
        } else {
            clients.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.role.contains(searchQuery, ignoreCase = true) ||
                        it.company.contains(searchQuery, ignoreCase = true) ||
                        it.project.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0E14),
        bottomBar = {
            DevFlowBottomBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 1. Top Bar Header (Logo + Notifications + User Avatar)
            item {
                TopHeaderBar(onNavigateToLogin = onNavigateToLogin)
            }

            // 2. Fixed Header Section (Title, Subtitle, Add Button, Metrics Grid)
            item {
                FixedHeaderSection(
                    activeCount = clients.size,
                    onAddClientClick = onAddClientClick
                )
            }

            // 3. Próxima Reunión Card
            item {
                NextMeetingCard(
                    onJoinClick = {
                        Toast.makeText(context, "Uniéndose a la reunión...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 4. Search & Filter Bar
            item {
                SearchAndFilterSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange
                )
            }

            // 5. Dynamic Clients List
            items(
                items = filteredClients,
                key = { it.id }
            ) { client ->
                ClientCardItem(
                    client = client,
                    onDelete = { onDeleteClient(client.id) },
                    onCallClick = {
                        Toast.makeText(context, "Llamando a ${client.name}", Toast.LENGTH_SHORT).show()
                    },
                    onEmailClick = {
                        Toast.makeText(context, "Enviando correo a ${client.name}", Toast.LENGTH_SHORT).show()
                    },
                    onChatClick = {
                        Toast.makeText(context, "Abriendo chat con ${client.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 6. Security Footer Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111622)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_check),
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agenda & contactos sincronizados con CRM DevFlow v2.4",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Top Bar Header with Logo, Dashboard subtitle, Notification icon, and user avatar.
 */
@Composable
fun TopHeaderBar(
    onNavigateToLogin: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DevFlowLogo(size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Dev",
                        style = TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Flow",
                        style = TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7))
                            )
                        )
                    )
                }
                Text(
                    text = "DASHBOARD",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Notification Bell with Cyan Badge Dot
            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF111622))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = "Notificaciones",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // User Avatar (Clickable to return to Login)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF635BFF), Color(0xFFA855F7))
                        )
                    )
                    .clickable { onNavigateToLogin() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SR",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Fixed Top Header Section from Image 2
 */
@Composable
fun FixedHeaderSection(
    activeCount: Int,
    onAddClientClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Clientes & Stakeholders",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF0F2937))
                            .border(1.dp, Color(0xFF155E75), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$activeCount Activos",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gestión de vínculos clave, canales de feedback y acuerdos",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add Client Button (Functional!)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF635BFF))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF635BFF))
                    .clickable { onAddClientClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_user_add),
                    contentDescription = "Agregar Cliente",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2x2 Metrics Grid
        MetricsGrid(activeCount = activeCount)
    }
}

/**
 * 2x2 Metrics Grid matching Image 2
 */
@Composable
fun MetricsGrid(activeCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Clientes Activos
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Clientes Activos",
                value = activeCount.toString(),
                unit = "activos",
                iconRes = R.drawable.ic_users_group,
                unitColor = Color(0xFF38BDF8)
            )
            // Card 2: Proyectos Asociados
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Proyectos\nAsociados",
                value = "6",
                unit = "en curso",
                iconRes = R.drawable.ic_folder,
                unitColor = Color(0xFF94A3B8)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Reuniones
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Reuniones",
                value = "2",
                unit = "esta semana",
                iconRes = R.drawable.ic_calendar,
                unitColor = Color(0xFFA855F7)
            )
            // Card 4: Satisfacción
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Satisfacción",
                value = "98%",
                unit = "feedback",
                iconRes = R.drawable.ic_smile,
                valueColor = Color(0xFF00E5FF),
                unitColor = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    unitColor: Color = Color(0xFF94A3B8)
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111622)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 14.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0D14)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = valueColor
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = unitColor
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Próxima Reunión Card
 */
@Composable
fun NextMeetingCard(
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111622)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRÓXIMA HITA",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF00E5FF)
                        )
                    )
                }
                Text(
                    text = "En 2 días",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sprint Review & Demo Funcional",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "📍 Viernes 28 Feb • 16:30 CET • FitLife Gym",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CM",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Carlos Méndez (Sponsor)",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    )
                }

                OutlinedButton(
                    onClick = onJoinClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF0A0D14)
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "🎧 Unirme",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * Search and Filter options section
 */
@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Buscar cliente, rol, empresa o proyecto...",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF111622),
                unfocusedContainerColor = Color(0xFF111622),
                focusedBorderColor = Color(0xFF635BFF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Filter / Sorting Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ORDEN:",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Última intervención ↓",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF111622))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Proyectos activos ↑",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}

/**
 * Individual Client / Stakeholder Card Item
 */
@Composable
fun ClientCardItem(
    client: ClientItem,
    onDelete: () -> Unit,
    onCallClick: () -> Unit,
    onEmailClick: () -> Unit,
    onChatClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111622)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Avatar, Name, Category Badge, 3-Dots Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFF635BFF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = client.name.take(2).uppercase(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = client.name,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CategoryBadge(category = client.category)
                        }
                        Text(
                            text = "${client.role} • ${client.company}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = "Opciones",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Eliminar Cliente", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Project & Progress Bar Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A0D14))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = client.project,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "${client.progress}% • ${client.statusText}",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (client.progress < 50) Color(0xFFF97316) else Color(0xFF38BDF8)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { client.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (client.progress < 50) Color(0xFFF97316) else Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tip / Note Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D111A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = client.tipCategory,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = client.tipNote,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Contact Row: Channel + Call/Email/Chat Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.contactChannel,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .clickable { onCallClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_phone),
                            contentDescription = "Llamar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Email Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .clickable { onEmailClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_email),
                            contentDescription = "Correo",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Chat Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0D14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .clickable { onChatClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat),
                            contentDescription = "Chat",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category Badge Tag
 */
@Composable
fun CategoryBadge(category: String) {
    val (bgColor, textColor) = when (category) {
        "Sponsor Clave" -> Color(0xFF2E1065) to Color(0xFFA855F7)
        "Cliente VIP" -> Color(0xFF082F49) to Color(0xFF38BDF8)
        "Startup" -> Color(0xFF14532D) to Color(0xFF22C55E)
        "Finance" -> Color(0xFF3B0764) to Color(0xFFC084FC)
        else -> Color(0xFF1E293B) to Color(0xFFCBD5E1)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

/**
 * Dialog Form to Add a New Client (Dynamic Client Creation)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onAddClient: (ClientItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sponsor Clave") }
    var project by remember { mutableStateOf("") }
    var progressStr by remember { mutableStateOf("50") }
    var tipNote by remember { mutableStateOf("") }
    var contactChannel by remember { mutableStateOf("") }

    val categories = listOf("Sponsor Clave", "Cliente VIP", "Startup", "Finance")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agregar Nuevo Cliente / Stakeholder",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Cargo / Rol", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Empresa", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text("Categoría:", style = TextStyle(fontSize = 12.sp, color = Color(0xFF94A3B8)))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val selected = cat == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(0xFF635BFF) else Color(0xFF1E293B))
                                .clickable { category = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cat, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = project,
                    onValueChange = { project = it },
                    label = { Text("Proyecto Asociado", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = tipNote,
                    onValueChange = { tipNote = it },
                    label = { Text("Tip / Notas de comunicación", color = Color(0xFF94A3B8)) },
                    singleLine = false,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val progressInt = progressStr.toIntOrNull() ?: 50
                        onAddClient(
                            ClientItem(
                                name = name,
                                role = role.ifBlank { "Sponsor" },
                                company = company.ifBlank { "Empresa" },
                                category = category,
                                project = project.ifBlank { "Proyecto DevFlow" },
                                progress = progressInt,
                                statusText = if (progressInt >= 80) "QA & UAT" else "En desarrollo",
                                tipCategory = "TIP DE COMUNICACIÓN",
                                tipNote = if (tipNote.isBlank()) "\"Sincronización semanal por Slack.\"" else "\"$tipNote\"",
                                contactChannel = contactChannel.ifBlank { "Canal: Preferencia Email / Slack" }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
            ) {
                Text("Guardar Cliente", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF111622)
    )
}

/**
 * Bottom Navigation Bar with 5 Tabs matching Image 1
 */
@Composable
fun DevFlowBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Dashboard" to R.drawable.ic_dashboard,
        "Proyectos" to R.drawable.ic_projects,
        "Guías" to R.drawable.ic_tasks,
        "Clientes" to R.drawable.ic_users_group,
        "Ajustes" to R.drawable.ic_settings
    )

    Surface(
        color = Color(0xFF0A0D14),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (label, iconRes) ->
                val isSelected = selectedTab == index
                val tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = tint
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DevFlowTheme {
        StatefulDashboardScreen()
    }
}
