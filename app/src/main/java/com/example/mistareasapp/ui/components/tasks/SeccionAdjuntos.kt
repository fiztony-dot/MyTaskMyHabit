package com.example.mistareasapp.ui.components.tasks

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.mistareasapp.core.network.AdjuntoDto
import com.example.mistareasapp.core.network.AdjuntoOpener
import com.example.mistareasapp.core.network.ApiException
import com.example.mistareasapp.core.network.TareasApiRepository
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_BYTES = 10L * 1024 * 1024 // 10 MB

private val EXT_PERMITIDAS = setOf(
    "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx", "txt"
)

/** Fichero seleccionado pendiente de subir (flujo de creación de tarea). */
data class AdjuntoPendiente(
    val bytes: ByteArray,
    val nombre: String,
    val mime: String,
    val uri: Uri?  // para preview de imagen; null si no disponible
)

private fun mimePorExtension(nombre: String, fallback: String?): String {
    val ext = nombre.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> fallback ?: "application/octet-stream"
    }
}

private fun iconoPorMime(mime: String): ImageVector = when {
    mime.startsWith("image/") -> Icons.Default.Image
    mime == "application/pdf" -> Icons.Default.PictureAsPdf
    mime == "application/msword" ||
        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> Icons.Default.Description
    mime == "application/vnd.ms-excel" ||
        mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> Icons.Default.TableChart
    mime == "text/plain" -> Icons.Default.Article
    else -> Icons.Default.InsertDriveFile
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

private fun consultarMetadatos(context: Context, uri: Uri): Pair<String, Long> {
    var nombre = "fichero"
    var tamano = -1L
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
        if (c.moveToFirst()) {
            if (nameIdx >= 0) nombre = c.getString(nameIdx) ?: nombre
            if (sizeIdx >= 0 && !c.isNull(sizeIdx)) tamano = c.getLong(sizeIdx)
        }
    }
    return nombre to tamano
}

/**
 * Sección de adjuntos del formulario de tarea.
 *
 * - Modo EDICIÓN ([tareaId] != null): carga, sube y elimina contra la API al
 *   instante; al pulsar un adjunto lo abre con el visor del sistema.
 * - Modo CREACIÓN ([tareaId] == null): acumula ficheros en [pendientes] (lista
 *   propiedad del padre). El padre, tras crear la tarea, sube esos pendientes.
 */
@Composable
fun SeccionAdjuntos(
    tareaId: Int?,
    modifier: Modifier = Modifier,
    pendientes: SnapshotStateList<AdjuntoPendiente>? = null,
    onCountChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val esCreacion = tareaId == null

    val adjuntos = remember { mutableStateListOf<AdjuntoDto>() }
    var cargando by remember { mutableStateOf(!esCreacion) }
    var subiendo by remember { mutableStateOf(false) }
    var abriendoId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var adjuntoAEliminar by remember { mutableStateOf<AdjuntoDto?>(null) }
    var uriCamara by remember { mutableStateOf<Uri?>(null) }

    fun refrescarCount() {
        onCountChange(if (esCreacion) (pendientes?.size ?: 0) else adjuntos.size)
    }

    LaunchedEffect(tareaId) {
        if (esCreacion) { cargando = false; refrescarCount(); return@LaunchedEffect }
        cargando = true
        try {
            val lista = TareasApiRepository.obtenerAdjuntos(tareaId!!)
            adjuntos.clear()
            adjuntos.addAll(lista)
            refrescarCount()
        } catch (_: Exception) {
            error = "No se pudieron cargar los adjuntos."
        } finally {
            cargando = false
        }
    }

    // Sube bytes a la tarea existente (modo edición)
    fun subir(bytes: ByteArray, nombre: String, mime: String) {
        scope.launch {
            subiendo = true
            error = null
            try {
                val nuevo = TareasApiRepository.subirAdjunto(tareaId!!, bytes, nombre, mime)
                adjuntos.add(nuevo)
                refrescarCount()
            } catch (e: ApiException) {
                error = when (e.code) {
                    413 -> "El fichero supera el límite de 10MB."
                    400 -> "Tipo de fichero no permitido."
                    else -> "Error al subir: ${e.message}"
                }
            } catch (_: Exception) {
                error = "Error al subir el fichero."
            } finally {
                subiendo = false
            }
        }
    }

    // Lee bytes de un Uri con validación. Devuelve triple(bytes, nombre, mime) o null (setea error).
    fun leerYValidar(uri: Uri): Triple<ByteArray, String, String>? {
        val (nombre, tamano) = consultarMetadatos(context, uri)
        val ext = nombre.substringAfterLast('.', "").lowercase()
        val mimeCr = context.contentResolver.getType(uri)
        val mime = mimePorExtension(nombre, mimeCr)
        val esValido = EXT_PERMITIDAS.contains(ext) ||
            mime.startsWith("image/") || mime == "application/pdf" || mime == "text/plain"
        if (!esValido) { error = "Tipo de fichero no permitido."; return null }
        if (tamano in 1..MAX_BYTES || tamano == -1L) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) { null }
            if (bytes == null) { error = "No se pudo leer el fichero."; return null }
            if (bytes.size > MAX_BYTES) { error = "El fichero supera el límite de 10MB."; return null }
            return Triple(bytes, nombre, mime)
        }
        error = "El fichero supera el límite de 10MB."
        return null
    }

    // Procesa un Uri seleccionado (galería/ficheros)
    fun procesarUri(uri: Uri) {
        error = null
        val datos = leerYValidar(uri) ?: return
        if (esCreacion) {
            pendientes?.add(AdjuntoPendiente(datos.first, datos.second, datos.third, uri))
            refrescarCount()
        } else {
            subir(datos.first, datos.second, datos.third)
        }
    }

    val ficherosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { procesarUri(it) } }

    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { procesarUri(it) } }

    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        val uri = uriCamara
        if (exito && uri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) { null }
            if (bytes == null) {
                error = "No se pudo leer la foto."
            } else if (bytes.size > MAX_BYTES) {
                error = "La foto supera el límite de 10MB."
            } else {
                val nombre = "foto_${System.currentTimeMillis()}.jpg"
                if (esCreacion) {
                    pendientes?.add(AdjuntoPendiente(bytes, nombre, "image/jpeg", uri))
                    refrescarCount()
                } else {
                    subir(bytes, nombre, "image/jpeg")
                }
            }
        }
    }

    fun lanzarCamara() {
        val dir = File(context.cacheDir, "adjuntos").apply { mkdirs() }
        val foto = File(dir, "cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", foto)
        uriCamara = uri
        camaraLauncher.launch(uri)
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) lanzarCamara() else error = "Permiso de cámara denegado." }

    fun pedirCamara() {
        val ya = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ya) lanzarCamara() else permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
    }

    fun abrir(adj: AdjuntoDto) {
        scope.launch {
            abriendoId = adj.id
            error = null
            val err = AdjuntoOpener.abrir(context, adj)
            if (err != null) error = err
            abriendoId = null
        }
    }

    // ── UI ──
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Adjuntos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        when {
            cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Cargando…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            esCreacion -> {
                val lista = pendientes ?: emptyList()
                if (lista.isEmpty()) {
                    Text(
                        "Añade ficheros; se subirán al crear la tarea.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    lista.forEachIndexed { idx, p ->
                        FilaPendiente(p = p, onQuitar = {
                            pendientes?.removeAt(idx)
                            refrescarCount()
                        })
                    }
                }
            }
            adjuntos.isEmpty() -> Text(
                "Sin adjuntos todavía.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            else -> adjuntos.forEach { adj ->
                FilaAdjunto(
                    adj = adj,
                    abriendo = abriendoId == adj.id,
                    onAbrir = { abrir(adj) },
                    onEliminar = { adjuntoAEliminar = adj }
                )
            }
        }

        if (subiendo) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Subiendo…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        OutlinedButton(
            onClick = { mostrarMenu = true },
            enabled = !subiendo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Añadir adjunto")
        }
    }

    if (mostrarMenu) {
        AlertDialog(
            onDismissRequest = { mostrarMenu = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { mostrarMenu = false }) { Text("Cancelar") } },
            title = { Text("Añadir adjunto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OpcionOrigen(Icons.Default.CameraAlt, "Cámara (foto nueva)") {
                        mostrarMenu = false; pedirCamara()
                    }
                    OpcionOrigen(Icons.Default.Image, "Galería (imagen)") {
                        mostrarMenu = false; galeriaLauncher.launch("image/*")
                    }
                    OpcionOrigen(Icons.Default.Folder, "Ficheros (documentos)") {
                        mostrarMenu = false
                        ficherosLauncher.launch(
                            arrayOf(
                                "image/*", "application/pdf", "text/plain",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                        )
                    }
                }
            }
        )
    }

    adjuntoAEliminar?.let { adj ->
        AlertDialog(
            onDismissRequest = { adjuntoAEliminar = null },
            confirmButton = {
                TextButton(onClick = {
                    val objetivo = adj
                    adjuntoAEliminar = null
                    scope.launch {
                        error = null
                        try {
                            TareasApiRepository.eliminarAdjunto(tareaId!!, objetivo.id)
                            adjuntos.remove(objetivo)
                            refrescarCount()
                        } catch (_: Exception) {
                            error = "Error al eliminar el adjunto."
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { adjuntoAEliminar = null }) { Text("Cancelar") } },
            title = { Text("¿Eliminar adjunto?") },
            text = { Text("Se eliminará \"${adj.nombreFichero}\" de forma permanente.") }
        )
    }
}

@Composable
private fun OpcionOrigen(icono: ImageVector, texto: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(texto)
        }
    }
}

@Composable
private fun FilaAdjunto(
    adj: AdjuntoDto,
    abriendo: Boolean,
    onAbrir: () -> Unit,
    onEliminar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(enabled = !abriendo, onClick = onAbrir)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (adj.tipoMime.startsWith("image/")) {
            AsyncImage(
                model = adj.urlFirmada,
                contentDescription = adj.nombreFichero,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFEDEDED), RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconoPorMime(adj.tipoMime),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(adj.nombreFichero, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (abriendo) "Abriendo…" else formatBytes(adj.tamanoBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        if (abriendo) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar adjunto", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FilaPendiente(p: AdjuntoPendiente, onQuitar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (p.mime.startsWith("image/") && p.uri != null) {
            AsyncImage(
                model = p.uri,
                contentDescription = p.nombre,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFEDEDED), RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconoPorMime(p.mime),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(p.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatBytes(p.bytes.size.toLong())} · pendiente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        IconButton(onClick = onQuitar) {
            Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
        }
    }
}
