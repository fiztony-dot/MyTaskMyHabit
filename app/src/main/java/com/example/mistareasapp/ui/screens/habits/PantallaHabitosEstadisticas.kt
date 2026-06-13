package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.History
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.viewmodel.Habits.HabitoConHistorialSemanal
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHabitosEstadisticas(viewModel: HabitosViewModel, modifier: Modifier = Modifier) {
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()
    val habitosSemana     by viewModel.habitosConHistorialSemanal.collectAsState()
    val categorias        by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val estadisticas      by viewModel.estadisticasHabito.collectAsState()
    var indexSeleccionado by remember { mutableStateOf(0) }
    var tabSeleccionada   by remember { mutableStateOf(0) } // 0=PorHábito, 1=PorCategoría

    LaunchedEffect(indexSeleccionado, habitosConProgreso) {
        if (habitosConProgreso.isNotEmpty()) {
            viewModel.seleccionarHabitoEstadisticas(habitosConProgreso[indexSeleccionado].habito.id)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-pestañas
        SecondaryTabRow(selectedTabIndex = tabSeleccionada, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 },
                text = { Text("Por hábito", fontWeight = FontWeight.Bold) })
            Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 },
                text = { Text("Por categoría", fontWeight = FontWeight.Bold) })
        }

        when (tabSeleccionada) {
            0 -> EstadisticasPorHabito(
                habitosConProgreso = habitosConProgreso,
                estadisticas = estadisticas,
                indexSeleccionado = indexSeleccionado,
                onIndexChange = { indexSeleccionado = it }
            )
            1 -> EstadisticasPorCategoria(
                habitosSemana = habitosSemana,
                categorias = categorias
            )
        }
    }
}

// ─── Vista "Por hábito" (contenido existente) ──────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstadisticasPorHabito(
    habitosConProgreso: List<com.example.mistareasapp.viewmodel.Habits.HabitoConProgreso>,
    estadisticas: com.example.mistareasapp.viewmodel.Habits.EstadisticasHabito,
    indexSeleccionado: Int,
    onIndexChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))

        // Selector de hábito + botón historial de pausas
        if (habitosConProgreso.isNotEmpty()) {
            val habitoActual = habitosConProgreso[indexSeleccionado].habito
            var expandido by remember { mutableStateOf(false) }
            var mostrarPausas by remember { mutableStateOf(false) }
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandido = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(habitoActual.nombre) }
                    DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                        habitosConProgreso.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(item.habito.nombre) },
                                onClick = { onIndexChange(index); expandido = false }
                            )
                        }
                    }
                }
                IconButton(onClick = { mostrarPausas = true }) {
                    Icon(Icons.Default.History, contentDescription = "Historial de pausas",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Fecha de inicio discreta
            Text(
                text = "Desde: ${habitoActual.fechaInicio.format(fmt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            // Diálogo historial de pausas
            if (mostrarPausas) {
                AlertDialog(
                    onDismissRequest = { mostrarPausas = false },
                    title = { Text("Períodos de pausa", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (habitoActual.fechaInicioPausa == null) {
                                Text("Este hábito no ha tenido períodos de pausa.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                val inicio = habitoActual.fechaInicioPausa!!.format(fmt)
                                val fin = habitoActual.fechaFinPausa?.format(fmt) ?: "actualidad"
                                val estado = if (habitoActual.pausado) " (activo)" else " (finalizado)"
                                Card(colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Pausa$estado", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Desde $inicio hasta $fin",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Text(
                                "Nota: El modelo de datos actual almacena únicamente el período de pausa más reciente.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { mostrarPausas = false }) { Text("Cerrar") }
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CardEstadistica("Racha Actual", "${estadisticas.rachaActual} días", Icons.Default.Whatshot, Color(0xFFFF9800), Modifier.weight(1f))
            CardEstadistica("Mejor Racha",  "${estadisticas.mejorRacha} días",  Icons.Default.Star,     Color(0xFFFFC107), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("% de cumplimiento (este año)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    CircularProgressIndicator(
                        progress = { estadisticas.porcentajeCumplimiento },
                        modifier = Modifier.fillMaxSize(), strokeWidth = 12.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text("${kotlin.math.round(estadisticas.porcentajeCumplimiento * 100).toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        if (habitosConProgreso.isNotEmpty()) {
            val frecuencia = habitosConProgreso[indexSeleccionado].habito.frecuencia
            val (labelSemana, labelMes, labelAno, labelTotal) = when (frecuencia) {
                com.example.mistareasapp.data.habits.FrecuenciaHabito.DIARIA  -> listOf("Semana", "Mes", "Año", "Total días")
                com.example.mistareasapp.data.habits.FrecuenciaHabito.SEMANAL -> listOf("Esta sem.", "Semanas/mes", "Semanas/año", "Total sem.")
                com.example.mistareasapp.data.habits.FrecuenciaHabito.MENSUAL -> listOf("Este mes", "—", "Meses/año", "Total mes.")
            }
            Text("Resumen histórico", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ItemMetrica(labelSemana, "${estadisticas.completadosSemana}/${estadisticas.diasEnSemana}")
                ItemMetrica(labelMes,    "${estadisticas.completadosMes}/${estadisticas.diasEnMes}")
                ItemMetrica(labelAno,    "${estadisticas.completadosAno}/${estadisticas.diasEnAno}")
                ItemMetrica(labelTotal,  "${estadisticas.totalCompletados}")
            }
        }
    }
}

// ─── Vista "Por categoría" ──────────────────────────────────────────────────

@Composable
private fun EstadisticasPorCategoria(
    habitosSemana: List<HabitoConHistorialSemanal>,
    categorias: List<CategoriaHabito>
) {
    var excluirPausados by remember { mutableStateOf(true) }
    val haceUnMes = LocalDate.now().minusMonths(1)

    val habitosFiltrados = habitosSemana.filter { item ->
        !excluirPausados ||
            !(item.habito.pausado && item.habito.fechaInicioPausa?.isBefore(haceUnMes) == true)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Toggle global
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Excluir pausados +1 mes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(checked = excluirPausados, onCheckedChange = { excluirPausados = it },
                    modifier = Modifier.height(24.dp))
            }
        }

        // Tarjeta por categoría
        categorias.forEach { cat ->
            val habitosCat = habitosFiltrados.filter { it.habito.categoriaId == cat.id }
            if (habitosCat.isNotEmpty()) {
                TarjetaCategoriaStats(cat, habitosCat)
            }
        }

        // Hábitos sin categoría
        val sinCat = habitosFiltrados.filter { h -> categorias.none { it.id == h.habito.categoriaId } }
        if (sinCat.isNotEmpty()) {
            TarjetaCategoriaStats(
                categoria = null,
                habitos = sinCat
            )
        }

        if (habitosFiltrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Sin hábitos activos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TarjetaCategoriaStats(
    categoria: CategoriaHabito?,
    habitos: List<HabitoConHistorialSemanal>
) {
    val colorCategoria = categoria?.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) }
        catch (_: Exception) { MaterialTheme.colorScheme.primary }
    } ?: MaterialTheme.colorScheme.onSurfaceVariant

    val pctAgregado = if (habitos.isEmpty()) 0f
        else habitos.map { it.porcentajeHistorico }.average().toFloat()

    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Cabecera: icono + nombre + count de hábitos
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (categoria != null) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(colorCategoria.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        Text(iconoAEmoji(categoria.icono), fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(categoria.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                } else {
                    Text("Sin categoría", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                }
                Text("${habitos.size} hábitos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // % agregado + barra de progreso
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${(pctAgregado * 100).toInt()}%",
                    fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colorCategoria)
                LinearProgressIndicator(
                    progress = { pctAgregado.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = colorCategoria,
                    trackColor = colorCategoria.copy(alpha = 0.12f)
                )
            }

            // Badges compactos — 3-4 por línea, sin icono, texto pequeño
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                habitos.forEach { item ->
                    val pct = kotlin.math.round(item.porcentajeHistorico * 100).toInt()
                    val badgeColor = when {
                        item.porcentajeHistorico >= 0.8f -> Color(0xFF4CAF50)
                        item.porcentajeHistorico >= 0.5f -> Color(0xFFFFB74D)
                        else                              -> Color(0xFFCF6679)
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.habito.nombre,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            "$pct%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                        Text("·", fontSize = 8.sp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

// ─── Componentes compartidos ────────────────────────────────────────────────

@Composable
fun CardEstadistica(titulo: String, valor: String, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icono, contentDescription = null, tint = color)
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ItemMetrica(label: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
