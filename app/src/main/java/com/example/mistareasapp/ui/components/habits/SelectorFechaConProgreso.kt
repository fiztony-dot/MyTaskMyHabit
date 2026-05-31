package com.example.mistareasapp.ui.components.habits

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun SelectorFechaConProgreso(
    fechaSeleccionada: LocalDate,
    progresoGeneral: Float,
    onFechaAnterior: () -> Unit,
    onFechaSiguiente: () -> Unit,
    modoSemanal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFechaAnterior) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                if (modoSemanal) {
                    val lunes = fechaSeleccionada.with(DayOfWeek.MONDAY)
                    val domingo = lunes.plusDays(6)
                    val fmtCorto = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
                    Text(
                        text = "${lunes.format(fmtCorto)} – ${domingo.format(fmtCorto)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val semanaActual = LocalDate.now().get(WeekFields.of(Locale("es")).weekOfWeekBasedYear())
                    val semana = lunes.get(WeekFields.of(Locale("es")).weekOfWeekBasedYear())
                    Text(
                        text = if (semana == semanaActual) "esta semana" else "semana $semana",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = fechaSeleccionada.format(
                            DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (fechaSeleccionada == LocalDate.now()) "hoy" else
                            fechaSeleccionada.format(DateTimeFormatter.ofPattern("EEEE", Locale("es"))),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onFechaSiguiente) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente")
            }
        }

        Text(
            text = "${(progresoGeneral * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}
