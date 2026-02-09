package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PantallaHabitosListado(viewModel: HabitosViewModel, modifier: Modifier = Modifier) {
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Navegar a pantalla crear hábito */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "Mis hábitos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(habitosConProgreso) { item ->
                    HabitoListadoCard(item)
                }
            }
        }
    }
}

@Composable
fun HabitoListadoCard(item: HabitoConProgreso) {
    val habito = item.habito
    val hoy = LocalDate.now()
    val lunes = hoy.with(DayOfWeek.MONDAY)
    val colorHabito = Color(android.graphics.Color.parseColor(habito.colorHex))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = colorHabito.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(habito.nombre.take(1).uppercase(), color = colorHabito, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(habito.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(habito.descripcion ?: "Sin descripción", style = MaterialTheme.typography.bodySmall)
                }
                Badge(containerColor = Color(0xFF4CAF50), modifier = Modifier.padding(4.dp)) {
                    Text("100%", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cuadrícula Semanal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..6).forEach { i ->
                    val fecha = lunes.plusDays(i.toLong())
                    val literalDia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(literalDia.take(2), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (fecha.isBefore(hoy) || fecha == hoy) colorHabito else colorHabito.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (fecha.isBefore(hoy) || fecha == hoy) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}