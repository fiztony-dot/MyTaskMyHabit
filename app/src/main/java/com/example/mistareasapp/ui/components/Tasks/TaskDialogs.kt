package com.example.mistareasapp.ui.components.tasks

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.mistareasapp.data.tasks.Tarea

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