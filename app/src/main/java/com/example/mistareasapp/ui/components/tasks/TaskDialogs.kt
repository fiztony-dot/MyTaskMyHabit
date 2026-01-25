package com.example.mistareasapp.ui.components.tasks

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.mistareasapp.data.tasks.Tarea
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun ConfirmarCompletarDialog(
    tarea: Tarea?,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit
) {
    if (tarea != null) {
        AlertDialog(
            onDismissRequest = onDescartar,
            title = { Text("¿Completar tarea?") },
            text = { Text("¿Quieres marcar como finalizada: \"${tarea.titulo}\"?") },
            confirmButton = {
                Button(onClick = onConfirmar) { Text("Completar") }
            },
            dismissButton = {
                TextButton(onClick = onDescartar) { Text("Cancelar") }
            }
        )
    }
}

@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun ConfirmarBorradoDialog(
    tarea: Tarea?,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit
) {
    if (tarea != null) {
        AlertDialog(
            onDismissRequest = onDescartar,
            title = { Text("Eliminar tarea") },
            text = { Text("¿Estás seguro de que quieres eliminar \"${tarea.titulo}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = onConfirmar,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = onDescartar) { Text("Cancelar") }
            }
        )
    }
}