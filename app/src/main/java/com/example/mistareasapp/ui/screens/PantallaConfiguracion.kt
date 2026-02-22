package com.example.mistareasapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    navController: NavController,
    viewModel: TareasViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("CONFIGURACIÓN") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // --- SECCIÓN: VISTAS ---
            Text(
                text = "Vistas",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            // Opción: Mostrar completadas
            ListItem(
                headlineContent = { Text("Mostrar tareas completadas") },
                supportingContent = { Text("Muestra u oculta las tareas marcadas como completadas") },
                trailingContent = {
                    Switch(
                        checked = viewModel.mostrarCompletadas,
                        onCheckedChange = {
                            viewModel.cambiarVisibilidadCompletadas(it)
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

