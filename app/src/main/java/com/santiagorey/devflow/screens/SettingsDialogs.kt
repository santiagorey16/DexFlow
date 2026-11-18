package com.santiagorey.devflow.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Diálogos de Ajustes: cambiar nombre, contraseña y eliminar la cuenta.

@Composable
internal fun EditNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("Editar nombre", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                singleLine = true,
                label = { Text("Nombre") },
                colors = devFlowTextFieldColors(filled = true),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim()) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DevFlowColors.TextSecondary) }
        }
    )
}

@Composable
internal fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (currentPassword: String, newPassword: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val error = when {
        new.isNotEmpty() && new.length < 8 -> "Mínimo 8 caracteres."
        confirm.isNotEmpty() && confirm != new -> "Las contraseñas no coinciden."
        new.isNotEmpty() && new == current -> "Debe ser distinta a la actual."
        else -> null
    }
    val isValid = current.isNotEmpty() && new.length >= 8 && new == confirm && new != current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("Cambiar contraseña", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(value = current, onValueChange = { current = it }, label = "Contraseña actual")
                PasswordField(value = new, onValueChange = { new = it }, label = "Nueva contraseña")
                PasswordField(value = confirm, onValueChange = { confirm = it }, label = "Confirmar nueva contraseña")
                if (error != null) {
                    Text(error, color = DevFlowColors.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(current, new) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Primary)
            ) { Text("Cambiar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DevFlowColors.TextSecondary) }
        }
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = devFlowTextFieldColors(filled = true),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Confirmación fuerte: hay que escribir ELIMINAR para borrar la cuenta. */
@Composable
internal fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DevFlowColors.Card,
        title = { Text("Eliminar mi cuenta", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Se borrarán para siempre tu cuenta, proyectos, clientes, tareas e historial del asistente. No se puede deshacer.",
                    color = DevFlowColors.TextSecondary, fontSize = 13.sp
                )
                Text("Escribe ELIMINAR para confirmar:", color = Color.White, fontSize = 13.sp)
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    colors = devFlowTextFieldColors(filled = true),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = typed.trim().equals("ELIMINAR", ignoreCase = true),
                colors = ButtonDefaults.buttonColors(containerColor = DevFlowColors.Red)
            ) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = DevFlowColors.TextSecondary) }
        }
    )
}

