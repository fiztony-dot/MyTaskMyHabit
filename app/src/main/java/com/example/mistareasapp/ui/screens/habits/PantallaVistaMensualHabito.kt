package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.data.habits.diasSemanaSet
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVistaMensualHabito(
    habitoId: Long,
    viewModel: HabitosViewModel,
    navController: NavController
) {
    val habito by viewModel.obtenerHabitoPorId(habitoId).collectAsState(initial = null)
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val hoy = LocalDate.now()
    var mesSeleccionado by remember { mutableStateOf(YearMonth.now()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportandoPdf by remember { mutableStateOf(false) }

    val historialMes by viewModel.obtenerHistorialMes(habitoId, mesSeleccionado)
        .collectAsState(initial = emptyMap())

    val pausasHabito by viewModel.obtenerPausasHabitoFlow(habitoId)
        .collectAsState(initial = emptyList())
    val versionesList by viewModel.obtenerVersionesHabitoFlow(habitoId)
        .collectAsState(initial = emptyList())

    var dialogoFecha by remember { mutableStateOf<LocalDate?>(null) }
    var pieMensual by remember { mutableStateOf<com.example.mistareasapp.viewmodel.Habits.HabitosViewModel.PieMensualData?>(null) }

    // Recalcular pie cuando cambian el hábito, el mes o el historial
    LaunchedEffect(habito?.id, mesSeleccionado, historialMes.size) {
        if (habito != null) {
            pieMensual = viewModel.calcularPieMensualVersionado(habito!!, mesSeleccionado, historialMes, hoy)
        }
    }

    val colorHabito = habito?.let {
        try { Color(android.graphics.Color.parseColor(it.colorHex)) }
        catch (_: Exception) { MaterialTheme.colorScheme.primary }
    } ?: MaterialTheme.colorScheme.primary

    val esCuantitativo = habito?.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
    val esLimiteMaximo = habito?.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO
    val esPorTareas = habito?.esCompuestoPorTareas ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    habito?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(iconoAEmoji(iconoEfectivoHabito(it.categoriaId, it.icono, categorias)), fontSize = 18.sp)
                                Text(it.nombre, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "desde ${it.fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (exportandoPdf) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = {
                                val h = habito ?: return@IconButton
                                exportandoPdf = true
                                scope.launch {
                                    try {
                                        val uri = viewModel.generarInformePdf(context, h, categorias)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Exportar informe"))
                                    } finally {
                                        exportandoPdf = false
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar informe PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Navegación de mes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mesSeleccionado = mesSeleccionado.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                }
                Text(
                    text = mesSeleccionado.month.getDisplayName(TextStyle.FULL, Locale("es"))
                        .replaceFirstChar { it.uppercase() } + " ${mesSeleccionado.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { mesSeleccionado = mesSeleccionado.plusMonths(1) },
                    enabled = mesSeleccionado < YearMonth.now()
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }

            // Cabeceras días semana
            val diasSemana = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")
            Row(modifier = Modifier.fillMaxWidth()) {
                diasSemana.forEach { dia ->
                    Text(
                        text = dia,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Detectar si un día cae en cualquier periodo de pausa (historial completo)
            fun diaPausado(fecha: LocalDate): Boolean = pausasHabito.any { p ->
                !fecha.isBefore(p.fechaInicio) &&
                (if (p.fechaFin != null) fecha.isBefore(p.fechaFin) else true)
            }
            // Naranja de la paleta existente — borde sin relleno
            val colorPausa = Color(0xFFFFAA50)
            val bgPausa    = Color.Transparent  // borde naranja sin relleno (según leyenda)

            // Grid de días
            val primerDia = mesSeleccionado.atDay(1)
            val offsetInicio = (primerDia.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val totalDias = mesSeleccionado.lengthOfMonth()
            val celdas = offsetInicio + totalDias
            val filas = (celdas + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until filas).forEach { fila ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        (0..6).forEach { col ->
                            val indice = fila * 7 + col
                            val numeroDia = indice - offsetInicio + 1
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (numeroDia in 1..totalDias) {
                                    val fecha = mesSeleccionado.atDay(numeroDia)
                                    val esPasadaOHoy = !fecha.isAfter(hoy)
                                    val historial = historialMes[fecha]
                                    val completado = historial?.completado ?: false
                                    val esHoy = fecha == hoy
                                    val esPausado = diaPausado(fecha)
                                    val esAntesDeInicio = habito != null && fecha.isBefore(habito!!.fechaInicio)

                                    val colorHoy = MaterialTheme.colorScheme.primary
                                    val verdeCompletado = Color(0xFF4CAF50)   // verde sólido → blanco
                                    val verdePastel     = Color(0xFFA8D5A2)   // verde suave → texto oscuro
                                    val verdeBorde      = Color(0xFF388E3C)
                                    val valorCuant = historial?.valorProgreso ?: 0
                                    val valorDecimalDia = historial?.valorProgresoDecimal ?: 0.0
                                    val limiteMaximo = habito?.limiteMaximo ?: 0.0
                                    val tieneValorCuant = esCuantitativo && esPasadaOHoy && valorCuant > 0
                                    val tieneValorLimite = esLimiteMaximo && esPasadaOHoy && valorDecimalDia > 0
                                    val limiteExcedido = tieneValorLimite && limiteMaximo > 0 && valorDecimalDia > limiteMaximo
                                    val tieneTareasParc = esPorTareas && esPasadaOHoy && !completado && valorCuant > 0
                                    val bgColor = when {
                                        esAntesDeInicio  -> Color(0xFF424242)
                                        esPausado        -> bgPausa
                                        tieneValorLimite && !limiteExcedido -> verdePastel
                                        tieneValorLimite && limiteExcedido  -> Color(0xFFFFCDD2)
                                        completado       -> verdeCompletado
                                        tieneValorCuant  -> verdePastel
                                        tieneTareasParc  -> Color(0xFFFFCB87)
                                        esHoy            -> colorHoy.copy(alpha = 0.15f)
                                        else             -> Color.Transparent
                                    }
                                    val borderColor = when {
                                        esAntesDeInicio  -> Color(0xFF616161)
                                        esPausado        -> colorPausa
                                        tieneValorLimite && !limiteExcedido -> Color(0xFF7CB87A)
                                        tieneValorLimite && limiteExcedido  -> Color(0xFFB71C1C)
                                        completado       -> verdeBorde
                                        tieneValorCuant  -> Color(0xFF7CB87A)
                                        tieneTareasParc  -> Color(0xFFFFAA50)
                                        esHoy            -> colorHoy
                                        esPasadaOHoy     -> MaterialTheme.colorScheme.outlineVariant
                                        else             -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    }
                                    val borderWidth = if (esHoy && !esPausado) 2.dp else 1.5.dp

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .border(borderWidth, borderColor, CircleShape)
                                                .then(
                                                    if (esPasadaOHoy && !esAntesDeInicio) Modifier.clickable {
                                                        when {
                                                            esPorTareas || esCuantitativo || esLimiteMaximo -> dialogoFecha = fecha
                                                            else -> viewModel.toggleHabitoCompletoEnFecha(habito!!, fecha)
                                                        }
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                // Antes del inicio del hábito: gris oscuro con número tenue
                                                esAntesDeInicio -> Text("$numeroDia", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                                                // Pausado: borde naranja, sin relleno → icono pausa en naranja
                                                esPausado -> Icon(Icons.Default.Pause, null, modifier = Modifier.size(14.dp), tint = colorPausa)
                                                // Límite Máximo con valor (antes que completado para mostrar decimal)
                                                tieneValorLimite -> {
                                                    val versionFecha = versionesList
                                                        .filter { !it.fechaInicio.isAfter(fecha) }
                                                        .maxByOrNull { it.fechaInicio }
                                                    val unidadCorta = (versionFecha?.unidad ?: habito?.unidad)
                                                        ?.take(3)?.trim() ?: ""
                                                    val decStr = if (valorDecimalDia == valorDecimalDia.toLong().toDouble())
                                                        valorDecimalDia.toLong().toString() else "%.1f".format(valorDecimalDia)
                                                    val textColor = if (limiteExcedido) Color(0xFFB71C1C) else Color(0xFF1A1A1A)
                                                    Text(
                                                        if (unidadCorta.isNotEmpty()) "$decStr\n$unidadCorta" else decStr,
                                                        fontSize = 9.sp,
                                                        color = textColor,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        lineHeight = 10.sp
                                                    )
                                                }
                                                // Completado: verde sólido → blanco
                                                completado -> Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                // Cuantitativo con valor: verde pastel → texto oscuro
                                                tieneValorCuant -> {
                                                    val versionFecha = versionesList
                                                        .filter { !it.fechaInicio.isAfter(fecha) }
                                                        .maxByOrNull { it.fechaInicio }
                                                    val unidadCorta = (versionFecha?.unidad ?: habito?.unidad)
                                                        ?.take(3)?.trim() ?: ""
                                                    Text(
                                                        if (unidadCorta.isNotEmpty()) "$valorCuant\n$unidadCorta" else "$valorCuant",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF1A1A1A),
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        lineHeight = 10.sp
                                                    )
                                                }
                                                // Tareas parciales: naranja pastel → blanco
                                                tieneTareasParc -> Icon(Icons.Default.List, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                // Tareas sin progreso (pasado/hoy, 0 tareas): sin fondo → onSurfaceVariant
                                                esPorTareas && esPasadaOHoy -> Icon(Icons.Default.List, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                // Hoy sin datos: fondo primario 15% → color primario
                                                esHoy -> Text("$numeroDia", fontSize = 11.sp, color = colorHoy, fontWeight = FontWeight.Bold)
                                                // Pasado sin datos: sin fondo → onSurfaceVariant
                                                esPasadaOHoy -> Text("$numeroDia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                // Futuro: transparente → gris suave
                                                else -> Text("$numeroDia", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            }
                                        }
                                        // El número del día siempre es visible bajo el círculo
                                        Text(
                                            "$numeroDia",
                                            fontSize = 9.sp,
                                            color = when {
                                                esPausado  -> colorPausa.copy(alpha = 0.7f)
                                                esHoy      -> colorHoy
                                                else       -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            },
                                            fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Leyenda de colores del calendario
            val tienePausa = pausasHabito.isNotEmpty()
            val tieneAntesDeInicio = habito != null && mesSeleccionado.atDay(1).isBefore(habito!!.fechaInicio)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    @Composable
                    fun LeyendaItem(bg: Color, border: Color, label: String) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(bg).border(1.5.dp, border, CircleShape))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val colorHoy2 = MaterialTheme.colorScheme.primary
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LeyendaItem(Color(0xFFA8D5A2), Color(0xFF7CB87A), "Cumplido")
                            LeyendaItem(colorHoy2.copy(alpha = 0.12f), colorHoy2, "Hoy")
                            if (tieneAntesDeInicio) {
                                LeyendaItem(Color(0xFF424242), Color(0xFF616161), "Antes del inicio")
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LeyendaItem(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outlineVariant, "Sin cumplir")
                            if (tienePausa) {
                                LeyendaItem(Color.Transparent, Color(0xFFFFAA50), "Pausado")
                            }
                        }
                    }
                }
            }

            // Pie de cumplimiento versionado (calculado con LaunchedEffect)
            pieMensual?.let { pie ->
                // Para LIMITE_MAXIMO usamos los campos decimales y pctTramo directamente
                val usaLimite = esLimiteMaximo && pie.pctTramo != null
                val pct: Int
                val textoValor: String
                val progressFraction: Float
                if (usaLimite) {
                    pct = pie.pctTramo!!
                    val acum = pie.progresoDecimal ?: pie.progreso.toDouble()
                    val lim = pie.objetivoDecimal ?: pie.objetivo.toDouble()
                    val acumStr = if (acum == acum.toLong().toDouble()) acum.toLong().toString() else "%.2f".format(acum).trimEnd('0').trimEnd('.')
                    val limStr = if (lim == lim.toLong().toDouble()) lim.toLong().toString() else "%.2f".format(lim).trimEnd('0').trimEnd('.')
                    textoValor = "$acumStr / $limStr ${pie.unidad} ($pct%)"
                    progressFraction = if (lim > 0) (acum / lim).toFloat().coerceIn(0f, 1f) else 0f
                } else {
                    val pctRaw = if (pie.objetivo > 0) (pie.progreso.toFloat() / pie.objetivo * 100).toInt() else 0
                    pct = when (habito?.tipoMedicion) {
                        com.example.mistareasapp.data.habits.TipoMedicion.PROPORCIONAL_SIN_TOPE -> pctRaw.coerceAtLeast(0)
                        com.example.mistareasapp.data.habits.TipoMedicion.BINARIO -> if (pctRaw >= 100) 100 else 0
                        else -> pctRaw.coerceAtMost(100)
                    }
                    textoValor = "${pie.progreso} / ${pie.objetivo} ${pie.unidad} ($pct%)"
                    progressFraction = (pie.progreso.toFloat() / pie.objetivo.coerceAtLeast(1)).coerceIn(0f, 1f)
                }
                val colorPie = when {
                    pct >= 80 -> Color(0xFF4CAF50)
                    pct >= 50 -> Color(0xFFFFB74D)
                    else -> colorHabito
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                pie.etiqueta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                textoValor,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colorPie
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = colorPie,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    dialogoFecha?.let { fecha ->
        val historialFecha = historialMes[fecha]
        if (esPorTareas) {
            DialogoTareasHabito(
                habito = habito!!,
                progreso = historialFecha,
                fecha = fecha,
                viewModel = viewModel,
                onDismiss = { dialogoFecha = null }
            )
        } else if (esCuantitativo) {
            DialogoCuantitativo(
                habito = habito!!,
                progreso = historialFecha,
                fecha = fecha,
                viewModel = viewModel,
                onDismiss = { dialogoFecha = null }
            )
        } else if (esLimiteMaximo) {
            DialogoLimiteMaximoFecha(
                habito = habito!!,
                historial = historialFecha,
                fecha = fecha,
                viewModel = viewModel,
                onDismiss = { dialogoFecha = null }
            )
        }
    }
}
