package com.example.mistareasapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.ui.components.HeaderSeccion
import com.example.mistareasapp.ui.components.TareaCard
import com.example.mistareasapp.ui.components.obtenerEstiloCategoria
import com.example.mistareasapp.viewmodel.TareasViewModel
import java.time.LocalDate
import com.example.mistareasapp.data.Prioridad

@Composable
fun VistaPorCategorias(
    viewModel: TareasViewModel,
    categorias: Map<String, List<Tarea>>,
    modifier: Modifier = Modifier,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.alternarCategoria(nombreCat) },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = estilo.color
                        )
                        Spacer(Modifier.width(8.dp))
                        HeaderSeccion(
                            titulo = "$nombreCat (${tareas.size})",
                            color = estilo.color
                        )
                    }
                }
            }

            if (abierta) {
                items(
                    items = tareas, // Aquí las tareas ya vienen ordenadas
                    key = { tarea -> "cat_${nombreCat}_id_${tarea.id}" }
                ) { tarea ->
                    TareaCard(
                        tarea = tarea,
                        categorias = listaCategoriasUI,
                        onTaskToggle = onTaskToggle,
                        onDelete = { viewModel.eliminar(tarea) },
                        onArchive = { /* lógica de archivar */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditTask(tarea.id) }
                    )
                }
            }
        }
    }
}
