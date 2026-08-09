package com.example.mistareasapp.ui.screens.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.mistareasapp.data.shopping.ListaCategoriaProducto
import com.example.mistareasapp.data.shopping.ListaProducto
import com.example.mistareasapp.viewmodel.Shopping.ListaCompraViewModel
import kotlinx.coroutines.launch
import java.text.Normalizer as JavaNormalizer

private fun normalizarProd(texto: String): String =
    JavaNormalizer.normalize(texto.trim().lowercase(), JavaNormalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGestionProductos(
    navController: NavHostController,
    vm: ListaCompraViewModel
) {
    val productos by vm.productos.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var busqueda by remember { mutableStateOf("") }
    var mostrarDialogoNuevo by remember { mutableStateOf(false) }
    var productoEditando by remember { mutableStateOf<ListaProducto?>(null) }
    var productoAEliminar by remember { mutableStateOf<ListaProducto?>(null) }
    // Set de IDs de categorías colapsadas (null = "Sin categoría")
    var colapsadas by remember { mutableStateOf(setOf<Long?>()) }

    // Filtrar y agrupar por categoría, ordenados alfabéticamente dentro de cada grupo
    val productosFiltrados = if (busqueda.isBlank()) productos
        else productos.filter { it.nombre.contains(busqueda, ignoreCase = true) }

    val productosPorCategoria: List<Pair<ListaCategoriaProducto?, List<ListaProducto>>> = run {
        val agrupados = productosFiltrados.groupBy { it.categoriaId }
        val result = mutableListOf<Pair<ListaCategoriaProducto?, List<ListaProducto>>>()
        categorias.forEach { cat ->
            val lista = agrupados[cat.id]
            if (!lista.isNullOrEmpty()) result.add(cat to lista.sortedBy { it.nombre })
        }
        val sinCat = agrupados[null]
        if (!sinCat.isNullOrEmpty()) result.add(null to sinCat.sortedBy { it.nombre })
        result
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar productos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    val todasColapsadas = productosPorCategoria.isNotEmpty() &&
                        productosPorCategoria.all { (cat, _) -> cat?.id in colapsadas }
                    TextButton(onClick = {
                        colapsadas = if (todasColapsadas) emptySet()
                        else productosPorCategoria.map { (cat, _) -> cat?.id }.toSet()
                    }) {
                        Text(if (todasColapsadas) "Expandir todo" else "Colapsar todo")
                    }
                    TextButton(onClick = { mostrarDialogoNuevo = true }) {
                        Text("+ Nuevo")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Buscar producto") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(50)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                productosPorCategoria.forEach { (cat, prods) ->
                    val catKey: Long? = cat?.id
                    val estaColapsada = catKey in colapsadas

                    // Cabecera de categoría — clickable para colapsar/expandir
                    item(key = "cat_${cat?.id ?: "sin"}") {
                        val color = cat?.let {
                            runCatching { Color(android.graphics.Color.parseColor(it.colorHex)) }.getOrElse { MaterialTheme.colorScheme.primary }
                        } ?: MaterialTheme.colorScheme.onSurfaceVariant

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    colapsadas = if (estaColapsada) colapsadas - catKey else colapsadas + catKey
                                }
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = (cat?.nombre ?: "Sin categoría").uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "(${prods.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = color.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (estaColapsada) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = if (estaColapsada) "Expandir" else "Colapsar",
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!estaColapsada) {
                        items(prods, key = { "prod_${it.id}" }) { prod ->
                            val aliasesTexto = prod.aliases.trim().let {
                                if (it.isBlank() || it == "[]") null else it
                            }
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                ListItem(
                                    headlineContent = { Text(prod.nombre, fontWeight = FontWeight.Medium) },
                                    supportingContent = if (aliasesTexto != null) {
                                        { Text("Aliases: $aliasesTexto", style = MaterialTheme.typography.bodySmall) }
                                    } else null,
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = { productoEditando = prod }) {
                                                Icon(Icons.Default.Edit, "Editar")
                                            }
                                            IconButton(onClick = { productoAEliminar = prod }) {
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

                if (productosPorCategoria.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (busqueda.isBlank()) "No hay productos en el catálogo."
                                else "No se encontraron productos para «$busqueda».",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoNuevo) {
        DialogoProducto(
            titulo = "Nuevo producto",
            nombreInicial = "",
            categoriaIdInicial = null,
            aliasesInicial = "",
            categorias = categorias,
            productosExistentes = productos,
            productoActualId = null,
            onCrearCategoria = vm::crearCategoriaYObtenerID,
            onConfirmar = { nombre, catId, aliases ->
                vm.añadirProducto(nombre, catId, aliases)
                mostrarDialogoNuevo = false
            },
            onDismiss = { mostrarDialogoNuevo = false }
        )
    }

    productoEditando?.let { prod ->
        DialogoProducto(
            titulo = "Editar producto",
            nombreInicial = prod.nombre,
            categoriaIdInicial = prod.categoriaId,
            aliasesInicial = prod.aliases.trim().let { if (it == "[]") "" else it },
            categorias = categorias,
            productosExistentes = productos,
            productoActualId = prod.id,
            onCrearCategoria = vm::crearCategoriaYObtenerID,
            onConfirmar = { nombre, catId, aliases ->
                vm.editarProducto(prod, nombre, catId, aliases)
                productoEditando = null
            },
            onDismiss = { productoEditando = null }
        )
    }

    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar «${prod.nombre}» del catálogo? Solo es posible si no está en ninguna lista activa.") },
            confirmButton = {
                TextButton(onClick = {
                    val p = prod
                    productoAEliminar = null
                    scope.launch {
                        val error = vm.eliminarProducto(p)
                        if (error != null) snackbarHostState.showSnackbar(error)
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DialogoProducto(
    titulo: String,
    nombreInicial: String,
    categoriaIdInicial: Long?,
    aliasesInicial: String,
    categorias: List<ListaCategoriaProducto>,
    productosExistentes: List<ListaProducto>,
    productoActualId: Long?,
    onCrearCategoria: suspend (String, String) -> Long,
    onConfirmar: (String, Long?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var aliases by remember { mutableStateOf(aliasesInicial) }
    var categoriaSeleccionada by remember { mutableStateOf(categoriaIdInicial) }
    var errorDuplicado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; errorDuplicado = false },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    isError = errorDuplicado,
                    supportingText = if (errorDuplicado) {
                        { Text("Ya existe un producto con este nombre", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Text(
                    "Categoría",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChipsCategoriasConNueva(
                    categorias = categorias,
                    seleccionada = categoriaSeleccionada,
                    onSeleccionar = { categoriaSeleccionada = it },
                    onCrearCategoria = onCrearCategoria
                )

                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text("Aliases (separados por coma)") },
                    placeholder = { Text("ej: leche, milk") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isNotBlank()) {
                        val normNombre = normalizarProd(nombre)
                        val duplicado = productosExistentes.any {
                            it.id != productoActualId && normalizarProd(it.nombre) == normNombre
                        }
                        if (duplicado) {
                            errorDuplicado = true
                        } else {
                            onConfirmar(nombre, categoriaSeleccionada, aliases)
                        }
                    }
                },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
