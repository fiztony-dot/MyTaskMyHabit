package com.example.mistareasapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.tasks.DatabaseBackup
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
@Composable
fun GestionDatosScreen(viewModel: TareasViewModel) {
    val context = LocalContext.current
    var mostrarInstruccionesPostRestore by remember { mutableStateOf(false) }

    // 1. Lanzador para EXPORTAR
    val exportarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            DatabaseBackup.exportDatabase(context, it)
            val nuevaDb = AppDatabase.getDatabase(context)
            viewModel.actualizarDaos(nuevaDb.tareaDao(), nuevaDb.categoriaDao())
            Toast.makeText(context, "Copia guardada", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Lanzador para IMPORTAR
    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            DatabaseBackup.importDatabase(context, it)
            mostrarInstruccionesPostRestore = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Text(
            "Copia de Seguridad",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Administra tus datos para no perder nada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta de Exportación
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Exportar", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Guarda una copia de tus tareas y hábitos en un archivo externo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { exportarLauncher.launch("mis_tareas_backup.db") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear archivo de copia")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta de Importación
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Restaurar", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Recupera tus datos desde un archivo de copia anterior.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                FilledTonalButton(
                    onClick = { importarLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seleccionar archivo")
                }
            }
        }

        // ... (Mantén tu AlertDialog de confirmación al final)
    }
}