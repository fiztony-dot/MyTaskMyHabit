package com.example.mistareasapp.core.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Descarga un adjunto desde su URL firmada (Supabase Storage, válida 1h) a la
 * caché de la app y lo abre con el visor del sistema (ACTION_VIEW) según su MIME.
 *
 * Se usa FileProvider (authority ${packageName}.fileprovider) para exponer el
 * fichero de forma segura a otras apps con permiso de lectura temporal.
 */
object AdjuntoOpener {

    /** Extensión sugerida a partir del MIME, para que el visor reconozca el fichero. */
    private fun extPorMime(mime: String): String = when (mime) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "application/vnd.ms-excel" -> "xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
        else -> "bin"
    }

    /**
     * Descarga y abre el adjunto. Debe llamarse desde una corrutina (hace IO en Dispatchers.IO).
     * Devuelve un mensaje de error si algo falla, o null si se lanzó el visor correctamente.
     */
    suspend fun abrir(context: Context, adjunto: AdjuntoDto): String? {
        return try {
            val fichero = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "adjuntos").apply { mkdirs() }
                // Nombre seguro: id + nombre original saneado, con extensión coherente
                val base = adjunto.nombreFichero
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .replace(Regex("[^\\w.\\-() ]"), "_")
                    .ifBlank { "adjunto_${adjunto.id}.${extPorMime(adjunto.tipoMime)}" }
                val destino = File(dir, "${adjunto.id}_$base")
                URL(adjunto.urlFirmada).openStream().use { input ->
                    destino.outputStream().use { output -> input.copyTo(output) }
                }
                destino
            }

            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", fichero
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, adjunto.tipoMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                null
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "No hay ninguna app para abrir este tipo de fichero.", Toast.LENGTH_LONG).show()
                "No hay ninguna app para abrir este tipo de fichero."
            }
        } catch (e: Exception) {
            "No se pudo abrir el adjunto: ${e.message}"
        }
    }
}
