package com.example.mistareasapp.ui.screens.shopping

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.mistareasapp.data.shopping.ListaCategoriaProducto
import com.example.mistareasapp.data.shopping.ListaProducto
import com.example.mistareasapp.data.shopping.TiendaItem
import com.example.mistareasapp.data.shopping.TipoItemCompra
import com.example.mistareasapp.viewmodel.Shopping.GrupoCategoriaItems
import com.example.mistareasapp.viewmodel.Shopping.ItemConProducto
import com.example.mistareasapp.viewmodel.Shopping.ListaCompraViewModel
import kotlinx.coroutines.launch
import java.text.Normalizer as JavaNormalizer

private val UNIDADES_PREDEFINIDAS = listOf("ud", "kg", "g", "l", "ml", "bolsa", "paquete", "lata", "bote", "docena")

private fun normalizarUI(texto: String): String =
    JavaNormalizer.normalize(texto.trim().lowercase(), JavaNormalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaCompra(
    navController: NavHostController,
    vm: ListaCompraViewModel,
    modifier: Modifier = Modifier
) {
    val lugares by vm.lugares.collectAsStateWithLifecycle()
    val lugarActivoId by vm.lugarActivoId.collectAsStateWithLifecycle()
    val grupos by vm.gruposItems.collectAsStateWithLifecycle()
    val hayComprados by vm.hayItemsComprados.collectAsStateWithLifecycle()
    val productos by vm.productos.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val nombresEnLista by vm.nombresProductosEnLista.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mostrarBottomSheet by remember { mutableStateOf(false) }
    var productoACatalogar by remember { mutableStateOf<Pair<Long, String>?>(null) }

    LaunchedEffect(Unit) {
        vm.productoNuevoCreado.collect { (productoId, nombre) ->
            val resultado = snackbarHostState.showSnackbar(
                message = "\"$nombre\" añadido como producto nuevo",
                actionLabel = "Catalogar",
                duration = SnackbarDuration.Long
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                productoACatalogar = productoId to nombre
            }
        }
    }

    val vozLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val texto = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@rememberLauncherForActivityResult
            val nombresTodos = texto
                .split(Regex("\\s+y\\s+", RegexOption.IGNORE_CASE))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val id = lugarActivoId
            if (nombresTodos.isNotEmpty() && id != null) {
                val nombresAAnadir = nombresTodos.filter { normalizarUI(it) !in nombresEnLista }
                val duplicados = nombresTodos.size - nombresAAnadir.size
                val lugarNombre = lugares.firstOrNull { it.id == id }?.nombre ?: "la lista"
                if (nombresAAnadir.isNotEmpty()) {
                    vm.añadirItemsDesdeVoz(nombresAAnadir, id, productos)
                }
                scope.launch {
                    val msg = when {
                        nombresAAnadir.isNotEmpty() && duplicados > 0 ->
                            "Añadidos ${nombresAAnadir.size} a $lugarNombre ($duplicados ya estaban)"
                        nombresAAnadir.isNotEmpty() ->
                            "Añadidos ${nombresAAnadir.size} producto(s) a $lugarNombre"
                        else -> "Todos los productos ya estaban en $lugarNombre"
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    fun lanzarVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di los productos separados por 'y'")
        }
        vozLauncher.launch(intent)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (hayComprados && lugarActivoId != null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            vm.finalizarCompra(lugarActivoId!!)
                            scope.launch { snackbarHostState.showSnackbar("Compra finalizada ✓") }
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        text = { Text("Finalizar compra") },
                        icon = { Icon(Icons.Default.ShoppingCart, null) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallFloatingActionButton(onClick = { lanzarVoz() }) {
                        Icon(Icons.Default.Mic, contentDescription = "Añadir por voz")
                    }
                    FloatingActionButton(onClick = { mostrarBottomSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir ítem")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Selector de lugares
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lugares) { lugar ->
                    FilterChip(
                        selected = lugar.id == lugarActivoId,
                        onClick = { vm.seleccionarLugar(lugar.id) },
                        label = { Text(lugar.nombre) }
                    )
                }
            }

            HorizontalDivider()

            val totalItems = grupos.sumOf { it.items.size }

            if (totalItems == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "La lista está vacía.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pulsa + para añadir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (hayComprados) 220.dp else 120.dp)
                ) {
                    grupos.forEachIndexed { index, grupo ->
                        item(key = "header_${index}_${grupo.categoria?.id ?: "null"}") {
                            CabeceraCategoria(grupo)
                        }
                        items(grupo.items, key = { "item_${it.item.id}" }) { itemConProducto ->
                            FilaItem(
                                itemConProducto = itemConProducto,
                                categorias = categorias,
                                onMarcar = { marcado -> vm.marcarItem(itemConProducto.item.id, marcado) },
                                onEliminar = { vm.eliminarItem(itemConProducto.item) },
                                onCambiarTipo = { nuevoTipo -> vm.cambiarTipoItem(itemConProducto.item.id, nuevoTipo) },
                                onCambiarTienda = { tienda -> vm.cambiarTienda(itemConProducto.item.id, tienda) },
                                onAsignarCategoria = { catId ->
                                    val prod = productos.firstOrNull { it.id == itemConProducto.item.productoId }
                                    vm.catalogarProducto(itemConProducto.item.productoId, catId, prod?.aliases ?: "")
                                },
                                onCrearCategoria = vm::crearCategoriaYObtenerID
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarBottomSheet) {
        val lugarNombre = lugares.firstOrNull { it.id == lugarActivoId }?.nombre ?: "la lista"
        BottomSheetAnadirItem(
            vm = vm,
            lugarId = lugarActivoId,
            todosLosProductos = productos,
            nombresEnLista = nombresEnLista,
            lugarNombre = lugarNombre,
            snackbarHostState = snackbarHostState,
            onDismiss = { mostrarBottomSheet = false }
        )
    }

    productoACatalogar?.let { (productoId, nombre) ->
        DialogoCatalogarProducto(
            nombreProducto = nombre,
            categorias = categorias,
            onDismiss = { productoACatalogar = null },
            onConfirmar = { categoriaId, aliases ->
                vm.catalogarProducto(productoId, categoriaId, aliases)
                productoACatalogar = null
            },
            onCrearCategoria = vm::crearCategoriaYObtenerID
        )
    }
}

@Composable
private fun CabeceraCategoria(grupo: GrupoCategoriaItems) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(grupo.colorHex))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = grupo.nombreCategoria.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaItem(
    itemConProducto: ItemConProducto,
    categorias: List<ListaCategoriaProducto>,
    onMarcar: (Boolean) -> Unit,
    onEliminar: () -> Unit,
    onCambiarTipo: (TipoItemCompra) -> Unit,
    onCambiarTienda: (TiendaItem) -> Unit,
    onAsignarCategoria: (Long?) -> Unit,
    onCrearCategoria: suspend (String, String) -> Long
) {
    val item = itemConProducto.item
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onEliminar(); true } else false
        },
        positionalThreshold = { it * 0.4f }
    )
    var mostrarDialogoCategoria by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bg by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                label = "swipe_bg"
            )
            val scale by animateFloatAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                label = "icon_scale"
            )
            Box(
                modifier = Modifier.fillMaxSize().background(bg).padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "Eliminar",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.scale(scale))
            }
        }
    ) {
        val comprado = item.marcadoComprado
        val esPlanificado = item.tipo == TipoItemCompra.PLANIFICADO
        val sinCategoria = itemConProducto.categoriaId == null
        val alphaBase = when {
            comprado -> 0.38f
            esPlanificado -> 0.6f
            else -> 1f
        }
        val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaBase)
        val tipoIconColor = if (esPlanificado)
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.primary
        val textDecoration = if (comprado) TextDecoration.LineThrough else TextDecoration.None

        val detalle = buildString {
            if (item.cantidad.isNotBlank() && item.cantidad != "1") append(item.cantidad)
            if (item.unidad.isNotBlank()) { if (isNotEmpty()) append(" "); append(item.unidad) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                IconButton(
                    onClick = {
                        onCambiarTipo(if (esPlanificado) TipoItemCompra.URGENTE else TipoItemCompra.PLANIFICADO)
                    },
                    modifier = Modifier.padding(start = 8.dp).size(36.dp)
                ) {
                    Icon(
                        imageVector = if (esPlanificado) Icons.Default.CalendarMonth else Icons.Default.Bolt,
                        contentDescription = if (esPlanificado) "Planificado" else "Urgente",
                        tint = tipoIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = itemConProducto.nombreProducto,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .clickable { mostrarDialogoCategoria = true },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (sinCategoria && !comprado)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else textColor,
                textDecoration = textDecoration,
                style = MaterialTheme.typography.bodyMedium
            )
            // Iconos tienda
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(horizontal = 2.dp)) {
                    // Mercadona
                    val esMercadona = item.tienda == TiendaItem.MERCADONA
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (esMercadona) Color(0xFF00854A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                            .clickable { onCambiarTienda(if (esMercadona) TiendaItem.NINGUNA else TiendaItem.MERCADONA) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", color = if (esMercadona) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    // Asiática
                    val esAsiatica = item.tienda == TiendaItem.ASIATICA
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (esAsiatica) Color(0xFFC0392B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                            .clickable { onCambiarTienda(if (esAsiatica) TiendaItem.NINGUNA else TiendaItem.ASIATICA) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("店", color = if (esAsiatica) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                text = detalle,
                modifier = Modifier.width(68.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                textDecoration = textDecoration,
                style = MaterialTheme.typography.bodySmall
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Checkbox(checked = comprado, onCheckedChange = onMarcar)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    }

    if (mostrarDialogoCategoria) {
        DialogoAsignarCategoria(
            categorias = categorias,
            categoriaIdInicial = itemConProducto.categoriaId,
            onDismiss = { mostrarDialogoCategoria = false },
            onConfirmar = { catId ->
                onAsignarCategoria(catId)
                mostrarDialogoCategoria = false
            },
            onCrearCategoria = onCrearCategoria
        )
    }
}

@Composable
private fun DialogoAsignarCategoria(
    categorias: List<ListaCategoriaProducto>,
    categoriaIdInicial: Long?,
    onDismiss: () -> Unit,
    onConfirmar: (Long?) -> Unit,
    onCrearCategoria: suspend (String, String) -> Long
) {
    var seleccionada by remember { mutableStateOf(categoriaIdInicial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Elige una categoría para clasificar este producto:",
                    style = MaterialTheme.typography.bodyMedium
                )
                ChipsCategoriasConNueva(
                    categorias = categorias,
                    seleccionada = seleccionada,
                    onSeleccionar = { seleccionada = it },
                    onCrearCategoria = onCrearCategoria
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(seleccionada) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetAnadirItem(
    vm: ListaCompraViewModel,
    lugarId: Long?,
    todosLosProductos: List<ListaProducto>,
    nombresEnLista: Set<String>,
    lugarNombre: String,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val busqueda by vm.busquedaProducto.collectAsStateWithLifecycle()
    val sugerencias by vm.sugerencias.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var productoSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var categoriaSeleccionada by remember { mutableStateOf<Long?>(null) }
    var campoTexto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var unidadSeleccionada by remember { mutableStateOf<String?>(null) }
    var mostrarCampoOtraUnidad by remember { mutableStateOf(false) }
    var otraUnidad by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf(TipoItemCompra.URGENTE) }
    val focusRequester = remember { FocusRequester() }

    val unidadFinal = when {
        mostrarCampoOtraUnidad -> otraUnidad.trim()
        else -> unidadSeleccionada ?: ""
    }

    fun doAnadir() {
        if (lugarId != null && campoTexto.isNotBlank()) {
            if (normalizarUI(campoTexto) in nombresEnLista) {
                scope.launch {
                    snackbarHostState.showSnackbar("Este producto ya está en la lista de $lugarNombre")
                }
                return
            }
            // Si es un producto existente del catálogo y se cambió la categoría, actualizarla
            val prodId = productoSeleccionadoId
            if (prodId != null) {
                val prod = todosLosProductos.firstOrNull { it.id == prodId }
                if (prod != null && prod.categoriaId != categoriaSeleccionada) {
                    vm.catalogarProducto(prodId, categoriaSeleccionada, prod.aliases)
                }
            }
            vm.añadirItem(productoSeleccionadoId, campoTexto, lugarId, cantidad, unidadFinal, todosLosProductos, tipoSeleccionado)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { vm.setBusqueda(""); onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Añadir producto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = campoTexto,
                onValueChange = { texto ->
                    campoTexto = texto
                    productoSeleccionadoId = null
                    vm.setBusqueda(texto)
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text("Nombre del producto") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            if (sugerencias.isNotEmpty() && productoSeleccionadoId == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn {
                        items(sugerencias) { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        productoSeleccionadoId = prod.id
                                        categoriaSeleccionada = prod.categoriaId
                                        campoTexto = prod.nombre
                                        vm.setBusqueda("")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(prod.nombre, style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cantidad (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )

            Text(
                "Unidad (opcional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(UNIDADES_PREDEFINIDAS) { unidad ->
                    val seleccionado = !mostrarCampoOtraUnidad && unidadSeleccionada == unidad
                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            mostrarCampoOtraUnidad = false
                            unidadSeleccionada = if (seleccionado) null else unidad
                        },
                        label = { Text(unidad) }
                    )
                }
                item {
                    FilterChip(
                        selected = mostrarCampoOtraUnidad,
                        onClick = {
                            mostrarCampoOtraUnidad = !mostrarCampoOtraUnidad
                            if (mostrarCampoOtraUnidad) unidadSeleccionada = null
                        },
                        label = { Text("Otra") }
                    )
                }
            }

            if (mostrarCampoOtraUnidad) {
                OutlinedTextField(
                    value = otraUnidad,
                    onValueChange = { otraUnidad = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Escribe la unidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { doAnadir() })
                )
            }

            // Selector de categoría — solo para productos del catálogo
            if (productoSeleccionadoId != null && categorias.isNotEmpty()) {
                Text(
                    "Categoría",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChipsCategoriasConNueva(
                    categorias = categorias,
                    seleccionada = categoriaSeleccionada,
                    onSeleccionar = { categoriaSeleccionada = it },
                    onCrearCategoria = vm::crearCategoriaYObtenerID
                )
            }

            Text(
                "Tipo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tipoSeleccionado == TipoItemCompra.URGENTE,
                    onClick = { tipoSeleccionado = TipoItemCompra.URGENTE },
                    label = { Text("Urgente") }
                )
                FilterChip(
                    selected = tipoSeleccionado == TipoItemCompra.PLANIFICADO,
                    onClick = { tipoSeleccionado = TipoItemCompra.PLANIFICADO },
                    label = { Text("Planificado") }
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { doAnadir() },
                modifier = Modifier.fillMaxWidth(),
                enabled = campoTexto.isNotBlank()
            ) {
                Text("Añadir")
            }
        }

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun DialogoCatalogarProducto(
    nombreProducto: String,
    categorias: List<ListaCategoriaProducto>,
    onDismiss: () -> Unit,
    onConfirmar: (categoriaId: Long?, aliases: String) -> Unit,
    onCrearCategoria: suspend (String, String) -> Long
) {
    var categoriaSeleccionadaId by remember { mutableStateOf<Long?>(null) }
    var aliases by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catalogar \"$nombreProducto\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Selecciona una categoría para que este producto se reconozca en futuras búsquedas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                ChipsCategoriasConNueva(
                    categorias = categorias,
                    seleccionada = categoriaSeleccionadaId,
                    onSeleccionar = { categoriaSeleccionadaId = it },
                    onCrearCategoria = onCrearCategoria
                )
                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Aliases (opcional, separados por coma)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(categoriaSeleccionadaId, aliases) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ahora no") }
        }
    )
}

// ─── Compartir ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccionesCompartirCompra(vm: ListaCompraViewModel) {
    val context = LocalContext.current
    var mostrarCompartir by remember { mutableStateOf(false) }

    IconButton(onClick = { mostrarCompartir = true }) {
        Icon(Icons.Default.Share, contentDescription = "Compartir lista")
    }

    if (mostrarCompartir) {
        BottomSheetCompartir(
            vm = vm,
            onCompartir = { seleccionados -> vm.compartirLista(context, seleccionados) },
            onDismiss = { mostrarCompartir = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetCompartir(
    vm: ListaCompraViewModel,
    onCompartir: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val grupos by vm.gruposPendientes.collectAsStateWithLifecycle()
    val todosLosIds = remember(grupos) { grupos.flatMap { it.items }.map { it.item.id }.toSet() }
    var seleccionados by remember(todosLosIds) { mutableStateOf(todosLosIds) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compartir lista",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    seleccionados = if (seleccionados.size == todosLosIds.size) emptySet()
                    else todosLosIds
                }) {
                    Text(if (seleccionados.size == todosLosIds.size) "Deseleccionar todo" else "Seleccionar todo")
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                grupos.forEach { grupo ->
                    item(key = "sh_hdr_${grupo.categoria?.id ?: "null"}") {
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(grupo.colorHex))
                        }.getOrElse { MaterialTheme.colorScheme.primary }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = grupo.nombreCategoria.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    items(grupo.items, key = { "sh_item_${it.item.id}" }) { ic ->
                        val item = ic.item
                        val seleccionado = item.id in seleccionados
                        val detalle = buildString {
                            if (item.cantidad.isNotBlank() && item.cantidad != "1") append(item.cantidad)
                            if (item.unidad.isNotBlank()) { if (isNotEmpty()) append(" "); append(item.unidad) }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    seleccionados = if (seleccionado) seleccionados - item.id
                                    else seleccionados + item.id
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = seleccionado,
                                onCheckedChange = { checked ->
                                    seleccionados = if (checked) seleccionados + item.id
                                    else seleccionados - item.id
                                }
                            )
                            Text(
                                text = if (detalle.isNotBlank()) "${ic.nombreProducto}  ($detalle)"
                                       else ic.nombreProducto,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            Button(
                onClick = { onCompartir(seleccionados); onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                enabled = seleccionados.isNotEmpty()
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir (${seleccionados.size})")
            }
        }
    }
}
