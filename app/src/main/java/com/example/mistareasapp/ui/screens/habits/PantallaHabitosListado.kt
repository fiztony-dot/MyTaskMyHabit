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
import com.example.mistareasapp.data.habits.TipoMedicion
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
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
    // Barra general: media ponderada por min(diasEfectivos,180)×dificultad
    val pesoTotal = habitosSemana.sumOf { (minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad).toLong() }
    val progresoGeneral = if (pesoTotal > 0)
        (habitosSemana.sumOf { it.porcentajeHistorico.coerceIn(0f, 1f).toDouble() * minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad } / pesoTotal).toFloat()
    else 0f

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

    val freqText = when (habito.frecuencia) {
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
                        // 3 estados para por tareas: 0→blanco, >0&&<min→naranja, >=min&&>0→verde
                        val esVerdeCirculo = aplica && when {
                            esPorTareas -> valorProgreso >= minimoTareasCirculo && valorProgreso > 0
                            esCuantitativo && !esDiaria -> valorProgreso > 0
                            else -> completado
                        }
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
                            esNaranjaCirculo -> Color(0xFFFFAA50)
                            esHoy -> colorHoyBorder
                            esPasadaOHoy -> Color(0xFFBBBBBB)
                            else -> Color(0xFFCCCCCC)
                        }
                        val bgColor = when {
                            !aplica -> Color(0xFFE8E8E8)
                            esVerdeCirculo -> pastGreenCirculo
                            esNaranjaCirculo -> pastOrangeCirculo
                            esHoy -> Color.White  // Hoy sin fondo coloreado, solo número morado
                            esPasadaOHoy -> Color.White
                            else -> Color(0xFFF0F0F0)
                        }
                        val contentColor = when {
                            !aplica -> Color(0xFFCCCCCC)
                            esVerdeCirculo -> Color(0xFF2E7D32)
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
                                            esPorTareas || esCuantitativo -> dialogoFecha = fecha
                                            else -> viewModel.toggleHabitoCompletoEnFecha(habito, fecha)
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val unidadCorta = (version?.unidad ?: habito.unidad)?.take(3)?.trim() ?: ""
                            when {
                                !aplica -> Text("–", fontSize = 10.sp, color = contentColor)
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
                if (esSemanal) {
                    val badgeColor = when {
                        porcentajeSemanaActual >= 0.8f -> Color(0xFF4CAF50)
                        porcentajeSemanaActual >= 0.5f -> Color(0xFFFFB74D)
                        else -> Color(0xFFCF6679)
                    }
                    Badge(containerColor = badgeColor) {
                        Text("${kotlin.math.round(porcentajeSemanaActual * 100).toInt()}%", color = Color.White)
                    }
                }
                if (esMensual) {
                    val pctMes = if (objetivoVersionado > 0) item.progresoMesActual.toFloat() / objetivoVersionado else 0f
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
                        "${item.progresoMesActual}/$objetivoVersionado días"
                    }
                    Badge(containerColor = badgeColorMes) {
                        Text(badgeText, color = Color.White)
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
        }
    }
}
