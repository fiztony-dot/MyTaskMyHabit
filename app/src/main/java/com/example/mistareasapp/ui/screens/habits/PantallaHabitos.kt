package com.example.mistareasapp.ui.screens.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mistareasapp.ui.components.habits.BarraFiltrosHabitos
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import androidx.compose.ui.text.font.FontWeight
import com.example.mistareasapp.viewmodel.Habits.TipoVistaHabitos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHabitos(
    navController: NavHostController,
    viewModel: HabitosViewModel,
    modifier: Modifier = Modifier,
    progresoGeneral: Float = 0f
) {
    val textoBusqueda by viewModel.textoBusqueda.collectAsState()
    val categoriaFiltro by viewModel.categoriaFiltro.collectAsState()
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {

        // 0. Buscador animado
        AnimatedVisibility(
            visible = viewModel.buscadorVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { viewModel.actualizarBusqueda(it) },
                placeholder = { Text("Buscar hábito…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = { viewModel.actualizarBusqueda("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                }
            )
        }

        // 0b. Barra de filtro por categoría animada
        AnimatedVisibility(
            visible = viewModel.mostrarBarraFiltro,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BarraFiltrosHabitos(
                categorias = categorias,
                seleccionada = categoriaFiltro,
                onSeleccionar = { viewModel.filtrarPorCategoria(it) }
            )
        }

        // 1. Pestañas de navegación
        SecondaryTabRow(
            selectedTabIndex = when (viewModel.vistaActual) {
                TipoVistaHabitos.FLASH -> 0
                TipoVistaHabitos.LISTADO -> 1
                TipoVistaHabitos.ESTADISTICAS -> 2
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = viewModel.vistaActual == TipoVistaHabitos.FLASH,
                onClick = { viewModel.cambiarVista(TipoVistaHabitos.FLASH) },
                text = { Text("Hoy", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = viewModel.vistaActual == TipoVistaHabitos.LISTADO,
                onClick = { viewModel.cambiarVista(TipoVistaHabitos.LISTADO) },
                text = { Text("Semana", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = viewModel.vistaActual == TipoVistaHabitos.ESTADISTICAS,
                onClick = { viewModel.cambiarVista(TipoVistaHabitos.ESTADISTICAS) },
                text = { Text("Estadísticas", fontWeight = FontWeight.Bold) }
            )
        }

        // 2. Barra secundaria con toggle de agrupación — solo en Hoy y Semana
        if (viewModel.vistaActual != TipoVistaHabitos.ESTADISTICAS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleAgruparPorCategoria() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.agruparPorCategoria) Icons.Default.List else Icons.Default.AccountTree,
                        contentDescription = if (viewModel.agruparPorCategoria) "Vista plana" else "Agrupar por categoría",
                        tint = if (viewModel.agruparPorCategoria)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3. Contenido dinámico
        Box(modifier = Modifier.weight(1f)) {
            when (viewModel.vistaActual) {
                TipoVistaHabitos.FLASH -> PantallaHabitosFlash(viewModel, navController, progresoGeneral)
                TipoVistaHabitos.LISTADO -> PantallaHabitosListado(viewModel, navController)
                TipoVistaHabitos.ESTADISTICAS -> PantallaHabitosEstadisticas(viewModel)
            }
        }
    }
}
