package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.ui.theme.M3CardHabito
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCalculoHabito(
    habitoId: Long,
    viewModel: HabitosViewModel,
    navController: NavHostController
) {
    val habito by viewModel.obtenerHabitoPorId(habitoId).collectAsState(initial = null)
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val pausas by viewModel.obtenerPausasHabitoFlow(habitoId).collectAsState(initial = emptyList())
    val versiones by viewModel.obtenerVersionesHabitoFlow(habitoId).collectAsState(initial = emptyList())
    val datosCalculos by viewModel.datosCalculos.collectAsState()

    val dato = datosCalculos.firstOrNull { it.habito.id == habitoId }

    var desglose by remember { mutableStateOf<HabitosViewModel.DesgloseData?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(habitoId, habito) {
        habito?.let { h ->
            scope.launch {
                desglose = viewModel.obtenerDesgloseHistorico(h)
            }
        }
    }

    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es"))

    // Cálculo de peso y contribución
    val todosLosDatos = datosCalculos
    val pesoTotal = todosLosDatos.sumOf { (minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad).toLong() }
    val diasCap = dato?.let { minOf(it.diasVidaEfectivos, 180) } ?: 0
    val tieneTope = dato != null && dato.diasVidaEfectivos > 180
    val peso = diasCap * (dato?.habito?.dificultad ?: 1)
    val contribucionPp = if (pesoTotal > 0 && dato != null)
        dato.porcentaje.toDouble() * peso / pesoTotal * 100 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habito?.nombre ?: "Detalle cálculo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("auditoria_habitos?habitoId=$habitoId") }) {
                        Icon(Icons.Default.ManageSearch, contentDescription = "Auditoría")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Cabecera: icono + nombre + %
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = M3CardHabito)
                ) {
                    val cardTexto = Color(0xFF1A1A1A)
                    val cardSub   = Color(0xFF555555)
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        habito?.let { h ->
                            Text(
                                iconoAEmoji(iconoEfectivoHabito(h.categoriaId, h.icono, categorias)),
                                fontSize = 32.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habito?.nombre ?: "—", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = cardTexto)
                            habito?.let { h ->
                                Text(
                                    "Desde ${h.fechaInicio.format(fmt)}",
                                    fontSize = 11.sp, color = cardSub
                                )
                                val frecLabel = when (h.frecuencia) {
                                    com.example.mistareasapp.data.habits.FrecuenciaHabito.DIARIA   -> "Diaria"
                                    com.example.mistareasapp.data.habits.FrecuenciaHabito.SEMANAL  -> "Semanal"
                                    com.example.mistareasapp.data.habits.FrecuenciaHabito.MENSUAL  -> "Mensual"
                                }
                                val tipoLabel = if (h.tipoObjetivo == com.example.mistareasapp.data.habits.TipoObjetivoHabito.CUANTITATIVO)
                                    "Cuantitativo" else "Frecuencia"
                                Text("$frecLabel · $tipoLabel", fontSize = 11.sp, color = cardSub)
                            }
                        }
                        if (dato != null) {
                            Text(
                                "${kotlin.math.round(dato.porcentaje * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = when {
                                    dato.porcentaje >= 0.8f -> Color(0xFF2E7D32)
                                    dato.porcentaje >= 0.5f -> Color(0xFFE65100)
                                    else                    -> Color(0xFFC62828)
                                }
                            )
                        }
                    }
                }
            }

            // Desglose de cumplimiento
            item {
                SectionCard("Desglose de cumplimiento") {
                    if (desglose != null) {
                        val d = desglose!!
                        if (d.esBinario) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom) {
                                Column {
                                    val periodosFormato = if (d.periodosCompletados % 1f == 0f)
                                        d.periodosCompletados.toInt().toString() else "%.1f".format(d.periodosCompletados)
                                    Text("$periodosFormato de ${d.periodosTotal} ${d.unidadPeriodo}",
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("completados al 100%", fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val pct = d.completados  // ya está redondeado
                                Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                                    color = when { pct >= 80 -> Color(0xFF2E7D32); pct >= 50 -> Color(0xFFE65100); else -> Color(0xFFC62828) })
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (d.periodosTotal > 0) (d.periodosCompletados.toFloat() / d.periodosTotal).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = Color(0xFF558B2F), trackColor = Color(0xFF558B2F).copy(alpha = 0.15f)
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom) {
                                Column {
                                    Text("Media: ${d.completados}%", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    val periodosFormato = if (d.periodosCompletados % 1f == 0f)
                                        d.periodosCompletados.toInt().toString() else "%.1f".format(d.periodosCompletados)
                                    Text("$periodosFormato de ${d.periodosTotal} ${d.unidadPeriodo} > 50%", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (d.completados / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = Color(0xFF558B2F), trackColor = Color(0xFF558B2F).copy(alpha = 0.15f)
                            )
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Periodos pausados
            item {
                SectionCard("Periodos pausados") {
                    if (pausas.isEmpty()) {
                        Text("Sin periodos pausados.", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            pausas.forEach { p ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(8.dp)
                                            .background(Color(0xFFFFAA50), CircleShape)
                                    )
                                    Text(
                                        "${p.fechaInicio.format(fmt)} → ${p.fechaFin?.format(fmt) ?: "activo"}",
                                        fontSize = 13.sp, color = Color(0xFFBF5000)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Historial de definición
            item {
                SectionCard("Historial de definición") {
                    if (versiones.isEmpty()) {
                        Text("Sin historial de definición.", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Consolidar tramos consecutivos con mismo objetivo
                        data class TramoConsolidado(val fechaInicio: LocalDate, val fechaFin: LocalDate?, val version: com.example.mistareasapp.data.habits.HabitoVersion)
                        fun mismoObjetivo(a: com.example.mistareasapp.data.habits.HabitoVersion, b: com.example.mistareasapp.data.habits.HabitoVersion) =
                            a.frecuencia == b.frecuencia &&
                            a.tipoObjetivo == b.tipoObjetivo &&
                            a.vecesPorDia == b.vecesPorDia &&
                            a.objetivoValor == b.objetivoValor &&
                            a.objetivoPorcentajeDias == b.objetivoPorcentajeDias

                        val tramos = mutableListOf<TramoConsolidado>()
                        var tramoInicio = versiones.first().fechaInicio
                        for (idx in versiones.indices) {
                            val v = versiones[idx]
                            val siguiente = versiones.getOrNull(idx + 1)
                            if (siguiente == null || !mismoObjetivo(v, siguiente)) {
                                val fechaFin = siguiente?.fechaInicio?.minusDays(1)
                                tramos.add(TramoConsolidado(tramoInicio, fechaFin, v))
                                if (siguiente != null) tramoInicio = siguiente.fechaInicio
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tramos.forEachIndexed { idx, tramo ->
                                val v = tramo.version
                                val rangoTexto = if (tramo.fechaFin != null)
                                    "${tramo.fechaInicio.format(fmt)} → ${tramo.fechaFin.format(fmt)}"
                                else
                                    "${tramo.fechaInicio.format(fmt)} → actualidad"
                                val frecLabel = when (v.frecuencia) {
                                    FrecuenciaHabito.DIARIA  -> "Diaria"
                                    FrecuenciaHabito.SEMANAL -> "Semanal"
                                    FrecuenciaHabito.MENSUAL -> "Mensual"
                                }
                                val objetivoTexto = when {
                                    v.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO && v.objetivoValor != null ->
                                        "${v.objetivoValor} ${v.unidad ?: "uds"}"
                                    v.objetivoPorcentajeDias != null ->
                                        "${v.objetivoPorcentajeDias}% días"
                                    else ->
                                        "${v.vecesPorDia}× / período"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rangoTexto, fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$frecLabel · $objetivoTexto", fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    if (tramo.fechaFin == null) {
                                        Text("actual", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (idx < tramos.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Parámetros del peso
            item {
                SectionCard("Parámetros del cálculo") {
                    if (dato != null) {
                        // Desglose de días efectivos (cuando el hábito ya está cargado)
                        if (habito != null) {
                            val hoy = LocalDate.now()
                            val finHist = when (habito!!.frecuencia) {
                                com.example.mistareasapp.data.habits.FrecuenciaHabito.DIARIA   -> hoy.minusDays(1)
                                com.example.mistareasapp.data.habits.FrecuenciaHabito.SEMANAL  -> hoy.with(DayOfWeek.MONDAY).minusDays(1)
                                com.example.mistareasapp.data.habits.FrecuenciaHabito.MENSUAL  -> hoy.withDayOfMonth(1).minusDays(1)
                            }
                            val diasDesdeInicio = if (!habito!!.fechaInicio.isAfter(finHist))
                                (ChronoUnit.DAYS.between(habito!!.fechaInicio, finHist).toInt() + 1) else 0
                            val diasPausados = (diasDesdeInicio - dato.diasVidaEfectivos).coerceAtLeast(0)
                            ParamRow("Días desde inicio", "${diasDesdeInicio}d")
                            if (diasPausados > 0) {
                                ParamRow("Días pausados", "− ${diasPausados}d")
                            }
                            ParamRow(
                                "Días de vida efectivos",
                                if (diasPausados > 0) "${diasDesdeInicio} − ${diasPausados} = ${dato.diasVidaEfectivos}d"
                                else "${dato.diasVidaEfectivos}d"
                            )
                        } else {
                            ParamRow("Días de vida efectivos", "${dato.diasVidaEfectivos}d")
                        }
                        ParamRow(
                            "Días efectivos con tope",
                            if (tieneTope) "180d (tope)" else "${diasCap}d"
                        )
                        ParamRow("Dificultad", "${dato.habito.dificultad} / 5")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        ParamRow("Peso (${diasCap}d × ${dato.habito.dificultad})", "$peso")
                        ParamRow(
                            "Contribución al % general",
                            "${"%.1f".format(contribucionPp)} pp",
                            highlight = true
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionCard(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
private fun DesgloseStat(label: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ParamRow(label: String, valor: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
