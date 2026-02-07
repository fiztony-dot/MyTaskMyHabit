package com.example.mistareasapp.ui.components.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.ui.navigation.Rutas
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import com.example.mistareasapp.data.tasks.Categoria
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun AccionesTopBarTareas(
    viewModel: TareasViewModel,
    navController: NavHostController,
    onLanzarVoz: () -> Unit,
    textoBusqueda: String,
    filtroActual: String? // Ajusta 'Categoria' a tu clase real
) {
    // 1. Botón de Búsqueda
    IconButton(onClick = {
        viewModel.buscadorVisible = !viewModel.buscadorVisible
        if (!viewModel.buscadorVisible) viewModel.actualizarBusqueda("")
    }) {
        Icon(
            imageVector = if (viewModel.buscadorVisible) Icons.Default.Close else Icons.Default.Search,
            contentDescription = "Buscar",
            tint = if (textoBusqueda.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }

    // 2. Botón de Filtro
    IconButton(onClick = { viewModel.mostrarBarraFiltro = !viewModel.mostrarBarraFiltro }) {
        Icon(
            imageVector = if (filtroActual == null) Icons.Default.FilterList else Icons.Default.FilterListOff,
            contentDescription = "Filtrar",
            tint = if (filtroActual != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }

    // 3. Botón Expandir/Colapsar
    IconButton(onClick = {
        viewModel.cambiarEstadoGlobalSecciones(!viewModel.todasSeccionesAbiertas)
    }) {
        Icon(
            imageVector = if (viewModel.todasSeccionesAbiertas) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
            contentDescription = "Contraer/Expandir todo"
        )
    }

    // 4. El botón de Añadir con Menú (Voz/Escribir)
    var expandedAdd by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expandedAdd) 45f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "rotation"
    )

    Box {
        IconButton(onClick = { expandedAdd = true }) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Añadir",
                    tint = Color.Black,
                    modifier = Modifier.padding(8.dp).graphicsLayer { rotationZ = rotation }
                )
            }
        }
        DropdownMenu(expanded = expandedAdd, onDismissRequest = { expandedAdd = false }) {
            DropdownMenuItem(
                text = { Text("🗣️ Por voz") },
                onClick = { expandedAdd = false; onLanzarVoz() }
            )
            DropdownMenuItem(
                text = { Text("📝 Escribir") },
                onClick = {
                    expandedAdd = false
                    navController.navigate(Rutas.PantallaCrearTarea.ruta)
                }
            )
        }
    }
}