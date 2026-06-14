package com.example.mistareasapp.viewmodel.Habits

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.data.habits.*
import com.example.mistareasapp.data.habits.diasSemanaSet
import com.example.mistareasapp.data.habits.tieneDefinicionDistintaA
import com.example.mistareasapp.data.habits.toVersion
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

    var agruparPorCategoria by mutableStateOf(true)
        private set

    fun toggleAgruparPorCategoria() {
        agruparPorCategoria = !agruparPorCategoria
    }

    // --- BÚSQUEDA Y FILTRO ---

    var buscadorVisible by mutableStateOf(false)
    var mostrarBarraFiltro by mutableStateOf(false)

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda = _textoBusqueda.asStateFlow()

    private val _categoriaFiltro = MutableStateFlow<Long?>(null)
    val categoriaFiltro = _categoriaFiltro.asStateFlow()

    fun actualizarBusqueda(texto: String) { _textoBusqueda.value = texto }
    fun filtrarPorCategoria(id: Long?) { _categoriaFiltro.value = id }

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
        habitoDao.observarCambiosHistorial(),   // Room re-emite automáticamente al escribir en habitos_historial
        _textoBusqueda,
        _categoriaFiltro
    ) { listaHabitos, fecha, _, busqueda, categoriaId ->
        listaHabitos
            .filter { habito ->
                val dias = habito.diasSemanaSet()
                val cumpleDias = dias == null || fecha.dayOfWeek in dias
                val cumpleBusqueda = busqueda.isBlank() || habito.nombre.contains(busqueda, ignoreCase = true)
                val cumpleCategoria = categoriaId == null || habito.categoriaId == categoriaId
                cumpleDias && cumpleBusqueda && cumpleCategoria
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
                val pctHist = calcularPorcentajeHistorico(habito)
                val dva = diasVidaEfectivos(habito)
                val totalTareas = if (habito.esCompuestoPorTareas) habitoDao.contarTareasDeHabito(habito.id) else 0
                HabitoConProgreso(habito, progreso, valorPeriodo, completadosPeriodo, pctHist, dva, totalTareas)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historial de toda la semana (para la vista Listado)
    val habitosConHistorialSemanal: StateFlow<List<HabitoConHistorialSemanal>> = combine(
        habitosActivos,
        _fechaSeleccionada,
        habitoDao.observarCambiosHistorial(),
        _textoBusqueda,
        _categoriaFiltro
    ) { listaHabitos, fecha, _, busqueda, categoriaId ->
        val lunes = fecha.with(DayOfWeek.MONDAY)
        val domingo = lunes.plusDays(6)
        listaHabitos
            .filter { habito ->
                val cumpleBusqueda = busqueda.isBlank() || habito.nombre.contains(busqueda, ignoreCase = true)
                val cumpleCategoria = categoriaId == null || habito.categoriaId == categoriaId
                cumpleBusqueda && cumpleCategoria
            }
            .map { habito ->
            val historial = habitoDao.obtenerHistorialEntreFechas(habito.id, lunes, domingo)
            val mapaSemana = (0..6).associate { i ->
                val dia = lunes.plusDays(i.toLong())
                dia to historial.filter { it.fecha == dia }.maxByOrNull { it.id }
            }
            val progresoHoy = historial.filter { it.fecha == fecha }.maxByOrNull { it.id }
            val pctHistorico = calcularPorcentajeHistorico(habito)
            val dva = diasVidaEfectivos(habito)
            val versionActiva = obtenerVersionActivaEn(habito.id, lunes)
            val progresoMes = if (habito.frecuencia == FrecuenciaHabito.MENSUAL) {
                val primerDiaMes = fecha.withDayOfMonth(1)
                val historialMes = habitoDao.obtenerHistorialEntreFechas(habito.id, primerDiaMes, fecha)
                if (habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO)
                    historialMes.sumOf { it.valorProgreso }
                else
                    historialMes.count { it.completado }
            } else 0
            HabitoConHistorialSemanal(habito, mapaSemana, progresoHoy, pctHistorico, dva, versionActiva, progresoMes)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estadísticas del hábito seleccionado
    private val _habitoIdEstadisticas = MutableStateFlow<Long?>(null)

    val estadisticasHabito: StateFlow<EstadisticasHabito> = _habitoIdEstadisticas
        .flatMapLatest { id ->
            if (id == null) flowOf(EstadisticasHabito())
            else combine(
                habitoDao.obtenerFechasCompletadas(id),
                habitosActivos
            ) { fechas, habitos -> Pair(fechas, habitos.firstOrNull { it.id == id }) }
            .transformLatest { (fechas, habito) ->
                emit(calcularEstadisticas(fechas, habito))
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
            habitoDao.eliminarVersionesDeHabito(habito.id)
            habitoDao.eliminarPausasDeHabito(habito.id)
            habitoDao.eliminarHabito(habito)
        }
    }

    /**
     * Si los parámetros de definición cambiaron entre [anterior] y [nuevo],
     * inserta una nueva versión vigente desde hoy (o desde [nuevo.fechaModificacion]).
     */
    fun guardarVersionSiCambio(anterior: Habito, nuevo: Habito, fechaVigencia: LocalDate? = null) {
        if (anterior.tieneDefinicionDistintaA(nuevo)) {
            viewModelScope.launch {
                val fecha = fechaVigencia ?: nuevo.fechaModificacion ?: LocalDate.now()
                habitoDao.insertarVersion(nuevo.toVersion(fecha))
            }
        }
    }

    /** Versiones de definición de un hábito (para mostrar historial en UI). */
    fun obtenerVersionesHabito(habitoId: Long): kotlinx.coroutines.flow.Flow<List<HabitoVersion>> =
        kotlinx.coroutines.flow.flow { emit(habitoDao.obtenerVersiones(habitoId)) }

    /** Devuelve la versión de definición vigente en [fecha] (la última cuyo fechaInicio <= fecha). */
    suspend fun obtenerVersionActivaEn(habitoId: Long, fecha: LocalDate): HabitoVersion? {
        val versiones = habitoDao.obtenerVersiones(habitoId) // ordenadas ASC por fechaInicio
        return versiones.filter { !it.fechaInicio.isAfter(fecha) }.maxByOrNull { it.fechaInicio }
    }

    fun pausarHabito(habito: Habito, fechaInicio: LocalDate) {
        viewModelScope.launch {
            habitoDao.actualizarHabito(habito.copy(pausado = true, fechaInicioPausa = fechaInicio, fechaFinPausa = null))
            habitoDao.insertarPausa(com.example.mistareasapp.data.habits.HabitoPausa(habitoId = habito.id, fechaInicio = fechaInicio, fechaFin = null))
        }
    }

    fun despausarHabito(habito: Habito, fechaFin: LocalDate) {
        viewModelScope.launch {
            habitoDao.actualizarHabito(habito.copy(pausado = false, fechaFinPausa = fechaFin))
            habitoDao.cerrarPausaActiva(habito.id, fechaFin)
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

    fun obtenerPausasHabitoFlow(habitoId: Long): kotlinx.coroutines.flow.Flow<List<com.example.mistareasapp.data.habits.HabitoPausa>> =
        habitoDao.obtenerPausasFlow(habitoId)

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

    fun moverCategoriaArriba(categoria: CategoriaHabito, todas: List<CategoriaHabito>) {
        val idx = todas.indexOfFirst { it.id == categoria.id }
        if (idx <= 0) return
        val anterior = todas[idx - 1]
        viewModelScope.launch {
            habitoDao.actualizarCategoria(categoria.copy(orden = anterior.orden))
            habitoDao.actualizarCategoria(anterior.copy(orden = categoria.orden))
        }
    }

    fun moverCategoriaAbajo(categoria: CategoriaHabito, todas: List<CategoriaHabito>) {
        val idx = todas.indexOfFirst { it.id == categoria.id }
        if (idx < 0 || idx >= todas.lastIndex) return
        val siguiente = todas[idx + 1]
        viewModelScope.launch {
            habitoDao.actualizarCategoria(categoria.copy(orden = siguiente.orden))
            habitoDao.actualizarCategoria(siguiente.copy(orden = categoria.orden))
        }
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
            val totalTareasDefinidas = estadoTareas.size.coerceAtLeast(1)
            val minimoRequerido = when (habito.criterioCumplimientoTareas) {
                CriterioCumplimientoTareas.TODAS -> totalTareasDefinidas
                CriterioCumplimientoTareas.PARCIAL -> habito.minimoTareasCumplimiento ?: totalTareasDefinidas
            }
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

    fun eliminarPausa(id: Long) {
        viewModelScope.launch { habitoDao.eliminarPausaPorId(id) }
    }

    // --- DATOS PARA PANTALLA CÁLCULOS ---

    data class DatoCalculo(
        val habito: Habito,
        val porcentaje: Float,
        val diasVidaEfectivos: Int
    )

    val datosCalculos: StateFlow<List<DatoCalculo>> = combine(
        habitosActivos,
        habitoDao.observarCambiosHistorial()
    ) { habitos, _ ->
        habitos.map { habito ->
            val pct = calcularPorcentajeHistorico(habito)
            val dva = diasVidaEfectivos(habito)
            DatoCalculo(habito, pct, dva)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CÁLCULO DE ESTADÍSTICAS ---

    private suspend fun calcularEstadisticas(fechas: List<LocalDate>, habito: Habito?): EstadisticasHabito {
        val hoy = LocalDate.now()
        val lunes = hoy.with(DayOfWeek.MONDAY)
        val primerDiaMes = hoy.withDayOfMonth(1)
        val primerDiaAno = hoy.withDayOfYear(1)

        val pctHistorico = if (habito != null) calcularPorcentajeHistorico(habito) else 0f

        val frecuencia = habito?.frecuencia ?: FrecuenciaHabito.DIARIA

        val rachaActual: Int
        val mejorRacha: Int
        val completadosSemana: Int
        val diasEnSemana: Int
        val completadosMes: Int
        val diasEnMes: Int
        val completadosAno: Int
        val diasEnAno: Int

        when (frecuencia) {
            FrecuenciaHabito.DIARIA -> {
                rachaActual = calcularRachaActualDias(fechas)
                mejorRacha = calcularMejorRachaDias(fechas)
                completadosSemana = fechas.count { !it.isBefore(lunes) && !it.isAfter(hoy) }
                diasEnSemana = (ChronoUnit.DAYS.between(lunes, hoy) + 1).toInt().coerceAtLeast(1)
                completadosMes = fechas.count { !it.isBefore(primerDiaMes) && !it.isAfter(hoy) }
                diasEnMes = hoy.dayOfMonth
                completadosAno = fechas.count { !it.isBefore(primerDiaAno) && !it.isAfter(hoy) }
                diasEnAno = hoy.dayOfYear
            }
            FrecuenciaHabito.SEMANAL -> {
                val semanasConMarca = fechas.map { it.with(DayOfWeek.MONDAY) }.toSortedSet()
                rachaActual = calcularRachaConsecutiva(semanasConMarca.toList()) { a, b -> a.plusWeeks(1) == b }
                mejorRacha = calcularMejorRachaConsecutiva(semanasConMarca.toList()) { a, b -> a.plusWeeks(1) == b }
                val semanaActual = lunes
                completadosSemana = if (semanasConMarca.contains(semanaActual)) 1 else 0
                diasEnSemana = 1
                val mesesConMarca = semanasConMarca.filter { !it.isBefore(primerDiaMes) && !it.isAfter(hoy) }.size
                completadosMes = mesesConMarca
                diasEnMes = (ChronoUnit.WEEKS.between(primerDiaMes.with(DayOfWeek.MONDAY), hoy.with(DayOfWeek.MONDAY)) + 1).toInt().coerceAtLeast(1)
                completadosAno = semanasConMarca.filter { !it.isBefore(primerDiaAno.with(DayOfWeek.MONDAY)) && !it.isAfter(hoy) }.size
                diasEnAno = (ChronoUnit.WEEKS.between(primerDiaAno.with(DayOfWeek.MONDAY), hoy.with(DayOfWeek.MONDAY)) + 1).toInt().coerceAtLeast(1)
            }
            FrecuenciaHabito.MENSUAL -> {
                val mesesConMarca = fechas.map { it.withDayOfMonth(1) }.toSortedSet()
                rachaActual = calcularRachaConsecutiva(mesesConMarca.toList()) { a, b -> a.plusMonths(1) == b }
                mejorRacha = calcularMejorRachaConsecutiva(mesesConMarca.toList()) { a, b -> a.plusMonths(1) == b }
                val mesActual = hoy.withDayOfMonth(1)
                completadosSemana = if (mesesConMarca.contains(mesActual)) 1 else 0
                diasEnSemana = 1
                completadosMes = completadosSemana
                diasEnMes = 1
                completadosAno = mesesConMarca.filter { !it.isBefore(primerDiaAno.withDayOfMonth(1)) && !it.isAfter(mesActual) }.size
                diasEnAno = (ChronoUnit.MONTHS.between(primerDiaAno.withDayOfMonth(1), mesActual) + 1).toInt().coerceAtLeast(1)
            }
        }

        return EstadisticasHabito(
            rachaActual = rachaActual,
            mejorRacha = mejorRacha,
            totalCompletados = fechas.size,
            completadosSemana = completadosSemana,
            diasEnSemana = diasEnSemana,
            completadosMes = completadosMes,
            diasEnMes = diasEnMes,
            completadosAno = completadosAno,
            diasEnAno = diasEnAno,
            porcentajeHistorico = pctHistorico
        )
    }

    private fun calcularRachaActualDias(fechas: List<LocalDate>): Int {
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

    private fun calcularMejorRachaDias(fechas: List<LocalDate>): Int {
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

    private fun calcularRachaConsecutiva(periodos: List<LocalDate>, esConsecutivo: (LocalDate, LocalDate) -> Boolean): Int {
        if (periodos.isEmpty()) return 0
        val hoy = LocalDate.now()
        val ultimo = periodos.last()
        if (ultimo.isAfter(hoy)) return 0
        var racha = 1
        for (i in periodos.lastIndex downTo 1) {
            if (esConsecutivo(periodos[i - 1], periodos[i])) racha++ else break
        }
        return racha
    }

    private fun calcularMejorRachaConsecutiva(periodos: List<LocalDate>, esConsecutivo: (LocalDate, LocalDate) -> Boolean): Int {
        if (periodos.isEmpty()) return 0
        var mejor = 1
        var actual = 1
        for (i in 1 until periodos.size) {
            if (esConsecutivo(periodos[i - 1], periodos[i])) {
                actual++
                if (actual > mejor) mejor = actual
            } else {
                actual = 1
            }
        }
        return mejor
    }

    // --- HELPERS DE PAUSA Y DÍAS EFECTIVOS ---

    /** Comprueba si [fecha] cae dentro de algún periodo de pausa de la lista precoargada. */
    private fun esFechaPausada(pausas: List<com.example.mistareasapp.data.habits.HabitoPausa>, fecha: LocalDate): Boolean =
        pausas.any { p ->
            !fecha.isBefore(p.fechaInicio) &&
            (if (p.fechaFin != null) fecha.isBefore(p.fechaFin) else true)
        }

    /**
     * Fin del horizonte histórico: el último día del periodo ANTERIOR al actual.
     * Garantiza que el periodo en curso no contamina el porcentaje histórico.
     */
    private fun periodoHistoricoFin(frecuencia: FrecuenciaHabito): LocalDate {
        val hoy = LocalDate.now()
        return when (frecuencia) {
            FrecuenciaHabito.DIARIA  -> hoy.minusDays(1)
            FrecuenciaHabito.SEMANAL -> hoy.with(DayOfWeek.MONDAY).minusDays(1)
            FrecuenciaHabito.MENSUAL -> hoy.withDayOfMonth(1).minusDays(1)
        }
    }

    private suspend fun diasVidaEfectivos(habito: Habito): Int {
        val pausas = habitoDao.obtenerPausas(habito.id)
        val fin = periodoHistoricoFin(habito.frecuencia)
        if (habito.fechaInicio.isAfter(fin)) return 1
        var count = 0
        var d = habito.fechaInicio
        while (!d.isAfter(fin)) {
            if (!esFechaPausada(pausas, d)) count++
            d = d.plusDays(1)
        }
        return count.coerceAtLeast(1)
    }

    // --- PORCENTAJE HISTÓRICO (con versionado de definición) ---

    /**
     * Calcula el % de cumplimiento histórico delegando en [obtenerDesgloseHistorico]
     * para garantizar que la tarjeta y el desglose muestren siempre el mismo valor.
     */
    private suspend fun calcularPorcentajeHistorico(habito: Habito): Float {
        val fin   = periodoHistoricoFin(habito.frecuencia)
        val inicio = habito.fechaInicio
        if (inicio.isAfter(fin)) return 0f
        val desglose = obtenerDesgloseHistorico(habito)
        // El porcentaje histórico acumulado siempre se topa al 100% (PROPORCIONAL_SIN_TOPE solo aplica por periodo individual)
        return (desglose.completados / 100f).coerceIn(0f, 1f)
    }

    /**
     * Calcula (progreso, objetivo) para un sub-período usando la definición
     * de una versión concreta. Usado tanto para % histórico como para el pie mensual.
     */
    fun calcularProgresoObjPorVersion(
        v: HabitoVersion,
        inicio: LocalDate,
        fin: LocalDate,
        completados: Int,
        totalValor: Int,
        isFechaPausada: (LocalDate) -> Boolean = { false }
    ): Pair<Double, Double> {
        val esCuant = v.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
        val diasSet = v.diasSemanaSet()

        return when (v.frecuencia) {
            FrecuenciaHabito.DIARIA -> {
                val totalDias = ChronoUnit.DAYS.between(inicio, fin).toInt() + 1
                val diasAplica = (0 until totalDias).count { i ->
                    val d = inicio.plusDays(i.toLong())
                    (diasSet == null || d.dayOfWeek in diasSet) && !isFechaPausada(d)
                }
                if (esCuant) Pair(totalValor.toDouble(), ((v.objetivoValor ?: v.vecesPorDia) * diasAplica).toDouble())
                else Pair(completados.toDouble(), diasAplica.toDouble())
            }
            FrecuenciaHabito.SEMANAL -> {
                // Cuenta semanas que tienen al menos un día activo (no pausado)
                var semanas = 0
                var lunes = inicio.with(DayOfWeek.MONDAY)
                val ultimoLunes = fin.with(DayOfWeek.MONDAY)
                while (!lunes.isAfter(ultimoLunes)) {
                    val s = if (lunes.isBefore(inicio)) inicio else lunes
                    val e = if (lunes.plusDays(6).isAfter(fin)) fin else lunes.plusDays(6)
                    var d = s
                    while (!d.isAfter(e)) {
                        if (!isFechaPausada(d)) { semanas++; break }
                        d = d.plusDays(1)
                    }
                    lunes = lunes.plusWeeks(1)
                }
                if (esCuant) Pair(totalValor.toDouble(), ((v.objetivoValor ?: v.vecesPorDia) * semanas).toDouble())
                else Pair(completados.toDouble(), (v.vecesPorDia * semanas).toDouble())
            }
            FrecuenciaHabito.MENSUAL -> {
                var mes = YearMonth.from(inicio); val mesFin = YearMonth.from(fin); var obj = 0.0
                while (!mes.isAfter(mesFin)) {
                    val d1 = if (mes.atDay(1).isBefore(inicio)) inicio else mes.atDay(1)
                    val d2 = if (mes.atEndOfMonth().isAfter(fin)) fin else mes.atEndOfMonth()
                    val diasActivosMes = (0 until ChronoUnit.DAYS.between(d1, d2).toInt() + 1)
                        .count { i -> !isFechaPausada(d1.plusDays(i.toLong())) }
                    if (diasActivosMes > 0) {
                        obj += when {
                            v.objetivoPorcentajeDias != null -> kotlin.math.ceil(diasActivosMes * v.objetivoPorcentajeDias!! / 100.0)
                            esCuant -> (v.objetivoValor ?: v.vecesPorDia).toDouble()
                            else -> v.vecesPorDia.toDouble()
                        }
                    }
                    mes = mes.plusMonths(1)
                }
                Pair(if (esCuant) totalValor.toDouble() else completados.toDouble(), obj)
            }
        }
    }

    /**
     * Versiones activas de un hábito, expuesto como Flow para la UI.
     */
    fun obtenerVersionesHabitoFlow(habitoId: Long): kotlinx.coroutines.flow.Flow<List<HabitoVersion>> =
        kotlinx.coroutines.flow.flow { emit(habitoDao.obtenerVersiones(habitoId)) }

    /**
     * Calcula el pie de la vista mensual con versionado y descuento de pausas.
     */
    suspend fun calcularPieMensualVersionado(
        habito: Habito,
        mesSeleccionado: YearMonth,
        historialMes: Map<LocalDate, HabitoHistorial?>,
        hoy: LocalDate
    ): PieMensualData {
        val diasEnMes = mesSeleccionado.lengthOfMonth()
        val esMesActual = mesSeleccionado == YearMonth.from(hoy)
        val limiteHoy = if (esMesActual) hoy else mesSeleccionado.atEndOfMonth()
        val esCuant = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO

        val pausas = habitoDao.obtenerPausas(habito.id)
        fun diaPausado(fecha: LocalDate): Boolean = esFechaPausada(pausas, fecha)
        fun diasActivosMes(): Int {
            var c = 0; var d = mesSeleccionado.atDay(1)
            while (!d.isAfter(limiteHoy)) { if (!diaPausado(d)) c++; d = d.plusDays(1) }
            return c
        }
        fun semanasActivasMes(): Int {
            val primerDia = mesSeleccionado.atDay(1); val ultimoDia = mesSeleccionado.atEndOfMonth()
            var lunes = primerDia.with(DayOfWeek.MONDAY); var count = 0
            while (!lunes.isAfter(ultimoDia)) {
                val dom = lunes.plusDays(6)
                val s = if (lunes.isBefore(primerDia)) primerDia else lunes
                val e = if (dom.isAfter(limiteHoy)) limiteHoy else dom
                if (!s.isAfter(e)) {
                    var d = s; var act = false
                    while (!d.isAfter(e)) { if (!diaPausado(d)) { act = true; break }; d = d.plusDays(1) }
                    if (act) count++
                }
                lunes = lunes.plusWeeks(1)
            }
            return count
        }

        val versiones = habitoDao.obtenerVersiones(habito.id).ifEmpty { listOf(habito.toVersion(habito.fechaInicio)) }
        val versionPrincipal = versiones.lastOrNull { !it.fechaInicio.isAfter(mesSeleccionado.atDay(1)) } ?: versiones.first()

        var totalProgreso = 0.0; var totalObjetivo = 0.0

        for (i in versiones.indices) {
            val v = versiones[i]
            val tDes = maxOf(v.fechaInicio, mesSeleccionado.atDay(1))
            val tHas = if (i + 1 < versiones.size) minOf(versiones[i + 1].fechaInicio.minusDays(1), limiteHoy) else limiteHoy
            if (tDes.isAfter(tHas)) continue

            val histT = historialMes.entries.filter { !it.key.isBefore(tDes) && !it.key.isAfter(tHas) }
            val cT = histT.count { it.value?.completado == true }
            val vT = histT.sumOf { it.value?.valorProgreso ?: 0 }

            var diasActT = 0; var d = tDes
            while (!d.isAfter(tHas)) { if (!diaPausado(d)) diasActT++; d = d.plusDays(1) }
            // Semanas con al menos un día activo (no pausado) en el tramo
            var semActT = 0
            var lunes = tDes.with(DayOfWeek.MONDAY)
            val ultimoLunes = tHas.with(DayOfWeek.MONDAY)
            while (!lunes.isAfter(ultimoLunes)) {
                val s = if (lunes.isBefore(tDes)) tDes else lunes
                val e = if (lunes.plusDays(6).isAfter(tHas)) tHas else lunes.plusDays(6)
                var dd = s
                while (!dd.isAfter(e)) {
                    if (!diaPausado(dd)) { semActT++; break }
                    dd = dd.plusDays(1)
                }
                lunes = lunes.plusWeeks(1)
            }

            val objT = when (v.frecuencia) {
                FrecuenciaHabito.DIARIA -> if (esCuant) (v.objetivoValor ?: v.vecesPorDia) * diasActT else diasActT
                FrecuenciaHabito.SEMANAL -> (if (esCuant) v.objetivoValor ?: v.vecesPorDia else v.vecesPorDia) * semActT
                FrecuenciaHabito.MENSUAL -> when {
                    v.objetivoPorcentajeDias != null -> kotlin.math.ceil(diasActT * v.objetivoPorcentajeDias!! / 100.0).toInt()
                    diasActT == 0 -> 0
                    else -> if (esCuant) v.objetivoValor ?: v.vecesPorDia else minOf(v.vecesPorDia, diasActT)
                }
            }
            totalProgreso += if (esCuant) vT else cT
            totalObjetivo += objT
        }

        val progreso = totalProgreso.toInt()
        val objetivo = totalObjetivo.toInt().coerceAtLeast(1)
        val unidad = when {
            esCuant -> habito.unidad ?: "uds"
            versionPrincipal.objetivoPorcentajeDias != null -> "días objetivo"
            else -> "días"
        }
        val etiqueta = when (habito.frecuencia) {
            FrecuenciaHabito.SEMANAL -> "${semanasActivasMes()} sem × ${versionPrincipal.vecesPorDia} veces"
            FrecuenciaHabito.MENSUAL -> if (versionPrincipal.objetivoPorcentajeDias != null)
                "${versionPrincipal.objetivoPorcentajeDias}% de ${diasActivosMes()} días activos" else "Objetivo mensual"
            FrecuenciaHabito.DIARIA -> "Días cumplidos"
        }
        return PieMensualData(progreso, objetivo, unidad, etiqueta)
    }

    data class PieMensualData(val progreso: Int, val objetivo: Int, val unidad: String, val etiqueta: String)

    /**
     * Desglose de cumplimiento por periodo.
     * @param completados para BINARIO = periodos al 100%; para PROPORCIONAL = media % (0-100)
     * @param total para BINARIO = periodos totales; para PROPORCIONAL = 100
     * @param periodosCompletados cuántos periodos se completaron: para BINARIO = completados; para PROPORCIONAL = cuántos > 50%
     * @param periodosTotal total de periodos activos evaluados
     */
    data class DesgloseData(
        val completados: Int,
        val total: Int,
        val unidadPeriodo: String,
        val esBinario: Boolean,
        val periodosCompletados: Float = 0f,  // Permite decimales para cálculos con promedio
        val periodosTotal: Int = 0
    )

    suspend fun obtenerDesgloseHistorico(habito: Habito): DesgloseData {
        val fin    = periodoHistoricoFin(habito.frecuencia)   // excluye periodo en curso
        val inicio = habito.fechaInicio
        val esBinario = habito.tipoMedicion == TipoMedicion.BINARIO
        val isCuant   = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
        val label     = periodLabel(habito)

        if (inicio.isAfter(fin)) return DesgloseData(0, 0, label, esBinario, 0f, 0)

        val pausas   = habitoDao.obtenerPausas(habito.id)
        val historial = habitoDao.obtenerHistorialEntreFechas(habito.id, inicio, fin)
            .groupBy { it.fecha }.values.map { it.maxByOrNull { e -> e.id }!! }
        val versiones = habitoDao.obtenerVersiones(habito.id).ifEmpty { listOf(habito.toVersion(inicio)) }

        // Versión vigente al inicio de un periodo atómico
        fun versionPara(fecha: LocalDate) =
            versiones.lastOrNull { !it.fechaInicio.isAfter(fecha) } ?: versiones.first()

        return when (habito.frecuencia) {

            FrecuenciaHabito.DIARIA -> {
                val diasSet = habito.diasSemanaSet()
                val totalDias = ChronoUnit.DAYS.between(inicio, fin).toInt() + 1
                val diasActivos = (0 until totalDias)
                    .map { inicio.plusDays(it.toLong()) }
                    .filter { d -> !esFechaPausada(pausas, d) && (diasSet == null || d.dayOfWeek in diasSet) }

                if (esBinario) {
                    val completos = diasActivos.count { d ->
                        val v   = versionPara(d)
                        val h   = historial.firstOrNull { it.fecha == d }
                        if (isCuant) {
                            val obj = v.objetivoValor ?: v.vecesPorDia
                            (h?.valorProgreso ?: 0) >= obj
                        } else h?.completado == true
                    }
                    val porcentaje = if (diasActivos.isNotEmpty())
                        kotlin.math.round(completos.toFloat() / diasActivos.size * 100).toInt() else 0
                    DesgloseData(porcentaje, diasActivos.size, label, true, completos.toFloat(), diasActivos.size)
                } else {
                    val sinTope = habito.tipoMedicion == TipoMedicion.PROPORCIONAL_SIN_TOPE
                    var sumaPct = 0.0
                    var diasSobreCinquenta = 0
                    diasActivos.forEach { d ->
                        val v   = versionPara(d)
                        val h   = historial.firstOrNull { it.fecha == d }
                        val obj = if (isCuant) (v.objetivoValor ?: v.vecesPorDia) else 1
                        val pct = if (isCuant && obj > 0) (h?.valorProgreso ?: 0).toDouble() / obj
                                  else if (h?.completado == true) 1.0 else 0.0
                        val pctFinal = if (sinTope) pct.coerceAtLeast(0.0) else pct.coerceIn(0.0, 1.0)
                        sumaPct += pctFinal
                        if (pctFinal > 0.5) diasSobreCinquenta++
                    }
                    val mediaBruta = if (diasActivos.isNotEmpty()) sumaPct / diasActivos.size else 0.0
                    val media = kotlin.math.round(mediaBruta * 100).toInt()
                    val periodosCompletadosConDecimal = (mediaBruta * diasActivos.size).toFloat()
                    DesgloseData(media, 100, label, false, periodosCompletadosConDecimal, diasActivos.size)
                }
            }

            FrecuenciaHabito.SEMANAL -> {
                val semanas = mutableListOf<LocalDate>()
                var lunes = inicio.with(DayOfWeek.MONDAY)
                while (!lunes.isAfter(fin)) {
                    val s = if (lunes.isBefore(inicio)) inicio else lunes
                    val e = if (lunes.plusDays(6).isAfter(fin)) fin else lunes.plusDays(6)
                    var d = s; var hayActivo = false
                    while (!d.isAfter(e)) { if (!esFechaPausada(pausas, d)) { hayActivo = true; break }; d = d.plusDays(1) }
                    if (hayActivo) semanas.add(lunes)
                    lunes = lunes.plusWeeks(1)
                }

                if (esBinario) {
                    val completas = semanas.count { lun ->
                        val v   = versionPara(lun)
                        val s   = if (lun.isBefore(inicio)) inicio else lun
                        val e   = if (lun.plusDays(6).isAfter(fin)) fin else lun.plusDays(6)
                        val hs  = historial.filter { !it.fecha.isBefore(s) && !it.fecha.isAfter(e) }
                        val obj = if (isCuant) v.objetivoValor ?: v.vecesPorDia else v.vecesPorDia
                        val prog = if (isCuant) hs.sumOf { it.valorProgreso } else hs.count { it.completado }
                        prog >= obj
                    }
                    val porcentaje = if (semanas.isNotEmpty())
                        kotlin.math.round(completas.toFloat() / semanas.size * 100).toInt() else 0
                    DesgloseData(porcentaje, semanas.size, label, true, completas.toFloat(), semanas.size)
                } else {
                    val sinTope = habito.tipoMedicion == TipoMedicion.PROPORCIONAL_SIN_TOPE
                    var sumaPct = 0.0
                    var semanasSobreCinquenta = 0
                    semanas.forEach { lun ->
                        val v   = versionPara(lun)
                        val s   = if (lun.isBefore(inicio)) inicio else lun
                        val e   = if (lun.plusDays(6).isAfter(fin)) fin else lun.plusDays(6)
                        val hs  = historial.filter { !it.fecha.isBefore(s) && !it.fecha.isAfter(e) }
                        val obj = if (isCuant) (v.objetivoValor ?: v.vecesPorDia) else v.vecesPorDia
                        val prog = if (isCuant) hs.sumOf { it.valorProgreso } else hs.count { it.completado }
                        val pctFinal = if (obj > 0) {
                            val r = prog.toDouble() / obj
                            if (sinTope) r.coerceAtLeast(0.0) else r.coerceIn(0.0, 1.0)
                        } else 0.0
                        sumaPct += pctFinal
                        if (pctFinal > 0.5) semanasSobreCinquenta++
                    }
                    val mediaBruta = if (semanas.isNotEmpty()) sumaPct / semanas.size else 0.0
                    val media = kotlin.math.round(mediaBruta * 100).toInt()
                    val periodosCompletadosConDecimal = (mediaBruta * semanas.size).toFloat()
                    DesgloseData(media, 100, label, false, periodosCompletadosConDecimal, semanas.size)
                }
            }

            FrecuenciaHabito.MENSUAL -> {
                val meses = mutableListOf<YearMonth>()
                var mes = YearMonth.from(inicio); val mesFin = YearMonth.from(fin)
                while (!mes.isAfter(mesFin)) {
                    val d1 = if (mes.atDay(1).isBefore(inicio)) inicio else mes.atDay(1)
                    val d2 = if (mes.atEndOfMonth().isAfter(fin)) fin else mes.atEndOfMonth()
                    val activos = (0 until ChronoUnit.DAYS.between(d1, d2).toInt() + 1)
                        .count { i -> !esFechaPausada(pausas, d1.plusDays(i.toLong())) }
                    if (activos > 0) meses.add(mes)
                    mes = mes.plusMonths(1)
                }

                if (esBinario) {
                    val completos = meses.count { m ->
                        val d1 = if (m.atDay(1).isBefore(inicio)) inicio else m.atDay(1)
                        val d2 = if (m.atEndOfMonth().isAfter(fin)) fin else m.atEndOfMonth()
                        val v  = versionPara(d1)
                        val hs = historial.filter { !it.fecha.isBefore(d1) && !it.fecha.isAfter(d2) }
                        val diasAct = (0 until ChronoUnit.DAYS.between(d1, d2).toInt() + 1)
                            .count { i -> !esFechaPausada(pausas, d1.plusDays(i.toLong())) }
                        val obj = when {
                            v.objetivoPorcentajeDias != null ->
                                kotlin.math.ceil(diasAct * v.objetivoPorcentajeDias!! / 100.0).toInt()
                            isCuant -> v.objetivoValor ?: v.vecesPorDia
                            else -> v.vecesPorDia
                        }
                        val prog = if (isCuant) hs.sumOf { it.valorProgreso } else hs.count { it.completado }
                        prog >= obj
                    }
                    val porcentaje = if (meses.isNotEmpty())
                        kotlin.math.round(completos.toFloat() / meses.size * 100).toInt() else 0
                    DesgloseData(porcentaje, meses.size, label, true, completos.toFloat(), meses.size)
                } else {
                    val sinTope = habito.tipoMedicion == TipoMedicion.PROPORCIONAL_SIN_TOPE
                    var sumaPct = 0.0
                    var mesesSobreCinquenta = 0
                    meses.forEach { m ->
                        val d1 = if (m.atDay(1).isBefore(inicio)) inicio else m.atDay(1)
                        val d2 = if (m.atEndOfMonth().isAfter(fin)) fin else m.atEndOfMonth()
                        val v  = versionPara(d1)
                        val hs = historial.filter { !it.fecha.isBefore(d1) && !it.fecha.isAfter(d2) }
                        val diasAct = (0 until ChronoUnit.DAYS.between(d1, d2).toInt() + 1)
                            .count { i -> !esFechaPausada(pausas, d1.plusDays(i.toLong())) }
                        val obj = when {
                            v.objetivoPorcentajeDias != null ->
                                kotlin.math.ceil(diasAct * v.objetivoPorcentajeDias!! / 100.0).toInt()
                            isCuant -> v.objetivoValor ?: v.vecesPorDia
                            else -> v.vecesPorDia
                        }
                        val prog = if (isCuant) hs.sumOf { it.valorProgreso } else hs.count { it.completado }
                        val pctFinal = if (obj > 0) {
                            val r = prog.toDouble() / obj
                            if (sinTope) r.coerceAtLeast(0.0) else r.coerceIn(0.0, 1.0)
                        } else 0.0
                        sumaPct += pctFinal
                        if (pctFinal > 0.5) mesesSobreCinquenta++
                    }
                    val mediaBruta = if (meses.isNotEmpty()) sumaPct / meses.size else 0.0
                    val media = kotlin.math.round(mediaBruta * 100).toInt()
                    val periodosCompletadosConDecimal = (mediaBruta * meses.size).toFloat()
                    DesgloseData(media, 100, label, false, periodosCompletadosConDecimal, meses.size)
                }
            }
        }
    }

    private fun periodLabel(habito: Habito): String = when (habito.frecuencia) {
        FrecuenciaHabito.DIARIA   -> "días"
        FrecuenciaHabito.SEMANAL  -> "semanas"
        FrecuenciaHabito.MENSUAL  -> "meses"
    }

    // --- AUDITORÍA DE CÁLCULOS ---

    data class PeriodoAuditoria(
        val inicio: LocalDate,
        val fin: LocalDate,
        val valorRegistrado: Int,
        val objetivo: Int,
        val porcentaje: Float,  // 0..1+ según tipo medición
        val excluido: Boolean,
        val motivoExclusion: String  // "Pausado", "Periodo en curso", ""
    )

    suspend fun obtenerAuditoriaPorHabito(habito: Habito): List<PeriodoAuditoria> {
        val hoy    = LocalDate.now()
        val finHist = periodoHistoricoFin(habito.frecuencia)
        val inicio = habito.fechaInicio
        val isCuant = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
        val sinTope = habito.tipoMedicion == TipoMedicion.PROPORCIONAL_SIN_TOPE

        val pausas   = habitoDao.obtenerPausas(habito.id)
        val histFin  = when (habito.frecuencia) {
            FrecuenciaHabito.DIARIA   -> hoy
            FrecuenciaHabito.SEMANAL  -> hoy.with(DayOfWeek.SUNDAY)
            FrecuenciaHabito.MENSUAL  -> YearMonth.from(hoy).atEndOfMonth()
        }
        val historial = habitoDao.obtenerHistorialEntreFechas(habito.id, inicio, histFin)
            .groupBy { it.fecha }.values.map { it.maxByOrNull { e -> e.id }!! }
        val versiones = habitoDao.obtenerVersiones(habito.id).ifEmpty { listOf(habito.toVersion(inicio)) }

        fun versionPara(fecha: LocalDate) =
            versiones.lastOrNull { !it.fechaInicio.isAfter(fecha) } ?: versiones.first()
        fun esPausadoPeriodo(d1: LocalDate, d2: LocalDate): Boolean {
            var d = d1; while (!d.isAfter(d2)) { if (!esFechaPausada(pausas, d)) return false; d = d.plusDays(1) }; return true
        }

        val result = mutableListOf<PeriodoAuditoria>()

        when (habito.frecuencia) {
            FrecuenciaHabito.DIARIA -> {
                val diasSet = habito.diasSemanaSet()
                var d = inicio
                while (!d.isAfter(hoy)) {
                    if (diasSet == null || d.dayOfWeek in diasSet) {
                        val v = versionPara(d)
                        val h = historial.firstOrNull { it.fecha == d }
                        val pausado = esFechaPausada(pausas, d)
                        val enCurso = d.isAfter(finHist)
                        val obj = if (isCuant) (v.objetivoValor ?: v.vecesPorDia) else 1
                        val prog = if (isCuant) (h?.valorProgreso ?: 0) else if (h?.completado == true) 1 else 0
                        val pct = if (pausado || enCurso || obj == 0) 0f
                                  else (prog.toFloat() / obj).let { r -> if (sinTope) r.coerceAtLeast(0f) else r.coerceIn(0f, 1f) }
                        result.add(PeriodoAuditoria(d, d, prog, obj, pct,
                            pausado || enCurso, when { pausado -> "Pausado"; enCurso -> "En curso"; else -> "" }))
                    }
                    d = d.plusDays(1)
                }
            }
            FrecuenciaHabito.SEMANAL -> {
                var lunes = inicio.with(DayOfWeek.MONDAY)
                val lunFin = hoy.with(DayOfWeek.MONDAY)
                while (!lunes.isAfter(lunFin)) {
                    val s = if (lunes.isBefore(inicio)) inicio else lunes
                    val e = lunes.plusDays(6)
                    val v = versionPara(s)
                    val hs = historial.filter { !it.fecha.isBefore(s) && !it.fecha.isAfter(e) }
                    val obj = if (isCuant) (v.objetivoValor ?: v.vecesPorDia) else v.vecesPorDia
                    val prog = if (isCuant) hs.sumOf { it.valorProgreso } else hs.count { it.completado }
                    val pausado = esPausadoPeriodo(s, minOf(e, hoy))
                    val enCurso = lunes.isAfter(finHist)
                    val pct = if (pausado || enCurso || obj == 0) 0f
                              else (prog.toFloat() / obj).let { r -> if (sinTope) r.coerceAtLeast(0f) else r.coerceIn(0f, 1f) }
                    result.add(PeriodoAuditoria(s, e, prog, obj, pct,
                        pausado || enCurso, when { pausado -> "Pausado"; enCurso -> "En curso"; else -> "" }))
                    lunes = lunes.plusWeeks(1)
                }
            }
            FrecuenciaHabito.MENSUAL -> {
                var mes = YearMonth.from(inicio)
                val mesFin = YearMonth.from(hoy)
                while (!mes.isAfter(mesFin)) {
                    val d1 = if (mes.atDay(1).isBefore(inicio)) inicio else mes.atDay(1)
                    val d2 = mes.atEndOfMonth()
                    val v = versionPara(d1)
                    val hs = historial.filter { !it.fecha.isBefore(d1) && !it.fecha.isAfter(d2) }
                    val diasAct = (0 until ChronoUnit.DAYS.between(d1, d2).toInt() + 1)
                        .count { i -> !esFechaPausada(pausas, d1.plusDays(i.toLong())) }
                    val obj = when {
                        v.objetivoPorcentajeDias != null ->
                            kotlin.math.ceil(diasAct * v.objetivoPorcentajeDias!! / 100.0).toInt()
                        isCuant -> v.objetivoValor ?: v.vecesPorDia
                        else -> v.vecesPorDia
                    }
                    val prog = if (isCuant) hs.sumOf { it.valorProgreso } else hs.count { it.completado }
                    val pausado = diasAct == 0
                    val enCurso = mes.isAfter(YearMonth.from(finHist))
                    val pct = if (pausado || enCurso || obj == 0) 0f
                              else (prog.toFloat() / obj).let { r -> if (sinTope) r.coerceAtLeast(0f) else r.coerceIn(0f, 1f) }
                    result.add(PeriodoAuditoria(d1, d2, prog, obj, pct,
                        pausado || enCurso, when { pausado -> "Pausado"; enCurso -> "En curso"; else -> "" }))
                    mes = mes.plusMonths(1)
                }
            }
        }
        return result
    }
}
