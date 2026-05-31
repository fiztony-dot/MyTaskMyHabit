package com.example.mistareasapp.viewmodel.Habits

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.data.habits.*
import com.example.mistareasapp.data.habits.diasSemanaSet
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class TipoVistaHabitos { FLASH, LISTADO, ESTADISTICAS }

class HabitosViewModel(private val habitoDao: HabitoDao) : ViewModel() {

    var vistaActual by mutableStateOf(TipoVistaHabitos.FLASH)
        private set

    fun cambiarVista(nuevaVista: TipoVistaHabitos) {
        vistaActual = nuevaVista
    }

    var agruparPorCategoria by mutableStateOf(false)
        private set

    fun toggleAgruparPorCategoria() {
        agruparPorCategoria = !agruparPorCategoria
    }

    // --- FLUJOS DE DATOS ---

    val todosLosHabitos: Flow<List<Habito>> = habitoDao.obtenerTodosLosHabitos()
    val habitosPausados: StateFlow<List<Habito>> = todosLosHabitos
        .map { it.filter { h -> h.pausado } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val habitosActivos: Flow<List<Habito>> = todosLosHabitos.map { it.filter { h -> !h.pausado } }
    val categoriasHabitos: Flow<List<CategoriaHabito>> = habitoDao.obtenerCategorias()

    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada = _fechaSeleccionada.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    // Progreso del día seleccionado (para la vista Flash)
    val habitosConProgreso: StateFlow<List<HabitoConProgreso>> = combine(
        habitosActivos,
        _fechaSeleccionada,
        _refreshTrigger
    ) { listaHabitos, fecha, _ ->
        listaHabitos
            .filter { habito ->
                // Ocultar hábitos que no aplican el día seleccionado
                val dias = habito.diasSemanaSet()
                dias == null || fecha.dayOfWeek in dias
            }
            .map { habito ->
                val progreso = habitoDao.obtenerProgresoDiario(habito.id, fecha)
                val (valorPeriodo, completadosPeriodo) = when (habito.frecuencia) {
                    FrecuenciaHabito.DIARIA -> Pair(
                        progreso?.valorProgreso ?: 0,
                        if (progreso?.completado == true) 1 else 0
                    )
                    FrecuenciaHabito.SEMANAL -> {
                        val inicio = fecha.with(DayOfWeek.MONDAY)
                        val fin = minOf(inicio.plusDays(6), LocalDate.now())
                        val hist = habitoDao.obtenerHistorialEntreFechas(habito.id, inicio, fin)
                            .groupBy { it.fecha }.values.map { it.maxByOrNull { e -> e.id }!! }
                        Pair(hist.sumOf { it.valorProgreso }, hist.count { it.completado })
                    }
                    FrecuenciaHabito.MENSUAL -> {
                        val inicio = fecha.withDayOfMonth(1)
                        val fin = minOf(fecha.withDayOfMonth(fecha.lengthOfMonth()), LocalDate.now())
                        val hist = habitoDao.obtenerHistorialEntreFechas(habito.id, inicio, fin)
                            .groupBy { it.fecha }.values.map { it.maxByOrNull { e -> e.id }!! }
                        Pair(hist.sumOf { it.valorProgreso }, hist.count { it.completado })
                    }
                }
                HabitoConProgreso(habito, progreso, valorPeriodo, completadosPeriodo)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historial de toda la semana (para la vista Listado)
    val habitosConHistorialSemanal: StateFlow<List<HabitoConHistorialSemanal>> = combine(
        habitosActivos,
        _fechaSeleccionada,
        _refreshTrigger
    ) { listaHabitos, fecha, _ ->
        val lunes = fecha.with(DayOfWeek.MONDAY)
        val domingo = lunes.plusDays(6)
        listaHabitos.map { habito ->
            val historial = habitoDao.obtenerHistorialEntreFechas(habito.id, lunes, domingo)
            val mapaSemana = (0..6).associate { i ->
                val dia = lunes.plusDays(i.toLong())
                dia to historial.filter { it.fecha == dia }.maxByOrNull { it.id }
            }
            val progresoHoy = historial.filter { it.fecha == fecha }.maxByOrNull { it.id }
            HabitoConHistorialSemanal(habito, mapaSemana, progresoHoy)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estadísticas del hábito seleccionado
    private val _habitoIdEstadisticas = MutableStateFlow<Long?>(null)

    val estadisticasHabito: StateFlow<EstadisticasHabito> = _habitoIdEstadisticas
        .flatMapLatest { id ->
            if (id == null) flowOf(EstadisticasHabito())
            else habitoDao.obtenerFechasCompletadas(id).map { fechas ->
                calcularEstadisticas(fechas)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EstadisticasHabito())

    fun seleccionarHabitoEstadisticas(habitoId: Long) {
        _habitoIdEstadisticas.value = habitoId
    }

    // --- CRUD HÁBITOS ---

    fun insertarHabito(habito: Habito, tareas: List<TareaHabito> = emptyList()) {
        viewModelScope.launch { habitoDao.insertarHabitoConTareas(habito, tareas) }
    }

    fun actualizarHabito(habito: Habito) {
        viewModelScope.launch { habitoDao.actualizarHabito(habito) }
    }

    fun eliminarHabito(habito: Habito) {
        viewModelScope.launch {
            habitoDao.eliminarHistorialDeHabito(habito.id)
            habitoDao.eliminarTareasHistorialDeHabito(habito.id)
            habitoDao.eliminarHabito(habito)
        }
    }

    fun pausarHabito(habito: Habito, fechaInicio: LocalDate) {
        viewModelScope.launch {
            habitoDao.actualizarHabito(habito.copy(pausado = true, fechaInicioPausa = fechaInicio, fechaFinPausa = null))
        }
    }

    fun despausarHabito(habito: Habito, fechaFin: LocalDate) {
        viewModelScope.launch {
            habitoDao.actualizarHabito(habito.copy(pausado = false, fechaFinPausa = fechaFin))
        }
    }

    fun obtenerHistorialMes(habitoId: Long, mes: YearMonth): Flow<Map<LocalDate, HabitoHistorial?>> {
        val inicio = mes.atDay(1)
        val fin = mes.atEndOfMonth()
        return habitoDao.obtenerHistorialEntreFechasFlow(habitoId, inicio, fin)
            .map { lista ->
                val mapa = lista.associateBy { it.fecha }
                (1..mes.lengthOfMonth()).associate { dia ->
                    mes.atDay(dia) to mapa[mes.atDay(dia)]
                }
            }
    }

    fun obtenerHabitoPorId(habitoId: Long): Flow<Habito?> =
        todosLosHabitos.map { it.firstOrNull { h -> h.id == habitoId } }

    // --- CRUD CATEGORÍAS ---

    fun insertarCategoriaHabito(categoria: CategoriaHabito) {
        viewModelScope.launch { habitoDao.insertarCategoria(categoria) }
    }

    fun actualizarCategoriaHabito(categoria: CategoriaHabito) {
        viewModelScope.launch { habitoDao.actualizarCategoria(categoria) }
    }

    fun eliminarCategoriaHabito(categoria: CategoriaHabito) {
        viewModelScope.launch { habitoDao.eliminarCategoria(categoria) }
    }

    // --- PROGRESO DIARIO ---

    fun cambiarFecha(nuevaFecha: LocalDate) {
        _fechaSeleccionada.value = nuevaFecha
    }

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
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun toggleHabitoCompleto(habito: Habito, progresoActual: HabitoHistorial?) {
        viewModelScope.launch {
            // Releer del DB para evitar estado desactualizado con pulsaciones rápidas
            val progreso = habitoDao.obtenerProgresoDiario(habito.id, _fechaSeleccionada.value) ?: progresoActual
            val yaEstabaCompletado = progreso?.completado ?: false
            val nuevoProgreso = progreso?.copy(
                completado = !yaEstabaCompletado,
                valorProgreso = if (!yaEstabaCompletado) habito.vecesPorDia else 0
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = _fechaSeleccionada.value,
                valorProgreso = if (!yaEstabaCompletado) habito.vecesPorDia else 0,
                completado = !yaEstabaCompletado
            )
            habitoDao.upsertProgreso(nuevoProgreso)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun registrarCumplimientoTareas(
        habito: Habito,
        progresoActual: HabitoHistorial?,
        estadoTareas: Map<Long, Boolean>,
        fecha: LocalDate = _fechaSeleccionada.value
    ) {
        viewModelScope.launch {
            // Guardar estado individual de cada tarea para esta fecha
            habitoDao.eliminarTareasHistorialPorFecha(habito.id, fecha)
            val historialTareas = estadoTareas.map { (tareaId, completada) ->
                TareaHabitoHistorial(tareaId = tareaId, habitoId = habito.id, fecha = fecha, completada = completada)
            }
            habitoDao.upsertTareasHistorial(historialTareas)

            // Guardar progreso agregado en HabitoHistorial
            val tareasCumplidasCount = estadoTareas.values.count { it }
            val minimoRequerido = habito.minimoTareasCumplimiento ?: habito.vecesPorDia
            val estaCompletado = tareasCumplidasCount >= minimoRequerido
            val nuevoProgreso = progresoActual?.copy(
                valorProgreso = tareasCumplidasCount,
                completado = estaCompletado
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = fecha,
                valorProgreso = tareasCumplidasCount,
                completado = estaCompletado
            )
            habitoDao.upsertProgreso(nuevoProgreso)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun obtenerEstadoTareasPorFecha(habitoId: Long, fecha: LocalDate): kotlinx.coroutines.flow.Flow<List<TareaHabitoHistorial>> =
        habitoDao.obtenerTareasHistorialPorFechaFlow(habitoId, fecha)

    fun registrarCumplimientoCuantitativo(habito: Habito, progresoActual: HabitoHistorial?, cantidad: Int, fecha: LocalDate = _fechaSeleccionada.value) {
        viewModelScope.launch {
            val objetivoFinal = habito.objetivoValor ?: habito.vecesPorDia
            val estaCompletado = cantidad >= objetivoFinal
            val nuevoProgreso = progresoActual?.copy(
                valorProgreso = cantidad,
                completado = estaCompletado
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = fecha,
                valorProgreso = cantidad,
                completado = estaCompletado
            )
            habitoDao.upsertProgreso(nuevoProgreso)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun toggleHabitoCompletoEnFecha(habito: Habito, fecha: LocalDate) {
        viewModelScope.launch {
            val progresoEnFecha = habitoDao.obtenerProgresoDiario(habito.id, fecha)
            val yaCompletado = progresoEnFecha?.completado ?: false
            val nuevoProgreso = progresoEnFecha?.copy(
                completado = !yaCompletado,
                valorProgreso = if (!yaCompletado) (habito.objetivoValor ?: habito.vecesPorDia) else 0
            ) ?: HabitoHistorial(
                habitoId = habito.id,
                fecha = fecha,
                valorProgreso = if (!yaCompletado) (habito.objetivoValor ?: habito.vecesPorDia) else 0,
                completado = !yaCompletado
            )
            habitoDao.upsertProgreso(nuevoProgreso)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun obtenerTareasDeHabito(habitoId: Long) = habitoDao.obtenerTareasDeHabito(habitoId)

    fun actualizarTareaHabito(tarea: TareaHabito) {
        viewModelScope.launch { habitoDao.actualizarTareaHabito(tarea) }
    }

    fun eliminarTareaHabito(tarea: TareaHabito) {
        viewModelScope.launch { habitoDao.eliminarTareaHabito(tarea) }
    }

    fun insertarTareaHabito(tarea: TareaHabito) {
        viewModelScope.launch { habitoDao.insertarTareaHabito(tarea) }
    }

    // --- CÁLCULO DE ESTADÍSTICAS ---

    private fun calcularEstadisticas(fechas: List<LocalDate>): EstadisticasHabito {
        val hoy = LocalDate.now()
        val lunes = hoy.with(DayOfWeek.MONDAY)
        val primerDiaMes = hoy.withDayOfMonth(1)
        val primerDiaAno = hoy.withDayOfYear(1)

        return EstadisticasHabito(
            rachaActual = calcularRachaActual(fechas),
            mejorRacha = calcularMejorRacha(fechas),
            totalCompletados = fechas.size,
            completadosSemana = fechas.count { !it.isBefore(lunes) && !it.isAfter(hoy) },
            diasEnSemana = (ChronoUnit.DAYS.between(lunes, hoy) + 1).toInt().coerceAtLeast(1),
            completadosMes = fechas.count { !it.isBefore(primerDiaMes) && !it.isAfter(hoy) },
            diasEnMes = hoy.dayOfMonth,
            completadosAno = fechas.count { !it.isBefore(primerDiaAno) && !it.isAfter(hoy) },
            diasEnAno = hoy.dayOfYear
        )
    }

    private fun calcularRachaActual(fechas: List<LocalDate>): Int {
        if (fechas.isEmpty()) return 0
        val fechasSet = fechas.toSet()
        val hoy = LocalDate.now()
        var diaActual = if (hoy in fechasSet) hoy else hoy.minusDays(1)
        if (diaActual !in fechasSet) return 0
        var racha = 0
        while (diaActual in fechasSet) {
            racha++
            diaActual = diaActual.minusDays(1)
        }
        return racha
    }

    private fun calcularMejorRacha(fechas: List<LocalDate>): Int {
        if (fechas.isEmpty()) return 0
        val ordenadas = fechas.sorted()
        var mejor = 1
        var actual = 1
        for (i in 1 until ordenadas.size) {
            if (ordenadas[i] == ordenadas[i - 1].plusDays(1)) {
                actual++
                if (actual > mejor) mejor = actual
            } else {
                actual = 1
            }
        }
        return mejor
    }
}
