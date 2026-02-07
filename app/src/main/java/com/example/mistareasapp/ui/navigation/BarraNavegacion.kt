package com.example.mistareasapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun BarraNavegacion(navController: NavHostController, rutaActual: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "Tareas") },
            label = { Text("Tareas") },
            selected = rutaActual == Rutas.PantallaTareas.ruta,
            onClick = {
                if (rutaActual != Rutas.PantallaTareas.ruta) {
                    navController.navigate(Rutas.PantallaTareas.ruta)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Hábitos") },
            label = { Text("Hábitos") },
            selected = rutaActual == Rutas.PantallaHabitos.ruta,
            onClick = {
                if (rutaActual != Rutas.PantallaHabitos.ruta) {
                    navController.navigate(Rutas.PantallaHabitos.ruta)
                }
            }
        )
    }
}