package com.example.mistareasapp.ui.screens.Tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.mistareasapp.OrdenCategorias
import com.example.mistareasapp.ui.components.Tasks.CuerpoListaTareas
import com.example.mistareasapp.ui.screens.Tasks.VistaPorCategorias
import com.example.mistareasapp.viewmodel.Tasks.MapasDeTareas
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import com.example.mistareasapp.viewmodel.Tasks.TipoVista
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mistareasapp.data.tasks.Tarea


@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaTareas(
    navController: NavHostController,
    viewModel: TareasViewModel,
    mapas: MapasDeTareas,
        // Asegúrate de que Tarea esté importado correctamente (ej. com.example.mistareasapp.data.model.Tarea)
    modifier: Modifier = Modifier
) {
    val vistaCategorias by viewModel.tareasPorCategoria.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 🔥 ESTADO PARA EL DIÁLOGO DE CONFIRMACIÓN (Faltaba esto)
    val ordenCategorias by viewModel.ordenCategorias.collectAsStateWithLifecycle()

    val vistaCategoriasOrdenadas = remember(vistaCategorias, ordenCategorias) {
        when (ordenCategorias) {
            OrdenCategorias.ALFABETICO -> vistaCategorias.toSortedMap()
            OrdenCategorias.POR_USO -> vistaCategorias.entries
                .sortedByDescending { it.value.size }
                .associate { it.key to it.value }
            OrdenCategorias.POR_PRIORIDAD -> vistaCategorias.toSortedMap()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = if (viewModel.vistaActual == TipoVista.VENCIMIENTO) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = viewModel.vistaActual == TipoVista.VENCIMIENTO,
                onClick = { viewModel.cambiarVista(TipoVista.VENCIMIENTO) },
                text = { Text("Vencimiento", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = viewModel.vistaActual == TipoVista.CATEGORIAS,
                onClick = { viewModel.cambiarVista(TipoVista.CATEGORIAS) },
                text = { Text("Categorías", fontWeight = FontWeight.Bold) }
            )
        }

        if (viewModel.vistaActual == TipoVista.VENCIMIENTO) {
            CuerpoListaTareas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                mapas = mapas,
                viewModel = viewModel,
                esVistaCategorias = false,
                onTaskToggle = { tarea, isChecked ->
                    // Aquí solo la lógica del Checkbox.
                    if (isChecked) {
                        viewModel.archivarTarea(tarea)
                    } else {
                        viewModel.actualizar(tarea.copy(estaCompletada = false))
                    }
                },
                onEditTask = { id ->
                    navController.navigate("editar_tarea/$id")
                }
            )
        } else {
            VistaPorCategorias(
                viewModel = viewModel,
                categorias = vistaCategoriasOrdenadas,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                onEditTask = { id -> navController.navigate("editar_tarea/$id") },
                onTaskToggle = { tarea, isChecked ->
                    if (isChecked) {
                        // En lugar de llamar al diálogo (que ya no existe aquí),
                        // completamos la tarea directamente
                        viewModel.archivarTarea(tarea)
                    } else {
                        viewModel.actualizar(tarea.copy(estaCompletada = false))
                    }
                }
            )
        }
    }

    // Aquí deberías añadir el diálogo que use mostrarConfirmacionCompletar
    // if (mostrarConfirmacionCompletar) { ... Diálogo ... }
}