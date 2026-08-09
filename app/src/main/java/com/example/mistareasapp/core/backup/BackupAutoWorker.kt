package com.example.mistareasapp.core.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mistareasapp.data.AppDatabase
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupAutoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val backupDir = getBackupDir(ctx).also { it.mkdirs() }
            val dbFile = ctx.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return Result.success()

            // Cerrar la BD para que todos los datos estén volcados al disco
            val db = AppDatabase.getDatabase(ctx)
            db.close()
            AppDatabase.resetearInstancia()

            // Copiar con timestamp
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
            val dest = File(backupDir, "backup_$ts.db")
            dbFile.copyTo(dest, overwrite = true)
            Log.d("BackupAuto", "Backup automático creado: ${dest.name}")

            // Conservar solo las 7 más recientes
            backupDir.listFiles { f -> f.name.startsWith("backup_") && f.name.endsWith(".db") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(7)
                ?.forEach { it.delete() }

            Result.success()
        } catch (e: Exception) {
            Log.e("BackupAuto", "Error en backup automático: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val DB_NAME = "tareas_db"

        fun getBackupDir(context: Context): File =
            File(context.getExternalFilesDir(null), "backups")
    }
}
