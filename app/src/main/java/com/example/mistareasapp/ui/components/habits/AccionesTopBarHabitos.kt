package com.example.mistareasapp.ui.components.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel

@Composable
fun AccionesTopBarHabitos(
    viewModel: HabitosViewModel,
    navController: NavHostController,
    onLanzarVoz: () -> Unit,
    textoBusqueda: String,
    filtroActual: Long?
) {
    // Botón de búsqueda
    IconButton(onClick = {
        viewModel.buscadorVisible = !viewModel.buscadorVisible
        if (!viewModel.buscadorVisible) viewModel.actualizarBusqueda("")
    }) {
        Icon(
            imageVector = if (viewModel.buscadorVisible) Icons.Default.Close else Icons.Default.Search,
            contentDescription = "Buscar",
            tint = if (textoBusqueda.isNotEmpty()) MaterialTheme.colorScheme.primary
                   else LocalContentColor.current
        )
    }

    // Botón de filtro por categoría
    IconButton(onClick = { viewModel.mostrarBarraFiltro = !viewModel.mostrarBarraFiltro }) {
        Icon(
            imageVector = if (filtroActual != null) Icons.Default.FilterListOff else Icons.Default.FilterList,
            contentDescription = "Filtrar por categoría",
            tint = if (filtroActual != null) MaterialTheme.colorScheme.primary
                   else LocalContentColor.current
        )
    }

    // Botón añadir hábito
    val rotation by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 200),
        label = "rotation"
    )
    IconButton(onClick = { navController.navigate("crear_habito") }) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir hábito",
                tint = Color.Black,
                modifier = Modifier
                    .padding(8.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}
