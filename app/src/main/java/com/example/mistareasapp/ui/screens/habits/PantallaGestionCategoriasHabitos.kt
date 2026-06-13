package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mistareasapp.EMOJIS_CATEGORIA
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGestionCategoriasHabitos(
    navController: NavController,
    viewModel: HabitosViewModel,
    modifier: Modifier = Modifier
) {
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    var nombreTexto by remember { mutableStateOf("") }
    var categoriaEnEdicion by remember { mutableStateOf<CategoriaHabito?>(null) }
    var mostrarDialogoEmojis by remember { mutableStateOf(false) }
    var categoriaParaEditarIcono by remember { mutableStateOf<CategoriaHabito?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Categorías de hábitos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            OutlinedTextField(
                value = nombreTexto,
                onValueChange = { nombreTexto = it },
                label = { Text(if (categoriaEnEdicion == null) "Nueva categoría" else "Renombrar categoría") },
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
                                    viewModel.insertarCategoriaHabito(CategoriaHabito(nombre = nombreTexto, icono = "⭐"))
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .clickable {
                                            categoriaParaEditarIcono = categoria
                                            mostrarDialogoEmojis = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (categoria.icono.any { it.code > 127 }) categoria.icono else "⭐",
                                        fontSize = 20.sp
                                    )
                                }
                            },
                            headlineContent = {
                                Text(categoria.nombre, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Text("Toca el emoji para cambiarlo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = { viewModel.moverCategoriaArriba(categoria, categorias) },
                                        modifier = Modifier.size(36.dp),
                                        enabled = categorias.indexOfFirst { it.id == categoria.id } > 0
                                    ) {
                                        Icon(Icons.Default.ArrowDropUp, null, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.moverCategoriaAbajo(categoria, categorias) },
                                        modifier = Modifier.size(36.dp),
                                        enabled = categorias.indexOfFirst { it.id == categoria.id } < categorias.lastIndex
                                    ) {
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.eliminarCategoriaHabito(categoria) }) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFCF6679))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoEmojis) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEmojis = false },
            title = { Text("Elige un icono para la categoría") },
            text = {
                val columnas = 6
                val scrollState = androidx.compose.foundation.rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EMOJIS_CATEGORIA.chunked(columnas).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            fila.forEach { emoji ->
                                val seleccionado = emoji == categoriaParaEditarIcono?.icono
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (seleccionado) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            categoriaParaEditarIcono?.let { cat ->
                                                viewModel.actualizarCategoriaHabito(cat.copy(icono = emoji))
                                            }
                                            mostrarDialogoEmojis = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                            repeat(columnas - fila.size) { Spacer(Modifier.size(44.dp)) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoEmojis = false }) { Text("Cerrar") }
            }
        )
    }
}
