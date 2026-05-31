package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.mistareasapp.data.habits.CategoriaHabito
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.time.Instant
import java.time.ZoneOffset
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.data.habits.diasSemanaSet
import com.example.mistareasapp.ui.components.habits.SelectorFechaConProgreso
import com.example.mistareasapp.iconoAEmoji
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
    val progresoGeneral = if (totalHabitos > 0)
        habitosSemana.map { it.porcentajeSemanal(hoy) }.average().toFloat()
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
                        HabitoListadoCard(it, hoy, viewModel, navController)
                    }
                }
                if (sinCategoria.isNotEmpty()) {
                    item(key = "cat_sin") { CabeceraCategoria("Sin categoría", null) }
                    items(sinCategoria, key = { it.habito.id }) {
                        HabitoListadoCard(it, hoy, viewModel, navController)
                    }
                }
            } else {
                items(habitosSemana) { item ->
                    HabitoListadoCard(item, hoy, viewModel, navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitoListadoCard(item: HabitoConHistorialSemanal, hoy: LocalDate, viewModel: HabitosViewModel, navController: NavHostController) {
    val habito = item.habito
    val colorHabito = Color(android.graphics.Color.parseColor(habito.colorHex))
    val porcentaje = item.porcentajeSemanal(hoy)
    val porcentajeTexto = "${(porcentaje * 100).toInt()}%"

    val esCuantitativo = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
    val esPorTareas = habito.esCompuestoPorTareas
    val esDiaria = habito.frecuencia == FrecuenciaHabito.DIARIA

    var dialogoFecha by remember { mutableStateOf<LocalDate?>(null) }
    var mostrarPausaDialog by remember { mutableStateOf(false) }

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

            // Header: emoji sin fondo + nombre
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = iconoAEmoji(habito.icono),
                    fontSize = 24.sp,
                    modifier = Modifier.size(36.dp).wrapContentSize(Alignment.Center)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(habito.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = cardTexto, modifier = Modifier.weight(1f))
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
                        Text(
                            literalDia.take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (fecha == hoy) colorHabito else cardTextoSub,
                            fontWeight = if (fecha == hoy) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val valorProgreso = historial?.valorProgreso ?: 0
                        val esVerdeCirculo = aplica && (if (esCuantitativo && !esDiaria) valorProgreso > 0 else completado)
                        val esNaranjaCirculo = aplica && esPorTareas && !completado && valorProgreso > 0
                        val pastGreenCirculo  = Color(0xFFA8D5A2)
                        val pastOrangeCirculo = Color(0xFFFFCB87)
                        val borderColor = when {
                            !aplica -> Color(0xFFD5D5D5)
                            esVerdeCirculo -> Color(0xFF7CB87A)
                            esNaranjaCirculo -> Color(0xFFFFAA50)
                            esPasadaOHoy -> Color(0xFFBBBBBB)
                            else -> Color(0xFFCCCCCC)
                        }
                        val bgColor = when {
                            !aplica -> Color(0xFFE8E8E8)
                            esVerdeCirculo -> pastGreenCirculo
                            esNaranjaCirculo -> pastOrangeCirculo
                            esPasadaOHoy -> Color.White
                            else -> Color(0xFFF0F0F0)
                        }
                        val contentColor = when {
                            !aplica -> Color(0xFFCCCCCC)
                            esVerdeCirculo -> Color(0xFF2E7D32)
                            esNaranjaCirculo -> Color(0xFFBF5000)
                            esPasadaOHoy -> if (fecha == hoy) colorHabito else Color(0xFFAAAAAA)
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
                            when {
                                !aplica -> Text("–", fontSize = 10.sp, color = contentColor)
                                esVerdeCirculo && esCuantitativo -> {
                                    val objetivo = habito.objetivoValor ?: habito.vecesPorDia
                                    val pct = if (objetivo > 0) (valorProgreso.toFloat() / objetivo * 100).toInt() else 0
                                    Text("$pct%", fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                }
                                esVerdeCirculo -> Icon(
                                    Icons.Default.Check,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = contentColor
                                )
                                esNaranjaCirculo -> {
                                    val objetivo = habito.objetivoValor ?: habito.vecesPorDia
                                    Text("$valorProgreso/$objetivo", fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold)
                                }
                                esCuantitativo && esPasadaOHoy && valorProgreso > 0 -> {
                                    val objetivo = habito.objetivoValor ?: habito.vecesPorDia
                                    val pct = if (objetivo > 0) (valorProgreso.toFloat() / objetivo * 100).toInt() else 0
                                    Text("$pct%", fontSize = 7.sp, color = contentColor, fontWeight = FontWeight.Bold)
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

            // Fila de acciones: badge + editar + vista mensual + pausar
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeColor = when {
                    porcentaje >= 0.8f -> Color(0xFF4CAF50)
                    porcentaje >= 0.5f -> Color(0xFFFFB74D)
                    else -> Color(0xFFCF6679)
                }
                Badge(containerColor = badgeColor) {
                    Text(porcentajeTexto, color = Color.White)
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
