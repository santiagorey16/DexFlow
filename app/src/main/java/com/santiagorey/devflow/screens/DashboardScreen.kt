package com.santiagorey.devflow.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.ui.theme.DevFlowTheme
import java.util.UUID


// =====================================================
// COLORES
// =====================================================

private object DevFlowColors {

    val Background = Color(0xFF0B0E14)
    val Card = Color(0xFF111622)
    val DarkCard = Color(0xFF0A0D14)
    val Border = Color(0xFF1E293B)

    val Primary = Color(0xFF635BFF)
    val Cyan = Color(0xFF38BDF8)
    val BrightCyan = Color(0xFF00E5FF)
    val Purple = Color(0xFFA855F7)

    val TextSecondary = Color(0xFF94A3B8)
    val TextDark = Color(0xFF64748B)
}


// =====================================================
// MODELO CLIENTE
// =====================================================

data class ClientItem(

    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val role: String,

    val company: String,

    val category: String,

    val project: String,

    val progress: Int,

    val statusText: String,

    val tipNote: String,

    val contactChannel: String
)


// =====================================================
// MODELO REUNIÓN
// =====================================================

data class MeetingItem(

    val id: String = UUID.randomUUID().toString(),

    val clientId: String,

    val title: String,

    val date: String,

    val time: String,

    val clientName: String
)


// =====================================================
// ESTADOS DEL DASHBOARD
// =====================================================

@Composable
fun StatefulDashboardScreen(
    currentUser: FirebaseUser? = null,
    onLogoutClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val clients = remember {
        mutableStateListOf<ClientItem>()
    }

    val meetings = remember {
        mutableStateListOf<MeetingItem>()
    }

    var selectedTab by rememberSaveable {
        mutableStateOf(0)
    }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var showAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    DashboardScreen(
        currentUser = currentUser,
        clients = clients,
        meetings = meetings,
        searchQuery = searchQuery,
        selectedTab = selectedTab,
        onSearchQueryChange = { searchQuery = it },
        onTabSelected = { selectedTab = it },
        onAddClientClick = { showAddDialog = true },
        onDeleteClient = { id ->
            clients.removeAll { it.id == id }
            meetings.removeAll { it.clientId == id }
        },
        onLogoutClick = onLogoutClick,
        onNavigateToLogin = onNavigateToLogin
    )

    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onAddClient = { client ->
                clients.add(client)
                showAddDialog = false
            }
        )
    }
}

// =====================================================
// DASHBOARD
// =====================================================

@Composable
fun DashboardScreen(
    currentUser: FirebaseUser? = null,
    clients: List<ClientItem>,
    meetings: List<MeetingItem>,
    searchQuery: String,
    selectedTab: Int,
    onSearchQueryChange: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onAddClientClick: () -> Unit,
    onDeleteClient: (String) -> Unit,
    onLogoutClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val filteredClients = clients.filter { client ->

        searchQuery.isBlank() ||

                client.name.contains(
                    searchQuery,
                    ignoreCase = true
                ) ||

                client.role.contains(
                    searchQuery,
                    ignoreCase = true
                ) ||

                client.company.contains(
                    searchQuery,
                    ignoreCase = true
                ) ||

                client.project.contains(
                    searchQuery,
                    ignoreCase = true
                )
    }


    Scaffold(

        modifier = modifier.fillMaxSize(),

        containerColor = DevFlowColors.Background,

        bottomBar = {

            DevFlowBottomBar(

                selectedTab = selectedTab,

                onTabSelected = onTabSelected
            )
        }

    ) { padding ->


        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {


            item {

                Spacer(
                    modifier = Modifier.height(5.dp)
                )
            }


            item {
                TopHeaderBar(
                    currentUser = currentUser,
                    onLogoutClick = onLogoutClick
                )
            }


            item {

                HeaderSection(

                    activeClients = clients.size,

                    onAddClientClick = onAddClientClick
                )
            }


            // =====================================================
            // REUNIONES
            // =====================================================

            item {

                MeetingsCard(
                    meetings = meetings
                )
            }


            item {

                SearchSection(

                    searchQuery = searchQuery,

                    onSearchQueryChange = onSearchQueryChange
                )
            }


            if (filteredClients.isEmpty()) {

                item {

                    EmptyClientsCard(

                        searchActive = searchQuery.isNotBlank()
                    )
                }

            } else {

                items(

                    items = filteredClients,

                    key = {
                        it.id
                    }

                ) { client ->


                    ClientCard(

                        client = client,

                        onDelete = {

                            onDeleteClient(client.id)
                        },

                        onProjectClick = {

                            // Luego aquí irá la navegación
                            // hacia el proyecto del cliente.
                        }
                    )
                }
            }


            item {

                SecurityFooter()
            }


            item {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }
        }
    }
}


// =====================================================
// LOGO
// =====================================================

@Composable
fun DevFlowLogo(
    size: Dp = 42.dp
) {

    Box(

        modifier = Modifier
            .size(size)
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(

                Brush.linearGradient(

                    listOf(

                        DevFlowColors.BrightCyan,

                        DevFlowColors.Primary,

                        DevFlowColors.Purple
                    )
                )
            ),

        contentAlignment = Alignment.Center

    ) {

        Text(

            text = "</>",

            color = Color.White,

            fontSize = 11.sp,

            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * Componente reutilizable ProfileAvatar para mostrar la foto de perfil del usuario o sus iniciales.
 *
 * Requisito Sección 11 y 12:
 * - Forma circular, tamaño 40.dp con borde en gradiente DevFlow.
 * - Muestra la photoUrl de FirebaseUser si existe (Google Sign-In).
 * - Preparado para la imagen local app/src/main/res/drawable/profile_photo.png cuando la agregues.
 * - Fallback: Muestra las iniciales del usuario sobre el fondo en gradiente DevFlow.
 */
@Composable
fun ProfileAvatar(
    photoUrl: String? = null,
    displayName: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // -----------------------------------------------------------------------------------------
    // CONFIGURACIÓN DE FOTO DE PERFIL LOCAL (profile_photo.png)
    // -----------------------------------------------------------------------------------------
    // Paso 1: Coloca tu imagen en: app/src/main/res/drawable/profile_photo.png
    // Paso 2: Cambia `useLocalPhoto` a `true` en la línea a continuación:
    val useLocalPhoto = false
    // -----------------------------------------------------------------------------------------

    val initials = remember(displayName) {
        if (!displayName.isNullOrBlank()) {
            val parts = displayName.trim().split(" ")
            if (parts.size >= 2) {
                "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            } else {
                displayName.take(2).uppercase()
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
                brush = Brush.linearGradient(
                    listOf(DevFlowColors.Primary, DevFlowColors.Purple)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(DevFlowColors.Cyan, DevFlowColors.Primary)
                ),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            useLocalPhoto -> {
                // Cuando agregues app/src/main/res/drawable/profile_photo.png, descomenta las siguientes líneas:
                // androidx.compose.foundation.Image(
                //     painter = androidx.compose.ui.res.painterResource(id = R.drawable.profile_photo),
                //     contentDescription = "Foto de perfil",
                //     contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                //     modifier = Modifier.fillMaxSize()
                // )
            }
            !photoUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
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
}

// =====================================================
// HEADER SUPERIOR
// =====================================================

@Composable
fun TopHeaderBar(
    currentUser: FirebaseUser? = null,
    onLogoutClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DevFlowLogo()
            Spacer(modifier = Modifier.width(10.dp))
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
                SmallLabel(text = "DASHBOARD")
            }
        }

        Box {
            ProfileAvatar(
                photoUrl = currentUser?.photoUrl?.toString(),
                displayName = currentUser?.displayName ?: currentUser?.email,
                onClick = { showMenu = true }
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = currentUser?.displayName ?: "Usuario DevFlow",
                                style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            )
                            if (currentUser?.email != null) {
                                Text(
                                    text = currentUser.email ?: "",
                                    style = TextStyle(color = Color(0xFF94A3B8), fontSize = 11.sp)
                                )
                            }
                        }
                    },
                    onClick = {}
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onLogoutClick()
                    }
                )
            }
        }
    }
}


// =====================================================
// HEADER DASHBOARD
// =====================================================

@Composable
fun HeaderSection(

    activeClients: Int,

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

                    text = "Clientes & Stakeholders",

                    fontSize = 20.sp,

                    color = Color.White,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(4.dp)
                )


                Text(

                    text = "Gestiona tus clientes y proyectos asociados",

                    fontSize = 12.sp,

                    color = DevFlowColors.TextSecondary
                )
            }


            Spacer(
                modifier = Modifier.width(8.dp)
            )


            Button(

                onClick = onAddClientClick,

                modifier = Modifier.height(42.dp),

                contentPadding = PaddingValues(
                    horizontal = 14.dp
                ),

                shape = RoundedCornerShape(10.dp),

                colors = ButtonDefaults.buttonColors(

                    containerColor = DevFlowColors.Primary
                )

            ) {

                Text(

                    text = "+ Agregar",

                    color = Color.White,

                    fontWeight = FontWeight.Bold,

                    fontSize = 12.sp
                )
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        MetricsGrid(
            activeCount = activeClients
        )
    }
}


// =====================================================
// MÉTRICAS
// =====================================================

@Composable
fun MetricsGrid(
    activeCount: Int
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {


        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.spacedBy(10.dp)

        ) {


            MetricCard(

                modifier = Modifier.weight(1f),

                title = "Clientes activos",

                value = activeCount.toString(),

                subtitle = "activos"
            )


            MetricCard(

                modifier = Modifier.weight(1f),

                title = "Proyectos",

                value = "0",

                subtitle = "en curso"
            )
        }


        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.spacedBy(10.dp)

        ) {


            MetricCard(

                modifier = Modifier.weight(1f),

                title = "Reuniones",

                value = "0",

                subtitle = "programadas"
            )


            MetricCard(

                modifier = Modifier.weight(1f),

                title = "Satisfacción",

                value = "--",

                subtitle = "sin datos",

                valueColor = DevFlowColors.BrightCyan
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


// =====================================================
// REUNIONES
// =====================================================

@Composable
fun MeetingsCard(
    meetings: List<MeetingItem>
) {

    DevFlowCard {


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {


            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {


                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            DevFlowColors.BrightCyan
                        )
                )


                Spacer(
                    modifier = Modifier.width(7.dp)
                )


                SmallLabel(
                    text = "PRÓXIMO HITO",
                    color = DevFlowColors.BrightCyan
                )
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            if (meetings.isEmpty()) {
                Text(
                    text = "No hay reuniones pendientes por el momento",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Las reuniones y sesiones de avance con tus clientes aparecerán aquí.",
                    color = DevFlowColors.TextSecondary,
                    fontSize = 12.sp
                )
            } else {

                val meeting = meetings.first()


                Text(
                    text = meeting.title,

                    color = Color.White,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(5.dp)
                )


                Text(
                    text =
                        "${meeting.date} • ${meeting.time} • ${meeting.clientName}",

                    color = DevFlowColors.TextSecondary,

                    fontSize = 12.sp
                )
            }
        }
    }
}


// =====================================================
// BUSCADOR
// =====================================================

@Composable
fun SearchSection(

    searchQuery: String,

    onSearchQueryChange: (String) -> Unit

) {

    OutlinedTextField(

        value = searchQuery,

        onValueChange = onSearchQueryChange,

        modifier = Modifier.fillMaxWidth(),

        placeholder = {

            Text(

                text = "Buscar cliente, empresa o proyecto...",

                color = DevFlowColors.TextDark,

                fontSize = 13.sp
            )
        },

        leadingIcon = {

            Text(

                text = "⌕",

                fontSize = 22.sp,

                color = DevFlowColors.TextSecondary
            )
        },

        singleLine = true,

        shape = RoundedCornerShape(12.dp),

        colors = devFlowTextFieldColors()
    )
}


// =====================================================
// SIN CLIENTES
// =====================================================

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

                    "No tienes clientes todavía"
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


// =====================================================
// TARJETA CLIENTE
// =====================================================

@Composable
fun ClientCard(

    client: ClientItem,

    onDelete: () -> Unit,

    onProjectClick: () -> Unit

) {

    var menuOpen by remember {
        mutableStateOf(false)
    }


    DevFlowCard {


        Column(
            modifier = Modifier.padding(16.dp)
        ) {


            Row(

                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically

            ) {


                ClientAvatar(
                    name = client.name
                )


                Spacer(
                    modifier = Modifier.width(10.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {


                    Text(

                        text = client.name,

                        color = Color.White,

                        fontWeight = FontWeight.Bold,

                        fontSize = 15.sp
                    )


                    Text(

                        text = "${client.role} • ${client.company}",

                        color = DevFlowColors.TextSecondary,

                        fontSize = 12.sp,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )
                }


                Box {


                    Text(

                        text = "⋮",

                        color = DevFlowColors.TextSecondary,

                        fontSize = 24.sp,

                        modifier = Modifier
                            .clickable {

                                menuOpen = true
                            }
                            .padding(5.dp)
                    )


                    DropdownMenu(

                        expanded = menuOpen,

                        onDismissRequest = {

                            menuOpen = false
                        }

                    ) {


                        DropdownMenuItem(

                            text = {

                                Text(

                                    text = "Eliminar cliente",

                                    color = Color.Red
                                )
                            },

                            onClick = {

                                menuOpen = false

                                onDelete()
                            }
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            CategoryBadge(
                category = client.category
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Box(

                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .background(
                        DevFlowColors.DarkCard
                    )
                    .padding(12.dp)

            ) {


                Column {


                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {


                        Text(

                            text = client.project,

                            color = Color.White,

                            fontWeight = FontWeight.Bold,

                            fontSize = 13.sp,

                            modifier = Modifier.weight(1f),

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis
                        )


                        Text(

                            text = "Entrar →",

                            color = DevFlowColors.Cyan,

                            fontSize = 12.sp,

                            modifier = Modifier.clickable {

                                onProjectClick()
                            }
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    LinearProgressIndicator(

                        progress = {

                            client.progress
                                .coerceIn(0, 100) / 100f
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(
                                RoundedCornerShape(4.dp)
                            ),

                        color = DevFlowColors.Cyan,

                        trackColor = DevFlowColors.Border
                    )


                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )


                    Text(

                        text =
                            "${client.progress}% • ${client.statusText}",

                        color = DevFlowColors.TextSecondary,

                        fontSize = 11.sp
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Text(

                text = client.tipNote,

                color = DevFlowColors.TextSecondary,

                fontSize = 12.sp
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            SmallLabel(

                text =
                    "CONTACTO: ${client.contactChannel}"
            )
        }
    }
}


// =====================================================
// AVATAR
// =====================================================

@Composable
fun ClientAvatar(
    name: String
) {

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

            text = name
                .trim()
                .take(2)
                .uppercase(),

            color = Color.White,

            fontWeight = FontWeight.Bold
        )
    }
}


// =====================================================
// CATEGORÍA
// =====================================================

@Composable
fun CategoryBadge(
    category: String
) {

    val color = when (category) {

        "Sponsor Clave" ->
            DevFlowColors.Purple

        "Cliente VIP" ->
            DevFlowColors.Cyan

        "Startup" ->
            Color(0xFF22C55E)

        "Finance" ->
            Color(0xFFC084FC)

        else ->
            DevFlowColors.TextSecondary
    }


    Box(

        modifier = Modifier
            .clip(
                RoundedCornerShape(5.dp)
            )
            .background(
                color.copy(
                    alpha = 0.15f
                )
            )
            .padding(
                horizontal = 8.dp,
                vertical = 3.dp
            )

    ) {

        Text(

            text = category,

            color = color,

            fontSize = 10.sp,

            fontWeight = FontWeight.Bold
        )
    }
}


// =====================================================
// AGREGAR CLIENTE
// =====================================================

@Composable
fun AddClientDialog(

    onDismiss: () -> Unit,

    onAddClient: (ClientItem) -> Unit

) {

    var name by rememberSaveable {
        mutableStateOf("")
    }


    var role by rememberSaveable {
        mutableStateOf("")
    }


    var company by rememberSaveable {
        mutableStateOf("")
    }


    var project by rememberSaveable {
        mutableStateOf("")
    }


    var progress by rememberSaveable {
        mutableStateOf("50")
    }


    var contact by rememberSaveable {
        mutableStateOf("")
    }


    var notes by rememberSaveable {
        mutableStateOf("")
    }


    var category by rememberSaveable {
        mutableStateOf("Sponsor Clave")
    }


    val categories = listOf(

        "Sponsor Clave",

        "Cliente VIP",

        "Startup",

        "Finance"
    )


    AlertDialog(

        onDismissRequest = onDismiss,

        containerColor = DevFlowColors.Card,

        title = {

            Text(

                text = "Nuevo cliente",

                color = Color.White,

                fontWeight = FontWeight.Bold
            )
        },

        text = {


            LazyColumn(

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)

            ) {


                item {

                    DevFlowTextField(

                        value = name,

                        label = "Nombre",

                        onValueChange = {

                            name = it
                        }
                    )
                }


                item {

                    DevFlowTextField(

                        value = role,

                        label = "Cargo / Rol",

                        onValueChange = {

                            role = it
                        }
                    )
                }


                item {

                    DevFlowTextField(

                        value = company,

                        label = "Empresa",

                        onValueChange = {

                            company = it
                        }
                    )
                }


                item {

                    Text(

                        text = "Categoría",

                        color =
                            DevFlowColors.TextSecondary,

                        fontSize = 12.sp
                    )
                }


                item {


                    Column(

                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)

                    ) {


                        categories
                            .chunked(2)
                            .forEach { row ->


                                Row(

                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            6.dp
                                        )

                                ) {


                                    row.forEach { item ->


                                        CategoryOption(

                                            text = item,

                                            selected =
                                                item ==
                                                        category,

                                            onClick = {

                                                category =
                                                    item
                                            }
                                        )
                                    }
                                }
                            }
                    }
                }


                item {

                    DevFlowTextField(

                        value = project,

                        label = "Proyecto",

                        onValueChange = {

                            project = it
                        }
                    )
                }


                item {

                    DevFlowTextField(

                        value = progress,

                        label = "Progreso 0 - 100",

                        onValueChange = { newValue ->

                            if (

                                newValue.all {

                                    it.isDigit()
                                }

                            ) {

                                progress = newValue
                            }
                        }
                    )
                }


                item {

                    DevFlowTextField(

                        value = contact,

                        label = "Canal de contacto",

                        onValueChange = {

                            contact = it
                        }
                    )
                }


                item {

                    DevFlowTextField(

                        value = notes,

                        label = "Notas",

                        onValueChange = {

                            notes = it
                        }
                    )
                }
            }
        },

        confirmButton = {


            Button(

                enabled = name.isNotBlank(),

                onClick = {


                    val progressNumber =

                        progress
                            .toIntOrNull()
                            ?.coerceIn(0, 100)
                            ?: 0


                    onAddClient(

                        ClientItem(

                            name = name.trim(),

                            role =
                                role.ifBlank {
                                    "Cliente"
                                },

                            company =
                                company.ifBlank {
                                    "Sin empresa"
                                },

                            category = category,

                            project =
                                project.ifBlank {
                                    "Nuevo proyecto"
                                },

                            progress =
                                progressNumber,

                            statusText =
                                when {

                                    progressNumber >= 100 ->
                                        "Completado"

                                    progressNumber >= 80 ->
                                        "QA & pruebas"

                                    progressNumber >= 40 ->
                                        "En desarrollo"

                                    else ->
                                        "Iniciando"
                                },

                            tipNote =
                                notes.ifBlank {
                                    "Sin notas registradas."
                                },

                            contactChannel =
                                contact.ifBlank {
                                    "Sin definir"
                                }
                        )
                    )
                },

                colors =
                    ButtonDefaults.buttonColors(

                        containerColor =
                            DevFlowColors.Primary
                    )

            ) {

                Text(
                    text = "Guardar cliente"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(

                    text = "Cancelar",

                    color =
                        DevFlowColors.TextSecondary
                )
            }
        }
    )
}


// =====================================================
// OPCIÓN CATEGORÍA
// =====================================================

@Composable
fun CategoryOption(

    text: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    Box(

        modifier = Modifier
            .clip(
                RoundedCornerShape(7.dp)
            )
            .background(

                if (selected) {

                    DevFlowColors.Primary

                } else {

                    DevFlowColors.Border
                }
            )
            .clickable {

                onClick()
            }
            .padding(
                horizontal = 9.dp,
                vertical = 6.dp
            )

    ) {

        Text(

            text = text,

            color = Color.White,

            fontSize = 11.sp
        )
    }
}


// =====================================================
// TEXTFIELD
// =====================================================

@Composable
fun DevFlowTextField(

    value: String,

    label: String,

    onValueChange: (String) -> Unit

) {

    OutlinedTextField(

        value = value,

        onValueChange = onValueChange,

        modifier = Modifier.fillMaxWidth(),

        label = {

            Text(
                text = label
            )
        },

        singleLine = true,

        colors = devFlowTextFieldColors()
    )
}


@Composable
private fun devFlowTextFieldColors() =

    OutlinedTextFieldDefaults.colors(

        focusedTextColor = Color.White,

        unfocusedTextColor = Color.White,

        focusedBorderColor =
            DevFlowColors.Primary,

        unfocusedBorderColor =
            DevFlowColors.Border,

        focusedLabelColor =
            DevFlowColors.Cyan,

        unfocusedLabelColor =
            DevFlowColors.TextSecondary,

        cursorColor =
            DevFlowColors.Cyan
    )


// =====================================================
// CARD
// =====================================================

@Composable
fun DevFlowCard(

    modifier: Modifier = Modifier,

    content: @Composable ColumnScope.() -> Unit

) {

    Card(

        modifier =
            modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    DevFlowColors.Card
            ),

        border =
            BorderStroke(

                1.dp,

                DevFlowColors.Border
            )

    ) {

        Column(
            content = content
        )
    }
}


// =====================================================
// LABEL
// =====================================================

@Composable
fun SmallLabel(

    text: String,

    color: Color =
        DevFlowColors.TextSecondary

) {

    Text(

        text = text,

        color = color,

        fontFamily =
            FontFamily.Monospace,

        fontWeight =
            FontWeight.Bold,

        fontSize = 10.sp,

        letterSpacing = 0.5.sp
    )
}


// =====================================================
// FOOTER
// =====================================================

@Composable
fun SecurityFooter() {

    DevFlowCard {


        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),

            horizontalArrangement =
                Arrangement.Center,

            verticalAlignment =
                Alignment.CenterVertically

        ) {


            Text(

                text = "✓",

                color =
                    DevFlowColors.Cyan,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )


            SmallLabel(

                text =
                    "DATOS SINCRONIZADOS CON DEVFLOW"
            )
        }
    }
}


// =====================================================
// BARRA INFERIOR
// =====================================================

@Composable
fun DevFlowBottomBar(

    selectedTab: Int,

    onTabSelected: (Int) -> Unit

) {

    val tabs = listOf(

        "▦" to "Dashboard",

        "◇" to "Proyectos",

        "▤" to "Guías",

        "♙" to "Clientes",

        "☷" to "Ajustes"
    )


    Surface(

        color =
            DevFlowColors.DarkCard,

        border =
            BorderStroke(

                1.dp,

                DevFlowColors.Border
            )

    ) {


        Row(

            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),

            horizontalArrangement =
                Arrangement.SpaceAround,

            verticalAlignment =
                Alignment.CenterVertically

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

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center

                ) {


                    Text(

                        text = item.first,

                        color = itemColor,

                        fontSize = 21.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
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


// =====================================================
// PREVIEW
// =====================================================

@Preview(
    showBackground = true
)
@Composable
fun DashboardPreview() {

    DevFlowTheme {

        StatefulDashboardScreen()
    }
}