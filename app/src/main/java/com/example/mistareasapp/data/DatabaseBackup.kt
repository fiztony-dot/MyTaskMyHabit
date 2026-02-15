package com.example.mistareasapp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mistareasapp.data.TareasDatabase
import java.io.File

object DatabaseBackup {
    // Usamos el nombre EXACTO que tienes en el Builder
    private const val DB_NAME = "tareas_db"
    private const val TAG = "DatabaseBackup"

    fun exportDatabase(context: Context, destinationUri: Uri) {
        synchronized(this) {
            try {
                val db = TareasDatabase.getDatabase(context)
                
                // 1. Checkpoint WAL to ensure all data is written to the main database file
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                    cursor.moveToFirst()
                    Log.d(TAG, "WAL checkpoint completed before export")
                }
                
                // 2. Close the database properly
                db.close()

                val dbFile: File = context.getDatabasePath(DB_NAME)
                
                // 3. Verify database file exists
                if (!dbFile.exists()) {
                    Log.e(TAG, "Database file does not exist: ${dbFile.absolutePath}")
                    throw IllegalStateException("Database file not found")
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    dbFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "Database exported successfully to $destinationUri")

                // 4. Reset instance to create a new connection on next access
                TareasDatabase.resetearInstancia()

            } catch (e: Exception) {
                Log.e(TAG, "Error during database export: ${e.message}", e)
                e.printStackTrace()
                throw e
            }
        }
    }

    fun importDatabase(context: Context, sourceUri: Uri) {
        synchronized(this) {
            var tempFile: File? = null
            try {
                // 1. Get the database and checkpoint WAL before closing
                val db = TareasDatabase.getDatabase(context)
                
                // Checkpoint WAL to ensure no pending writes
                try {
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                        cursor.moveToFirst()
                        Log.d(TAG, "WAL checkpoint completed before import")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "WAL checkpoint failed (database may not exist yet): ${e.message}")
                }
                
                // 2. Close database properly and reset instance
                db.close()
                TareasDatabase.resetearInstancia()
                
                // Give the system time to release file handles
                Thread.sleep(100)

                val dbFile: File = context.getDatabasePath(DB_NAME)
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")

                // 3. Create temporary file for atomic operation
                tempFile = File(dbFile.parent, "${DB_NAME}.tmp")
                
                // 4. Copy to temporary file first
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // 5. Validate the backup file before proceeding
                if (tempFile.length() == 0L) {
                    throw IllegalStateException("Backup file is empty")
                }
                
                Log.d(TAG, "Temporary file created: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")

                // 6. Delete WAL and SHM files safely
                try {
                    if (walFile.exists()) {
                        if (!walFile.delete()) {
                            Log.w(TAG, "Failed to delete WAL file, attempting to continue")
                        }
                    }
                    if (shmFile.exists()) {
                        if (!shmFile.delete()) {
                            Log.w(TAG, "Failed to delete SHM file, attempting to continue")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting WAL/SHM files: ${e.message}", e)
                }

                // 7. Delete existing database file
                if (dbFile.exists()) {
                    if (!dbFile.delete()) {
                        Log.e(TAG, "Failed to delete existing database file")
                        throw IllegalStateException("Cannot replace existing database")
                    }
                }

                // 8. Atomically rename temp file to actual database file
                if (!tempFile.renameTo(dbFile)) {
                    // If rename fails, try copy as fallback
                    tempFile.inputStream().use { input ->
                        dbFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.delete()
                }
                
                // 9. Verify the restored file
                if (!dbFile.exists() || dbFile.length() == 0L) {
                    throw IllegalStateException("Database restore failed - file is missing or empty")
                }

                Log.d(TAG, "Database restored successfully to ${dbFile.absolutePath}, size: ${dbFile.length()} bytes")
                
                // 10. Force a new database instance to be created on next access
                TareasDatabase.resetearInstancia()

            } catch (e: Exception) {
                Log.e(TAG, "Error during database import: ${e.message}", e)
                e.printStackTrace()
                
                // Cleanup temp file on error
                tempFile?.let {
                    if (it.exists()) {
                        it.delete()
                    }
                }
                
                throw e
            }
        }
    }
}


