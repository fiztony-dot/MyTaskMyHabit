package com.example.mistareasapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.OrdenCategorias
import com.example.mistareasapp.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

// --- DATA CLASS PARA LA ESTRUCTURA DE LA UI ---
data class MapasDeTareas(
    val vencidas: List<Tarea> = emptyList(),
    val hoy: List<Tarea> = emptyList(),
    val estaSemana: List<Tarea> = emptyList(),
    val esteMes: List<Tarea> = emptyList(),
    val resto: List<Tarea> = emptyList(),
    val completadas: List<Tarea> = emptyList()
)

enum class TipoVista { VENCIMIENTO, CATEGORIAS }

// --- VIEWMODEL PRINCIPAL ---
class TareasViewModel(
    private var tareaDao: TareaDao,
    private var categoriaDao: CategoriaDao
) : ViewModel() {

    // --- 1. ESTADOS DE CONTROL DE INTERFAZ (UI STATE) ---
    var mostrarBarraFiltro by mutableStateOf(false)
    var mostrarCompletadas by mutableStateOf(false)
        private set
    var vistaActual by mutableStateOf(TipoVista.VENCIMIENTO)
        private set
    var todasSeccionesAbiertas by mutableStateOf(true)
        private set

    // --- 2. ESTADOS DE FILTRO Y NOTIFICACIONES ---
    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)
    val categoriaSeleccionada = _categoriaSeleccionada.asStateFlow()

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val _categoriasExpandidas = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val categoriasExpandidas = _categoriasExpandidas.asStateFlow()

    private val _mensajeConfirmacion = MutableSharedFlow<String>()
    val mensajeConfirmacion = _mensajeConfirmacion.asSharedFlow()

    private val _ordenCategorias = MutableStateFlow(OrdenCategorias.ALFABETICO)
    val ordenCategorias: StateFlow<OrdenCategorias> = _ordenCategorias

    // El RELOJ: Emite la fecha/hora actual cada minuto para refrescar los vencimientos en tiempo real
    private val _reloj = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDateTime.now())

    // --- 3. FLUJOS DE DATOS (DATA FLOWS) ---

    // Exponemos las categorías directamente desde el DAO
    val todasLasCategorias: Flow<List<Categoria>> = categoriaDao.obtenerTodas()

    @OptIn(ExperimentalCoroutinesApi::class)
    val listaTareas: Flow<List<Tarea>> = refreshTrigger.flatMapLatest {
        try {
            tareaDao.obtenerTodas()
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    // FUENTE DE VERDAD FILTRADA: Todas las pantallas (Vencimiento y Categorías) consumen de aquí
    @OptIn(ExperimentalCoroutinesApi::class)
    val tareasFiltradas: Flow<List<Tarea>> = combine(
        listaTareas,
        _categoriaSeleccionada
    ) { lista, filtro ->
        if (filtro == null) lista
        else lista.filter { it.categoria == filtro }
    }

    // --- 4. CLASIFICACIÓN POR VENCIMIENTO (VISTA PRINCIPAL) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val tareasClasificadas: StateFlow<MapasDeTareas> = combine(
        tareasFiltradas,
        _reloj
    ) { lista, ahora ->
        val hoyFecha = ahora.toLocalDate()
        val limiteSemana = hoyFecha.plusDays(7)
        val diasHastaDomingo = 7 - hoyFecha.dayOfWeek.value.toLong()
        val finDeSemanaActual = hoyFecha.plusDays(diasHastaDomingo)
        val finDeMesActual = hoyFecha.withDayOfMonth(hoyFecha.lengthOfMonth())

        // 1. Tareas Vencidas
        val vencidas = lista.filter {
            if (it.estaCompletada) return@filter false
            val momentoActual = LocalDateTime.now()

            when {
                it.fechaLimite == null -> false
                it.fechaLimite.isBefore(hoyFecha) -> true
                it.fechaLimite.isEqual(hoyFecha) -> {
                    if (it.horaLimite != null) {
                        val fechaHoraTarea = LocalDateTime.of(it.fechaLimite, it.horaLimite)
                        fechaHoraTarea.isBefore(momentoActual)
                    } else {
                        false
                    }
                }
                else -> false
            }
        }.sortedBy { it.toComparableDateTime() }

        // 2. Tareas para Hoy
        val esHoy = lista.filter {
            if (it.estaCompletada || it.fechaLimite != hoyFecha) return@filter false
            it.horaLimite == null || it.toComparableDateTime().isAfter(ahora)
        }.sortedBy { it.horaLimite }

        // 3. Tareas para esta Semana
        val estaSemana = lista.filter {
            if (it.estaCompletada || it.fechaLimite == null) return@filter false
            it.fechaLimite!!.isAfter(hoyFecha) && !it.fechaLimite!!.isAfter(finDeSemanaActual)
        }.sortedBy { it.fechaLimite }

        // 4. Tareas para este Mes
        val esteMes = lista.filter {
            !it.estaCompletada && it.fechaLimite != null &&
                    it.fechaLimite!!.isAfter(finDeSemanaActual) && !it.fechaLimite!!.isAfter(finDeMesActual)
        }.sortedBy { it.fechaLimite }

        // 5. Tareas Completadas
        val completadas = lista.filter { it.estaCompletada }.sortedByDescending { it.fechaCreacion }

        // 6. Resto de Tareas
        val resto = lista.filter {
            !it.estaCompletada && it !in vencidas && it !in esHoy && it !in estaSemana && it !in esteMes
        }.sortedWith(
            compareBy<Tarea> { it.fechaLimite == null }.thenBy { it.toComparableDateTime() }
        )

        MapasDeTareas(vencidas, esHoy, estaSemana, esteMes, resto, completadas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapasDeTareas())

    // --- 5. CLASIFICACIÓN POR CATEGORÍAS (VISTA POR CATEGORÍAS) ---
    val tareasPorCategoria: StateFlow<Map<String, List<Tarea>>> =
        tareasFiltradas
            .map { lista ->
                lista
                    .filter { !it.estaCompletada }
                    .groupBy { it.categoria ?: "Sin categoría" }
                    .mapValues { (_, tareas) ->
                        tareas.sortedWith(
                            compareByDescending<Tarea> { t ->
                                when(t.prioridad) {
                                    Prioridad.ALTA -> 3
                                    Prioridad.MEDIA -> 2
                                    Prioridad.BAJA -> 1
                                    else -> 0
                                }
                            }.thenBy { it.fechaLimite ?: LocalDate.MAX }
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- 6. OPERACIONES DE TAREAS (CRUD) ---

    fun insertar(tarea: Tarea) = viewModelScope.launch { tareaDao.insertar(tarea) }

    fun actualizar(tarea: Tarea) = viewModelScope.launch { tareaDao.actualizar(tarea) }

    fun eliminar(tarea: Tarea) = viewModelScope.launch { tareaDao.eliminar(tarea) }

    fun obtenerTareaPorId(id: Int): Flow<Tarea?> = tareaDao.obtenerTareaPorId(id)

    fun completarTarea(tarea: Tarea, context: Context) {
        viewModelScope.launch {
            actualizar(tarea.copy(estaCompletada = true))

            // Cancelación de notificaciones en WorkManager
            val wm = androidx.work.WorkManager.getInstance(context)
            wm.cancelUniqueWork("notif_${tarea.id}")
            wm.cancelUniqueWork("notif_${tarea.id}_principal")
            wm.cancelUniqueWork("notif_${tarea.id}_repeticion")

            Log.d("LOG-NOTIF", "🚫 Notificaciones canceladas para ID: ${tarea.id}")

            // Lógica de repetición
            if (tarea.repeticion != "Sin repetición" && tarea.fechaLimite != null) {
                val nuevaFecha = cuandoSeraLaSiguiente(tarea.fechaLimite, tarea.repeticion)
                val nuevaTarea = tarea.copy(
                    id = 0,
                    estaCompletada = false,
                    fechaLimite = nuevaFecha,
                    fechaCreacion = System.currentTimeMillis()
                )
                insertar(nuevaTarea)
            }
        }
    }

    private fun cuandoSeraLaSiguiente(fechaActual: LocalDate, repeticion: String): LocalDate {
        return when (repeticion) {
            "Una vez al día" -> fechaActual.plusDays(1)
            "Una vez a la semana" -> fechaActual.plusWeeks(1)
            "Una vez al mes" -> fechaActual.plusMonths(1)
            "Una vez al año" -> fechaActual.plusYears(1)
            "Cada domingo" -> fechaActual.plusWeeks(1)
            "Día 1 de cada mes" -> fechaActual.plusMonths(1).withDayOfMonth(1)
            else -> fechaActual
        }
    }

    // --- 7. GESTIÓN DE CATEGORÍAS ---

    fun insertarCategoria(categoria: Categoria) = viewModelScope.launch {
        categoriaDao.insertar(categoria)
    }

    fun eliminarCategoria(categoria: Categoria) = viewModelScope.launch {
        categoriaDao.eliminar(categoria)
    }

    fun actualizarCategoria(categoria: Categoria) = viewModelScope.launch {
        categoriaDao.actualizar(categoria)
    }

    fun alternarCategoria(nombreCat: String) {
        val mapaActual = _categoriasExpandidas.value
        val estadoActual = mapaActual[nombreCat] ?: true
        _categoriasExpandidas.value = mapaActual.toMutableMap().apply {
            this[nombreCat] = !estadoActual
        }
    }

    fun filtrarPor(categoria: String?) {
        _categoriaSeleccionada.value = categoria
    }

    // --- 8. LÓGICA DE VISTA Y EXPANSIÓN GLOBAL ---

    fun cambiarVista(nuevaVista: TipoVista) {
        vistaActual = nuevaVista
    }

    fun cambiarEstadoGlobalSecciones(abierto: Boolean) {
        todasSeccionesAbiertas = abierto
        viewModelScope.launch {
            val nombres = tareasPorCategoria.value.keys
            val nuevoMapa = nombres.associateWith { abierto }
            _categoriasExpandidas.value = nuevoMapa
        }
    }

    fun cambiarVisibilidadCompletadas(mostrar: Boolean) {
        mostrarCompletadas = mostrar
    }

    fun cambiarOrdenCategorias(nuevoOrden: OrdenCategorias) {
        _ordenCategorias.value = nuevoOrden
    }

    // --- 9. BACKUP, REFRESH Y VOZ ---

    fun actualizarDaos(nuevoTareaDao: TareaDao, nuevoCategoriaDao: CategoriaDao) {
        this.tareaDao = nuevoTareaDao
        this.categoriaDao = nuevoCategoriaDao
        refrescarDatos()
    }

    fun refrescarDatos() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    fun crearTareaDesdeVoz(texto: String) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            try {
                val nuevaTarea = Tarea(
                    titulo = texto,
                    descripcion = "",
                    estaCompletada = false,
                    fechaCreacion = System.currentTimeMillis()
                )
                insertar(nuevaTarea)
                _mensajeConfirmacion.emit("Tarea creada: $texto")
            } catch (e: Exception) {
                _mensajeConfirmacion.emit("Error al guardar: ${e.message}")
            }
        }
    }

    fun eliminarTarea(tarea: Tarea) {
        viewModelScope.launch {
            tareaDao.eliminar(tarea)
        }
    }

    fun archivarTarea(tarea: Tarea) {
        viewModelScope.launch {
            actualizar(tarea.copy(estaCompletada = true))
        }
    }

    init {
        viewModelScope.launch {
            todasLasCategorias.collect { lista ->
                Log.d("DEBUG", "Categorías cargadas: ${lista.size}")
            }
        }
    }
}