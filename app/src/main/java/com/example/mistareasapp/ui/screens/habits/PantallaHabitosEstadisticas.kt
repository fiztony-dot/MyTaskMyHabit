package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel

@Composable
fun PantallaHabitosEstadisticas(viewModel: HabitosViewModel, modifier: Modifier = Modifier) {
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()
    var indexSeleccionado by remember { mutableStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selector de Hábito
        if (habitosConProgreso.isNotEmpty()) {
            var expandido by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandido = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(habitosConProgreso[indexSeleccionado].habito.nombre)
                }
                DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                    habitosConProgreso.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(item.habito.nombre) },
                            onClick = { indexSeleccionado = index; expandido = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid de Rachas
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CardEstadistica("Racha Actual", "5", Icons.Default.Whatshot, Color(0xFFFF9800), Modifier.weight(1f))
            CardEstadistica("Mejor Racha", "12", Icons.Default.Star, Color(0xFFFFC107), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card de Cumplimiento
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("% de cumplimiento", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    CircularProgressIndicator(
                        progress = { 0.85f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text("85%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Resumen Histórico", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ItemMetrica("Semana", "6/7")
            ItemMetrica("Mes", "24/30")
            ItemMetrica("Año", "120/365")
            ItemMetrica("Total", "245")
        }
    }
}

@Composable
fun CardEstadistica(titulo: String, valor: String, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icono, contentDescription = null, tint = color)
            Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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