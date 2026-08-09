package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPausados(
    viewModel: HabitosViewModel,
    navController: NavController
) {
    val pausados by viewModel.habitosPausados.collectAsState()
    var habitoADespausar by remember { mutableStateOf<Habito?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var habitoAArchivar by remember { mutableStateOf<Habito?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hábitos pausados") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (pausados.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("▶️", fontSize = 48.sp)
                    Text("No hay hábitos pausados", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pausados) { habito ->
                    val colorHabito = try {
                        Color(android.graphics.Color.parseColor(habito.colorHex))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorHabito.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(iconoAEmoji(habito.icono), fontSize = 20.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(habito.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                habito.fechaInicioPausa?.let {
                                    Text(
                                        "Pausado desde: ${it.format(fmt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { habitoAArchivar = habito }
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = "Archivar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    habitoADespausar = habito
                                    mostrarDatePicker = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Reanudar",
                                    tint = colorHabito
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (habitoAArchivar != null) {
        AlertDialog(
            onDismissRequest = { habitoAArchivar = null },
            title = { Text("Archivar hábito") },
            text = {
                Text(
                    "¿Archivar '${habitoAArchivar!!.nombre}'?\n\n" +
                    "El hábito desaparecerá de todas las vistas y no contribuirá al % general. " +
                    "Sus datos históricos se conservan íntegramente. " +
                    "Puedes desarchivarlo en cualquier momento desde 'Hábitos archivados'."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.archivarHabito(habitoAArchivar!!)
                    habitoAArchivar = null
                }) { Text("Archivar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { habitoAArchivar = null }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDatePicker && habitoADespausar != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false; habitoADespausar = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fecha = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        viewModel.despausarHabito(habitoADespausar!!, fecha)
                    }
                    mostrarDatePicker = false
                    habitoADespausar = null
                }) { Text("Reanudar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false; habitoADespausar = null }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState, title = { Text("Fecha de reanudación", modifier = Modifier.padding(16.dp)) })
        }
    }
}
