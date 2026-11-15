package com.santiagorey.devflow.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiagorey.devflow.notifications.DevFlowAlert

/** Avisos de la campana: entregas, CI y seguridad. */
@Composable
fun NotificationsScreen(
    alerts: List<DevFlowAlert>,
    qualityAlertsEnabled: Boolean,
    onBackClick: () -> Unit,
    onAlertClick: (DevFlowAlert) -> Unit
) {
    Scaffold(
        containerColor = DevFlowColors.Background,
        topBar = { BackTopBar(title = "Notificaciones", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (alerts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Todo al día ✓",
                        subtitle = "No tienes entregas vencidas ni próximas" +
                            if (qualityAlertsEnabled) ", ni fallos de CI o secretos detectados." else "."
                    )
                }
            }
            items(alerts, key = { it.id }) { alert ->
                val color = if (alert.severity == DevFlowAlert.Severity.HIGH) DevFlowColors.Red else Color(0xFFFBBF24)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DevFlowColors.Card)
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable(enabled = alert.projectId != null || alert.url != null) { onAlertClick(alert) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(alert.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(alert.detail, color = DevFlowColors.TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
            if (!qualityAlertsEnabled) {
                item {
                    InfoNote("Las alertas de CI y seguridad están apagadas. Actívalas en Ajustes → Mentor y calidad.")
                }
            }
        }
    }
}
