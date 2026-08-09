package com.example.mistareasapp.viewmodel.Shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.data.shopping.ListaCompraDao
import com.example.mistareasapp.data.shopping.ListaCategoriaProducto
import com.example.mistareasapp.data.shopping.ListaItem
import com.example.mistareasapp.data.shopping.ListaLugar
import com.example.mistareasapp.data.shopping.ListaProducto
import com.example.mistareasapp.data.shopping.TiendaItem
import com.example.mistareasapp.data.shopping.TipoItemCompra
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemConProducto(
    val item: ListaItem,
    val nombreProducto: String,
    val categoriaId: Long?
)

data class GrupoCategoriaItems(
    val categoria: ListaCategoriaProducto?,
    val nombreCategoria: String,
    val colorHex: String,
    val items: List<ItemConProducto>
)

@OptIn(ExperimentalCoroutinesApi::class)
class ListaCompraViewModel(
    private val app: Application,
    private val dao: ListaCompraDao
) : AndroidViewModel(app) {

    // ─── Datos principales ────────────────────────────────────────────────────

    val lugares: StateFlow<List<ListaLugar>> = dao.obtenerLugares()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lugarActivoId = MutableStateFlow<Long?>(null)
    val lugarActivoId: StateFlow<Long?> = _lugarActivoId.asStateFlow()

    val categorias: StateFlow<List<ListaCategoriaProducto>> = dao.obtenerCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productos: StateFlow<List<ListaProducto>> = dao.obtenerProductos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busquedaProducto = MutableStateFlow("")
    val busquedaProducto: StateFlow<String> = _busquedaProducto.asStateFlow()

    val sugerencias: StateFlow<List<ListaProducto>> = combine(
        _busquedaProducto, productos
    ) { query, lista ->
        if (query.isEmpty()) emptyList()
        else lista.filter { p ->
            p.nombre.contains(query, ignoreCase = true) ||
            p.aliases.contains(query, ignoreCase = true)
        }.take(20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val itemsDelLugar: StateFlow<List<ListaItem>> = _lugarActivoId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else dao.obtenerItemsPorLugar(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filtroTipo = MutableStateFlow<TipoItemCompra?>(null)
    val filtroTipo: StateFlow<TipoItemCompra?> = _filtroTipo.asStateFlow()

    fun setFiltroTipo(tipo: TipoItemCompra?) { _filtroTipo.value = tipo }

    private val _filtroTienda = MutableStateFlow<TiendaItem?>(null)
    val filtroTienda: StateFlow<TiendaItem?> = _filtroTienda.asStateFlow()

    fun setFiltroTienda(tienda: TiendaItem?) { _filtroTienda.value = tienda }

    fun limpiarFiltros() { _filtroTipo.value = null; _filtroTienda.value = null }

    private val _productoNuevoCreado = MutableSharedFlow<Pair<Long, String>>()
    val productoNuevoCreado: SharedFlow<Pair<Long, String>> = _productoNuevoCreado.asSharedFlow()

    @Suppress("UNCHECKED_CAST")
    val gruposItems: StateFlow<List<GrupoCategoriaItems>> = combine(
        itemsDelLugar, productos, categorias, _filtroTipo, _filtroTienda
    ) { arr ->
        val items = arr[0] as List<ListaItem>
        val prods = arr[1] as List<ListaProducto>
        val cats = arr[2] as List<ListaCategoriaProducto>
        val filtro = arr[3] as TipoItemCompra?
        val filtroTienda = arr[4] as TiendaItem?
        val itemsFiltrados = items
            .let { if (filtro == null) it else it.filter { i -> i.tipo == filtro } }
            .let { if (filtroTienda == null) it else it.filter { i -> i.tienda == filtroTienda } }
        val productoMap = prods.associateBy { it.id }
        val itemsConProducto = itemsFiltrados.map { item ->
            val prod = productoMap[item.productoId]
            ItemConProducto(
                item = item,
                nombreProducto = prod?.nombre ?: "Producto",
                categoriaId = prod?.categoriaId
            )
        }
        val agrupados = itemsConProducto.groupBy { it.categoriaId }
        val grupos = mutableListOf<GrupoCategoriaItems>()
        cats.forEach { cat ->
            val itemsCat = agrupados[cat.id]
            if (!itemsCat.isNullOrEmpty()) {
                grupos.add(GrupoCategoriaItems(cat, cat.nombre, cat.colorHex, itemsCat))
            }
        }
        val sinCategoria = agrupados[null]
        if (!sinCategoria.isNullOrEmpty()) {
            grupos.add(GrupoCategoriaItems(null, "Otros", "#607D8B", sinCategoria))
        }
        grupos
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grupos de ítems PENDIENTES (no comprados) sin aplicar filtroTipo — para compartir
    val gruposPendientes: StateFlow<List<GrupoCategoriaItems>> = combine(
        itemsDelLugar, productos, categorias
    ) { items, prods, cats ->
        val pendientes = items.filter { !it.marcadoComprado }
        val productoMap = prods.associateBy { it.id }
        val itemsConProducto = pendientes.map { item ->
            val prod = productoMap[item.productoId]
            ItemConProducto(item = item, nombreProducto = prod?.nombre ?: "Producto", categoriaId = prod?.categoriaId)
        }
        val agrupados = itemsConProducto.groupBy { it.categoriaId }
        val grupos = mutableListOf<GrupoCategoriaItems>()
        cats.forEach { cat ->
            val itemsCat = agrupados[cat.id]
            if (!itemsCat.isNullOrEmpty()) grupos.add(GrupoCategoriaItems(cat, cat.nombre, cat.colorHex, itemsCat))
        }
        val sinCategoria = agrupados[null]
        if (!sinCategoria.isNullOrEmpty()) grupos.add(GrupoCategoriaItems(null, "Otros", "#607D8B", sinCategoria))
        grupos
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nombresProductosEnLista: StateFlow<Set<String>> = combine(itemsDelLugar, productos) { items, prods ->
        val prodMap = prods.associateBy { it.id }
        items.filter { !it.marcadoComprado }.mapNotNull { item ->
            prodMap[item.productoId]?.nombre?.let { normalizar(it) }
        }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val hayItemsComprados: StateFlow<Boolean> = itemsDelLugar
        .flatMapLatest { items -> flowOf(items.any { it.marcadoComprado }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val itemsPendientesCount: StateFlow<Int> = itemsDelLugar
        .flatMapLatest { items -> flowOf(items.count { !it.marcadoComprado }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            lugares.collect { lista ->
                if (_lugarActivoId.value == null && lista.isNotEmpty()) {
                    val default = lista.firstOrNull { it.esDefault } ?: lista.first()
                    _lugarActivoId.value = default.id
                }
            }
        }
    }

    // ─── Acciones de la lista principal ──────────────────────────────────────

    fun seleccionarLugar(id: Long) { _lugarActivoId.value = id }

    fun setBusqueda(query: String) { _busquedaProducto.value = query }

    fun marcarItem(itemId: Long, marcado: Boolean) {
        viewModelScope.launch { dao.marcarItem(itemId, marcado); actualizarWidget() }
    }

    fun cambiarTipoItem(itemId: Long, nuevoTipo: TipoItemCompra) {
        viewModelScope.launch { dao.cambiarTipoItem(itemId, nuevoTipo); actualizarWidget() }
    }

    fun cambiarTienda(itemId: Long, tienda: TiendaItem) {
        viewModelScope.launch { dao.cambiarTienda(itemId, tienda) }
    }

    fun eliminarItem(item: ListaItem) {
        viewModelScope.launch { dao.eliminarItem(item); actualizarWidget() }
    }

    fun finalizarCompra(lugarId: Long) {
        viewModelScope.launch { dao.eliminarItemsComprados(lugarId); actualizarWidget() }
    }

    fun añadirItem(
        productoId: Long?,
        nombreLibre: String,
        lugarId: Long,
        cantidad: String,
        unidad: String,
        todosLosProductos: List<ListaProducto>,
        tipo: TipoItemCompra = TipoItemCompra.URGENTE
    ) {
        viewModelScope.launch {
            val nombreCap = capitalizar(nombreLibre)
            var esNuevo = false
            val resolvedProductoId: Long = when {
                productoId != null -> productoId
                else -> {
                    val existente = todosLosProductos.firstOrNull {
                        normalizar(it.nombre) == normalizar(nombreLibre)
                    }
                    if (existente != null) {
                        existente.id
                    } else {
                        esNuevo = true
                        dao.insertarProducto(ListaProducto(nombre = nombreCap, categoriaId = null))
                    }
                }
            }
            dao.insertarItem(
                ListaItem(
                    productoId = resolvedProductoId,
                    lugarId = lugarId,
                    cantidad = cantidad.ifBlank { "1" },
                    unidad = unidad.trim(),
                    tipo = tipo
                )
            )
            _busquedaProducto.value = ""
            actualizarWidget()
            if (esNuevo) _productoNuevoCreado.emit(resolvedProductoId to nombreCap)
        }
    }

    fun añadirItemsDesdeVoz(
        nombres: List<String>,
        lugarId: Long,
        todosLosProductos: List<ListaProducto>,
        tipo: TipoItemCompra = TipoItemCompra.URGENTE
    ) {
        viewModelScope.launch {
            nombres.forEach { nombre ->
                val nombreCap = capitalizar(nombre)
                val normalizado = normalizar(nombre)
                val existente = todosLosProductos.firstOrNull { p ->
                    normalizar(p.nombre) == normalizado ||
                    p.aliases.split(",").any { normalizar(it.trim()) == normalizado }
                }
                val esNuevo = existente == null
                val resolvedId = existente?.id ?: dao.insertarProducto(
                    ListaProducto(nombre = nombreCap, categoriaId = null)
                )
                dao.insertarItem(
                    ListaItem(productoId = resolvedId, lugarId = lugarId, cantidad = "1", unidad = "", tipo = tipo)
                )
                if (esNuevo) _productoNuevoCreado.emit(resolvedId to nombreCap)
            }
            actualizarWidget()
        }
    }

    fun catalogarProducto(productoId: Long, categoriaId: Long?, aliases: String) {
        viewModelScope.launch {
            val prod = productos.value.firstOrNull { it.id == productoId } ?: return@launch
            dao.actualizarProducto(prod.copy(categoriaId = categoriaId, aliases = aliases.trim()))
        }
    }

    private fun capitalizar(texto: String): String =
        texto.trim().replaceFirstChar { it.uppercase() }

    private fun normalizar(texto: String): String =
        java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    private fun actualizarWidget() {
        // Widget clásico AppWidgetProvider — se actualiza solo via onUpdate
    }

    // ─── Gestión de Lugares ───────────────────────────────────────────────────

    fun añadirLugar(nombre: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch { dao.insertarLugar(ListaLugar(nombre = nombre.trim())) }
    }

    fun editarNombreLugar(lugar: ListaLugar, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch { dao.actualizarLugar(lugar.copy(nombre = nuevoNombre.trim())) }
    }

    suspend fun eliminarLugar(lugar: ListaLugar): String? {
        if (lugar.esDefault) return "No se puede eliminar el lugar predeterminado"
        val numItems = dao.contarItemsPorLugar(lugar.id)
        if (numItems > 0) return "Este lugar tiene $numItems ítem(s) activo(s) y no puede eliminarse"
        dao.eliminarLugar(lugar)
        return null
    }

    fun marcarLugarDefault(lugar: ListaLugar) {
        viewModelScope.launch {
            dao.limpiarDefaultLugares()
            dao.actualizarLugar(lugar.copy(esDefault = true))
            _lugarActivoId.value = lugar.id
        }
    }

    // ─── Gestión de Categorías ────────────────────────────────────────────────

    fun añadirCategoria(nombre: String, colorHex: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val maxOrden = dao.obtenerMaxOrdenCategoria()
            dao.insertarCategoria(
                ListaCategoriaProducto(nombre = nombre.trim(), colorHex = colorHex, orden = maxOrden + 1)
            )
        }
    }

    suspend fun crearCategoriaYObtenerID(nombre: String, colorHex: String): Long {
        val maxOrden = dao.obtenerMaxOrdenCategoria()
        return dao.insertarCategoria(
            ListaCategoriaProducto(nombre = nombre.trim(), colorHex = colorHex, orden = maxOrden + 1)
        )
    }

    fun editarCategoria(cat: ListaCategoriaProducto, nombre: String, colorHex: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.actualizarCategoria(cat.copy(nombre = nombre.trim(), colorHex = colorHex))
        }
    }

    suspend fun eliminarCategoria(cat: ListaCategoriaProducto): String? {
        val numProductos = dao.contarProductosPorCategoria(cat.id)
        if (numProductos > 0) return "Esta categoría tiene $numProductos producto(s) asignado(s) y no puede eliminarse"
        dao.eliminarCategoria(cat)
        return null
    }

    fun moverCategoriaArriba(cat: ListaCategoriaProducto, lista: List<ListaCategoriaProducto>) {
        val idx = lista.indexOfFirst { it.id == cat.id }
        if (idx <= 0) return
        viewModelScope.launch {
            val prev = lista[idx - 1]
            dao.actualizarCategoria(cat.copy(orden = prev.orden))
            dao.actualizarCategoria(prev.copy(orden = cat.orden))
        }
    }

    fun moverCategoriaAbajo(cat: ListaCategoriaProducto, lista: List<ListaCategoriaProducto>) {
        val idx = lista.indexOfFirst { it.id == cat.id }
        if (idx < 0 || idx >= lista.size - 1) return
        viewModelScope.launch {
            val next = lista[idx + 1]
            dao.actualizarCategoria(cat.copy(orden = next.orden))
            dao.actualizarCategoria(next.copy(orden = cat.orden))
        }
    }

    // ─── Gestión de Productos ─────────────────────────────────────────────────

    fun añadirProducto(nombre: String, categoriaId: Long?, aliases: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.insertarProducto(
                ListaProducto(nombre = nombre.trim(), categoriaId = categoriaId, aliases = aliases.trim())
            )
        }
    }

    fun editarProducto(prod: ListaProducto, nombre: String, categoriaId: Long?, aliases: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.actualizarProducto(prod.copy(nombre = nombre.trim(), categoriaId = categoriaId, aliases = aliases.trim()))
        }
    }

    suspend fun eliminarProducto(prod: ListaProducto): String? {
        val numItems = dao.contarItemsPorProducto(prod.id)
        if (numItems > 0) return "Este producto está en uso en $numItems ítem(s) activo(s)"
        dao.eliminarProducto(prod)
        return null
    }

    // ─── Compartir lista ──────────────────────────────────────────────────────

    fun generarTextoCompartir(seleccionados: Set<Long>): String {
        val grupos = gruposPendientes.value
        val lugarNombre = lugares.value.firstOrNull { it.id == lugarActivoId.value }?.nombre ?: "la lista"
        val fecha = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val sb = StringBuilder()
        sb.appendLine("Lista de la Compra — $lugarNombre")
        sb.appendLine(fecha)
        grupos.forEach { grupo ->
            val items = grupo.items.filter { it.item.id in seleccionados }
            if (items.isEmpty()) return@forEach
            val emoji = emojiParaCategoria(grupo.nombreCategoria)
            sb.appendLine()
            sb.appendLine("$emoji ${grupo.nombreCategoria.uppercase()}")
            items.forEach { ic ->
                val item = ic.item
                val detalle = buildString {
                    if (item.cantidad.isNotBlank() && item.cantidad != "1") append(item.cantidad)
                    if (item.unidad.isNotBlank()) { if (isNotEmpty()) append(" "); append(item.unidad) }
                }
                sb.append("- ${ic.nombreProducto}")
                if (detalle.isNotBlank()) sb.append(" ($detalle)")
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }

    fun compartirLista(context: android.content.Context, seleccionados: Set<Long>) {
        val texto = generarTextoCompartir(seleccionados)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartir lista"))
    }

    private fun emojiParaCategoria(nombre: String): String {
        val n = normalizar(nombre)
        return when {
            n.contains("lacteo") -> "🥛"
            n.contains("bebida") -> "🥤"
            n.contains("fruta") -> "🍎"
            n.contains("verdura") -> "🥦"
            n.contains("carne") || n.contains("pescado") -> "🥩"
            n.contains("panaderia") || n.contains("pan") -> "🍞"
            n.contains("congelado") -> "🧊"
            n.contains("pasta") || n.contains("arroz") -> "🍝"
            n.contains("conserva") -> "🥫"
            n.contains("limpieza") -> "🧹"
            n.contains("higiene") -> "🧴"
            n.contains("desayuno") || n.contains("cereal") -> "🥣"
            n.contains("snack") -> "🍿"
            else -> "🛒"
        }
    }
}
