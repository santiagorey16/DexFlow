package com.santiagorey.devflow.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.R
import com.santiagorey.devflow.ai.AiProvider
import com.santiagorey.devflow.ai.AiSetupState
import com.santiagorey.devflow.ui.theme.DevFlowTheme

/**
 * Tutorial "Conecta tu IA": el usuario elige su IA de confianza, consigue su clave,
 * la pega, se prueba la conexión y elige el modelo.
 */
@Composable
fun AiSetupScreen(
    setup: AiSetupState,
    onBackClick: () -> Unit = {},
    onTestKey: (AiProvider, String) -> Unit = { _, _ -> },
    onSelectModel: (String) -> Unit = {},
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var providerName by rememberSaveable { mutableStateOf<String?>(null) }
    var apiKey by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    val provider = AiProvider.fromName(providerName)

    // Clave probada con éxito: pasa al paso de elegir modelo.
    LaunchedEffect(setup.testedProvider) {
        if (setup.testedProvider != null && setup.testedProvider == provider) step = 3
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay un navegador disponible.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = {
            BackTopBar(
                title = "Conecta tu IA",
                onBackClick = { if (step > 0 && !setup.isTesting) step-- else onBackClick() }
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
            StepIndicator(current = step, total = 4)

            when (step) {
                // ── Paso 1: elegir IA ──────────────────────────────
                0 -> {
                    SetupHeader(
                        step = "PASO 1 DE 4",
                        title = "Elige tu IA de confianza",
                        subtitle = "DevFlow funciona con la IA que tú prefieras. Usarás tu propia cuenta: tú decides cuánto gastar y tus datos van directo a ese proveedor."
                    )
                    AiProvider.entries.forEach { option ->
                        ProviderOptionCard(
                            provider = option,
                            selected = option == provider,
                            onClick = { providerName = option.name }
                        )
                    }
                    PrimaryButton(
                        text = "Continuar",
                        enabled = provider != null,
                        onClick = { step = 1 }
                    )
                }

                // ── Paso 2: conseguir la clave ─────────────────────
                1 -> if (provider != null) {
                    SetupHeader(
                        step = "PASO 2 DE 4",
                        title = "Consigue tu clave de ${provider.displayName}",
                        subtitle = "Una \"API key\" es como una contraseña que deja a DevFlow hablar con ${provider.displayName} en tu nombre."
                    )
                    DevFlowCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            provider.steps.forEachIndexed { index, text ->
                                NumberedStep(number = index + 1, text = text)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { openUrl(provider.keyUrl) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DevFlowColors.Cyan)
                    ) {
                        Text("Abrir ${provider.company} ↗", color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold)
                    }
                    if (provider.isFreeTier) {
                        InfoNote("Con la capa gratuita de Gemini no necesitas tarjeta. Tiene límites de uso por minuto y por día, suficientes para estudiar y proyectos pequeños.")
                    } else {
                        InfoNote("Consejo: en la consola de ${provider.company} puedes poner un límite de gasto mensual para no llevarte sorpresas.")
                    }
                    PrimaryButton(text = "Ya tengo mi clave", onClick = { step = 2 })
                }

                // ── Paso 3: pegar y probar ─────────────────────────
                2 -> if (provider != null) {
                    SetupHeader(
                        step = "PASO 3 DE 4",
                        title = "Pega tu clave",
                        subtitle = "Mantén presionado el campo y elige \"Pegar\". Vamos a probarla antes de guardarla."
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !setup.isTesting,
                        label = { Text("API key de ${provider.displayName}") },
                        placeholder = { Text("${provider.keyPrefix}…", color = DevFlowColors.TextDark) },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    painter = painterResource(if (keyVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                    contentDescription = if (keyVisible) "Ocultar clave" else "Mostrar clave",
                                    tint = DevFlowColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = devFlowTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (apiKey.isNotEmpty() && !provider.looksLikeValidKey(apiKey)) {
                        Text(
                            "Las claves de ${provider.displayName} suelen empezar por \"${provider.keyPrefix}\". Revisa que la copiaste completa.",
                            style = TextStyle(fontSize = 12.sp, color = Color(0xFFFBBF24))
                        )
                    }
                    setup.error?.let { error ->
                        Text(error, style = TextStyle(fontSize = 13.sp, color = DevFlowColors.Red))
                    }
                    InfoNote("🔒 Tu clave se guarda cifrada solo en este teléfono. DevFlow no tiene servidores que la vean: se usa únicamente para hablar directo con ${provider.company}.")
                    PrimaryButton(
                        text = if (setup.isTesting) "Probando conexión…" else "Probar conexión",
                        enabled = apiKey.isNotBlank() && !setup.isTesting,
                        loading = setup.isTesting,
                        onClick = { onTestKey(provider, apiKey) }
                    )
                }

                // ── Paso 4: elegir modelo ──────────────────────────
                else -> if (provider != null) {
                    SetupHeader(
                        step = "PASO 4 DE 4",
                        title = "¡Conexión exitosa! ✓",
                        subtitle = "Elige el modelo que usará el asistente. Te dejamos marcado el recomendado; puedes cambiarlo cuando quieras."
                    )
                    DevFlowCard {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            setup.models.take(MAX_MODELS_SHOWN).forEach { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectModel(model.id) }
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = model.id == setup.selectedModelId,
                                        onClick = { onSelectModel(model.id) },
                                        colors = RadioButtonDefaults.colors(selectedColor = DevFlowColors.Cyan)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(model.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (model.displayName != model.id) {
                                            Text(model.id, color = DevFlowColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PrimaryButton(
                        text = "Guardar y empezar",
                        enabled = setup.selectedModelId != null,
                        onClick = onFinish
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private const val MAX_MODELS_SHOWN = 25

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index <= current) DevFlowColors.Cyan else DevFlowColors.Border)
            )
        }
    }
}

@Composable
private fun SetupHeader(step: String, title: String, subtitle: String) {
    Column {
        Text(step, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevFlowColors.Cyan, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White))
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, style = TextStyle(fontSize = 13.sp, color = DevFlowColors.TextSecondary, lineHeight = 18.sp))
    }
}

@Composable
private fun ProviderOptionCard(provider: AiProvider, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DevFlowColors.DarkCard else DevFlowColors.Card)
            .border(1.dp, if (selected) DevFlowColors.Cyan else DevFlowColors.Border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = DevFlowColors.Cyan)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(provider.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(provider.company, color = DevFlowColors.TextSecondary, fontSize = 12.sp)
                if (provider.isFreeTier) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(text = "GRATIS", color = DevFlowColors.Green)
                }
            }
            Text(provider.tagline, color = DevFlowColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DevFlowColors.Primary),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun InfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DevFlowColors.Primary.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(text, color = DevFlowColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DevFlowColors.BrightCyan,
            disabledContainerColor = DevFlowColors.Border
        )
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text, color = if (enabled) Color.Black else DevFlowColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun AiSetupPreview() {
    DevFlowTheme {
        AiSetupScreen(setup = AiSetupState())
    }
}
