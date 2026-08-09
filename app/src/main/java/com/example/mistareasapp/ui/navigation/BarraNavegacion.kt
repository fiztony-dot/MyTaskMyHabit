package com.example.mistareasapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

// Definimos la lista de rutas que pertenecen al submódulo de hábitos
val rutasHabitos = listOf(
    Rutas.HabitosFlash.ruta,
    Rutas.HabitosListado.ruta,
    Rutas.HabitosEstadisticas.ruta
)

@Composable
fun BarraNavegacion(navController: NavHostController, rutaActual: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "Tareas") },
            label = { Text("Tareas") },
            selected = rutaActual == Rutas.PantallaTareas.ruta,
            onClick = {
                if (rutaActual != Rutas.PantallaTareas.ruta) {
                    navController.navigate(Rutas.PantallaTareas.ruta) {
                        popUpTo(Rutas.PantallaTareas.ruta) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Hábitos") },
            label = { Text("Hábitos") },
            selected = rutaActual == Rutas.PantallaHabitos.ruta,
            onClick = {
                if (rutaActual != Rutas.PantallaHabitos.ruta) {
                    navController.navigate(Rutas.PantallaHabitos.ruta) {
                        popUpTo(Rutas.PantallaTareas.ruta) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Compra") },
            label = { Text("Compra") },
            selected = rutaActual == Rutas.PantallaListaCompra.ruta,
            onClick = {
                if (rutaActual != Rutas.PantallaListaCompra.ruta) {
                    navController.navigate(Rutas.PantallaListaCompra.ruta) {
                        popUpTo(Rutas.PantallaTareas.ruta) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}

@Composable
fun BarraNavegacionHabitos(navController: NavHostController, rutaActual: String?) {
    NavigationBar {
        // Opción FLASH
        NavigationBarItem(
            icon = { Icon(Icons.Default.Bolt, contentDescription = "Flash") },
            label = { Text("Flash") },
            selected = rutaActual == Rutas.HabitosFlash.ruta,
            onClick = {
                if (rutaActual != Rutas.HabitosFlash.ruta) {
                    navController.navigate(Rutas.HabitosFlash.ruta) {
                        launchSingleTop = true
                    }
                }
            }
        )
        // Opción HÁBITOS (Listado)
        NavigationBarItem(
            icon = { Icon(Icons.Default.TaskAlt, contentDescription = "Hábitos") },
            label = { Text("Hábitos") },
            selected = rutaActual == Rutas.HabitosListado.ruta,
            onClick = {
                if (rutaActual != Rutas.HabitosListado.ruta) {
                    navController.navigate(Rutas.HabitosListado.ruta) {
                        launchSingleTop = true
                    }
                }
            }
        )
        // Opción ESTADÍSTICAS
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Estadísticas") },
            label = { Text("Estadísticas") },
            selected = rutaActual == Rutas.HabitosEstadisticas.ruta,
            onClick = {
                if (rutaActual != Rutas.HabitosEstadisticas.ruta) {
                    navController.navigate(Rutas.HabitosEstadisticas.ruta) {
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}