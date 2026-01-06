package com.example.mistareasapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.ui.components.HeaderSeccion
import com.example.mistareasapp.ui.components.TareaCard
import com.example.mistareasapp.ui.components.obtenerEstiloCategoria
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import com.example.mistareasapp.viewmodel.TareasViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun VistaPorCategorias(
    viewModel: TareasViewModel,
    categorias: Map<String, List<Tarea>>,
    modifier: Modifier = Modifier,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onEditTask: (Int) -> Unit
) {
    val estadosExpandido = remember { mutableStateMapOf<String, Boolean>() }
    val listaCategorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorias
            .toSortedMap()
            .forEach { (categoria, tareas) ->

                val estilo = obtenerEstiloCategoria(categoria)
                val expandido = estadosExpandido[categoria] ?: true

                // Encabezado con icono a la izquierda
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { estadosExpandido[categoria] = !expandido }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = estilo.color.copy(alpha = 0.8f)
                        )

                        Spacer(Modifier.width(8.dp))

                        HeaderSeccion(
                            titulo = "$categoria (${tareas.size})",
                            color = estilo.color.copy(alpha = 0.25f)
                        )
                    }
                }

                // Tareas con ancho completo
                item {
                    AnimatedVisibility(
                        visible = expandido,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tareas
                                .sortedBy { it.toComparableDateTime() }
                                .forEach { tarea ->
                                    TareaCard(
                                        tarea = tarea,
                                        categorias = listaCategorias,
                                        onTaskToggle = onTaskToggle,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEditTask(tarea.id) }
                                    )
                                }
                        }
                    }
                }
            }
    }
}

