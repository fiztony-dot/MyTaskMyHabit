package com.example.mistareasapp.data.tasks

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mistareasapp.data.AppDatabase
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup y restore exclusivo del módulo de Tareas.
 * Cubre: tareas, categorias.
 * NO toca ninguna tabla del módulo de Hábitos.
 */
object TareasBackupJson {

    private val TABLAS = listOf("categorias_table", "tareas_table")

    fun exportar(context: Context, destinationUri: Uri) {
        try {
            val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
            val json = JSONObject()
            json.put("modulo", "tareas")
            json.put("version", 1)
            TABLAS.forEach { tabla -> json.put(tabla, volcarTabla(db, tabla)) }

            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Backup de tareas guardado ✓", Toast.LENGTH_SHORT).show()
            Log.d("BACKUP_TAREAS", "Exportado en $destinationUri")
        } catch (e: Exception) {
            Log.e("BACKUP_TAREAS", "Error: ${e.message}")
            Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun importar(context: Context, sourceUri: Uri) {
        try {
            val content = context.contentResolver.openInputStream(sourceUri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("No se pudo leer el archivo")

            val json = JSONObject(content)
            if (json.optString("modulo") != "tareas") {
                Toast.makeText(context, "El archivo no corresponde a un backup de Tareas", Toast.LENGTH_LONG).show()
                return
            }

            val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
            db.beginTransaction()
            try {
                // Borrar en orden inverso (tareas antes que categorías por FK)
                db.execSQL("DELETE FROM tareas_table")
                db.execSQL("DELETE FROM categorias_table")

                // Insertar
                TABLAS.forEach { tabla ->
                    if (json.has(tabla)) cargarTabla(db, tabla, json.getJSONArray(tabla))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            AppDatabase.resetearInstancia()
            Toast.makeText(context, "Tareas restauradas. Reinicia la app.", Toast.LENGTH_LONG).show()
            Log.d("RESTORE_TAREAS", "Restaurado desde $sourceUri")
        } catch (e: Exception) {
            Log.e("RESTORE_TAREAS", "Error: ${e.message}")
            Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun volcarTabla(db: SupportSQLiteDatabase, tabla: String): JSONArray {
        val array = JSONArray()
        val cursor = db.query("SELECT * FROM \"$tabla\"")
        cursor.use {
            while (it.moveToNext()) {
                val row = JSONObject()
                for (i in 0 until it.columnCount) {
                    val col = it.getColumnName(i)
                    when (it.getType(i)) {
                        Cursor.FIELD_TYPE_NULL    -> row.put(col, JSONObject.NULL)
                        Cursor.FIELD_TYPE_INTEGER -> row.put(col, it.getLong(i))
                        Cursor.FIELD_TYPE_FLOAT   -> row.put(col, it.getDouble(i))
                        else                      -> row.put(col, it.getString(i))
                    }
                }
                array.put(row)
            }
        }
        return array
    }

    private fun cargarTabla(db: SupportSQLiteDatabase, tabla: String, data: JSONArray) {
        if (data.length() == 0) return
        val keys = data.getJSONObject(0).keys().asSequence().toList()
        val cols = keys.joinToString(",") { "\"$it\"" }
        val placeholders = keys.joinToString(",") { "?" }
        val sql = "INSERT OR REPLACE INTO \"$tabla\" ($cols) VALUES ($placeholders)"

        for (i in 0 until data.length()) {
            val row = data.getJSONObject(i)
            val values: Array<Any?> = keys.map { key ->
                when (val v = row.opt(key)) {
                    null, JSONObject.NULL -> null
                    is Long    -> v
                    is Int     -> v.toLong()
                    is Double  -> v
                    is Boolean -> if (v) 1L else 0L
                    else       -> v.toString()
                }
            }.toTypedArray()
            db.execSQL(sql, values)
        }
    }
}
