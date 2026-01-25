package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mistareasapp.Rutas
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHabitos(navController: NavController, viewModel: TareasViewModel, modifier: Modifier = Modifier.Companion) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Rutas.PantallaCrearTarea.ruta) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Hábito")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Text("Pantalla de Hábitos en construcción")
        }
    }
}