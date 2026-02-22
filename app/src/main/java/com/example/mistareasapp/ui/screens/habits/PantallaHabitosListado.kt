package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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

/**
 * Pantalla principal de listado de hábitos.
 * Muestra un selector de semana en la parte superior y una lista de tarjetas de hábitos.
 * Cada tarjeta muestra el progreso semanal del hábito.
 *
 * @param viewModel ViewModel que gestiona el estado de los hábitos
 * @param modifier Modificador opcional para personalizar el layout
 */
@Composable
fun PantallaHabitosListado(viewModel: HabitosViewModel, modifier: Modifier = Modifier) {
    // Observar la lista de hábitos con su progreso desde el ViewModel
    val habitosConProgreso by viewModel.habitosConProgreso.collectAsState()

    Scaffold(
        // Botón flotante para crear un nuevo hábito
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
            // Selector de semana con flechas de navegación
            SelectorSemana()

            Spacer(modifier = Modifier.height(16.dp))

            // Lista scrolleable de tarjetas de hábitos
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(habitosConProgreso) { item ->
                    HabitoListadoCard(item)
                }
            }
        }
    }
}

/**
 * Tarjeta individual que muestra un hábito con su progreso semanal.
 * Incluye:
 * - Header con icono, nombre, descripción y badge de porcentaje de cumplimiento
 * - Cuadrícula semanal (7 días) mostrando el estado de cada día
 *
 * @param item HabitoConProgreso que contiene el hábito y su estado de progreso
 */
@Composable
fun HabitoListadoCard(item: HabitoConProgreso) {
    val habito = item.habito
    val hoy = LocalDate.now()
    // Calcular el lunes de la semana actual
    val lunes = hoy.with(DayOfWeek.MONDAY)
    // Parsear el color del hábito desde su representación hexadecimal
    val colorHabito = Color(android.graphics.Color.parseColor(habito.colorHex))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- HEADER DEL HÁBITO ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono circular con la inicial del hábito
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

                // Nombre y descripción del hábito
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        habito.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        habito.descripcion ?: "Sin descripción",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }

                // Badge con el porcentaje de cumplimiento
                Badge(containerColor = Color(0xFF4CAF50), modifier = Modifier.padding(4.dp)) {
                    Text("100%", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- CUADRÍCULA SEMANAL (7 DÍAS) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Generar un círculo para cada día de la semana
                (0..6).forEach { i ->
                    val fecha = lunes.plusDays(i.toLong())
                    val literalDia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Etiqueta del día (Lu, Ma, Mi, etc.)
                        Text(
                            literalDia.take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Círculo que representa el estado del día
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    // Si es día pasado o hoy: color del hábito (completado)
                                    // Si es día futuro: color tenue (pendiente)
                                    if (fecha.isBefore(hoy) || fecha == hoy) colorHabito else colorHabito.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Check mark para días completados (pasados y hoy)
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

/**
 * Componente selector de semana.
 * Muestra el rango de fechas de la semana actual con flechas de navegación.
 *
 * Formato: "17 feb - 23 feb"
 *
 * TODO: Añadir lógica para navegar entre semanas con las flechas
 */
@Composable
fun SelectorSemana() {
    val hoy = LocalDate.now()
    // Calcular el lunes de la semana actual
    val lunes = hoy.with(DayOfWeek.MONDAY)
    // Calcular el domingo (6 días después del lunes)
    val domingo = lunes.plusDays(6)

    // Formatear el rango de fechas para mostrar: "día mes - día mes"
    val rangoSemana = "${lunes.dayOfMonth} ${lunes.month.getDisplayName(TextStyle.SHORT, Locale("es"))} - " +
            "${domingo.dayOfMonth} ${domingo.month.getDisplayName(TextStyle.SHORT, Locale("es"))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flecha izquierda - para retroceder a la semana anterior
            IconButton(onClick = { /* TODO: Lógica para retroceder semana */ }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Semana anterior",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Texto central con el rango de fechas de la semana
            Text(
                text = rangoSemana,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Flecha derecha - para avanzar a la semana siguiente
            IconButton(onClick = { /* TODO: Lógica para avanzar semana */ }) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Semana siguiente",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
