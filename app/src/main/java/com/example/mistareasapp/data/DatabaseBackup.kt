package com.example.mistareasapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.mistareasapp.core.backup.BackupAutoWorker
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DatabaseBackup {
    private const val TAG = "DatabaseBackup"
    private const val DB_NAME = "tareas_db"

    // Tablas internas de SQLite/Room que NUNCA deben tocarse en el restore.
    private val TABLAS_SISTEMA = setOf("sqlite_sequence", "android_metadata", "room_master_table")

    // ── Tablas por módulo ─────────────────────────────────────────────────────
    // NOTA: los nombres deben coincidir exactamente con los @Entity(tableName=...) de Room.

    // Tareas: tareas_table y categorias_table
    val TABLAS_TAREAS = listOf("categorias_table", "tareas_table")

    // Hábitos: orden de BORRADO (hijos primero para respetar FK aunque FK esté OFF)
    val TABLAS_HABITOS = listOf(
        "habitos_pausas", "habitos_versiones", "tareas_habito_historial",
        "habitos_historial", "habitos_tareas_especificas", "habitos", "habitos_categorias"
    )
    // Hábitos: orden de INSERCIÓN (padres primero)
    private val TABLAS_HABITOS_INSERT = listOf(
        "habitos_categorias", "habitos", "habitos_tareas_especificas",
        "habitos_historial", "tareas_habito_historial", "habitos_versiones", "habitos_pausas"
    )

    // Lista de la Compra: orden de BORRADO (hijos primero)
    val TABLAS_LISTA = listOf(
        "lista_items", "lista_productos", "lista_categorias_producto", "lista_lugares"
    )
    // Lista de la Compra: orden de INSERCIÓN (padres primero)
    private val TABLAS_LISTA_INSERT = listOf(
        "lista_lugares", "lista_categorias_producto", "lista_productos", "lista_items"
    )

    // ── Export/Import manual (SAF — compatibilidad existente) ─────────────────

    fun exportDatabase(context: Context, destinationUri: Uri) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.close()
            AppDatabase.resetearInstancia()
            val dbFile: File = context.getDatabasePath(DB_NAME)
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                dbFile.inputStream().use { it.copyTo(output) }
            }
            Log.d(TAG, "Backup SAF guardado en ${destinationUri.path}")
            Toast.makeText(context, "Backup realizado correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error en export SAF: ${e.message}")
            Toast.makeText(context, "Error en backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun importDatabase(context: Context, sourceUri: Uri) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.close()
            AppDatabase.resetearInstancia()
            val dbFile: File = context.getDatabasePath(DB_NAME)
            borrarArchivosTemporales(dbFile)
            dbFile.delete()
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dbFile.outputStream().use { input.copyTo(it) }
            }
            Toast.makeText(context, "Restauración completada. Por favor, cierra y reabre la app.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en import SAF: ${e.message}")
            Toast.makeText(context, "Error en restauración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Backup de seguridad (a carpeta propia de la app) ──────────────────────

    fun crearBackupSeguridad(context: Context): File? {
        return try {
            val db = AppDatabase.getDatabase(context)
            db.close()
            AppDatabase.resetearInstancia()
            val dbFile = context.getDatabasePath(DB_NAME)
            val backupDir = BackupAutoWorker.getBackupDir(context).also { it.mkdirs() }
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
            val dest = File(backupDir, "backup_$ts.db")
            dbFile.copyTo(dest, overwrite = true)
            Log.d(TAG, "Backup de seguridad creado: ${dest.name} (${dest.length()} bytes)")
            dest
        } catch (e: Exception) {
            Log.e(TAG, "Error creando backup de seguridad: ${e.message}")
            null
        }
    }

    // ── Restaurar todo — usa ATTACH DATABASE con descubrimiento dinámico ──────
    //
    // Se descubren las tablas de usuario desde el backup (sqlite_master) en lugar
    // de usar una lista fija. Esto garantiza que el restore incluye TODAS las
    // tablas presentes en el backup, sin depender de que la lista de constantes
    // esté actualizada. Las tablas del sistema (room_master_table, android_metadata,
    // sqlite_sequence) se excluyen para no romper el esquema Room del DB actual.

    fun restaurarDesdeArchivo(context: Context, backupFile: File) {
        require(backupFile.exists()) { "Fichero de backup no encontrado: ${backupFile.absolutePath}" }
        require(backupFile.length() > 0) { "Fichero de backup vacío: ${backupFile.name}" }

        val tablasBackup = obtenerTablasUsuario(backupFile)
        Log.d(TAG, "RESTORE TODO — backup: ${backupFile.name} (${backupFile.length()} bytes)")
        Log.d(TAG, "RESTORE TODO — tablas en backup: $tablasBackup")

        if (tablasBackup.isEmpty()) {
            throw IllegalStateException("El backup '${backupFile.name}' no contiene tablas de usuario")
        }

        val db = AppDatabase.getDatabase(context)
        db.close()
        AppDatabase.resetearInstancia()

        val dbFile = context.getDatabasePath(DB_NAME)
        val currDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        currDb.use { curr ->
            curr.execSQL("PRAGMA foreign_keys = OFF")
            curr.execSQL("ATTACH DATABASE '${backupFile.absolutePath}' AS src")
            curr.beginTransaction()
            try {
                // Borrar todas las tablas de usuario de la BD actual que existan en el backup
                tablasBackup.forEach { tabla ->
                    if (tablaExisteEnSchema(curr, tabla, "main")) {
                        curr.execSQL("DELETE FROM main.`$tabla`")
                        Log.d(TAG, "RESTORE TODO — borrado: $tabla")
                    } else {
                        Log.w(TAG, "RESTORE TODO — tabla '$tabla' no existe en BD actual, se omite borrado")
                    }
                }
                // Insertar desde el backup
                var insertadas = 0
                tablasBackup.forEach { tabla ->
                    if (tablaExisteEnSchema(curr, tabla, "main")) {
                        curr.execSQL("INSERT INTO main.`$tabla` SELECT * FROM src.`$tabla`")
                        val count = curr.rawQuery("SELECT changes()", null).use { c ->
                            if (c.moveToFirst()) c.getInt(0) else 0
                        }
                        Log.d(TAG, "RESTORE TODO — insertado: $tabla ($count filas)")
                        insertadas++
                    } else {
                        Log.w(TAG, "RESTORE TODO — tabla '$tabla' del backup no existe en BD actual, se omite inserción")
                    }
                }
                curr.setTransactionSuccessful()
                Log.d(TAG, "RESTORE TODO — completado: $insertadas tablas restauradas de ${tablasBackup.size}")
            } catch (e: Exception) {
                Log.e(TAG, "RESTORE TODO — error durante transacción: ${e.message}", e)
                throw e
            } finally {
                curr.endTransaction()
            }
            curr.execSQL("DETACH DATABASE src")
            curr.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Restaurar parcial por módulo usando ATTACH DATABASE ───────────────────

    fun restaurarTablas(context: Context, backupFile: File, tablas: List<String>) {
        require(backupFile.exists()) { "Fichero de backup no encontrado: ${backupFile.absolutePath}" }

        Log.d(TAG, "RESTORE PARCIAL — backup: ${backupFile.name}, módulo: $tablas")

        val db = AppDatabase.getDatabase(context)
        db.close()
        AppDatabase.resetearInstancia()

        val dbFile = context.getDatabasePath(DB_NAME)
        val currDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        currDb.use { curr ->
            curr.execSQL("PRAGMA foreign_keys = OFF")
            curr.execSQL("ATTACH DATABASE '${backupFile.absolutePath}' AS src")
            curr.beginTransaction()
            try {
                val ordenBorrado  = ordenadoParaBorrado(tablas)
                val ordenInsercion = ordenadoParaInsercion(tablas)

                // Borrar en orden (hijos primero)
                ordenBorrado.forEach { tabla ->
                    val enSrc  = tablaExisteEnSchema(curr, tabla, "src")
                    val enMain = tablaExisteEnSchema(curr, tabla, "main")
                    when {
                        !enMain -> Log.w(TAG, "RESTORE PARCIAL — '$tabla' no existe en BD actual, se omite borrado")
                        !enSrc  -> Log.w(TAG, "RESTORE PARCIAL — '$tabla' no existe en backup, se omite borrado")
                        else    -> {
                            curr.execSQL("DELETE FROM main.`$tabla`")
                            Log.d(TAG, "RESTORE PARCIAL — borrado: $tabla")
                        }
                    }
                }

                // Insertar en orden (padres primero)
                var insertadas = 0
                ordenInsercion.forEach { tabla ->
                    val enSrc  = tablaExisteEnSchema(curr, tabla, "src")
                    val enMain = tablaExisteEnSchema(curr, tabla, "main")
                    when {
                        !enSrc  -> Log.w(TAG, "RESTORE PARCIAL — '$tabla' no existe en backup, se omite inserción")
                        !enMain -> Log.w(TAG, "RESTORE PARCIAL — '$tabla' no existe en BD actual, se omite inserción")
                        else    -> {
                            curr.execSQL("INSERT INTO main.`$tabla` SELECT * FROM src.`$tabla`")
                            val count = curr.rawQuery("SELECT changes()", null).use { c ->
                                if (c.moveToFirst()) c.getInt(0) else 0
                            }
                            Log.d(TAG, "RESTORE PARCIAL — insertado: $tabla ($count filas)")
                            insertadas++
                        }
                    }
                }
                curr.setTransactionSuccessful()
                Log.d(TAG, "RESTORE PARCIAL — completado: $insertadas/${ordenInsercion.size} tablas restauradas")
            } catch (e: Exception) {
                Log.e(TAG, "RESTORE PARCIAL — error durante transacción: ${e.message}", e)
                throw e
            } finally {
                curr.endTransaction()
            }
            curr.execSQL("DETACH DATABASE src")
            curr.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Reinicio de la app ────────────────────────────────────────────────────

    fun reiniciarApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private fun borrarArchivosTemporales(dbFile: File) {
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).takeIf { it.exists() }?.delete()
        }
    }

    // Devuelve los nombres de tablas de usuario en un archivo SQLite,
    // excluyendo las tablas internas del sistema y de Room.
    private fun obtenerTablasUsuario(archivo: File): List<String> {
        return try {
            SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
                    null
                ).use { cursor ->
                    val nombres = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val nombre = cursor.getString(0)
                        if (nombre !in TABLAS_SISTEMA) nombres.add(nombre)
                    }
                    nombres
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo tablas del backup '${archivo.name}': ${e.message}")
            emptyList()
        }
    }

    private fun tablaExisteEnSchema(db: SQLiteDatabase, tabla: String, schema: String): Boolean {
        return try {
            db.rawQuery(
                "SELECT 1 FROM $schema.sqlite_master WHERE type='table' AND name=?",
                arrayOf(tabla)
            ).use { it.moveToFirst() }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando tabla '$tabla' en schema '$schema': ${e.message}")
            false
        }
    }

    private fun ordenadoParaBorrado(tablas: List<String>): List<String> {
        val todoOrdenBorrado = TABLAS_HABITOS + TABLAS_LISTA + TABLAS_TAREAS
        return todoOrdenBorrado.filter { it in tablas }
    }

    private fun ordenadoParaInsercion(tablas: List<String>): List<String> {
        val todoOrdenInsercion = TABLAS_HABITOS_INSERT + TABLAS_LISTA_INSERT + TABLAS_TAREAS.reversed()
        return todoOrdenInsercion.filter { it in tablas }
    }
}
