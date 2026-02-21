package com.example.mistareasapp.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.mistareasapp.data.AppDatabase
import java.io.File

object DatabaseBackup {
    // Usamos el nombre EXACTO que tienes en el Builder de AppDatabase
    private const val DB_NAME = "tareas_db"

    fun exportDatabase(context: Context, destinationUri: Uri) {
        try {
            val db = AppDatabase.getDatabase(context)
            // 1. Cerramos para asegurar que el archivo de disco esté actualizado
            db.close()

            val dbFile: File = context.getDatabasePath(DB_NAME)

            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                dbFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            Log.d("BACKUP", "Backup guardado correctamente en ${destinationUri.path}")
            Toast.makeText(context, "Backup realizado correctamente", Toast.LENGTH_SHORT).show()

            // 2. ¡CLAVE! Resetear la instancia para que la próxima llamada
            // a AppDatabase.getDatabase(context) cree una conexión NUEVA.
            AppDatabase.resetearInstancia()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("BACKUP", "Error en backup: ${e.message}")
            Toast.makeText(context, "Error en backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun importDatabase(context: Context, sourceUri: Uri) {
        try {
            Log.d("RESTORE", "Iniciando restauración desde $sourceUri")

            // 1. Forzar el cierre de la base de datos
            val db = AppDatabase.getDatabase(context)
            db.close()

            val dbFile: File = context.getDatabasePath(DB_NAME)

            Log.d("RESTORE", "BD cerrada. Ruta: ${dbFile.absolutePath}")

            // 2. IMPORTANTE: Borrar archivos temporales ANTES de copiar
            // Si estos archivos existen, Room ignorará el nuevo .db
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            val journalFile = File(dbFile.path + "-journal")

            if (walFile.exists()) {
                walFile.delete()
                Log.d("RESTORE", "Archivo WAL eliminado")
            }
            if (shmFile.exists()) {
                shmFile.delete()
                Log.d("RESTORE", "Archivo SHM eliminado")
            }
            if (journalFile.exists()) {
                journalFile.delete()
                Log.d("RESTORE", "Archivo JOURNAL eliminado")
            }
            if (dbFile.exists()) {
                dbFile.delete()
                Log.d("RESTORE", "Archivo BD anterior eliminado")
            }

            // 3. Copiar el archivo del Backup
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("RESTORE", "Archivo copiado con éxito a ${dbFile.absolutePath}")

            // 4. Resetear la instancia de la BD para que Room la recargue
            AppDatabase.resetearInstancia()

            Log.d("RESTORE", "Instancia resetada. Usuario debe reiniciar la app")
            Toast.makeText(context, "Restauración completada. Por favor, cierra y reabre la app.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RESTORE", "Error crítico en restauración: ${e.message}")
            Toast.makeText(context, "Error en restauración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

