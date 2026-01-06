package com.example.mistareasapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mistareasapp.data.Categoria
import com.example.mistareasapp.viewmodel.TareasViewModel
import com.example.mistareasapp.ui.components.obtenerIcono
import com.example.mistareasapp.ui.components.obtenerColorIcono

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaGestionCategorias(
    navController: NavController,
    viewModel: TareasViewModel,
    modifier: Modifier = Modifier
) {
    val categorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())
    var nombreTexto by remember { mutableStateOf("") }
    var categoriaEnEdicion by remember { mutableStateOf<Categoria?>(null) }

    var mostrarDialogoIconos by remember { mutableStateOf(false) }
    var categoriaParaEditarIcono by remember { mutableStateOf<Categoria?>(null) }

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
                title = { Text("GESTIONAR CATEGORÍAS") },
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
                                    viewModel.insertarCategoria(Categoria(titulo = nombreTexto))
                                } else {
                                    viewModel.actualizarCategoria(categoriaEnEdicion!!.copy(titulo = nombreTexto))
                                    categoriaEnEdicion = null
                                }
                                nombreTexto = ""
                            }
                        }) { Icon(if (categoriaEnEdicion == null) Icons.Default.Add else Icons.Default.Check, null) }
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
                            nombreTexto = categoria.titulo
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
                                        tint = obtenerColorIcono(categoria.icono) // Aplica el color real
                                    )
                                }
                            },
                            headlineContent = { Text(categoria.titulo) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = categoria.activa,
                                        onCheckedChange = { viewModel.actualizarCategoria(categoria.copy(activa = it)) }
                                    )
                                    IconButton(onClick = { viewModel.eliminarCategoria(categoria) }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                                    }
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
                                viewModel.actualizarCategoria(cat.copy(icono = nombre))
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