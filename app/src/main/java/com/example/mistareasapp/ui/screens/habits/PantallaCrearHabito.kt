package com.example.mistareasapp.ui.screens.habits

// --- ANDROID ---
import android.app.TimePickerDialog
import android.widget.Toast

// --- COMPOSE ---
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType

// --- COMPOSE UI ELEMENTS ---
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

// --- KOTLIN / TIME ---
import java.time.LocalDate
import java.time.LocalTime

// --- TU APP (MODELOS, ENUMS, VIEWMODEL) ---
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearHabitoScreen(
    viewModel: HabitosViewModel,
    onGuardar: () -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaId by remember { mutableStateOf("0") }

    var fechaInicio by remember { mutableStateOf(LocalDate.now()) }
    var frecuencia by remember { mutableStateOf(FrecuenciaHabito.DIARIA) }
    var vecesPorDia by remember { mutableStateOf("1") }

    var objetivoValor by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("") }

    var objetivoRachaSemanas by remember { mutableStateOf("4") }

    var recordatoriosActivos by remember { mutableStateOf(false) }
    var horaRecordatorio by remember { mutableStateOf(LocalTime.of(9, 0)) }

    var icono by remember { mutableStateOf("favorite") }
    var colorHex by remember { mutableStateOf("#FF0000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Hábito") },
                navigationIcon = {
                    IconButton(onClick = onGuardar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .imePadding()
            ) {
        Text("Nombre del hábito", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            placeholder = { Text("Ej: Hacer ejercicio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // DESCRIPCIÓN
        Text("Descripción", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            placeholder = { Text("Descripción del hábito...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // CATEGORÍA
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = categoriaId,
            onValueChange = { categoriaId = it },
            placeholder = { Text("ID de categoría") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(16.dp))

        // FRECUENCIA
        Text("Frecuencia", style = MaterialTheme.typography.labelLarge)
        DropdownMenuFrecuencia(
            frecuenciaActual = frecuencia,
            onFrecuenciaSeleccionada = { frecuencia = it }
        )

        Spacer(Modifier.height(16.dp))

        // VECES POR DÍA
        Text("Veces por día", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = vecesPorDia,
            onValueChange = { vecesPorDia = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // OBJETIVO
        Text("Objetivo opcional", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = objetivoValor,
            onValueChange = { objetivoValor = it },
            placeholder = { Text("Valor objetivo (ej: 10)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = unidad,
            onValueChange = { unidad = it },
            placeholder = { Text("Unidad (ej: pasos, minutos)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // RACHA
        Text("Racha objetivo (semanas)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = objetivoRachaSemanas,
            onValueChange = { objetivoRachaSemanas = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // RECORDATORIOS
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = recordatoriosActivos,
                onCheckedChange = { recordatoriosActivos = it }
            )
            Text("Activar recordatorios")
        }

        if (recordatoriosActivos) {
            TimePickerField(
                hora = horaRecordatorio,
                onHoraSeleccionada = { horaRecordatorio = it }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ICONO
        Text("Icono", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = icono,
            onValueChange = { icono = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // COLOR
        Text("Color HEX", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = colorHex,
            onValueChange = { colorHex = it },
            modifier = Modifier.fillMaxWidth()
        )

            Spacer(Modifier.height(24.dp))

            // BOTONES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onGuardar) {
                    Text("Cancelar")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (nombre.isBlank()) {
                            Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val nuevoHabito = Habito(
                            nombre = nombre,
                            descripcion = descripcion.ifBlank { null },
                            categoriaId = categoriaId.toLongOrNull() ?: 0,
                            fechaInicio = fechaInicio,
                            frecuencia = frecuencia,
                            vecesPorDia = vecesPorDia.toIntOrNull() ?: 1,
                            objetivoValor = objetivoValor.toIntOrNull(),
                            unidad = unidad.ifBlank { null },
                            objetivoRachaSemanas = objetivoRachaSemanas.toIntOrNull() ?: 4,
                            recordatoriosActivos = recordatoriosActivos,
                            horaRecordatorio = if (recordatoriosActivos) horaRecordatorio else null,
                            icono = icono,
                            colorHex = colorHex
                        )

                        viewModel.insertarHabito(nuevoHabito)
                        Toast.makeText(context, "Hábito creado", Toast.LENGTH_SHORT).show()
                        onGuardar()
                    }
                ) {
                    Text("Crear")
                }
            } // Cierra Row
            } // Cierra Column
        } // Cierra Box
    } // Cierra Scaffold
}


@Composable
fun DropdownMenuFrecuencia(
    frecuenciaActual: FrecuenciaHabito,
    onFrecuenciaSeleccionada: (FrecuenciaHabito) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(frecuenciaActual.name)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FrecuenciaHabito.values().forEach { freq ->
                DropdownMenuItem(
                    text = { Text(freq.name) },
                    onClick = {
                        onFrecuenciaSeleccionada(freq)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
fun TimePickerField(
    hora: LocalTime,
    onHoraSeleccionada: (LocalTime) -> Unit
) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            val picker = TimePickerDialog(
                context,
                { _, h, m -> onHoraSeleccionada(LocalTime.of(h, m)) },
                hora.hour,
                hora.minute,
                true
            )
            picker.show()
        }
    ) {
        Text("Hora recordatorio: ${hora.hour.toString().padStart(2, '0')}:${hora.minute.toString().padStart(2, '0')}")
    }
}
