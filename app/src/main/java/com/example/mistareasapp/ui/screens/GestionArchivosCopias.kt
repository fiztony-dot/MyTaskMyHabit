package com.example.mistareasapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.core.backup.BackupAutoWorker
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.DatabaseBackup
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
fun GestionDatosScreen(viewModel: TareasViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Estado ────────────────────────────────────────────────────────────────
    var backups by remember { mutableStateOf<List<File>>(emptyList()) }
    var backupSeleccionado by remember { mutableStateOf<File?>(null) }
    var accionPendiente by remember { mutableStateOf<String?>(null) }  // "todo","tareas","habitos","lista"
    var mostrarDialogoOpcion by remember { mutableStateOf(false) }
    var mostrarDialogoConfirm by remember { mutableStateOf(false) }
    var enProgreso by remember { mutableStateOf(false) }

    // Cargar lista al entrar
    LaunchedEffect(Unit) {
        backups = cargarBackups(context)
    }

    // ── Lanzadores SAF (compatibilidad con ficheros externos) ─────────────────
    val exportarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                DatabaseBackup.exportDatabase(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.actualizarDaos()
                }
            }
        }
    }

    val importarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                DatabaseBackup.importDatabase(context, it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Reiniciando app…", Toast.LENGTH_SHORT).show()
                }
                kotlinx.coroutines.delay(800)
                DatabaseBackup.reiniciarApp(context)
            }
        }
    }

    // ── Diálogo: elegir tipo de restauración ──────────────────────────────────
    if (mostrarDialogoOpcion && backupSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOpcion = false },
            title = { Text("¿Qué deseas restaurar?") },
            text = {
                Column {
                    Text(
                        backupSeleccionado!!.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OpcionRestore("Restaurar todo", Icons.Default.RestorePage, "todo") {
                        accionPendiente = it; mostrarDialogoOpcion = false; mostrarDialogoConfirm = true
                    }
                    OpcionRestore("Solo Tareas", Icons.Default.Checklist, "tareas") {
                        accionPendiente = it; mostrarDialogoOpcion = false; mostrarDialogoConfirm = true
                    }
                    OpcionRestore("Solo Hábitos", Icons.Default.Loop, "habitos") {
                        accionPendiente = it; mostrarDialogoOpcion = false; mostrarDialogoConfirm = true
                    }
                    OpcionRestore("Solo Lista de la Compra", Icons.Default.ShoppingCart, "lista") {
                        accionPendiente = it; mostrarDialogoOpcion = false; mostrarDialogoConfirm = true
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarDialogoOpcion = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: confirmación antes de restaurar ───────────────────────────────
    if (mostrarDialogoConfirm && backupSeleccionado != null && accionPendiente != null) {
        val descripcion = when (accionPendiente) {
            "tareas"  -> "las Tareas y Categorías de tareas"
            "habitos" -> "los Hábitos y todo su historial"
            "lista"   -> "la Lista de la Compra"
            else      -> "toda la base de datos"
        }
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirmar restauración") },
            text = {
                Text(
                    "Se reemplazará $descripcion con los datos del backup:\n\n" +
                    "${backupSeleccionado!!.name}\n\n" +
                    "Antes de restaurar se creará automáticamente un backup de seguridad del estado actual.\n\n" +
                    "La app se reiniciará al terminar."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoConfirm = false
                        val archivo = backupSeleccionado!!
                        val accion = accionPendiente!!
                        enProgreso = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                // 1. Backup de seguridad del estado actual
                                DatabaseBackup.crearBackupSeguridad(context)

                                // 2. Ejecutar restauración
                                when (accion) {
                                    "todo"    -> DatabaseBackup.restaurarDesdeArchivo(context, archivo)
                                    "tareas"  -> DatabaseBackup.restaurarTablas(context, archivo, DatabaseBackup.TABLAS_TAREAS)
                                    "habitos" -> DatabaseBackup.restaurarTablas(context, archivo, DatabaseBackup.TABLAS_HABITOS)
                                    "lista"   -> DatabaseBackup.restaurarTablas(context, archivo, DatabaseBackup.TABLAS_LISTA)
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Restauración completada. Reiniciando…", Toast.LENGTH_SHORT).show()
                                }
                                kotlinx.coroutines.delay(800)
                                DatabaseBackup.reiniciarApp(context)
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    enProgreso = false
                                    Toast.makeText(context, "Error en restauración: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    // ── UI principal ──────────────────────────────────────────────────────────
    if (enProgreso) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Restaurando…")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Título
        item {
            Text("Copias de Seguridad", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("Gestión automática y manual de tus datos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }

        // Sección: Crear copia ahora
        item {
            SectionHeader("Crear copia ahora")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Guarda una copia inmediata en la carpeta de backups de la app.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val f = DatabaseBackup.crearBackupSeguridad(context)
                                backups = cargarBackups(context)
                                withContext(Dispatchers.Main) {
                                    val msg = if (f != null) "Copia creada: ${f.name}" else "Error al crear copia"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Crear copia ahora")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Sección: Lista de copias disponibles
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Copias disponibles")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { backups = cargarBackups(context) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                }
            }
            if (backups.isEmpty()) {
                Text("No hay copias automáticas todavía. Se crean cada día a las 3:00 AM.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        items(backups) { archivo ->
            BackupItem(archivo) {
                backupSeleccionado = archivo
                mostrarDialogoOpcion = true
            }
        }

        // Sección: Fichero externo (SAF)
        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader("Archivo externo")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar a archivo", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Guarda la base de datos como archivo .db donde quieras.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = { exportarLauncher.launch("mis_tareas_backup.db") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Exportar…") }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Importar desde archivo", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Restaura toda la base de datos desde un archivo .db externo.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp))
                    FilledTonalButton(
                        onClick = { importarLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Seleccionar archivo…") }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun BackupItem(archivo: File, onRestore: () -> Unit) {
    val fechaMod = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(archivo.lastModified()), ZoneId.systemDefault()
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Storage, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(archivo.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(fechaMod.format(FMT_DISPLAY),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%.1f MB".format(archivo.length() / 1_048_576.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onRestore) { Text("Restaurar") }
        }
    }
}

@Composable
private fun SectionHeader(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun OpcionRestore(
    titulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    accion: String,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(accion) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(titulo, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun cargarBackups(context: android.content.Context): List<File> {
    val dir = BackupAutoWorker.getBackupDir(context)
    return dir.listFiles { f -> f.name.startsWith("backup_") && f.name.endsWith(".db") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}
