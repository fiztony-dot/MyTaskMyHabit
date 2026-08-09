package com.example.mistareasapp.ui.screens.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.mistareasapp.data.shopping.ListaCategoriaProducto
import com.example.mistareasapp.viewmodel.Shopping.ListaCompraViewModel
import kotlinx.coroutines.launch

private val PALETA_COLORES = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#2196F3", "#00BCD4", "#4CAF50", "#8BC34A",
    "#FFC107", "#FF9800", "#FF5722", "#795548"
)

private enum class OrdenCategorias { MANUAL, ALFABETICO, POR_COLOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGestionCategoriasCompra(
    navController: NavHostController,
    vm: ListaCompraViewModel
) {
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var categoriaEditando by remember { mutableStateOf<ListaCategoriaProducto?>(null) }
    var categoriaAEliminar by remember { mutableStateOf<ListaCategoriaProducto?>(null) }
    var orden by remember { mutableStateOf(OrdenCategorias.MANUAL) }

    val categoriasOrdenadas = when (orden) {
        OrdenCategorias.MANUAL -> categorias
        OrdenCategorias.ALFABETICO -> categorias.sortedBy { it.nombre }
        OrdenCategorias.POR_COLOR -> categorias.sortedBy { it.colorHex }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar categorías") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { mostrarDialogoNueva = true }) {
                        Text("+ Nueva")
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
            // Selector de orden
            item(key = "orden_selector") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Orden:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OrdenCategorias.entries.forEach { opcion ->
                        val etiqueta = when (opcion) {
                            OrdenCategorias.MANUAL -> "Manual"
                            OrdenCategorias.ALFABETICO -> "A–Z"
                            OrdenCategorias.POR_COLOR -> "Color"
                        }
                        FilterChip(
                            selected = orden == opcion,
                            onClick = { orden = opcion },
                            label = { Text(etiqueta) }
                        )
                    }
                }
            }

            items(categoriasOrdenadas, key = { it.id }) { cat ->
                val color = runCatching { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                    .getOrElse { MaterialTheme.colorScheme.primary }
                val idxEnLista = categorias.indexOfFirst { it.id == cat.id }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = cat.nombre,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Botones de orden solo en modo MANUAL
                        if (orden == OrdenCategorias.MANUAL) {
                            IconButton(
                                onClick = { vm.moverCategoriaArriba(cat, categorias) },
                                enabled = idxEnLista > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, "Subir", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { vm.moverCategoriaAbajo(cat, categorias) },
                                enabled = idxEnLista < categorias.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, "Bajar", modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = { categoriaEditando = cat },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, "Editar", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { categoriaAEliminar = cat },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Eliminar",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoNueva) {
        DialogoCategoria(
            titulo = "Nueva categoría",
            nombreInicial = "",
            colorInicial = PALETA_COLORES.first(),
            onConfirmar = { nombre, color ->
                vm.añadirCategoria(nombre, color)
                mostrarDialogoNueva = false
            },
            onDismiss = { mostrarDialogoNueva = false }
        )
    }

    categoriaEditando?.let { cat ->
        DialogoCategoria(
            titulo = "Editar categoría",
            nombreInicial = cat.nombre,
            colorInicial = cat.colorHex,
            onConfirmar = { nombre, color ->
                vm.editarCategoria(cat, nombre, color)
                categoriaEditando = null
            },
            onDismiss = { categoriaEditando = null }
        )
    }

    categoriaAEliminar?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoriaAEliminar = null },
            title = { Text("Eliminar categoría") },
            text = { Text("¿Eliminar «${cat.nombre}»? Solo es posible si no tiene productos asignados.") },
            confirmButton = {
                TextButton(onClick = {
                    val c = cat
                    categoriaAEliminar = null
                    scope.launch {
                        val error = vm.eliminarCategoria(c)
                        if (error != null) snackbarHostState.showSnackbar(error)
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { categoriaAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DialogoCategoria(
    titulo: String,
    nombreInicial: String,
    colorInicial: String,
    onConfirmar: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var colorSeleccionado by remember { mutableStateOf(colorInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETA_COLORES.chunked(6).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { hex ->
                                val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                    .getOrElse { Color.Gray }
                                val seleccionado = hex == colorSeleccionado
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (seleccionado) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { colorSeleccionado = hex }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nombre.isNotBlank()) onConfirmar(nombre, colorSeleccionado) },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
