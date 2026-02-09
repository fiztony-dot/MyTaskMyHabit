package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.mistareasapp.viewmodel.Habits.HabitoConProgreso
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PantallaHabitosFlash(viewModel: HabitosViewModel, modifier: Modifier = Modifier) {
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()

    val totalHabitos = habitosConProgreso.size
    val habitosCompletados = habitosConProgreso.count { it.progreso?.completado == true }
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
    val estaCompletado = progreso?.completado ?: false
    val valorActual = progreso?.valorProgreso ?: 0
    val total = habito.vecesPorDia

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (estaCompletado) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habito.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "${habito.frecuencia.name} • $valorActual / $total veces",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) valorActual.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(android.graphics.Color.parseColor(habito.colorHex)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            FilledIconButton(
                onClick = {
                    if (total > 1) viewModel.incrementarProgreso(habito, progreso)
                    else viewModel.toggleHabitoCompleto(habito, progreso)
                },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (estaCompletado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                if (estaCompletado) {
                    Icon(Icons.Default.Check, contentDescription = "Completado", tint = Color.White)
                } else {
                    Text(if (total > 1) "+1" else "✓", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

