package com.example.mistareasapp.ui.components.tasks

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * COMPONENTE PRINCIPAL: DateTimePickers
 * Este componente centraliza la lógica de los diálogos de selección de fecha y hora.
 * Al extraerlo aquí, evitamos ensuciar las pantallas de "Crear" o "Editar" tarea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickers(
    showDatePicker: Boolean,      // Controla si se ve el calendario
    showTimePicker: Boolean,      // Controla si se ve el reloj
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    onDateSelected: (LocalDate) -> Unit, // Qué hacer cuando se elige fecha
    onTimeSelected: (LocalTime) -> Unit, // Qué hacer cuando se elige hora
    onDismissDate: () -> Unit,           // Cerrar calendario
    onDismissTime: () -> Unit            // Cerrar reloj
) {
    // --- BLOQUE: SELECCIÓN DE FECHA ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = onDismissDate,
            confirmButton = {
                TextButton(onClick = {
                    // Convertimos los milisegundos seleccionados a un objeto LocalDate
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fecha = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(fecha)
                    }
                    onDismissDate()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDate) { Text("Cancelar") }
            }
        ) {
            // El calendario real de Material3
            DatePicker(state = datePickerState)
        }
    }

    // --- BLOQUE: SELECCIÓN DE HORA ---
    if (showTimePicker) {
        // Usamos nuestro envoltorio personalizado definido abajo
        TimePickerDialog(
            onDismissRequest = onDismissTime,
            confirmButton = {
                TextButton(onClick = {
                    // Creamos la hora a partir de lo que el usuario movió en el reloj
                    val hora = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    onTimeSelected(hora)
                    onDismissTime()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissTime) { Text("Cancelar") }
            }
        ) {
            // El selector de reloj de Material3
            TimePicker(state = timePickerState)
        }
    }
}

/**
 * COMPONENTE AYUDANTE: TimePickerDialog
 * Material3 no incluye un "Diálogo" para el reloj por defecto (solo el reloj suelto).
 * Esta función envuelve el reloj en un AlertDialog para que se vea como una ventana flotante.
 */
@Composable
fun TimePickerDialog(
    title: String = "Seleccionar Hora",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = { content() }, // Aquí es donde se "dibuja" el reloj
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}