package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHabitosArchivados(
    viewModel: HabitosViewModel,
    navController: NavController
) {
    val archivados by viewModel.habitosArchivados.collectAsState()
    var habitoADesarchivar by remember { mutableStateOf<Habito?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hábitos archivados") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (archivados.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📦", fontSize = 48.sp)
                    Text("No hay hábitos archivados", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Puedes archivar un hábito pausado desde la pantalla de hábitos pausados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Los hábitos archivados no aparecen en ninguna vista ni contribuyen al % general. Sus datos históricos se conservan íntegramente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                items(archivados) { habito ->
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
                                color = colorHabito.copy(alpha = 0.12f),
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
                                habito.fechaInicio.let {
                                    Text(
                                        "Iniciado: ${it.format(fmt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { habitoADesarchivar = habito }) {
                                Icon(
                                    Icons.Default.Unarchive,
                                    contentDescription = "Desarchivar",
                                    tint = colorHabito
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (habitoADesarchivar != null) {
        AlertDialog(
            onDismissRequest = { habitoADesarchivar = null },
            title = { Text("Desarchivar hábito") },
            text = {
                Text(
                    "¿Desarchivar '${habitoADesarchivar!!.nombre}'?\n\n" +
                    "El hábito volverá a estado pausado y reaparecerá en la pantalla de hábitos pausados."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.desarchivarHabito(habitoADesarchivar!!)
                    habitoADesarchivar = null
                }) { Text("Desarchivar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { habitoADesarchivar = null }) { Text("Cancelar") }
            }
        )
    }
}
