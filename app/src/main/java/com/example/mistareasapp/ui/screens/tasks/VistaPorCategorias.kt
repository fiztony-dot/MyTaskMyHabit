package com.example.mistareasapp.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.components.tasks.HeaderSeccion
import com.example.mistareasapp.ui.components.tasks.TareaCard
import com.example.mistareasapp.ui.components.tasks.obtenerEstiloCategoria
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import java.time.LocalDate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text


@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun VistaPorCategorias(
    viewModel: TareasViewModel,
    categorias: Map<String, List<Tarea>>,
    modifier: Modifier = Modifier.Companion,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onEditTask: (Int) -> Unit
) {
    val mapaExpansiones by viewModel.categoriasExpandidas.collectAsState()
    val estadoGlobal = viewModel.todasSeccionesAbiertas
    val listaCategoriasUI by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

    // --- NUEVA LÓGICA DE ORDENACIÓN ---
    // Procesamos el mapa para que cada lista de tareas esté ordenada
    val categoriasOrdenadas = remember(categorias) {
        categorias.mapValues { (_, listaDeTareas) ->
            listaDeTareas.sortedWith { t1, t2 ->
                // 1. Convertimos prioridad a número para comparar
                val p1 = when (t1.prioridad) {
                    Prioridad.ALTA -> 3
                    Prioridad.MEDIA -> 2
                    else -> 1
                }
                val p2 = when (t2.prioridad) {
                    Prioridad.ALTA -> 3
                    Prioridad.MEDIA -> 2
                    else -> 1
                }

                if (p1 != p2) {
                    p2.compareTo(p1) // Mayor prioridad (3) arriba
                } else {
                    // 2. A igual prioridad, comparamos fecha (la más cercana arriba)
                    val f1 = t1.fechaLimite ?: LocalDate.MAX
                    val f2 = t2.fechaLimite ?: LocalDate.MAX
                    f1.compareTo(f2)
                }
            }
        }
    }
// --- ESTADOS PARA DIÁLOGOS ---
    var tareaAEliminar by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    var tareaACompletar by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogoCompletar by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Usamos el mapa ordenado en lugar del original
        categoriasOrdenadas.forEach { (nombreCat, tareas) ->

            val abierta = mapaExpansiones[nombreCat] ?: estadoGlobal
            val estilo = obtenerEstiloCategoria(nombreCat)

            // HEADER DE CATEGORÍA
            item(key = "header_$nombreCat") {
                Surface(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .clickable { viewModel.alternarCategoria(nombreCat) },
                    color = Color.Companion.Transparent
                ) {
                    Row(
                        modifier = Modifier.Companion.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Companion.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = estilo.color
                        )
                        Spacer(Modifier.Companion.width(8.dp))
                        HeaderSeccion(
                            titulo = "$nombreCat (${tareas.size})",
                            color = estilo.color
                        )
                    }
                }
            }

            if (abierta) {
                items(
                    items = tareas,
                    key = { tarea -> "cat_${nombreCat}_id_${tarea.id}" }
                ) { tarea ->
                    TareaCard(
                        tarea = tarea,
                        categorias = listaCategoriasUI,
                        onTaskToggle = onTaskToggle,
                        onDelete = { t ->
                            tareaAEliminar = t
                            mostrarDialogoEliminar = true
                        },
                        onArchive = { t ->
                            tareaACompletar = t
                            mostrarDialogoCompletar = true
                        },
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .clickable { onEditTask(tarea.id) }
                    )
                }
            }
        }
    }
    // Diálogo para Eliminar
    if (mostrarDialogoEliminar && tareaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("¿Eliminar tarea?") },
            text = { Text("¿Estás seguro de que quieres borrar \"${tareaAEliminar?.titulo}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    tareaAEliminar?.let { viewModel.eliminarTarea(it) }
                    mostrarDialogoEliminar = false
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) { Text("Cancelar") }
            }
        )
    }

// Diálogo para Completar
    if (mostrarDialogoCompletar && tareaACompletar != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCompletar = false },
            title = { Text("¿Completar tarea?") },
            text = { Text("¿Quieres marcar como terminada \"${tareaACompletar?.titulo}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    tareaACompletar?.let { viewModel.archivarTarea(it) }
                    mostrarDialogoCompletar = false
                }) { Text("Completar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCompletar = false }) { Text("Cancelar") }
            }
        )
    }
}