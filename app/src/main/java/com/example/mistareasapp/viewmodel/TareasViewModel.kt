package com.example.mistareasapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.data.TareaDao
import com.example.mistareasapp.data.Categoria
import com.example.mistareasapp.data.CategoriaDao // Importante añadir este
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi // Necesario para flatMapLatest
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit // Opcional, pero útil si quieres hacer cálculos avanzados
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.mistareasapp.OrdenCategorias



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

// --- VIEWMODEL ---
// Pasamos ambos DAOs por el constructor
class TareasViewModel(
    private var tareaDao: TareaDao,
    private var categoriaDao: CategoriaDao
) : ViewModel() {
    private val _mensajeConfirmacion = MutableSharedFlow<String>()
    val mensajeConfirmacion = _mensajeConfirmacion.asSharedFlow()

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())
    // 1. Exponemos las categorías directamente desde el DAO
    val todasLasCategorias: Flow<List<Categoria>> = categoriaDao.obtenerTodas()

    var mostrarCompletadas by mutableStateOf(false)
        private set

    fun cambiarVisibilidadCompletadas(mostrar: Boolean) {
        mostrarCompletadas = mostrar
    }

    // 2. EL RELOJ: Emite la fecha/hora actual cada minuto
    private val _reloj = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDateTime.now())

    // 3. ORIGEN DE DATOS
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val listaTareas: Flow<List<Tarea>> = refreshTrigger.flatMapLatest {
        try {
            tareaDao.obtenerTodas()
        } catch (e: Exception) {
            // Si hay un error de conexión cerrada, devolvemos flujo vacío
            // hasta que el trigger se vuelva a disparar
            flowOf(emptyList())
        }
    }

    // 4. Clasificacion por Vencimiento
    @OptIn(ExperimentalCoroutinesApi::class)
    val tareasClasificadas: StateFlow<MapasDeTareas> = refreshTrigger.flatMapLatest {
        // Al meter el combine AQUÍ DENTRO, forzamos a que todo el sistema
        // de carpetas se destruya y se cree de nuevo con la conexión limpia
        combine(tareaDao.obtenerTodas(), _reloj) { lista, ahora ->
            val hoyFecha = ahora.toLocalDate()
            val limiteSemana = hoyFecha.plusDays(7)

            // Tareas Vencidas
            val vencidas = lista.filter {
                if (it.estaCompletada) return@filter false

                val hoy = LocalDate.now()
                val momentoActual = LocalDateTime.now()

                when {
                    // 1. Si no tiene fecha, no puede estar vencida
                    it.fechaLimite == null -> false

                    // 2. Si la fecha es anterior a hoy (ayer o antes), está vencida sí o sí
                    it.fechaLimite.isBefore(hoy) -> true

                    // 3. Si la fecha es HOY:
                    it.fechaLimite.isEqual(hoy) -> {
                        if (it.horaLimite != null) {
                            // Si tiene hora, comparamos con la hora actual
                            val fechaHoraTarea = LocalDateTime.of(it.fechaLimite, it.horaLimite)
                            fechaHoraTarea.isBefore(momentoActual)
                        } else {
                            // SI NO TIENE HORA, no vence hasta mañana
                            // Por tanto, hoy todavía NO está vencida
                            false
                        }
                    }

                    // 4. Si la fecha es en el futuro, no está vencida
                    else -> false
                }
            }.sortedBy { it.toComparableDateTime() }

            //Tareas que vencen Hoy
            val esHoy = lista.filter {
                if (it.estaCompletada || it.fechaLimite != hoyFecha) return@filter false

                // Si es hoy, se queda en la lista si:
                // A) No tiene hora (vence al final del día)
                // B) Tiene hora y aún no ha pasado
                it.horaLimite == null || it.toComparableDateTime().isAfter(ahora)
            }.sortedBy { it.horaLimite }


            // Tareas que vencen en esta semana
            val diasHastaDomingo = 7 - hoyFecha.dayOfWeek.value.toLong()
            val finDeSemanaActual = hoyFecha.plusDays(diasHastaDomingo)
            val estaSemana = lista.filter {
                if (it.estaCompletada || it.fechaLimite == null) return@filter false

                // Debe ser posterior a hoy Y anterior o igual al domingo de esta semana
                it.fechaLimite!!.isAfter(hoyFecha) && !it.fechaLimite!!.isAfter(finDeSemanaActual)
            }.sortedBy { it.fechaLimite }

            // Tareas que vencen este mes
            val finDeMesActual = hoyFecha.withDayOfMonth(hoyFecha.lengthOfMonth())
            val esteMes = lista.filter {
                !it.estaCompletada && it.fechaLimite != null &&
                        // Debe ser después del domingo de esta semana
                        it.fechaLimite!!.isAfter(finDeSemanaActual) &&
                        // Pero dentro del mes actual (antes o igual al último día del mes)
                        !it.fechaLimite!!.isAfter(finDeMesActual)
            }.sortedBy { it.fechaLimite }

            // Tareas Completadas
            val completadas = lista.filter { it.estaCompletada }.sortedByDescending { it.fechaCreacion }

            // Resto de Tareas (no vencidas, no hoy, no esta semana, no este mes
            val resto = lista.filter {
                !it.estaCompletada &&
                        it !in vencidas &&
                        it !in esHoy &&
                        it !in estaSemana &&
                        it !in esteMes // <--- Importante añadir la nueva lista aquí
            }.sortedWith(
                compareBy<Tarea> { it.fechaLimite == null }
                    .thenBy { it.toComparableDateTime() }
            )

            MapasDeTareas(vencidas, esHoy, estaSemana, esteMes, resto, completadas)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(0), // Importante el 0 para que no guarde basura
        initialValue = MapasDeTareas()
    )

    // --- OPERACIONES ---
    fun insertar(tarea: Tarea) = viewModelScope.launch { tareaDao.insertar(tarea) }

    // CAMBIA ESTO EN TU VIEWMODEL:
    fun obtenerTareaPorId(id: Int): Flow<Tarea?> {
        return tareaDao.obtenerTareaPorId(id)
    }

    fun actualizar(tarea: Tarea) = viewModelScope.launch { tareaDao.actualizar(tarea) }

    fun eliminar(tarea: Tarea) = viewModelScope.launch { tareaDao.eliminar(tarea) }

    init {
        viewModelScope.launch {
            todasLasCategorias.collect { lista ->
                println("DEBUG: Categorías cargadas: ${lista.size}")
                lista.forEach { println("DEBUG: Categoria encontrada: ${it.titulo}") }
            }
        }
    }
    fun refrescarDatos() {
        // Esto obliga al Flow a desconectarse de la DB vieja y
        // conectarse a la nueva instancia que creamos tras el backup
        refreshTrigger.value = System.currentTimeMillis()
    }

    // --- OPERACIONES DE CATEGORÍAS (Añade estas) ---
    fun insertarCategoria(categoria: Categoria) = viewModelScope.launch {
        categoriaDao.insertar(categoria)
    }

    fun eliminarCategoria(categoria: Categoria) = viewModelScope.launch {
        categoriaDao.eliminar(categoria)
    }

    fun actualizarCategoria(categoria: Categoria) = viewModelScope.launch {
        categoriaDao.actualizar(categoria)
    }

    // Esta es la función que llama MiApp.kt tras el backup
    fun actualizarDaos(nuevoTareaDao: TareaDao, nuevoCategoriaDao: CategoriaDao) {
        this.tareaDao = nuevoTareaDao
        this.categoriaDao = nuevoCategoriaDao
        refrescarDatos()
    }
    fun completarTarea(tarea: Tarea, context: android.content.Context) {
        viewModelScope.launch {
            // 1. Marcar la tarea actual como completada
            actualizar(tarea.copy(estaCompletada = true))

            // 2. CANCELAR LA NOTIFICACIÓN (Usamos el context que ahora recibe la función)
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("notif_${tarea.id}")
            android.util.Log.d("NOTIF_DEBUG", "Cancelando repetición para tarea ID: ${tarea.id}")
            // ----------------------------------------------

            // 2. Si tiene repetición y fecha límite, crear la siguiente
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
    // Función auxiliar para calcular la fecha
    private fun cuandoSeraLaSiguiente(fechaActual: LocalDate, repeticion: String): LocalDate {
        return when (repeticion) {
            "Una vez al día" -> fechaActual.plusDays(1)
            "Una vez a la semana" -> fechaActual.plusWeeks(1)
            "Una vez al mes" -> fechaActual.plusMonths(1)
            "Una vez al año" -> fechaActual.plusYears(1)
            "Cada domingo" -> {
                fechaActual.plusWeeks(1)
            }
            "Día 1 de cada mes" -> {
                // Avanzamos al mes siguiente y forzamos que sea el día 1
                fechaActual.plusMonths(1).withDayOfMonth(1)
            }
            else -> fechaActual
        }
    }
    // Al final de TareasViewModel.kt
    var vistaActual by mutableStateOf(TipoVista.VENCIMIENTO)
        private set

    fun cambiarVista(nuevaVista: TipoVista) {
        vistaActual = nuevaVista
    }

    // El estado que controla si el icono es "Expandir" o "Contraer"
    var todasSeccionesAbiertas by mutableStateOf(true)
        private set

    // La función que llama el botón de la barra superior
    fun cambiarEstadoGlobalSecciones(abierto: Boolean) {
        todasSeccionesAbiertas = abierto
    }

    // Mapa de Categorías
    val mapasCategorias: StateFlow<MapasDeTareas> = listaTareas
        .map { lista ->
            val grupos = lista.filter { !it.estaCompletada }
                .groupBy { it.categoria ?: "Sin categoría" }

            MapasDeTareas(
                vencidas = grupos["Vencidas"] ?: emptyList(),
                hoy = grupos["Hoy"] ?: emptyList(),
                estaSemana = grupos["Esta Semana"] ?: emptyList(),
                resto = grupos["Resto"] ?: emptyList(),
                completadas = emptyList()
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MapasDeTareas()
        )



    // Mapa de Vencimiento (Creado aquí para evitar errores en la UI)
    val mapasVencimiento: StateFlow<MapasDeTareas> = tareasClasificadas
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MapasDeTareas()
        )
    val tareasPorCategoria: StateFlow<Map<String, List<Tarea>>> =
        listaTareas
            .map { lista ->
                lista
                    .filter { !it.estaCompletada }
                    .groupBy { it.categoria ?: "Sin categoría" }
                    .mapValues { (_, tareas) ->
                        tareas.sortedBy { it.toComparableDateTime() }
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyMap()
            )

    private val _ordenCategorias = MutableStateFlow(OrdenCategorias.ALFABETICO)
    val ordenCategorias: StateFlow<OrdenCategorias> = _ordenCategorias


    fun cambiarOrdenCategorias(nuevoOrden: OrdenCategorias) {
        _ordenCategorias.value = nuevoOrden
    }

    fun crearTareaDesdeVoz(texto: String) {
        if (texto.isBlank()) return

        viewModelScope.launch {
            try {
                // Creamos la tarea usando los campos exactos de tu ViewModel
                val nuevaTarea = Tarea(
                    titulo = texto,
                    descripcion = "",
                    estaCompletada = false,
                    fechaCreacion = System.currentTimeMillis()
                    // Si tu clase Tarea tiene más campos obligatorios (como categoria o prioridad),
                    // asegúrate de ponerles un valor por defecto aquí.
                )

                // USAMOS tareaDao e insertar (que son los nombres que tienes en tu clase)
                insertar(nuevaTarea)

                _mensajeConfirmacion.emit("Tarea creada: $texto")
            } catch (e: Exception) {
                _mensajeConfirmacion.emit("Error al guardar: ${e.message}")
                println("ERROR CRÍTICO BBDD: ${e.message}")
            }
        }
    }
    fun eliminarTarea(tarea: Tarea) {
        viewModelScope.launch {
            // Usamos tareaDao porque es lo que tienes definido arriba en tu constructor
            tareaDao.eliminar(tarea)
        }
    }

    fun archivarTarea(tarea: Tarea) {
        viewModelScope.launch {
            // Para archivar, normalmente marcamos la tarea como completada
            // o podrías añadir un campo "archivada" a tu clase Tarea.
            // Por ahora, vamos a marcarla como completada:
            actualizar(tarea.copy(estaCompletada = true))

            println("Tarea archivada: ${tarea.titulo}")
        }
    }
}

