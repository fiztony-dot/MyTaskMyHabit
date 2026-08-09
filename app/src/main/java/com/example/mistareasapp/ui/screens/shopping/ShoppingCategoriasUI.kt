package com.example.mistareasapp.ui.screens.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.shopping.ListaCategoriaProducto
import kotlinx.coroutines.launch

internal val PALETA_COLORES_SHOPPING = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#2196F3", "#00BCD4", "#4CAF50", "#8BC34A",
    "#FFC107", "#FF9800", "#FF5722", "#795548"
)

/**
 * Fila horizontal de chips de categorías con opción "+ Nueva categoría" al final.
 * Al pulsar "+ Nueva" se abre un diálogo inline; tras crear, la nueva queda preseleccionada.
 */
@Composable
internal fun ChipsCategoriasConNueva(
    categorias: List<ListaCategoriaProducto>,
    seleccionada: Long?,
    onSeleccionar: (Long?) -> Unit,
    onCrearCategoria: suspend (nombre: String, colorHex: String) -> Long
) {
    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(categorias, key = { it.id }) { cat ->
            FilterChip(
                selected = seleccionada == cat.id,
                onClick = { onSeleccionar(if (seleccionada == cat.id) null else cat.id) },
                label = { Text(cat.nombre) }
            )
        }
        item {
            AssistChip(
                onClick = { mostrarDialogoNueva = true },
                label = { Text("+ Nueva") }
            )
        }
    }

    if (mostrarDialogoNueva) {
        DialogoCrearCategoriaRapida(
            onDismiss = { mostrarDialogoNueva = false },
            onCrear = { nombre, colorHex ->
                mostrarDialogoNueva = false
                scope.launch {
                    val newId = onCrearCategoria(nombre, colorHex)
                    onSeleccionar(newId)
                }
            }
        )
    }
}

@Composable
internal fun DialogoCrearCategoriaRapida(
    onDismiss: () -> Unit,
    onCrear: (nombre: String, colorHex: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var colorSeleccionado by remember { mutableStateOf(PALETA_COLORES_SHOPPING.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETA_COLORES_SHOPPING.chunked(6).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { hex ->
                                val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                    .getOrElse { Color.Gray }
                                val seleccionado = hex == colorSeleccionado
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (seleccionado) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { colorSeleccionado = hex }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nombre.isNotBlank()) onCrear(nombre, colorSeleccionado) },
                enabled = nombre.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
