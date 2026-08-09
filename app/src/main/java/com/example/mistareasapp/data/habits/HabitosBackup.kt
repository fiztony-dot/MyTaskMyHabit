package com.example.mistareasapp.data.habits

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
 * Backup y restore exclusivo del módulo de Hábitos.
 * Cubre: habitos, habitos_historial, habitos_categorias,
 *        habitos_tareas_especificas, tareas_habito_historial.
 * NO toca ninguna tabla del módulo de Tareas.
 */
object HabitosBackup {

    // Orden de inserción (padres antes que hijos, respetando FKs)
    private val TABLAS = listOf(
        "habitos_categorias",
        "habitos",
        "habitos_tareas_especificas",
        "habitos_versiones",
        "habitos_pausas",
        "habitos_historial",
        "tareas_habito_historial"
    )

    fun exportar(context: Context, destinationUri: Uri) {
        try {
            val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
            val json = JSONObject()
            json.put("modulo", "habitos")
            json.put("version", 1)
            TABLAS.forEach { tabla -> json.put(tabla, volcarTabla(db, tabla)) }

            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Backup de hábitos guardado ✓", Toast.LENGTH_SHORT).show()
            Log.d("BACKUP_HABITOS", "Exportado en $destinationUri")
        } catch (e: Exception) {
            Log.e("BACKUP_HABITOS", "Error: ${e.message}")
            Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun importar(context: Context, sourceUri: Uri) {
        try {
            val content = context.contentResolver.openInputStream(sourceUri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("No se pudo leer el archivo")

            val json = JSONObject(content)
            if (json.optString("modulo") != "habitos") {
                Toast.makeText(context, "El archivo no corresponde a un backup de Hábitos", Toast.LENGTH_LONG).show()
                return
            }

            val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
            db.beginTransaction()
            try {
                // Borrar en orden inverso (hijos antes que padres)
                db.execSQL("DELETE FROM tareas_habito_historial")
                db.execSQL("DELETE FROM habitos_historial")
                db.execSQL("DELETE FROM habitos_pausas")
                db.execSQL("DELETE FROM habitos_versiones")
                db.execSQL("DELETE FROM habitos_tareas_especificas")
                db.execSQL("DELETE FROM habitos")
                db.execSQL("DELETE FROM habitos_categorias")

                // Insertar en orden correcto
                TABLAS.forEach { tabla ->
                    if (json.has(tabla)) cargarTabla(db, tabla, json.getJSONArray(tabla))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            AppDatabase.resetearInstancia()
            Toast.makeText(context, "Hábitos restaurados. Reinicia la app.", Toast.LENGTH_LONG).show()
            Log.d("RESTORE_HABITOS", "Restaurado desde $sourceUri")
        } catch (e: Exception) {
            Log.e("RESTORE_HABITOS", "Error: ${e.message}")
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
