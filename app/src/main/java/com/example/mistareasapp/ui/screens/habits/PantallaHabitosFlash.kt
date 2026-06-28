package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import androidx.navigation.NavHostController
import java.time.LocalDate
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.ui.theme.M3CardHabito
import com.example.mistareasapp.ui.components.habits.SelectorFechaConProgreso
import com.example.mistareasapp.viewmodel.Habits.HabitoConProgreso
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import com.example.mistareasapp.data.habits.TipoBebidaUBE
import com.example.mistareasapp.data.habits.mlAUbe

@Composable
fun PantallaHabitosFlash(
    viewModel: HabitosViewModel,
    navController: NavHostController,
    progresoGeneral: Float = 0f,
    modifier: Modifier = Modifier
) {
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()

    // Barra general: media ponderada por min(diasEfectivos,180)×dificultad
    val pesoTotal = habitosConProgreso.sumOf { (minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad).toLong() }
    val progresoCalculado = if (pesoTotal > 0)
        (habitosConProgreso.sumOf { it.porcentajeHistorico.coerceIn(0f, 1f).toDouble() * minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad } / pesoTotal).toFloat()
    else 0f
    val progreso = if (progresoGeneral > 0) progresoGeneral else progresoCalculado
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val agrupar = viewModel.agruparPorCategoria

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        SelectorFechaConProgreso(
            fechaSeleccionada = fechaSeleccionada,
            progresoGeneral = progreso,
            onFechaAnterior = { viewModel.cambiarFecha(fechaSeleccionada.minusDays(1)) },
            onFechaSiguiente = { viewModel.cambiarFecha(fechaSeleccionada.plusDays(1)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (agrupar) {
                habitosConProgresoPorCategoria(habitosConProgreso, categorias) { item ->
                    HabitoFlashItem(item, viewModel, navController, categorias)
                }
            } else {
                items(habitosConProgreso) { item ->
                    HabitoFlashItem(item, viewModel, navController, categorias)
                }
            }
        }
    }
}

/** Emite items agrupados por categoría dentro de un LazyListScope. */
fun LazyListScope.habitosConProgresoPorCategoria(
    habitos: List<HabitoConProgreso>,
    categorias: List<CategoriaHabito>,
    itemContent: @Composable (HabitoConProgreso) -> Unit
) {
    val porCategoria = categorias.map { cat ->
        cat to habitos.filter { it.habito.categoriaId == cat.id }
    }.filter { it.second.isNotEmpty() }
    val sinCategoria = habitos.filter { h -> categorias.none { it.id == h.habito.categoriaId } }

    porCategoria.forEach { (cat, lista) ->
        item(key = "cat_${cat.id}") { CabeceraCategoria(cat.nombre, cat.color) }
        items(lista, key = { it.habito.id }) { itemContent(it) }
    }
    if (sinCategoria.isNotEmpty()) {
        item(key = "cat_sin") { CabeceraCategoria("Sin categoría", null) }
        items(sinCategoria, key = { it.habito.id }) { itemContent(it) }
    }
}

@Composable
fun HabitoFlashItem(item: HabitoConProgreso, viewModel: HabitosViewModel, navController: NavHostController, categorias: List<CategoriaHabito> = emptyList()) {
    val habito = item.habito
    val progreso = item.progreso
    val estaCompletado = item.estaCompletado
    val valorActual = item.valorActual

    var mostrarDialogoTareas by remember { mutableStateOf(false) }
    var mostrarDialogoCuantitativo by remember { mutableStateOf(false) }
    var mostrarDialogoLimite by remember { mutableStateOf(false) }

    val colorHabito = try {
        Color(habito.colorHex.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val esCuantitativo = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
    val esLimiteMaximo = habito.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO
    val esPorTareas = habito.esCompuestoPorTareas
    val esDiaria = habito.frecuencia == FrecuenciaHabito.DIARIA
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    // Límite efectivo del periodo actual (prorrateado si es el primer mes parcial)
    val limiteMaximoEfectivo: Double = run {
        val base = habito.limiteMaximo ?: 0.0
        if (esLimiteMaximo && habito.frecuencia == FrecuenciaHabito.MENSUAL) {
            val primerDiaMes = fechaSeleccionada.withDayOfMonth(1)
            if (habito.fechaInicio.isAfter(primerDiaMes)) {
                val diasTotales = fechaSeleccionada.lengthOfMonth().toDouble()
                val diasActivos = (fechaSeleccionada.lengthOfMonth() - habito.fechaInicio.dayOfMonth + 1).toDouble()
                base * diasActivos / diasTotales
            } else base
        } else base
    }
    val objetivo = habito.objetivoValor ?: habito.vecesPorDia
    // Para hábitos por tareas: usar el total real de tareas como denominador
    val totalTareasReal = if (esPorTareas) item.totalTareas.takeIf { it > 0 } ?: (habito.minimoTareasCumplimiento ?: habito.vecesPorDia) else objetivo
    val valorPeriodo = item.valorPeriodo
    val completadosPeriodo = item.completadosPeriodo

    // Objetivo por % de días (solo para hábitos no diarios de frecuencia)
    val hoyLocal = LocalDate.now()
    val diasEnPeriodo = when (habito.frecuencia) {
        FrecuenciaHabito.SEMANAL -> 7
        FrecuenciaHabito.MENSUAL -> hoyLocal.lengthOfMonth()
        FrecuenciaHabito.DIARIA -> 1
    }
    val objetivoDiasPct: Int? = habito.objetivoPorcentajeDias?.let { pct ->
        kotlin.math.ceil(diasEnPeriodo * pct / 100.0).toInt().coerceAtLeast(1)
    }
    // Objetivo efectivo de días: si hay %, usa ese; si no, vecesPorDia
    val objetivoDias = objetivoDiasPct ?: habito.vecesPorDia

    // Completado visual:
    // - Cuantitativo no-diario → cualquier valor registrado en el periodo
    // - Cuantitativo diario → valor actual >= objetivo
    // - Tareas no-diario → ¿el periodo alcanza el objetivo? (periodo)
    // - Simple no-diario → binario: ¿está marcado HOY? (igual que diario)
    // - Resto → flag completado del día
    val minimoTareas = when (habito.criterioCumplimientoTareas) {
        com.example.mistareasapp.data.habits.CriterioCumplimientoTareas.TODAS -> totalTareasReal
        com.example.mistareasapp.data.habits.CriterioCumplimientoTareas.PARCIAL -> habito.minimoTareasCumplimiento ?: totalTareasReal
    }
    val completadoVisual = when {
        esCuantitativo && !esDiaria -> valorActual > 0
        esCuantitativo -> valorActual >= objetivo
        !esDiaria && esPorTareas -> completadosPeriodo >= objetivoDias
        esPorTareas -> valorActual >= minimoTareas
        else -> estaCompletado
    }

    // Tipo de medición: si es SIN_TOPE y no es diaria, no limitamos al 100%
    val superarPermitido = habito.tipoMedicion == com.example.mistareasapp.data.habits.TipoMedicion.PROPORCIONAL_SIN_TOPE
    // Porcentaje para el checkbox de cuantitativas (usa el periodo)
    val porcentajePeriodo = if (objetivo > 0) {
        val raw = valorPeriodo.toFloat() / objetivo
        if (superarPermitido) raw.coerceAtLeast(0f) else raw.coerceIn(0f, 1f)
    } else 0f
    // Porcentaje para hábitos de frecuencia no diarios
    val porcentajeDias = if (objetivoDias > 0) {
        val raw = completadosPeriodo.toFloat() / objetivoDias
        if (superarPermitido) raw.coerceAtLeast(0f) else raw.coerceIn(0f, 1f)
    } else 0f
    // Porcentaje diario (para barra de progreso visual — siempre 0-1)
    val porcentajeDiario = if (objetivo > 0) (valorActual.toFloat() / objetivo).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = M3CardHabito),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        // Colores para texto sobre card clara
        val cardTexto = Color(0xFF1A1A1A)
        val cardTextoSub = Color(0xFF555555)
        val cardIcono = Color(0xFF444444)

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Icono de categoría (fallback al propio del hábito)
                Text(
                    text = iconoAEmoji(iconoEfectivoHabito(habito.categoriaId, habito.icono, categorias)),
                    fontSize = 24.sp,
                    modifier = Modifier.size(36.dp).wrapContentSize(Alignment.Center)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habito.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = cardTexto
                    )
                    val unidad = habito.unidad?.let { " $it" } ?: ""
                    val freqText = when {
                        esLimiteMaximo -> {
                            val limStr = limiteMaximoEfectivo.toBigDecimal().stripTrailingZeros().toPlainString()
                            "Límite: $limStr$unidad/${when(habito.frecuencia){ FrecuenciaHabito.DIARIA->"día"; FrecuenciaHabito.SEMANAL->"sem"; FrecuenciaHabito.MENSUAL->"mes" }}"
                        }
                        else -> when (habito.frecuencia) {
                            FrecuenciaHabito.DIARIA -> if (esCuantitativo) "$objetivo$unidad/día" else if (esPorTareas) "${totalTareasReal} tareas" else "${habito.vecesPorDia}x/día"
                            FrecuenciaHabito.SEMANAL -> if (esCuantitativo) "$objetivo$unidad/sem" else "${objetivoDias}x/sem"
                            FrecuenciaHabito.MENSUAL -> if (esCuantitativo) "$objetivo$unidad/mes" else "${objetivoDias}x/mes"
                        }
                    }
                    val progressText = when {
                        esLimiteMaximo -> {
                            val acum = item.valorPeriodoDecimal
                            val acumStr = if (acum == acum.toLong().toDouble()) acum.toLong().toString() else "%.1f".format(acum)
                            val limStr = if (limiteMaximoEfectivo == limiteMaximoEfectivo.toLong().toDouble()) limiteMaximoEfectivo.toLong().toString() else "%.1f".format(limiteMaximoEfectivo)
                            "  •  $acumStr/$limStr$unidad"
                        }
                        esCuantitativo -> {
                            val pct = (porcentajePeriodo * 100).toInt()
                            "  •  $valorPeriodo/$objetivo ($pct%)"
                        }
                        esPorTareas -> "  •  $valorActual/$totalTareasReal tareas"
                        !esDiaria -> {
                            val pct = (porcentajeDias * 100).toInt()
                            "  •  $completadosPeriodo/$objetivoDias días ($pct%)"
                        }
                        else -> ""
                    }
                    Text(
                        text = "$freqText$progressText",
                        fontSize = 11.sp,
                        color = cardTextoSub
                    )
                }

                // Botón editar
                IconButton(
                    onClick = { navController.navigate("editar_habito/${habito.id}") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = cardIcono, modifier = Modifier.size(16.dp))
                }

                // 3 estados según spec:
                // tareasCompletadas==0 → blanco; >0 && <minimo → naranja; >=minimo → verde
                val esVerde = when {
                    esLimiteMaximo -> item.valorPeriodoDecimal > 0 && item.valorPeriodoDecimal <= limiteMaximoEfectivo
                    esPorTareas -> valorActual >= minimoTareas && valorActual > 0
                    else -> completadoVisual
                }
                val esNaranja = esPorTareas && valorActual > 0 && valorActual < minimoTareas
                val pastGreen  = Color(0xFFA8D5A2)
                val pastOrange = Color(0xFFFFCB87)
                val checkShape = RoundedCornerShape(8.dp)
                val checkBg     = when { esVerde -> pastGreen; esNaranja -> pastOrange; else -> Color.White }
                val checkBorder = when { esVerde -> Color(0xFF7CB87A); esNaranja -> Color(0xFFFFAA50); else -> Color(0xFFBBBBBB) }
                val checkContent= when { esVerde -> Color(0xFF2E7D32); esNaranja -> Color(0xFFBF5000); else -> Color(0xFFAAAAAA) }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(checkShape)
                        .background(checkBg)
                        .border(1.5.dp, checkBorder, checkShape)
                        .clickable {
                            when {
                                esLimiteMaximo -> mostrarDialogoLimite = true
                                esPorTareas -> mostrarDialogoTareas = true
                                esCuantitativo -> mostrarDialogoCuantitativo = true
                                else -> viewModel.toggleHabitoCompleto(habito, progreso)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        esLimiteMaximo -> if (item.valorPeriodoDecimal > 0) Icon(
                            Icons.Default.Check,
                            contentDescription = "Registrado",
                            tint = checkContent,
                            modifier = Modifier.size(18.dp)
                        ) else Unit
                        esCuantitativo && !esDiaria -> if (valorActual > 0) Icon(
                            Icons.Default.Check,
                            contentDescription = "Registrado hoy",
                            tint = checkContent,
                            modifier = Modifier.size(18.dp)
                        ) else Unit
                        esCuantitativo -> Text(
                            text = "${(porcentajeDiario * 100).toInt()}%",
                            color = checkContent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        esPorTareas && esVerde -> Icon(
                            Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = checkContent,
                            modifier = Modifier.size(18.dp)
                        )
                        esPorTareas && esNaranja -> Text(
                            text = "$valorActual/$totalTareasReal",
                            color = checkContent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        // esPorTareas && valorActual==0 → caja blanca vacía (sin contenido)
                        // Para hábitos simples (diarios o no-diarios), el checkbox es binario:
                        // ✓ si completado hoy, vacío si no.
                        estaCompletado -> Icon(
                            Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = checkContent,
                            modifier = Modifier.size(18.dp)
                        )
                        else -> {} // sin marcar: caja blanca vacía
                    }
                }
            }

            // Barra de progreso para cuantitativos y límite máximo
            if (esLimiteMaximo && limiteMaximoEfectivo > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                val pctLimite = (item.valorPeriodoDecimal / limiteMaximoEfectivo).toFloat().coerceIn(0f, 1f)
                val barColor = if (pctLimite >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                LinearProgressIndicator(
                    progress = { pctLimite },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
            if (esCuantitativo && objetivo > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (if (esDiaria) porcentajeDiario else porcentajePeriodo).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }
    }

    if (mostrarDialogoTareas) {
        DialogoTareasHabito(
            habito = habito,
            progreso = progreso,
            fecha = fechaSeleccionada,
            viewModel = viewModel,
            onDismiss = { mostrarDialogoTareas = false }
        )
    }
    if (mostrarDialogoCuantitativo) {
        DialogoCuantitativo(
            habito = habito,
            progreso = progreso,
            fecha = fechaSeleccionada,
            viewModel = viewModel,
            onDismiss = { mostrarDialogoCuantitativo = false }
        )
    }
    if (mostrarDialogoLimite) {
        DialogoLimiteMaximo(
            habito = habito,
            progreso = item,
            limiteEfectivo = limiteMaximoEfectivo,
            fecha = fechaSeleccionada,
            viewModel = viewModel,
            onDismiss = { mostrarDialogoLimite = false }
        )
    }
}

// --- CABECERA DE CATEGORÍA (compartida por Flash y Listado) ---

@Composable
fun CabeceraCategoria(nombre: String, colorHex: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (colorHex != null) {
            val color = try { Color(android.graphics.Color.parseColor(colorHex)) }
                        catch (_: Exception) { MaterialTheme.colorScheme.primary }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
        Text(
            text = nombre.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

// --- DIÁLOGO TAREAS ---

@Composable
fun DialogoTareasHabito(
    habito: com.example.mistareasapp.data.habits.Habito,
    progreso: com.example.mistareasapp.data.habits.HabitoHistorial?,
    fecha: java.time.LocalDate,
    viewModel: HabitosViewModel,
    onDismiss: () -> Unit
) {
    val tareas by viewModel.obtenerTareasDeHabito(habito.id).collectAsState(initial = emptyList())
    val tareasHistorial by viewModel.obtenerEstadoTareasPorFecha(habito.id, fecha).collectAsState(initial = emptyList())

    // Inicializa desde el historial de esa fecha concreta; si no hay historial todas quedan sin marcar
    var estadoTareas by remember(tareas, tareasHistorial) {
        val historialMap = tareasHistorial.associateBy { it.tareaId }
        mutableStateOf(tareas.associate { tarea ->
            tarea.id to (historialMap[tarea.id]?.completada ?: false)
        })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habito.nombre, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (tareas.isEmpty()) {
                    Text("Este hábito no tiene tareas definidas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val todasMarcadas = tareas.all { estadoTareas[it.id] == true }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = todasMarcadas,
                            onCheckedChange = { marcarTodas ->
                                estadoTareas = tareas.associate { it.id to marcarTodas }
                            }
                        )
                        Text(
                            if (todasMarcadas) "Deseleccionar todas" else "Seleccionar todas",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider()
                    tareas.forEach { tarea ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = estadoTareas[tarea.id] ?: false,
                                onCheckedChange = { checked ->
                                    estadoTareas = estadoTareas.toMutableMap().also { it[tarea.id] = checked }
                                }
                            )
                            Column {
                                Text(tarea.nombre, fontSize = 14.sp)
                                if (!tarea.descripcion.isNullOrBlank()) {
                                    Text(tarea.descripcion, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.registrarCumplimientoTareas(habito, progreso, estadoTareas, fecha)
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// --- DIÁLOGO CUANTITATIVO ---

@Composable
fun DialogoCuantitativo(
    habito: com.example.mistareasapp.data.habits.Habito,
    progreso: com.example.mistareasapp.data.habits.HabitoHistorial?,
    fecha: java.time.LocalDate,
    viewModel: HabitosViewModel,
    onDismiss: () -> Unit
) {
    val objetivo = habito.objetivoValor ?: habito.vecesPorDia
    val unidad = habito.unidad ?: ""
    var valor by remember { mutableStateOf(progreso?.valorProgreso ?: 0) }
    // TextFieldValue con selección inicial de todo el texto para poder escribir sin borrar antes
    var textoValor by remember {
        val initText = if (valor == 0) "" else valor.toString()
        mutableStateOf(TextFieldValue(text = initText, selection = TextRange(0, initText.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val porcentaje = if (objetivo > 0) (valor.toFloat() / objetivo).coerceIn(0f, 1f) else 0f

    val quickValues = when {
        objetivo <= 20 -> listOf(1, 2, 5)
        objetivo <= 100 -> listOf(5, 10, 25)
        objetivo <= 1000 -> listOf(10, 50, 100)
        else -> listOf(100, 500, 1000)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título y cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(habito.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Objetivo: $objetivo $unidad",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))

                // Valor grande
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text(
                        "$valor",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        " / $objetivo",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Barra de progreso
                LinearProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(porcentaje * 100).toInt()}% completado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                Text("Cantidad registrada", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))

                // Fila -/input/+
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val newVal = (valor - 1).coerceAtLeast(0)
                            valor = newVal
                            val t = if (newVal == 0) "" else newVal.toString()
                            textoValor = TextFieldValue(t, TextRange(t.length))
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Restar 1")
                    }
                    OutlinedTextField(
                        value = textoValor,
                        onValueChange = { tv ->
                            textoValor = tv
                            valor = tv.text.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("0", textAlign = TextAlign.Center) }
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val newVal = valor + 1
                            valor = newVal
                            val t = newVal.toString()
                            textoValor = TextFieldValue(t, TextRange(t.length))
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Sumar 1")
                    }
                }
                if (unidad.isNotBlank()) {
                    Text(unidad, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(12.dp))

                // Botones rápidos — fila de negativos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickValues.forEach { v ->
                        OutlinedButton(
                            onClick = {
                                val newVal = (valor - v).coerceAtLeast(0)
                                valor = newVal
                                val t = if (newVal == 0) "" else newVal.toString()
                                textoValor = TextFieldValue(t, TextRange(t.length))
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) { Text("-$v", fontSize = 12.sp) }
                    }
                }

                // Botones rápidos — fila de positivos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickValues.forEach { v ->
                        Button(
                            onClick = {
                                val newVal = valor + v
                                valor = newVal
                                val t = newVal.toString()
                                textoValor = TextFieldValue(t, TextRange(t.length))
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) { Text("+$v", fontSize = 12.sp) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Cancelar / Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            viewModel.registrarCumplimientoCuantitativo(habito, progreso, valor, fecha)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoLimiteMaximo(
    habito: com.example.mistareasapp.data.habits.Habito,
    progreso: HabitoConProgreso,
    limiteEfectivo: Double = habito.limiteMaximo ?: 0.0,
    fecha: LocalDate,
    viewModel: HabitosViewModel,
    onDismiss: () -> Unit
) {
    val limiteMaximo = limiteEfectivo
    val acumulado = progreso.valorPeriodoDecimal
    val unidad = habito.unidad ?: ""

    var textoValor by remember { mutableStateOf("") }
    var mostrarUbe by remember { mutableStateOf(false) }
    var tipoBebida by remember { mutableStateOf(TipoBebidaUBE.CERVEZA_SIDRA) }
    var textoMl by remember { mutableStateOf("") }
    var expandidoBebida by remember { mutableStateOf(false) }

    val valorDecimal = textoValor.replace(",", ".").toDoubleOrNull() ?: 0.0
    val mlDecimal = textoMl.replace(",", ".").toDoubleOrNull() ?: 0.0

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                Text(habito.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Acumulado vs límite
                val pctActual = if (limiteMaximo > 0) (acumulado / limiteMaximo).toFloat().coerceIn(0f, 1f) else 0f
                val colorBarra = if (acumulado > limiteMaximo) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                val acumStr = if (acumulado == acumulado.toLong().toDouble()) acumulado.toLong().toString()
                    else "%.1f".format(acumulado)
                val limStr = if (limiteMaximo == limiteMaximo.toLong().toDouble()) limiteMaximo.toLong().toString()
                    else "%.1f".format(limiteMaximo)
                Text(
                    "Acumulado: $acumStr / $limStr $unidad".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { pctActual },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = colorBarra
                )

                HorizontalDivider()

                // UBE converter (opcional)
                if (habito.ubeActivo) {
                    Text("Convertir a UBE", style = MaterialTheme.typography.labelMedium)
                    // Selector bebida
                    ExposedDropdownMenuBox(
                        expanded = expandidoBebida,
                        onExpandedChange = { expandidoBebida = it }
                    ) {
                        OutlinedTextField(
                            value = tipoBebida.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo bebida") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoBebida) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandidoBebida,
                            onDismissRequest = { expandidoBebida = false }
                        ) {
                            TipoBebidaUBE.entries.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text("${tipo.label} (${tipo.mlPorUbe.toInt()} ml/UBE)") },
                                    onClick = { tipoBebida = tipo; expandidoBebida = false }
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textoMl,
                            onValueChange = { textoMl = it },
                            label = { Text("ml") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val ube = mlAUbe(mlDecimal, tipoBebida)
                                textoValor = "%.2f".format(ube).trimEnd('0').trimEnd('.')
                            },
                            enabled = mlDecimal > 0
                        ) { Text("→ UBE") }
                    }
                    HorizontalDivider()
                }

                // Campo valor a registrar
                Text("Cantidad a registrar ($unidad)".trim(), style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = textoValor,
                    onValueChange = { textoValor = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Botones rápidos
                val quickValues = listOf(0.5, 1.0, 2.0, 5.0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickValues.forEach { v ->
                        OutlinedButton(
                            onClick = {
                                val nuevo = (valorDecimal + v)
                                textoValor = if (nuevo == nuevo.toLong().toDouble()) nuevo.toLong().toString()
                                    else "%.1f".format(nuevo)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) { Text("+${if (v == v.toLong().toDouble()) v.toLong() else v}", fontSize = 12.sp) }
                    }
                }

                // Cancelar / Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            if (valorDecimal > 0) {
                                viewModel.registrarLimiteMaximo(habito, progreso.progreso, valorDecimal, fecha)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = valorDecimal > 0
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
