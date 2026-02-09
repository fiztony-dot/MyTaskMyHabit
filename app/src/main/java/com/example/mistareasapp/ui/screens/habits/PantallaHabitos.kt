package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.example.mistareasapp.viewmodel.Habits.TipoVistaHabitos

// Archivo: ui/screens/habits/PantallaHabitos.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHabitos(
    navController: NavHostController,
    viewModel: HabitosViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 1. Navegación superior homogénea con "Mis Tareas"
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
                text = { Text("Flash", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = viewModel.vistaActual == TipoVistaHabitos.LISTADO,
                onClick = { viewModel.cambiarVista(TipoVistaHabitos.LISTADO) },
                text = { Text("Hábitos", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = viewModel.vistaActual == TipoVistaHabitos.ESTADISTICAS,
                onClick = { viewModel.cambiarVista(TipoVistaHabitos.ESTADISTICAS) },
                text = { Text("Estadísticas", fontWeight = FontWeight.Bold) }
            )
        }

        // 2. Contenido dinámico
        Box(modifier = Modifier.weight(1f)) {
            when (viewModel.vistaActual) {
                TipoVistaHabitos.FLASH -> PantallaHabitosFlash(viewModel)
                TipoVistaHabitos.LISTADO -> PantallaHabitosListado(viewModel)
                TipoVistaHabitos.ESTADISTICAS -> PantallaHabitosEstadisticas(viewModel)
            }
        }
    }
}