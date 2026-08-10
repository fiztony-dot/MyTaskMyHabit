package com.example.mistareasapp.viewmodel.Tasks

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.core.network.TareasApiRepository
import com.example.mistareasapp.core.notifications.tasks.NotificationHelper
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.components.tasks.OrdenCategorias
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ViewModel para Tareas que usa la API REST en lugar de Room.
 * Mantiene la misma interfaz pública que TareasViewModel para que las pantallas
 * no necesiten cambios.
 */
class TareasViewModel : ViewModel() {

    // --- 1. ESTADOS DE CONTROL DE INTERFAZ (UI STATE) ---
    var mostrarBarraFiltro by mutableStateOf(false)
    var mostrarCompletadas by mutableStateOf(false)
        private set
    var vistaActual by mutableStateOf(TipoVista.VENCIMIENTO)
        private set
    var todasSeccionesAbiertas by mutableStateOf(true)
        private set

    // --- 2. ESTADOS DE FILTRO ---
    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)
    val categoriaSeleccionada = _categoriaSeleccionada.asStateFlow()

    private val _categoriasExpandidas = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val categoriasExpandidas = _categoriasExpandidas.asStateFlow()

    private val _mensajeConfirmacion = MutableSharedFlow<String>()
    val mensajeConfirmacion = _mensajeConfirmacion.asSharedFlow()

    private val _ordenCategorias = MutableStateFlow(OrdenCategorias.ALFABETICO)
    val ordenCategorias: StateFlow<OrdenCategorias> = _ordenCategorias

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda = _textoBusqueda.asStateFlow()
    var buscadorVisible by mutableStateOf(false)

    // --- 3. ESTADOS DE CARGA Y ERROR ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorRed = MutableStateFlow<String?>(null)
    val errorRed = _errorRed.asStateFlow()

    // --- 4. DATOS ---
    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())

    val todasLasCategorias: StateFlow<List<Categoria>> = _categorias

    // Reloj para vencimientos
    private val _reloj = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDateTime.now())

    val listaTareas: StateFlow<List<Tarea>> = _tareas

    // Filtrado
    val tareasFiltradas: Flow<List<Tarea>> = combine(
        _tareas,
        _categoriaSeleccionada,
        _textoBusqueda
    ) { lista, filtro, texto ->
        lista.filter { tarea ->
            val coincideCategoria = filtro == null || tarea.categoria == filtro
            val coincideTexto = tarea.titulo.contains(texto, ignoreCase = true) ||
                    tarea.descripcion?.contains(texto, ignoreCase = true) == true
            coincideCategoria && coincideTexto
        }
    }

    // Clasificación por vencimiento
    val tareasClasificadas: StateFlow<MapasDeTareas> = combine(
        tareasFiltradas,
        _reloj
    ) { lista, ahora ->
        clasificarTareas(lista, ahora)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapasDeTareas())

    // Clasificación por categorías
    val tareasPorCategoria: StateFlow<Map<String, List<Tarea>>> =
        tareasFiltradas
            .map { lista ->
                lista.filter { !it.estaCompletada }
                    .groupBy { it.categoria ?: "Sin categoría" }
                    .mapValues { (_, tareas) ->
                        tareas.sortedWith(
                            compareByDescending<Tarea> { t ->
                                when (t.prioridad) {
                                    Prioridad.ALTA -> 3
                                    Prioridad.MEDIA -> 2
                                    Prioridad.BAJA -> 1
                                }
                            }.thenBy { it.fechaLimite ?: LocalDate.MAX }
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- 5. CARGA INICIAL ---

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorRed.value = null
            try {
                val categorias = TareasApiRepository.obtenerCategorias()
                _categorias.value = categorias
                TareasApiRepository.actualizarMapaCategorias(categorias)

                val tareas = TareasApiRepository.obtenerTodas()
                _tareas.value = tareas
            } catch (e: Exception) {
                Log.e("TareasApiVM", "Error cargando datos: ${e.message}")
                _errorRed.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refrescarDatos() = cargarDatos()

    fun limpiarError() { _errorRed.value = null }

    // --- 6. CRUD TAREAS ---

    fun insertar(tarea: Tarea, context: Context) = viewModelScope.launch {
        try {
            val nuevoId = TareasApiRepository.insertar(tarea)
            if (tarea.fechaLimite != null) {
                NotificationHelper.programarNotificacion(context, tarea.copy(id = nuevoId.toInt()))
            }
            cargarDatos()
        } catch (e: Exception) {
            _errorRed.value = "Error al crear tarea: ${e.message}"
        }
    }

    fun actualizar(tarea: Tarea, context: Context) = viewModelScope.launch {
        try {
            TareasApiRepository.actualizar(tarea)
            NotificationHelper.cancelarNotificacion(context, tarea.id)
            if (tarea.fechaLimite != null && !tarea.estaCompletada) {
                NotificationHelper.programarNotificacion(context, tarea)
            }
            cargarDatos()
        } catch (e: Exception) {
            _errorRed.value = "Error al actualizar: ${e.message}"
        }
    }

    fun eliminar(tarea: Tarea) = viewModelScope.launch {
        try {
            TareasApiRepository.eliminar(tarea)
            cargarDatos()
        } catch (e: Exception) {
            _errorRed.value = "Error al eliminar: ${e.message}"
        }
    }

    fun obtenerTareaPorId(id: Int): Flow<Tarea?> = flow {
        try {
            val tarea = TareasApiRepository.obtenerTareaPorId(id)
            emit(tarea)
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun completarTarea(tarea: Tarea, context: Context) {
        viewModelScope.launch {
            try {
                TareasApiRepository.completar(tarea, true)
                NotificationHelper.cancelarNotificacion(context, tarea.id)

                // Lógica de repetición (igual que el VM original)
                if (tarea.repeticion != "Sin repetición" && tarea.fechaLimite != null) {
                    val nuevaFecha = cuandoSeraLaSiguiente(tarea.fechaLimite, tarea.repeticion)
                    val nuevoContador = tarea.repeticionContador + 1
                    val limiteVecesAlcanzado = tarea.repeticionVeces != null && nuevoContador >= tarea.repeticionVeces
                    val limiteFinAlcanzado = tarea.repeticionFin != null && !nuevaFecha.isBefore(tarea.repeticionFin)
                    if (!limiteVecesAlcanzado && !limiteFinAlcanzado) {
                        val nuevaTarea = tarea.copy(
                            id = 0,
                            estaCompletada = false,
                            fechaLimite = nuevaFecha,
                            fechaCreacion = System.currentTimeMillis(),
                            repeticionContador = nuevoContador
                        )
                        TareasApiRepository.insertar(nuevaTarea)
                    }
                }
                cargarDatos()
            } catch (e: Exception) {
                _errorRed.value = "Error al completar: ${e.message}"
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

    // --- 7. CATEGORÍAS ---

    fun insertarCategoria(categoria: Categoria) = viewModelScope.launch {
        try {
            TareasApiRepository.insertarCategoria(categoria)
            cargarDatos()
        } catch (e: Exception) {
            _errorRed.value = "Error al crear categoría: ${e.message}"
        }
    }

    fun eliminarCategoria(categoria: Categoria) = viewModelScope.launch {
        try {
            TareasApiRepository.eliminarCategoria(categoria)
            cargarDatos()
        } catch (e: Exception) {
            _errorRed.value = "Error al eliminar categoría: ${e.message}"
        }
    }

    fun actualizarCategoria(categoria: Categoria) = viewModelScope.launch {
        try {
            TareasApiRepository.actualizarCategoria(categoria)
            cargarDatos()
        } catch (e: Exception) {
            _errorRed.value = "Error al actualizar categoría: ${e.message}"
        }
    }

    // --- 8. UI LOGIC (idéntica al VM original) ---

    fun actualizarBusqueda(nuevoTexto: String) { _textoBusqueda.value = nuevoTexto }

    fun alternarCategoria(nombreCat: String) {
        val mapaActual = _categoriasExpandidas.value
        val estadoActual = mapaActual[nombreCat] ?: true
        _categoriasExpandidas.value = mapaActual.toMutableMap().apply { this[nombreCat] = !estadoActual }
    }

    fun filtrarPor(categoria: String?) { _categoriaSeleccionada.value = categoria }

    fun cambiarVista(nuevaVista: TipoVista) { vistaActual = nuevaVista }

    fun cambiarEstadoGlobalSecciones(abierto: Boolean) {
        todasSeccionesAbiertas = abierto
        viewModelScope.launch {
            val nombres = tareasPorCategoria.value.keys
            _categoriasExpandidas.value = nombres.associateWith { abierto }
        }
    }

    fun cambiarVisibilidadCompletadas(mostrar: Boolean) { mostrarCompletadas = mostrar }

    fun cambiarOrdenCategorias(nuevoOrden: OrdenCategorias) { _ordenCategorias.value = nuevoOrden }

    /** Compatibilidad con la pantalla de backup/restore. Con la API no hace nada — los datos están en el servidor. */
    @Suppress("UNUSED_PARAMETER")
    fun actualizarDaos(vararg args: Any?) {
        // No-op: con la API no se necesitan DAOs locales. Recargar datos del servidor.
        cargarDatos()
    }

    fun crearTareaDesdeVoz(texto: String) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            try {
                val nuevaTarea = Tarea(
                    titulo = texto,
                    descripcion = "",
                    estaCompletada = false,
                    fechaCreacion = System.currentTimeMillis(),
                    pendienteClasificar = true
                )
                TareasApiRepository.insertar(nuevaTarea)
                _mensajeConfirmacion.emit("Tarea creada: $texto")
                cargarDatos()
            } catch (e: Exception) {
                _mensajeConfirmacion.emit("Error al guardar: ${e.message}")
            }
        }
    }

    fun marcarClasificada(tarea: Tarea) {
        viewModelScope.launch {
            try {
                TareasApiRepository.actualizar(tarea.copy(pendienteClasificar = false))
                cargarDatos()
            } catch (e: Exception) {
                _errorRed.value = "Error: ${e.message}"
            }
        }
    }

    fun eliminarTarea(tarea: Tarea, context: Context) {
        viewModelScope.launch {
            try {
                TareasApiRepository.eliminar(tarea)
                NotificationHelper.cancelarNotificacion(context, tarea.id)
                cargarDatos()
            } catch (e: Exception) {
                _errorRed.value = "Error al eliminar: ${e.message}"
            }
        }
    }

    fun archivarTarea(tarea: Tarea, context: Context) {
        viewModelScope.launch {
            try {
                TareasApiRepository.completar(tarea, true)
                NotificationHelper.cancelarNotificacion(context, tarea.id)
                cargarDatos()
            } catch (e: Exception) {
                _errorRed.value = "Error al archivar: ${e.message}"
            }
        }
    }

    // --- Helper para clasificar tareas (misma lógica que el VM original) ---
    private fun clasificarTareas(lista: List<Tarea>, ahora: LocalDateTime): MapasDeTareas {
        val pendientesClasificar = lista.filter { it.pendienteClasificar && !it.estaCompletada }
        val listaFiltrada = lista.filter { !it.pendienteClasificar }
        val hoyFecha = ahora.toLocalDate()
        val diasHastaDomingo = 7 - hoyFecha.dayOfWeek.value.toLong()
        val finDeSemanaActual = hoyFecha.plusDays(diasHastaDomingo)
        val finDeMesActual = hoyFecha.withDayOfMonth(hoyFecha.lengthOfMonth())

        val vencidas = listaFiltrada.filter {
            if (it.estaCompletada) return@filter false
            val momentoActual = LocalDateTime.now()
            when {
                it.fechaLimite == null -> false
                it.fechaLimite.isBefore(hoyFecha) -> true
                it.fechaLimite.isEqual(hoyFecha) -> {
                    if (it.horaLimite != null) {
                        LocalDateTime.of(it.fechaLimite, it.horaLimite).isBefore(momentoActual)
                    } else false
                }
                else -> false
            }
        }.sortedBy { it.toComparableDateTime() }

        val esHoy = listaFiltrada.filter {
            if (it.estaCompletada || it.fechaLimite != hoyFecha) return@filter false
            it.horaLimite == null || it.toComparableDateTime().isAfter(ahora)
        }.sortedBy { it.horaLimite }

        val estaSemana = listaFiltrada.filter {
            if (it.estaCompletada || it.fechaLimite == null) return@filter false
            it.fechaLimite!!.isAfter(hoyFecha) && !it.fechaLimite!!.isAfter(finDeSemanaActual)
        }.sortedBy { it.fechaLimite }

        val esteMes = listaFiltrada.filter {
            !it.estaCompletada && it.fechaLimite != null &&
                    it.fechaLimite!!.isAfter(finDeSemanaActual) && !it.fechaLimite!!.isAfter(finDeMesActual)
        }.sortedBy { it.fechaLimite }

        val completadas = listaFiltrada.filter { it.estaCompletada }.sortedByDescending { it.fechaCreacion }

        val resto = listaFiltrada.filter {
            !it.estaCompletada && it !in vencidas && it !in esHoy && it !in estaSemana && it !in esteMes
        }.sortedWith(compareBy<Tarea> { it.fechaLimite == null }.thenBy { it.toComparableDateTime() })

        return MapasDeTareas(pendientesClasificar, vencidas, esHoy, estaSemana, esteMes, resto, completadas)
    }
}
