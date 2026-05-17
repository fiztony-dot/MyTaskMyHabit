package com.example.mistareasapp.viewmodel.Habits

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.data.habits.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// Definir el enum de vistas
enum class TipoVistaHabitos { FLASH, LISTADO, ESTADISTICAS }

class HabitosViewModel(private val habitoDao: HabitoDao) : ViewModel() {

    // --- ESTADO DE NAVEGACIÓN INTERNA ---
    var vistaActual by mutableStateOf(TipoVistaHabitos.FLASH)
        private set

    fun cambiarVista(nuevaVista: TipoVistaHabitos) {
        vistaActual = nuevaVista
    }
    // --- 1. FLUJOS DE DATOS (ESTADO) ---

    // Todos los hábitos de la base de datos
    val todosLosHabitos: Flow<List<Habito>> = habitoDao.obtenerTodosLosHabitos()

    // Categorías de hábitos
    val categoriasHabitos: Flow<List<CategoriaHabito>> = habitoDao.obtenerCategorias()

    // Fecha que el usuario está viendo actualmente
    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada = _fechaSeleccionada.asStateFlow()

    // La lista combinada para la UI (Hábito + su progreso de ese día)
    val habitosConProgreso: StateFlow<List<HabitoConProgreso>> = combine(
        todosLosHabitos,
        _fechaSeleccionada
    ) { listaHabitos, fecha ->
        listaHabitos.map { habito ->
            val progreso = habitoDao.obtenerProgresoDiario(habito.id, fecha)
            HabitoConProgreso(habito, progreso)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- 2. GESTIÓN DE HÁBITOS (CRUD) ---

    fun insertarHabito(habito: Habito, tareas: List<TareaHabito> = emptyList()) {
        viewModelScope.launch {
            habitoDao.insertarHabitoConTareas(habito, tareas)
        }
    }

    fun actualizarHabito(habito: Habito) {
        viewModelScope.launch {
            habitoDao.actualizarHabito(habito)
        }
    }

    fun eliminarHabito(habito: Habito) {
        viewModelScope.launch {
            // Borramos el historial primero y luego el hábito
            habitoDao.eliminarHistorialDeHabito(habito.id)
            habitoDao.eliminarHabito(habito)
        }
    }

    // --- 2B. GESTIÓN DE CATEGORÍAS DE HÁBITOS ---

    fun insertarCategoriaHabito(categoria: CategoriaHabito) {
        viewModelScope.launch {
            habitoDao.insertarCategoria(categoria)
        }
    }

    fun actualizarCategoriaHabito(categoria: CategoriaHabito) {
        viewModelScope.launch {
            habitoDao.actualizarCategoria(categoria)
        }
    }

    fun eliminarCategoriaHabito(categoria: CategoriaHabito) {
        viewModelScope.launch {
            habitoDao.eliminarCategoria(categoria)
        }
    }


    // --- 3. ACCIONES DE PROGRESO DIARIO ---

    fun cambiarFecha(nuevaFecha: LocalDate) {
        _fechaSeleccionada.value = nuevaFecha
    }

    // Para hábitos numéricos (ej: +1 vaso de agua)
    fun incrementarProgreso(habito: Habito, progresoActual: HabitoHistorial?) {
        viewModelScope.launch {
            val nuevoValor = (progresoActual?.valorProgreso ?: 0) + 1
            val estaCompletado = nuevoValor >= habito.vecesPorDia

            val nuevoProgreso = progresoActual?.copy(
                valorProgreso = nuevoValor,
                completado = estaCompletado
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = _fechaSeleccionada.value,
                valorProgreso = nuevoValor,
                completado = estaCompletado
            )

            habitoDao.upsertProgreso(nuevoProgreso)
        }
    }

    // Para hábitos de Check (Hecho/No hecho)
    fun toggleHabitoCompleto(habito: Habito, progresoActual: HabitoHistorial?) {
        viewModelScope.launch {
            val yaEstabaCompletado = progresoActual?.completado ?: false
            val nuevoProgreso = progresoActual?.copy(
                completado = !yaEstabaCompletado,
                valorProgreso = if (!yaEstabaCompletado) habito.vecesPorDia else 0
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = _fechaSeleccionada.value,
                valorProgreso = if (!yaEstabaCompletado) habito.vecesPorDia else 0,
                completado = !yaEstabaCompletado
            )

            habitoDao.upsertProgreso(nuevoProgreso)
        }
    }

    // --- 4. OBTENER TAREAS DE UN HÁBITO ---

    fun obtenerTareasDeHabito(habitoId: Long) = habitoDao.obtenerTareasDeHabito(habitoId)

    // --- 5. REGISTRAR CUMPLIMIENTO POR TAREAS ---

    fun registrarCumplimientoTareas(habito: Habito, progresoActual: HabitoHistorial?, tareasCumplidasCount: Int) {
        viewModelScope.launch {
            val minimoRequerido = habito.minimoTareasCumplimiento ?: habito.vecesPorDia
            val estaCompletado = tareasCumplidasCount >= minimoRequerido

            val nuevoProgreso = progresoActual?.copy(
                valorProgreso = tareasCumplidasCount,
                completado = estaCompletado
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = _fechaSeleccionada.value,
                valorProgreso = tareasCumplidasCount,
                completado = estaCompletado
            )

            habitoDao.upsertProgreso(nuevoProgreso)
        }
    }

    // --- 6. REGISTRAR CUMPLIMIENTO CUANTITATIVO ---

    fun registrarCumplimientoCuantitativo(habito: Habito, progresoActual: HabitoHistorial?, cantidad: Int) {
        viewModelScope.launch {
            val objetivoFinal = habito.objetivoValor ?: habito.vecesPorDia
            val estaCompletado = cantidad >= objetivoFinal

            val nuevoProgreso = progresoActual?.copy(
                valorProgreso = cantidad,
                completado = estaCompletado
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = _fechaSeleccionada.value,
                valorProgreso = cantidad,
                completado = estaCompletado
            )

            habitoDao.upsertProgreso(nuevoProgreso)
        }
    }
}