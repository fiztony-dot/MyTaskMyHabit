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
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaTareas(
    navController: NavHostController,
    viewModel: TareasViewModel,
    mapas: MapasDeTareas, // <--- Añade esta línea
    modifier: Modifier = Modifier.Companion
) {
    val vistaCategorias by viewModel.tareasPorCategoria.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 🔥 Nuevo: estado del orden seleccionado
    val ordenCategorias by viewModel.ordenCategorias.collectAsStateWithLifecycle()

    // 🔥 Nuevo: aplicar orden antes de enviar a la UI
    val vistaCategoriasOrdenadas = remember(vistaCategorias, ordenCategorias) {
        when (ordenCategorias) {

            OrdenCategorias.ALFABETICO ->
                vistaCategorias.toSortedMap()

            OrdenCategorias.POR_USO ->
                vistaCategorias.entries
                    .sortedByDescending { it.value.size }
                    .associate { it.key to it.value }

            // Temporalmente, mismo orden que alfabético o uso
            OrdenCategorias.POR_PRIORIDAD ->
                vistaCategorias.toSortedMap()
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
                text = { Text("Vencimiento", fontWeight = FontWeight.Companion.Bold) }
            )
            Tab(
                selected = viewModel.vistaActual == TipoVista.CATEGORIAS,
                onClick = { viewModel.cambiarVista(TipoVista.CATEGORIAS) },
                text = { Text("Categorías", fontWeight = FontWeight.Companion.Bold) }
            )
        }

        if (viewModel.vistaActual == TipoVista.VENCIMIENTO) {
            CuerpoListaTareas(
                // CAMBIO AQUÍ: Usamos weight(1f) para que no haya huecos y quitamos el padding duplicado
                modifier = Modifier.Companion.weight(1f).fillMaxWidth(),
                mapas = mapas,
                viewModel = viewModel,         // <--- 1. ESTO ES OBLIGATORIO
                esVistaCategorias = false,      // <--- 2. ESTO TAMBIÉN
                onTaskToggle = { tarea, check ->
                    // 3. Usamos 'context' (LocalContext.current) que ya tienes definido
                    viewModel.completarTarea(tarea, context)
                },
                onEditTask = { id ->
                    navController.navigate("editar_tarea/$id")
                }
            )
        } else {

            // 🔥 Aquí enviamos las categorías ya ordenadas
            VistaPorCategorias(
                viewModel = viewModel,
                categorias = vistaCategoriasOrdenadas,
                // CAMBIO AQUÍ: Usamos weight(1f) para que use el mismo espacio que Vencimiento
                modifier = Modifier.Companion.weight(1f).fillMaxWidth(),
                onEditTask = { id -> navController.navigate("editar_tarea/$id") },
                onTaskToggle = { tarea, isChecked ->
                    if (isChecked) viewModel.completarTarea(tarea, context)
                    else viewModel.actualizar(tarea.copy(estaCompletada = false))
                }
            )
        }
    }
}