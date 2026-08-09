package com.example.mistareasapp.ui.screens.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.mistareasapp.data.shopping.ListaLugar
import com.example.mistareasapp.viewmodel.Shopping.ListaCompraViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGestionLugares(
    navController: NavHostController,
    vm: ListaCompraViewModel
) {
    val lugares by vm.lugares.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nuevoNombre by remember { mutableStateOf("") }
    var lugarEditando by remember { mutableStateOf<ListaLugar?>(null) }
    var nombreEditando by remember { mutableStateOf("") }
    var lugarAEliminar by remember { mutableStateOf<ListaLugar?>(null) }
    val focusNuevo = remember { FocusRequester() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar lugares") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Añadir nuevo lugar
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nuevo lugar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nuevoNombre,
                                onValueChange = { nuevoNombre = it },
                                modifier = Modifier.weight(1f).focusRequester(focusNuevo),
                                label = { Text("Nombre del lugar") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (nuevoNombre.isNotBlank()) {
                                        vm.añadirLugar(nuevoNombre)
                                        nuevoNombre = ""
                                    }
                                })
                            )
                            Button(
                                onClick = {
                                    if (nuevoNombre.isNotBlank()) {
                                        vm.añadirLugar(nuevoNombre)
                                        nuevoNombre = ""
                                    }
                                },
                                enabled = nuevoNombre.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lugares existentes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Lista de lugares
            items(lugares, key = { it.id }) { lugar ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    if (lugarEditando?.id == lugar.id) {
                        // Modo edición inline
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nombreEditando,
                                onValueChange = { nombreEditando = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (nombreEditando.isNotBlank()) {
                                        vm.editarNombreLugar(lugar, nombreEditando)
                                        lugarEditando = null
                                    }
                                })
                            )
                            IconButton(onClick = {
                                if (nombreEditando.isNotBlank()) {
                                    vm.editarNombreLugar(lugar, nombreEditando)
                                    lugarEditando = null
                                }
                            }) {
                                Icon(Icons.Default.Check, "Guardar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // Modo visualización
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = lugar.nombre,
                                    fontWeight = if (lugar.esDefault) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = if (lugar.esDefault) {
                                { Text("Lugar predeterminado", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary) }
                            } else null,
                            leadingContent = {
                                if (lugar.esDefault) {
                                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    IconButton(onClick = { vm.marcarLugarDefault(lugar) }) {
                                        Icon(Icons.Default.Star, "Marcar como predeterminado",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    }
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        lugarEditando = lugar
                                        nombreEditando = lugar.nombre
                                    }) {
                                        Icon(Icons.Default.Edit, "Editar")
                                    }
                                    IconButton(onClick = { lugarAEliminar = lugar }) {
                                        Icon(Icons.Default.Delete, "Eliminar",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    lugarAEliminar?.let { lugar ->
        AlertDialog(
            onDismissRequest = { lugarAEliminar = null },
            title = { Text("Eliminar lugar") },
            text = { Text("¿Eliminar «${lugar.nombre}»?") },
            confirmButton = {
                TextButton(onClick = {
                    val l = lugar
                    lugarAEliminar = null
                    scope.launch {
                        val error = vm.eliminarLugar(l)
                        if (error != null) snackbarHostState.showSnackbar(error)
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { lugarAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}
