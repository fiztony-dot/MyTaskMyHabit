// ui/screens/habits/PantallaHabitosFlash.kt

package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.obtenerIconoPorNombre
import com.example.mistareasapp.viewmodel.Habits.HabitoConProgreso
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PantallaHabitosFlash(viewModel: HabitosViewModel, modifier: Modifier = Modifier) {
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()

    val totalHabitos = habitosConProgreso.size
    val habitosCompletados = habitosConProgreso.count { it.estaCompletado }
    val progresoGeneral = if (totalHabitos > 0) (habitosCompletados.toFloat() / totalHabitos) else 0f

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Selector de Fecha superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.cambiarFecha(fechaSeleccionada.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fechaSeleccionada.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (fechaSeleccionada == java.time.LocalDate.now()) "hoy" else fechaSeleccionada.format(DateTimeFormatter.ofPattern("EEEE", Locale("es"))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { viewModel.cambiarFecha(fechaSeleccionada.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card de Resumen de Progreso
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    CircularProgressIndicator(
                        progress = { progresoGeneral },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "${(progresoGeneral * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Progreso Diario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("$habitosCompletados de $totalHabitos hábitos completados", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lista de Hábitos del día
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(habitosConProgreso) { item ->
                HabitoFlashItem(item, viewModel)
            }
        }
    }
}

@Composable
fun HabitoFlashItem(item: HabitoConProgreso, viewModel: HabitosViewModel) {
    val habito = item.habito
    val progreso = item.progreso
    val estaCompletado = item.estaCompletado
    val valorActual = item.valorActual

    // El objetivo puede ser vecesPorDia o un valor numérico (objetivoValor)
    val totalGoal = habito.objetivoValor ?: habito.vecesPorDia
    val porcentaje = if (totalGoal > 0) (valorActual.toFloat() / totalGoal).coerceIn(0f, 1f) else 0f

    val colorHabito = try {
        Color(android.graphics.Color.parseColor(habito.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Icono dinámico
                Icon(
                    imageVector = com.example.mistareasapp.obtenerIconoPorNombre(habito.icono),
                    contentDescription = null,
                    tint = colorHabito,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Textos centrales (Nombre y Subtítulo dinámico)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habito.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Texto de frecuencia (1x/día, 5d/sem, etc)
                    val freqText = when(habito.frecuencia) {
                        FrecuenciaHabito.DIARIA -> "${habito.vecesPorDia}x/día"
                        FrecuenciaHabito.SEMANAL -> if(habito.objetivoValor != null) "${habito.objetivoValor} ${habito.unidad ?: ""}/sem" else "5d/sem"
                        FrecuenciaHabito.MENSUAL -> "Mensual"
                    }

                    // Texto de progreso numérico (• 55/150 (37%))
                    val progressText = if (habito.objetivoValor != null || habito.vecesPorDia > 1) {
                        "  •  $valorActual/$totalGoal (${(porcentaje * 100).toInt()}%)"
                    } else ""

                    Text(
                        text = "$freqText$progressText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Badge de Acción/Estado
                Surface(
                    onClick = {
                        if (totalGoal > 1) viewModel.incrementarProgreso(habito, progreso)
                        else viewModel.toggleHabitoCompleto(habito, progreso)
                    },
                    color = when {
                        estaCompletado -> Color(0xFF4CAF50) // Verde si está ok
                        habito.objetivoValor != null -> Color(0xFF64B5F6) // Azul para objetivos de valor
                        valorActual > 0 -> Color(0xFFFF9800) // Naranja si hay progreso parcial
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(width = 68.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (estaCompletado) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        } else {
                            val badgeContent = when {
                                habito.objetivoValor != null -> "${(porcentaje * 100).toInt()}%"
                                habito.vecesPorDia > 1 -> "$valorActual/$totalGoal"
                                else -> ""
                            }

                            if (badgeContent.isNotEmpty()) {
                                Text(
                                    text = badgeContent,
                                    color = if (valorActual > 0) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // 4. Barra de progreso inferior (se muestra si tiene objetivo numérico o varias veces al día)
            if (habito.objetivoValor != null || habito.vecesPorDia > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (habito.objetivoValor != null) Color(0xFF64B5F6) else colorHabito,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }
    }
}