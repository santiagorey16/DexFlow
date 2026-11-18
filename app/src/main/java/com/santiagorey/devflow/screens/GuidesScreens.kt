package com.santiagorey.devflow.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ui.theme.DevFlowTheme

/**
 * Pestaña Guías: guías de buenas prácticas, plantillas para copiar, el reto/ruta de aprendizaje
 * ([learningCard]) y la auditoría de repositorios ([auditCard]).
 * Esas dos tarjetas llegan como slots para que esta pantalla no dependa del AssistantViewModel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidesScreens(
    currentUser: FirebaseUser? = null,
    onTabSelected: (Int) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    auditCard: @Composable () -> Unit = {},
    learningCard: @Composable () -> Unit = {},
    onAskAi: (prompt: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = guides.map { it.category }.distinct()
    val visibleGuides = guides
        .filter { selectedCategory == null || it.category == selectedCategory }
        .filter { guide ->
            searchQuery.isBlank() ||
                guide.title.contains(searchQuery, ignoreCase = true) ||
                guide.description.contains(searchQuery, ignoreCase = true) ||
                guide.tags.any { it.contains(searchQuery, ignoreCase = true) }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DevFlowColors.Background,
        bottomBar = {
            DevFlowBottomBar(
                selectedTab = 2, // Pestaña Guías
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
                section = "GUÍAS"
            )

            // Header Badge & Title
            Column {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0F2937))
                        .border(1.dp, Color(0xFF155E75), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("● ${guides.size} estándares de referencia", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevFlowColors.Cyan))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Guías y Normativas de Calidad",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Buenas prácticas de ingeniería, seguridad y convenciones de equipo para construir software resiliente y escalable.",
                    style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary, lineHeight = 17.sp)
                )
            }

            SearchSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                placeholder = "Buscar por OWASP, Clean Architecture..."
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipBox(
                    text = "Todas (${guides.size})",
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
                categories.forEach { category ->
                    FilterChipBox(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // Reto del día y ruta de aprendizaje
            learningCard()

            // Auditoría real del repositorio (ver RepoAuditCard)
            auditCard()

            if (visibleGuides.isEmpty()) {
                EmptyStateCard(
                    title = "Sin resultados",
                    subtitle = "No encontramos guías con esa búsqueda."
                )
            }

            visibleGuides.forEach { guide ->
                GuideCard(guide = guide, onAskAi = onAskAi)
            }

            // Plantillas listas para copiar en tu repositorio
            Text("Plantillas para tu repositorio", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White))
            Text(
                "Cópialas y pégalas en un archivo nuevo de tu proyecto. Suben tu puntaje en la auditoría.",
                style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary)
            )

            guideTemplates.forEach { template ->
                TemplateCard(
                    template = template,
                    onCopy = {
                        copyToClipboard(context, template.fileName, template.content)
                        Toast.makeText(context, "${template.fileName} copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Una guía de buenas prácticas (texto fijo de la app). */
private data class Guide(
    val tag: String,
    val tagColor: Color,
    val code: String,
    val category: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val details: String
)

/** Lista de guías que se muestran en la pestaña. */
private val guides = listOf(
    Guide(
        tag = "OBLIGATORIO",
        tagColor = Color(0xFFEF4444),
        code = "DEV-STD-001",
        category = "Arquitectura",
        title = "Clean Architecture & Hexagonal",
        description = "Aislamiento de la lógica de negocio frente a frameworks y bases de datos para proyectos escalables y desacoplados.",
        tags = listOf("Domain-Driven", "DTOs", "Repository Pattern", "Ports & Adapters"),
        details = "• Las capas internas (dominio) no dependen de las externas (UI, BD, red).\n• Define interfaces (puertos) en el dominio e impleméntalas en infraestructura (adaptadores).\n• Usa DTOs para cruzar fronteras entre capas."
    ),
    Guide(
        tag = "CRÍTICO",
        tagColor = Color(0xFFF97316),
        code = "SEC-STD-002",
        category = "Seguridad",
        title = "Seguridad OWASP Top 10 para APIs",
        description = "Defensa en profundidad contra inyecciones, autenticación rota, exposición de datos y abuso de peticiones.",
        tags = listOf("Validación de entradas", "JWT", "Rate-Limit", "CORS restrictivo"),
        details = "• Valida y sanitiza toda entrada del usuario.\n• Nunca guardes secretos en el código ni en el repositorio.\n• Aplica límites de peticiones y el principio de mínimo privilegio."
    ),
    Guide(
        tag = "RECOMENDADO",
        tagColor = DevFlowColors.Cyan,
        code = "QA-STD-003",
        category = "QA",
        title = "Estrategia de Pruebas: Pirámide de Testing",
        description = "Distribución equilibrada entre tests unitarios, de integración y flujos E2E críticos. Cobertura mínima sugerida: 75%.",
        tags = listOf("Unitarias", "Integración", "E2E"),
        details = "• Muchas pruebas unitarias rápidas en la base.\n• Menos pruebas de integración en el medio.\n• Pocas pruebas E2E, solo para los flujos críticos."
    ),
    Guide(
        tag = "ESTÁNDAR",
        tagColor = DevFlowColors.Cyan,
        code = "DPS-STD-004",
        category = "CI/CD",
        title = "Conventional Commits & Trunk-Based Development",
        description = "Historial de cambios semántico para generar CHANGELOG y versionado SemVer automáticamente.",
        tags = listOf("feat:", "fix:", "chore:"),
        details = "• feat: nueva funcionalidad (sube versión menor).\n• fix: corrección de errores (sube versión de parche).\n• Ramas cortas que se integran a main con frecuencia."
    )
)


/** Tarjeta de una guía; se expande al tocarla y tiene "Preguntar a la IA". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideCard(guide: Guide, onAskAi: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DevFlowCard(modifier = Modifier.clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(guide.tagColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("● ${guide.tag}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = guide.tagColor), maxLines = 1)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = guide.code + if (expanded) " ▲" else " ▼",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevFlowColors.TextSecondary),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(guide.title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White))
            Spacer(modifier = Modifier.height(4.dp))
            Text(guide.description, style = TextStyle(fontSize = 12.sp, color = DevFlowColors.TextSecondary))

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(guide.details, style = TextStyle(fontSize = 12.sp, color = Color.White, lineHeight = 18.sp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🤖 Pregúntale a la IA cómo aplicarla",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DevFlowColors.Purple),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onAskAi(
                                "Explícame la guía \"${guide.title}\" (${guide.code}) y cómo aplicarla paso a paso " +
                                    "en mi proyecto, con un ejemplo concreto de código."
                            )
                        }
                        .padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                guide.tags.forEach { t ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DevFlowColors.Background)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(t, style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary))
                    }
                }
            }
        }
    }
}

/** Tarjeta de una plantilla (README, .gitignore…) con botón para copiarla. */
@Composable
private fun TemplateCard(template: GuideTemplate, onCopy: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DevFlowCard(modifier = Modifier.clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DevFlowColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_folder), contentDescription = null, tint = DevFlowColors.Cyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(template.fileName, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevFlowColors.Cyan), maxLines = 1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(template.description, style = TextStyle(fontSize = 11.sp, color = DevFlowColors.TextSecondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DevFlowColors.Primary)
                        .clickable { onCopy() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Copiar", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White))
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    template.content,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE2E8F0), lineHeight = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DevFlowColors.Background)
                        .padding(10.dp)
                )
            } else {
                Text("Toca para ver el contenido", style = TextStyle(fontSize = 10.sp, color = DevFlowColors.TextDark), modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** Copia el texto al portapapeles del teléfono y muestra un aviso. */
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Preview(showBackground = true)
@Composable
fun GuidesScreensPreview() {
    DevFlowTheme {
        GuidesScreens()
    }
}
