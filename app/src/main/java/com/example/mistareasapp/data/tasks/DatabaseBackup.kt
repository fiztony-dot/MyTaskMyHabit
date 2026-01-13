package com.example.mistareasapp

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mistareasapp.data.tasks.TareasDatabase
import java.io.File

object DatabaseBackup {
    // Usamos el nombre EXACTO que tienes en el Builder
    private const val DB_NAME = "tareas_db"

    fun exportDatabase(context: Context, destinationUri: Uri) {
        try {
            val db = TareasDatabase.getDatabase(context)
            // 1. Cerramos para asegurar que el archivo de disco esté actualizado
            db.close()

            val dbFile: File = context.getDatabasePath("tareas_db")

            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                dbFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            // 2. ¡CLAVE! Borramos la instancia cerrada para que la próxima llamada
            // a TareasDatabase.getDatabase(context) cree una conexión NUEVA.
            TareasDatabase.resetearInstancia()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importDatabase(context: Context, sourceUri: Uri) {
        try {
            // 1. Forzar el cierre de la base de datos
            val db = TareasDatabase.getDatabase(context)
            db.close()

            val dbFile: File = context.getDatabasePath(DB_NAME)

            // 2. IMPORTANTE: Borrar archivos temporales ANTES de copiar
            // Si estos archivos existen, Room ignorará el nuevo .db
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            if (dbFile.exists()) dbFile.delete() // Borramos el actual para asegurar limpieza

            // 3. Copiar el archivo del Backup
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("RESTORE", "Archivo copiado con éxito a ${dbFile.absolutePath}")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RESTORE", "Error crítico: ${e.message}")
        }
    }
}


