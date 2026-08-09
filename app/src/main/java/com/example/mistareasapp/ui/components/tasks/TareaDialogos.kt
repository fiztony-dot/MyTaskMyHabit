package com.example.mistareasapp.ui.components.tasks

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mistareasapp.data.tasks.Tarea

/**
 * COMPONENTE: TareaDialogos
 * Contiene los dos bloques de AlertDialog que estaban en TareaUIComponents.
 * Se ha mantenido la lógica original de estados.
 */
@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun TareaDialogos(
    // Estados para el diálogo de eliminar
    mostrarDialogo: Boolean,
    tareaAEliminar: Tarea?,
    onConfirmEliminar: () -> Unit,
    onDismissEliminar: () -> Unit,

    // Estados para el diálogo de completar
    mostrarDialogoCompletar: Boolean,
    tareaACompletar: Tarea?,
    onConfirmCompletar: () -> Unit,
    onDismissCompletar: () -> Unit
) {
    // --- BLOQUE DEL DIÁLOGO ELIMINAR ---
    if (mostrarDialogo && tareaAEliminar != null) {
        AlertDialog(
            onDismissRequest = onDismissEliminar,
            title = { Text("¿Eliminar tarea?") },
            text = { Text("¿Estás seguro de que quieres borrar \"${tareaAEliminar.titulo}\"?") },
            confirmButton = {
                TextButton(onClick = onConfirmEliminar) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEliminar) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- BLOQUE DEL DIÁLOGO COMPLETAR ---
    if (mostrarDialogoCompletar && tareaACompletar != null) {
        AlertDialog(
            onDismissRequest = onDismissCompletar,
            title = { Text("¿Completar tarea?") },
            text = { Text("¿Quieres marcar como terminada \"${tareaACompletar.titulo}\"?") },
            confirmButton = {
                TextButton(onClick = onConfirmCompletar) {
                    Text("Completar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCompletar) {
                    Text("Cancelar")
                }
            }
        )
    }
}