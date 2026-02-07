package com.example.mistareasapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun BotonFlotantePrincipal(navController: NavHostController, rutaActual: String?) {
    // El botón solo aparece en las pantallas principales
    when (rutaActual) {
        Rutas.PantallaTareas.ruta -> {
            FloatingActionButton(onClick = { navController.navigate(Rutas.PantallaCrearTarea.ruta) }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Tarea")
            }
        }
        Rutas.PantallaHabitos.ruta -> {
            FloatingActionButton(onClick = { /* Aquí irá la ruta de crear hábito */ }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Hábito")
            }
        }
    }
}