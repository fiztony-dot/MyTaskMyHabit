package com.example.mistareasapp.ui.components.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    filtroActual: String?
) {
    var expandedAdd by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expandedAdd) 45f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "rotation"
    )

    // Botón + simple - navega a pantalla de crear
    IconButton(onClick = { navController.navigate("crear_habito") }) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir",
                tint = Color.Black,
                modifier = Modifier
                    .padding(8.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}
