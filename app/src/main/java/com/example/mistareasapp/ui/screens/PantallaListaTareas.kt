package com.example.mistareasapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.mistareasapp.ui.components.CuerpoListaTareas
import com.example.mistareasapp.viewmodel.TareasViewModel
import com.example.mistareasapp.viewmodel.TipoVista
import com.example.mistareasapp.ui.components.obtenerEstiloCategoria
import com.example.mistareasapp.OrdenCategorias
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
// Para arreglar NavHostController
import androidx.navigation.NavHostController

// Para arreglar MapasDeTareas (ajusta el paquete si el tuyo es distinto)
import com.example.mistareasapp.viewmodel.MapasDeTareas

// Para arreglar collectAsStateWithLifecycle (por si acaso)
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaTareas(
    navController: NavHostController,
    viewModel: TareasViewModel,
    mapas: MapasDeTareas, // <--- Añade esta línea
    modifier: Modifier = Modifier
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
                // CAMBIO AQUÍ: Usamos weight(1f) para que no haya huecos y quitamos el padding duplicado
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onEditTask = { id -> navController.navigate("editar_tarea/$id") },
                onTaskToggle = { tarea, isChecked ->
                    if (isChecked) viewModel.completarTarea(tarea, context)
                    else viewModel.actualizar(tarea.copy(estaCompletada = false))
                }
            )
        }
    }
}