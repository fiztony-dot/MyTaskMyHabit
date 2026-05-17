// ui/screens/habits/PantallaHabitosFlash.kt

package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.core.graphics.toColorInt
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.obtenerIconoPorNombre
import com.example.mistareasapp.ui.components.habits.SelectorFechaConProgreso
import com.example.mistareasapp.viewmodel.Habits.HabitoConProgreso
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel

@Composable
fun PantallaHabitosFlash(
    viewModel: HabitosViewModel,
    progresoGeneral: Float = 0f,
    modifier: Modifier = Modifier
) {
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()


    val totalHabitos = habitosConProgreso.size
    val habitosCompletados = habitosConProgreso.count { it.estaCompletado }
    val progresoCalculado = if (totalHabitos > 0) (habitosCompletados.toFloat() / totalHabitos) else 0f
    // Usar el progresoGeneral pasado como parámetro
    val progreso = if (progresoGeneral > 0) progresoGeneral else progresoCalculado

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de fecha con progreso (componente reutilizable)
        SelectorFechaConProgreso(
            fechaSeleccionada = fechaSeleccionada,
            progresoGeneral = progreso,
            onFechaAnterior = { viewModel.cambiarFecha(fechaSeleccionada.minusDays(1)) },
            onFechaSiguiente = { viewModel.cambiarFecha(fechaSeleccionada.plusDays(1)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de Hábitos del día
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        Color(habito.colorHex.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Icono dinámico
                Icon(
                    imageVector = obtenerIconoPorNombre(habito.icono),
                    contentDescription = null,
                    tint = colorHabito,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // 2. Textos centrales (Nombre y Subtítulo dinámico)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habito.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
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
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Badge de Acción/Estado - CUADRADO con fondo blanco y texto azul oscuro
                Surface(
                    onClick = {
                        if (totalGoal > 1) viewModel.incrementarProgreso(habito, progreso)
                        else viewModel.toggleHabitoCompleto(habito, progreso)
                    },
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (estaCompletado) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            val badgeContent = when {
                                habito.objetivoValor != null -> "${(porcentaje * 100).toInt()}%"
                                habito.vecesPorDia > 1 -> "$valorActual/$totalGoal"
                                else -> ""
                            }

                            if (badgeContent.isNotEmpty()) {
                                Text(
                                    text = badgeContent,
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0).copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Barra de progreso inferior (se muestra si tiene objetivo numérico o varias veces al día)
            if (habito.objetivoValor != null || habito.vecesPorDia > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = if (habito.objetivoValor != null) Color(0xFF64B5F6) else colorHabito,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }
    }
}