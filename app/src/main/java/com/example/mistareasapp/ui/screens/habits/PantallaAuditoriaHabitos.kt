package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAuditoriaHabitos(
    viewModel: HabitosViewModel,
    navController: NavHostController,
    habitoIdInicial: Long? = null
) {
    val datosCalculos by viewModel.datosCalculos.collectAsState()
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var habitoSeleccionadoId by remember { mutableStateOf<Long?>(habitoIdInicial) }
    var expandirSelector by remember { mutableStateOf(false) }
    var periodos by remember { mutableStateOf<List<HabitosViewModel.PeriodoAuditoria>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }

    val habitoSeleccionado = datosCalculos.firstOrNull { it.habito.id == habitoSeleccionadoId }

    // Seleccionar el primero por defecto cuando carguen los datos (solo si no hay preselección)
    LaunchedEffect(datosCalculos) {
        if (habitoSeleccionadoId == null && datosCalculos.isNotEmpty()) {
            habitoSeleccionadoId = datosCalculos.first().habito.id
        }
    }

    // Cargar periodos al cambiar de hábito
    LaunchedEffect(habitoSeleccionadoId, datosCalculos) {
        val dato = datosCalculos.firstOrNull { it.habito.id == habitoSeleccionadoId } ?: return@LaunchedEffect
        cargando = true
        scope.launch {
            periodos = viewModel.obtenerAuditoriaPorHabito(dato.habito)
            cargando = false
        }
    }

    val pesoTotal = datosCalculos.sumOf { it.pesoEfectivo }

    val fmtCorto = DateTimeFormatter.ofPattern("d MMM", Locale("es"))

    // Colores de contraste para filas de tabla
    // Fondos claros → texto oscuro (#1A1A1A); excluidos (gris) → onSurfaceVariant
    val textoOscuro = Color(0xFF1A1A1A)
    val bgVerdeSuave  = Color(0xFFD4EDDA)  // verde claro
    val bgAmarillo    = Color(0xFFFFF3CD)  // amarillo claro
    val bgGris        = Color(0xFFEEEEEE)  // gris claro para excluidos

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auditoría de cálculos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // --- Selector de hábito ---
            Box {
                OutlinedButton(
                    onClick = { expandirSelector = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val nombre = habitoSeleccionado?.habito?.let { h ->
                        "${iconoAEmoji(iconoEfectivoHabito(h.categoriaId, h.icono, categorias))} ${h.nombre}"
                    } ?: "Selecciona un hábito"
                    Text(nombre, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expandirSelector,
                    onDismissRequest = { expandirSelector = false }
                ) {
                    datosCalculos.forEach { dato ->
                        val icono = iconoAEmoji(iconoEfectivoHabito(dato.habito.categoriaId, dato.habito.icono, categorias))
                        val peso = dato.pesoEfectivo
                        val contrib = if (pesoTotal > 0) dato.porcentaje.toDouble() * peso / pesoTotal * 100 else 0.0
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("$icono ${dato.habito.nombre}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${(dato.porcentaje * 100).roundToInt()}%  •  peso $peso  •  ${contrib.roundToInt()}pp al general",
                                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                habitoSeleccionadoId = dato.habito.id
                                expandirSelector = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (cargando) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                return@Column
            }

            val dato = habitoSeleccionado ?: return@Column
            val habito = dato.habito
            val activosPct = periodos.filter { !it.excluido }
            val subtotalBruto = if (activosPct.isNotEmpty()) activosPct.map { it.porcentaje }.average().toFloat() else 0f
            val pctFinal = subtotalBruto.coerceIn(0f, 1f)
            val peso = dato.pesoEfectivo
            val contrib = if (pesoTotal > 0) pctFinal.toDouble() * peso / pesoTotal * 100 else 0.0

            // --- Resumen del hábito ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("% histórico", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${(dato.porcentaje * 100).roundToInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Periodos activos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${activosPct.size} de ${periodos.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Media bruta", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${(subtotalBruto * 100).roundToInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (subtotalBruto > 1f) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tras tope 100%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${(pctFinal * 100).roundToInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Peso ($peso) × ${(pctFinal * 100).roundToInt()}% / $pesoTotal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${contrib.roundToInt()}pp al general", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Tabla de periodos ---
            // Cabecera fija
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("Periodo", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("Reg.", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("Obj.", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("%", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("Estado", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                val periodosVis = if (periodos.size > 365) periodos.takeLast(365) else periodos
                items(periodosVis) { p ->
                    val label = if (p.inicio == p.fin) p.inicio.format(fmtCorto)
                                else "${p.inicio.format(fmtCorto)} – ${p.fin.format(fmtCorto)}"
                    val pctStr = if (p.excluido) "–" else "${(p.porcentaje * 100).roundToInt()}%"

                    // Fondos claros → texto oscuro; gris de excluidos → onSurfaceVariant
                    val bgRow = when {
                        p.excluido       -> bgGris
                        p.porcentaje >= 1f -> bgVerdeSuave
                        p.porcentaje > 0f  -> bgAmarillo
                        else             -> Color.Transparent
                    }
                    // Todos los fondos usados son claros → siempre texto oscuro
                    val textoFila = when {
                        p.excluido -> MaterialTheme.colorScheme.onSurfaceVariant
                        else       -> textoOscuro
                    }
                    val textoPct = when {
                        p.excluido         -> MaterialTheme.colorScheme.onSurfaceVariant
                        p.porcentaje >= 1f -> Color(0xFF1B5E20)  // verde oscuro, legible sobre verde claro
                        p.porcentaje > 0f  -> Color(0xFF7B4F00)  // marrón oscuro, legible sobre amarillo
                        else               -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(bgRow)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 9.sp, color = textoFila, modifier = Modifier.weight(2f))
                        Text("${p.valorRegistrado}", fontSize = 9.sp, color = textoFila, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                        Text("${p.objetivo}", fontSize = 9.sp, color = textoFila, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                        Text(pctStr, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textoPct,
                            modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                        Text(p.motivoExclusion, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }

            // --- Bloque paso a paso ---
            if (periodos.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val sumaPct = activosPct.sumOf { it.porcentaje.toDouble() }
                val nActivos = activosPct.size
                val mediaBrutaPct = if (nActivos > 0) subtotalBruto * 100 else 0f
                val necesitaTope = subtotalBruto > 1f

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Cálculo paso a paso", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        HorizontalDivider()
                        CalcRow("Suma porcentajes activos", "${"%.1f".format(sumaPct * 100)}% (${nActivos} periodos × media)")
                        CalcRow("Nº periodos activos", "$nActivos")
                        CalcRow(
                            "Media bruta",
                            "${"%.1f".format(sumaPct * 100)}% ÷ $nActivos = ${"%.1f".format(mediaBrutaPct)}%"
                        )
                        if (necesitaTope) {
                            CalcRow("Tras tope 100%", "100%", Color(0xFF1B5E20))
                        }
                        CalcRow(
                            "% histórico final",
                            "${(pctFinal * 100).roundToInt()}%",
                            if (necesitaTope) Color(0xFF1B5E20) else textoOscuro
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CalcRow(label: String, valor: String, colorValor: Color = Color.Unspecified) {
    val textoValor = if (colorValor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else colorValor
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textoValor,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
