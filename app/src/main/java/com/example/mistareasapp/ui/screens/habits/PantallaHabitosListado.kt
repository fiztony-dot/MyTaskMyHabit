package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.mistareasapp.data.habits.CategoriaHabito
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.time.Instant
import java.time.ZoneOffset
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.TipoMedicion
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.data.habits.TipoBebidaUBE
import com.example.mistareasapp.data.habits.mlAUbe
import com.example.mistareasapp.data.habits.calcularPorcentajeLimite
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.example.mistareasapp.data.habits.diasSemanaSet
import com.example.mistareasapp.ui.components.habits.SelectorFechaConProgreso
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.ui.theme.M3CardHabito
import com.example.mistareasapp.viewmodel.Habits.HabitoConHistorialSemanal
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PantallaHabitosListado(viewModel: HabitosViewModel, navController: NavHostController, modifier: Modifier = Modifier) {
    val habitosSemana by viewModel.habitosConHistorialSemanal.collectAsState()
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val hoy = LocalDate.now()
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val agrupar = viewModel.agruparPorCategoria

    val totalHabitos = habitosSemana.size
    // Barra general: % ponderado incluyendo hábitos pausados (congelados en su valor histórico)
    val progresoGeneral by viewModel.porcentajeGeneralConPausados.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        SelectorFechaConProgreso(
            fechaSeleccionada = fechaSeleccionada,
            progresoGeneral = progresoGeneral,
            onFechaAnterior = { viewModel.cambiarFecha(fechaSeleccionada.minusWeeks(1)) },
            onFechaSiguiente = { viewModel.cambiarFecha(fechaSeleccionada.plusWeeks(1)) },
            modoSemanal = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (agrupar) {
                val porCategoria = categorias.map { cat ->
                    cat to habitosSemana.filter { it.habito.categoriaId == cat.id }
                }.filter { it.second.isNotEmpty() }
                val sinCategoria = habitosSemana.filter { h ->
                    categorias.none { it.id == h.habito.categoriaId }
                }
                porCategoria.forEach { (cat, lista) ->
                    item(key = "cat_${cat.id}") { CabeceraCategoria(cat.nombre, cat.color) }
                    items(lista, key = { it.habito.id }) {
                        HabitoListadoCard(it, hoy, viewModel, navController, categorias)
                    }
                }
                if (sinCategoria.isNotEmpty()) {
                    item(key = "cat_sin") { CabeceraCategoria("Sin categoría", null) }
                    items(sinCategoria, key = { it.habito.id }) {
                        HabitoListadoCard(it, hoy, viewModel, navController, categorias)
                    }
                }
            } else {
                items(habitosSemana) { item ->
                    HabitoListadoCard(item, hoy, viewModel, navController, categorias)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitoListadoCard(item: HabitoConHistorialSemanal, hoy: LocalDate, viewModel: HabitosViewModel, navController: NavHostController, categorias: List<com.example.mistareasapp.data.habits.CategoriaHabito> = emptyList()) {
    val habito = item.habito
    val colorHabito = Color(android.graphics.Color.parseColor(habito.colorHex))
    val esSemanal = habito.frecuencia == FrecuenciaHabito.SEMANAL

    val esCuantitativo = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
    val esLimiteMaximo = habito.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO
    val esPorTareas = habito.esCompuestoPorTareas
    val esDiaria = habito.frecuencia == FrecuenciaHabito.DIARIA
    val esMensual = habito.frecuencia == FrecuenciaHabito.MENSUAL

    // Versión vigente en la semana mostrada (para objetivos históricos correctos)
    val version = item.versionActiva
    val objetivoVersionado = version?.objetivoValor ?: habito.objetivoValor ?: version?.vecesPorDia ?: habito.vecesPorDia
    val vecesPorDiaVersionado = version?.vecesPorDia ?: habito.vecesPorDia
    val pctDiasVersionado = version?.objetivoPorcentajeDias ?: habito.objetivoPorcentajeDias
    val unidad = (version?.unidad ?: habito.unidad)?.let { " $it" } ?: ""
    val esSinTope = habito.tipoMedicion == com.example.mistareasapp.data.habits.TipoMedicion.PROPORCIONAL_SIN_TOPE
    // Mínimo de tareas para considerar el hábito cumplido (para 3-state checkbox)
    val minimoTareasCirculo = when (habito.criterioCumplimientoTareas) {
        com.example.mistareasapp.data.habits.CriterioCumplimientoTareas.TODAS -> vecesPorDiaVersionado
        com.example.mistareasapp.data.habits.CriterioCumplimientoTareas.PARCIAL -> habito.minimoTareasCumplimiento ?: vecesPorDiaVersionado
    }

    // Cumplimiento de la semana mostrada (para badge inferior, usando objetivos versionados)
    val porcentajeSemanaActual: Float = run {
        val semanaEntradas = item.historialSemana
        val raw = if (esCuantitativo) {
            val acum = semanaEntradas.values.sumOf { it?.valorProgreso ?: 0 }
            val obj = objetivoVersionado
            if (obj > 0) acum.toFloat() / obj else 0f
        } else {
            val diasAplicables = habito.diasSemanaSet()
            val aplicables = semanaEntradas.entries.filter { (f, _) ->
                !f.isAfter(hoy) && (diasAplicables == null || f.dayOfWeek in diasAplicables)
            }
            val objetivo = pctDiasVersionado?.let { pct ->
                val total = semanaEntradas.size
                kotlin.math.ceil(total * pct / 100.0).toInt().coerceAtLeast(1)
            } ?: vecesPorDiaVersionado
            val completados = aplicables.count { (_, h) -> h?.completado == true }
            if (objetivo > 0) completados.toFloat() / objetivo else 0f
        }
        // Punto 7: para SIN_TOPE el badge puede superar 100%
        if (esSinTope) raw.coerceAtLeast(0f) else raw.coerceIn(0f, 1f)
    }

    // Texto de objetivo (usa versión vigente)
    val objetivo = objetivoVersionado
    val diasEnPeriodo = when (habito.frecuencia) {
        FrecuenciaHabito.SEMANAL -> 7
        FrecuenciaHabito.MENSUAL -> hoy.lengthOfMonth()
        FrecuenciaHabito.DIARIA -> 1
    }
    val objetivoDiasPct: Int? = pctDiasVersionado?.let { pct ->
        kotlin.math.ceil(diasEnPeriodo * pct / 100.0).toInt().coerceAtLeast(1)
    }
    val objetivoDias = objetivoDiasPct ?: vecesPorDiaVersionado

    // Acumulado de la semana en curso para hábitos cuantitativos semanales
    val acumuladoSemana = if (esCuantitativo && habito.frecuencia == FrecuenciaHabito.SEMANAL)
        item.historialSemana.values.sumOf { it?.valorProgreso ?: 0 } else 0

    val freqText = if (esLimiteMaximo) {
        val lim = habito.limiteMaximo?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "?"
        val per = when (habito.frecuencia) { FrecuenciaHabito.DIARIA -> "día"; FrecuenciaHabito.SEMANAL -> "sem"; FrecuenciaHabito.MENSUAL -> "mes" }
        "Límite: $lim$unidad/$per"
    } else when (habito.frecuencia) {
        FrecuenciaHabito.DIARIA -> if (esCuantitativo) "$objetivo$unidad/día" else "${vecesPorDiaVersionado}x/día"
        FrecuenciaHabito.SEMANAL -> if (esCuantitativo) "$objetivo$unidad/sem · $acumuladoSemana/$objetivo$unidad" else "${objetivoDias}x/sem"
        FrecuenciaHabito.MENSUAL -> if (esCuantitativo) "$objetivo$unidad/mes" else "${objetivoDias}x/mes"
    }

    val pausasHabito by viewModel.obtenerPausasHabitoFlow(habito.id)
        .collectAsState(initial = emptyList())

    var dialogoFecha by remember { mutableStateOf<LocalDate?>(null) }
    var mostrarPausaDialog by remember { mutableStateOf(false) }
    var mostrarHistorialPausas by remember { mutableStateOf(false) }
    var pausaAEliminar by remember { mutableStateOf<com.example.mistareasapp.data.habits.HabitoPausa?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = M3CardHabito),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        // Colores para texto sobre card clara
        val cardTexto = Color(0xFF1A1A1A)
        val cardTextoSub = Color(0xFF555555)
        val cardIcono = Color(0xFF444444)

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

            // Header: emoji + nombre + objetivo + % histórico (esquina superior derecha)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = iconoAEmoji(iconoEfectivoHabito(habito.categoriaId, habito.icono, categorias)),
                    fontSize = 24.sp,
                    modifier = Modifier.size(36.dp).wrapContentSize(Alignment.Center)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(habito.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = cardTexto)
                    Text(freqText, fontSize = 11.sp, color = cardTextoSub)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "histórico",
                        fontSize = 8.sp,
                        color = cardTextoSub
                    )
                    Text(
                        text = "${kotlin.math.round(item.porcentajeHistorico * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = cardTexto
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid semanal con círculos clicables
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val diasAplicables = habito.diasSemanaSet()
                item.historialSemana.entries.sortedBy { it.key }.forEach { (fecha, historial) ->
                    val esPasadaOHoy = !fecha.isAfter(hoy)
                    val aplica = diasAplicables == null || fecha.dayOfWeek in diasAplicables
                    val completado = historial?.completado ?: false
                    val literalDia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val colorHoyLabel = Color(0xFF4F378B)  // púrpura oscuro, contraste sobre fondo claro
                        Text(
                            literalDia.take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (fecha == hoy) colorHoyLabel else cardTextoSub,
                            fontWeight = if (fecha == hoy) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val valorProgreso = historial?.valorProgreso ?: 0
                        val valorProgresoDecimal = historial?.valorProgresoDecimal ?: 0.0
                        // 3 estados para por tareas: 0→blanco, >0&&<min→naranja, >=min&&>0→verde
                        val esVerdeCirculo = aplica && when {
                            esPorTareas -> valorProgreso >= minimoTareasCirculo && valorProgreso > 0
                            esCuantitativo && !esDiaria -> valorProgreso > 0
                            esLimiteMaximo -> valorProgresoDecimal > 0 && valorProgresoDecimal <= (habito.limiteMaximo ?: Double.MAX_VALUE)
                            else -> completado
                        }
                        val esRojoCirculo = aplica && esLimiteMaximo && valorProgresoDecimal > (habito.limiteMaximo ?: Double.MAX_VALUE)
                        val esNaranjaCirculo = aplica && esPorTareas && valorProgreso > 0 && valorProgreso < minimoTareasCirculo
                        val pastGreenCirculo  = Color(0xFFA8D5A2)
                        val pastOrangeCirculo = Color(0xFFFFCB87)
                        val colorHoyBg     = Color(0xFFE8D5FF)  // lavanda claro para fondo del círculo hoy
                        val colorHoyBorder = Color(0xFF4F378B)  // púrpura oscuro, contraste suficiente
                        val colorHoyText   = Color(0xFF4F378B)
                        val esHoy = fecha == hoy
                        val borderColor = when {
                            !aplica -> Color(0xFFD5D5D5)
                            esVerdeCirculo -> Color(0xFF7CB87A)
                            esRojoCirculo -> Color(0xFFB71C1C)
                            esNaranjaCirculo -> Color(0xFFFFAA50)
                            esHoy -> colorHoyBorder
                            esPasadaOHoy -> Color(0xFFBBBBBB)
                            else -> Color(0xFFCCCCCC)
                        }
                        val bgColor = when {
                            !aplica -> Color(0xFFE8E8E8)
                            esVerdeCirculo -> pastGreenCirculo
                            esRojoCirculo -> Color(0xFFFFCDD2)
                            esNaranjaCirculo -> pastOrangeCirculo
                            esHoy -> Color.White  // Hoy sin fondo coloreado, solo número morado
                            esPasadaOHoy -> Color.White
                            else -> Color(0xFFF0F0F0)
                        }
                        val contentColor = when {
                            !aplica -> Color(0xFFCCCCCC)
                            esVerdeCirculo -> Color(0xFF2E7D32)
                            esRojoCirculo -> Color(0xFFB71C1C)
                            esNaranjaCirculo -> Color(0xFFBF5000)
                            esHoy -> colorHoyText
                            esPasadaOHoy -> Color(0xFFAAAAAA)
                            else -> Color(0xFFCCCCCC)
                        }
                        val diaShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(diaShape)
                                .background(bgColor)
                                .border(1.5.dp, borderColor, diaShape)
                                .then(
                                    if (esPasadaOHoy && aplica) Modifier.clickable {
                                        when {
                                            esPorTareas || esCuantitativo || esLimiteMaximo -> dialogoFecha = fecha
                                            else -> viewModel.toggleHabitoCompletoEnFecha(habito, fecha)
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val unidadCorta = (version?.unidad ?: habito.unidad)?.take(3)?.trim() ?: ""
                            val decStr = if (valorProgresoDecimal == valorProgresoDecimal.toLong().toDouble())
                                valorProgresoDecimal.toLong().toString() else "%.1f".format(valorProgresoDecimal)
                            when {
                                !aplica -> Text("–", fontSize = 10.sp, color = contentColor)
                                (esVerdeCirculo || esRojoCirculo) && esLimiteMaximo -> {
                                    Text(
                                        if (unidadCorta.isNotEmpty()) "$decStr\n$unidadCorta" else decStr,
                                        fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center, lineHeight = 8.sp
                                    )
                                }
                                esLimiteMaximo && esPasadaOHoy && valorProgresoDecimal > 0 -> {
                                    Text(
                                        if (unidadCorta.isNotEmpty()) "$decStr\n$unidadCorta" else decStr,
                                        fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center, lineHeight = 8.sp
                                    )
                                }
                                esVerdeCirculo && esCuantitativo -> {
                                    Text(
                                        if (unidadCorta.isNotEmpty()) "$valorProgreso\n$unidadCorta" else "$valorProgreso",
                                        fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center, lineHeight = 8.sp
                                    )
                                }
                                esVerdeCirculo -> Icon(
                                    Icons.Default.Check,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = contentColor
                                )
                                esNaranjaCirculo -> {
                                    Text("$valorProgreso/$objetivoVersionado", fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                }
                                esCuantitativo && esPasadaOHoy && valorProgreso > 0 -> {
                                    Text(
                                        if (unidadCorta.isNotEmpty()) "$valorProgreso\n$unidadCorta" else "$valorProgreso",
                                        fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center, lineHeight = 8.sp
                                    )
                                }
                                else -> Text(
                                    "${fecha.dayOfMonth}",
                                    fontSize = 9.sp,
                                    color = contentColor,
                                    fontWeight = if (fecha == hoy) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Fila de acciones: badge semanal (solo semanales) + editar + vista mensual + pausar
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (esSemanal && !esLimiteMaximo) {
                    val badgeColor = when {
                        porcentajeSemanaActual >= 0.8f -> Color(0xFF4CAF50)
                        porcentajeSemanaActual >= 0.5f -> Color(0xFFFFB74D)
                        else -> Color(0xFFCF6679)
                    }
                    Badge(containerColor = badgeColor) {
                        Text("${kotlin.math.round(porcentajeSemanaActual * 100).toInt()}%", color = Color.White)
                    }
                }
                if (esMensual && !esLimiteMaximo) {
                    // Para no-cuantitativo mensual usar objetivoDias (respeta objetivoPorcentajeDias)
                    val denominadorMes = if (esCuantitativo) objetivoVersionado else objetivoDias
                    val pctMes = if (denominadorMes > 0) item.progresoMesActual.toFloat() / denominadorMes else 0f
                    val badgeColorMes = when {
                        pctMes >= 0.8f -> Color(0xFF4CAF50)
                        pctMes >= 0.5f -> Color(0xFFFFB74D)
                        else -> Color(0xFFCF6679)
                    }
                    val unidadCorta = (version?.unidad ?: habito.unidad)?.take(3)?.trim() ?: ""
                    val badgeText = if (esCuantitativo) {
                        if (unidadCorta.isNotEmpty()) "${item.progresoMesActual}/$objetivoVersionado $unidadCorta"
                        else "${item.progresoMesActual}/$objetivoVersionado"
                    } else {
                        "${item.progresoMesActual}/$objetivoDias días"
                    }
                    Badge(containerColor = badgeColorMes) {
                        Text(badgeText, color = Color.White)
                    }
                }
                if (esLimiteMaximo) {
                    val limiteBase = habito.limiteMaximo ?: 0.0
                    // Para MENSUAL: usar acumulado del mes completo y límite proporcional
                    val (acumPeriodo, limitePeriodo) = if (esMensual) {
                        val hoy = LocalDate.now()
                        val primerDiaMes = hoy.withDayOfMonth(1)
                        val diasTotalesMes = hoy.lengthOfMonth().toDouble()
                        val d1 = if (habito.fechaInicio.isAfter(primerDiaMes)) habito.fechaInicio else primerDiaMes
                        val diasActivosMes = (hoy.lengthOfMonth() - d1.dayOfMonth + 1).toDouble().coerceAtLeast(1.0)
                        val lim = if (d1 > primerDiaMes) limiteBase * diasActivosMes / diasTotalesMes else limiteBase
                        Pair(item.progresoMesDecimal, lim)
                    } else {
                        Pair(item.historialSemana.values.sumOf { it?.valorProgresoDecimal ?: 0.0 }, limiteBase)
                    }
                    // Normalizar el valor al límite base para que los tramos (definidos sobre el límite completo) apliquen correctamente en meses parciales
                    val valorParaTramos = if (limitePeriodo > 0.0 && limitePeriodo < limiteBase)
                        acumPeriodo * limiteBase / limitePeriodo else acumPeriodo
                    val pctTramo = calcularPorcentajeLimite(valorParaTramos, limiteBase, habito.tramosLimite)
                    val badgeColorLim = when {
                        pctTramo >= 100 -> Color(0xFF4CAF50)
                        pctTramo >= 50  -> Color(0xFFFFB74D)
                        else            -> Color(0xFFCF6679)
                    }
                    val unidadCorta = (version?.unidad ?: habito.unidad)?.take(3)?.trim() ?: ""
                    val acumStr = if (acumPeriodo == acumPeriodo.toLong().toDouble()) acumPeriodo.toLong().toString() else "%.1f".format(acumPeriodo)
                    val limStr = if (limitePeriodo == limitePeriodo.toLong().toDouble()) limitePeriodo.toLong().toString() else "%.1f".format(limitePeriodo)
                    val rawText = if (unidadCorta.isNotEmpty()) "$acumStr/$limStr $unidadCorta" else "$acumStr/$limStr"
                    Badge(containerColor = badgeColorLim) {
                        Text("$pctTramo%  $rawText", color = Color.White)
                    }
                }
                // Icono periodos pausados (visible solo si hay alguno)
                if (pausasHabito.isNotEmpty()) {
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick = { mostrarHistorialPausas = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Ver periodos pausados",
                            tint = cardIcono,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = { navController.navigate("editar_habito/${habito.id}") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = cardIcono, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = { navController.navigate("vista_mensual/${habito.id}") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.CalendarViewMonth, contentDescription = "Vista mensual", tint = cardIcono, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = { mostrarPausaDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (habito.pausado) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (habito.pausado) "Reanudar" else "Pausar",
                        tint = if (habito.pausado) Color(0xFF4CAF50) else cardIcono,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }

    // Diálogo de pausa / reanudación
    if (mostrarPausaDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { mostrarPausaDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (habito.pausado) viewModel.despausarHabito(habito, fecha)
                        else viewModel.pausarHabito(habito, fecha)
                    }
                    mostrarPausaDialog = false
                }) { Text(if (habito.pausado) "Reanudar" else "Pausar") }
            },
            dismissButton = { TextButton(onClick = { mostrarPausaDialog = false }) { Text("Cancelar") } }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        if (habito.pausado) "Fecha de reanudación" else "Fecha de inicio de pausa",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
    }

    // Diálogo historial de periodos pausados
    if (mostrarHistorialPausas) {
        AlertDialog(
            onDismissRequest = { mostrarHistorialPausas = false },
            title = { Text("Periodos pausados — ${habito.nombre}", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (pausasHabito.isEmpty()) {
                        Text("Sin periodos pausados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("es"))
                        pausasHabito.forEach { p ->
                            val inicio = p.fechaInicio.format(fmt)
                            val fin = p.fechaFin?.format(fmt) ?: "activo"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFAA50), androidx.compose.foundation.shape.CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text("$inicio → $fin", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { pausaAEliminar = p },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarHistorialPausas = false }) { Text("Cerrar") }
            }
        )
    }

    // Confirmación de borrado de pausa
    pausaAEliminar?.let { p ->
        val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("es"))
        AlertDialog(
            onDismissRequest = { pausaAEliminar = null },
            title = { Text("Eliminar periodo pausado") },
            text = { Text("¿Eliminar el periodo ${p.fechaInicio.format(fmt)} → ${p.fechaFin?.format(fmt) ?: "activo"}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarPausa(p.id)
                    pausaAEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pausaAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogos por fecha
    dialogoFecha?.let { fecha ->
        val historialFecha = item.historialSemana[fecha]
        if (esPorTareas) {
            DialogoTareasHabito(
                habito = habito,
                progreso = historialFecha,
                fecha = fecha,
                viewModel = viewModel,
                onDismiss = { dialogoFecha = null }
            )
        } else if (esCuantitativo) {
            DialogoCuantitativo(
                habito = habito,
                progreso = historialFecha,
                fecha = fecha,
                viewModel = viewModel,
                onDismiss = { dialogoFecha = null }
            )
        } else if (esLimiteMaximo) {
            DialogoLimiteMaximoFecha(
                habito = habito,
                historial = historialFecha,
                fecha = fecha,
                viewModel = viewModel,
                onDismiss = { dialogoFecha = null }
            )
        }
    }
}

@Suppress("NAME_SHADOWING")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoLimiteMaximoFecha(
    habito: com.example.mistareasapp.data.habits.Habito,
    historial: HabitoHistorial?,
    fecha: LocalDate,
    viewModel: HabitosViewModel,
    onDismiss: () -> Unit
) {
    val limiteMaximo = habito.limiteMaximo ?: 0.0
    val unidad = habito.unidad ?: ""
    val registrado = historial?.valorProgresoDecimal ?: 0.0

    var textoValor by remember { mutableStateOf("") }
    var tipoBebida by remember { mutableStateOf(TipoBebidaUBE.CERVEZA_SIDRA) }
    var textoMl by remember { mutableStateOf("") }
    var expandidoBebida by remember { mutableStateOf(false) }

    val valorDecimal = textoValor.replace(",", ".").toDoubleOrNull() ?: 0.0
    val mlDecimal = textoMl.replace(",", ".").toDoubleOrNull() ?: 0.0
    val limStr = if (limiteMaximo == limiteMaximo.toLong().toDouble()) limiteMaximo.toLong().toString() else "%.1f".format(limiteMaximo)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${habito.nombre} — ${fecha.dayOfMonth}/${fecha.monthValue}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Límite: $limStr $unidad".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val limStr2 = if (limiteMaximo == limiteMaximo.toLong().toDouble()) limiteMaximo.toLong().toString() else "%.1f".format(limiteMaximo)
                val regStr = if (registrado == registrado.toLong().toDouble()) registrado.toLong().toString() else "%.1f".format(registrado)
                val pct = if (limiteMaximo > 0) (registrado / limiteMaximo).toFloat().coerceIn(0f, 1f) else 0f
                val colorBarra = if (registrado > limiteMaximo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(
                    "Acumulado hoy: $regStr / $limStr2 $unidad".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = colorBarra
                )
                HorizontalDivider()
                // UBE converter
                if (habito.ubeActivo) {
                    Text("Convertir a UBE", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(expanded = expandidoBebida, onExpandedChange = { expandidoBebida = it }) {
                        OutlinedTextField(
                            value = tipoBebida.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo bebida") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoBebida) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandidoBebida, onDismissRequest = { expandidoBebida = false }) {
                            TipoBebidaUBE.entries.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text("${tipo.label} (${tipo.mlPorUbe.toInt()} ml/UBE)") },
                                    onClick = { tipoBebida = tipo; expandidoBebida = false }
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = textoMl, onValueChange = { textoMl = it },
                            label = { Text("ml") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val ube = mlAUbe(mlDecimal, tipoBebida)
                            textoValor = "%.2f".format(ube).trimEnd('0').trimEnd('.')
                        }, enabled = mlDecimal > 0) { Text("→ UBE") }
                    }
                    HorizontalDivider()
                }
                // Campo valor
                Text("Cantidad ($unidad)".trim(), style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = textoValor,
                    onValueChange = { textoValor = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                )
                // Botones rápidos suma
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5, 1.0, 2.0, 5.0).forEach { v ->
                        OutlinedButton(
                            onClick = {
                                val nuevo = valorDecimal + v
                                textoValor = if (nuevo == nuevo.toLong().toDouble()) nuevo.toLong().toString() else "%.1f".format(nuevo)
                            },
                            modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)
                        ) { Text("+${if (v == v.toLong().toDouble()) v.toLong() else v}", fontSize = 12.sp) }
                    }
                }
                // Botones rápidos resta
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5, 1.0, 2.0, 5.0).forEach { v ->
                        OutlinedButton(
                            onClick = {
                                val nuevo = (valorDecimal - v).coerceAtLeast(0.0)
                                textoValor = if (nuevo == nuevo.toLong().toDouble()) nuevo.toLong().toString() else "%.1f".format(nuevo)
                            },
                            modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)
                        ) { Text("-${if (v == v.toLong().toDouble()) v.toLong() else v}", fontSize = 12.sp) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            if (valorDecimal > 0) {
                                viewModel.registrarLimiteMaximo(habito, historial, valorDecimal, fecha)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f), enabled = valorDecimal > 0
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
