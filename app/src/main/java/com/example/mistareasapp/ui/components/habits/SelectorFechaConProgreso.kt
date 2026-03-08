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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Componente reutilizable que muestra un selector de fecha con navegación
 * y el porcentaje de cumplimiento general.
 *
 * @param fechaSeleccionada Fecha actualmente seleccionada
 * @param progresoGeneral Porcentaje de cumplimiento (0.0 a 1.0)
 * @param onFechaAnterior Callback cuando se navega al día anterior
 * @param onFechaSiguiente Callback cuando se navega al día siguiente
 * @param modifier Modificador opcional
 */
@Composable
fun SelectorFechaConProgreso(
    fechaSeleccionada: LocalDate,
    progresoGeneral: Float,
    onFechaAnterior: () -> Unit,
    onFechaSiguiente: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selector de fecha con flechas
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
                Text(
                    text = fechaSeleccionada.format(
                        DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (fechaSeleccionada == LocalDate.now()) {
                        "hoy"
                    } else {
                        fechaSeleccionada.format(
                            DateTimeFormatter.ofPattern("EEEE", Locale("es"))
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onFechaSiguiente) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente")
            }
        }

        // Indicador de cumplimiento
        Text(
            text = "${(progresoGeneral * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

