package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.ui.components.tasks.obtenerColorIcono
import com.example.mistareasapp.ui.components.tasks.obtenerIcono
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaGestionCategoriasHabitos(
    navController: NavController,
    viewModel: HabitosViewModel,
    modifier: Modifier = Modifier
) {
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    var nombreTexto by remember { mutableStateOf("") }
    var categoriaEnEdicion by remember { mutableStateOf<CategoriaHabito?>(null) }

    var mostrarDialogoIconos by remember { mutableStateOf(false) }
    var categoriaParaEditarIcono by remember { mutableStateOf<CategoriaHabito?>(null) }

    // LISTA COMPLETA DE ICONOS PARA EL SELECTOR
    val listaIconosElegibles = listOf(
        "list", "shopping_cart", "work", "home", "star", "event",
        "settings", "person", "code", "lightbulb", "restaurant",
        "directions_car", "fitness_center", "payments", "medical_services",
        "school", "pet_page", "favorite", "build", "call"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("GESTIONAR CATEGORÍAS DE HÁBITOS") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            /**
             * BLOQUE 3: FORMULARIO DE TEXTO
             */
            OutlinedTextField(
                value = nombreTexto,
                onValueChange = { nombreTexto = it },
                label = { Text(if (categoriaEnEdicion == null) "Nueva Categoría" else "Renombrar categoría") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        if (categoriaEnEdicion != null) {
                            IconButton(onClick = {
                                categoriaEnEdicion = null
                                nombreTexto = ""
                            }) { Icon(Icons.Default.Close, null) }
                        }
                        IconButton(onClick = {
                            if (nombreTexto.isNotBlank()) {
                                if (categoriaEnEdicion == null) {
                                    viewModel.insertarCategoriaHabito(CategoriaHabito(nombre = nombreTexto))
                                } else {
                                    viewModel.actualizarCategoriaHabito(categoriaEnEdicion!!.copy(nombre = nombreTexto))
                                    categoriaEnEdicion = null
                                }
                                nombreTexto = ""
                            }
                        }) {
                            Icon(
                                if (categoriaEnEdicion == null) Icons.Default.Add else Icons.Default.Check,
                                null
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            /**
             * BLOQUE 4: LISTA DE CATEGORÍAS CON COLORES DINÁMICOS
             */
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categorias) { categoria ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            categoriaEnEdicion = categoria
                            nombreTexto = categoria.nombre
                        }
                    ) {
                        ListItem(
                            leadingContent = {
                                IconButton(onClick = {
                                    categoriaParaEditarIcono = categoria
                                    mostrarDialogoIconos = true
                                }) {
                                    Icon(
                                        imageVector = obtenerIcono(categoria.icono),
                                        contentDescription = "Icono",
                                        tint = obtenerColorIcono(categoria.icono)
                                    )
                                }
                            },
                            headlineContent = { Text(categoria.nombre) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.eliminarCategoriaHabito(categoria) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * BLOQUE 5: SELECTOR DE ICONOS (Parrilla completa)
     */
    if (mostrarDialogoIconos) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoIconos = false },
            title = { Text("Elige un icono") },
            text = {
                // FlowRow organiza los iconos automáticamente en filas
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 4
                ) {
                    listaIconosElegibles.forEach { nombre ->
                        IconButton(onClick = {
                            categoriaParaEditarIcono?.let { cat ->
                                viewModel.actualizarCategoriaHabito(cat.copy(icono = nombre))
                            }
                            mostrarDialogoIconos = false
                        }) {
                            Icon(
                                imageVector = obtenerIcono(nombre),
                                contentDescription = nombre,
                                tint = obtenerColorIcono(nombre)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoIconos = false }) { Text("Cerrar") }
            }
        )
    }
}

