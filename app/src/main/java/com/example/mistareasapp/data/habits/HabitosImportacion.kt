package com.example.mistareasapp.data.habits

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mistareasapp.data.AppDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Importación de hábitos desde dos ficheros JSON seleccionados mediante el selector de archivos.
 * La operación es completamente transaccional: si falla en cualquier punto,
 * se hace rollback y los datos previos quedan intactos.
 * NO toca ninguna tabla del módulo de Tareas.
 */
object HabitosImportacion {

    sealed class Resultado {
        data class Exito(val habitos: Int, val registrosHistorial: Int) : Resultado()
        data class Error(val mensaje: String) : Resultado()
    }

    // ── Punto de entrada ─────────────────────────────────────────────────────

    fun importar(context: Context, uriDefinicion: Uri, uriHistorial: Uri): Resultado {
        return try {
            val textoDef = context.contentResolver.openInputStream(uriDefinicion)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return Resultado.Error("No se pudo leer el fichero de definición")
            val textoHist = context.contentResolver.openInputStream(uriHistorial)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return Resultado.Error("No se pudo leer el fichero de historial")

            // Parsear arrays (acepta wrapper object o array directo)
            val jsonDef: JSONArray = parsearComoArray(textoDef, listOf("habitos", "data", "items"))
                ?: return Resultado.Error("Formato inesperado en habitos_definicion.json")
            val jsonHist: JSONArray = parsearComoArray(textoHist,
                listOf("habitos_historial", "historial", "registros", "data", "items"))
                ?: return Resultado.Error("Formato inesperado en habitos_historial.json")

            val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
            var habitosImportados   = 0
            var historialImportados = 0

            db.beginTransaction()
            try {
                // ── Punto 4: Limpieza (solo tablas de hábitos, orden FK-safe) ──
                db.execSQL("DELETE FROM tareas_habito_historial")
                db.execSQL("DELETE FROM habitos_historial")
                db.execSQL("DELETE FROM habitos_pausas")
                db.execSQL("DELETE FROM habitos_versiones")
                db.execSQL("DELETE FROM habitos_tareas_especificas")
                db.execSQL("DELETE FROM habitos")
                db.execSQL("DELETE FROM habitos_categorias")

                val mapaNombreAId = mutableMapOf<String, Long>()

                // ── Punto 2: Importar definición ─────────────────────────────
                for (i in 0 until jsonDef.length()) {
                    val obj = jsonDef.getJSONObject(i)
                    val nombre = obj.optString("nombre", "Hábito ${i + 1}")

                    // Categoría
                    val catObj    = obj.optJSONObject("categoria")
                    val catNombre = catObj?.optString("nombre") ?: obj.optString("categoria", "General")
                    val catColor  = catObj?.optString("color") ?: "#ADADAD"
                    val catId     = buscarOCrearCategoria(db, catNombre.trim(), catColor)

                    // Mapeo frecuencia / tipoObjetivo
                    val frecuenciaRaw = obj.optString("frecuencia", "DIARIO")
                    val periodoTiempo = obj.optString("periodoTiempo", "")
                    val (frecuencia, tipoObjetivo) = mapearFrecuencia(frecuenciaRaw, periodoTiempo)

                    // cantidadObjetivo puede ser null, Int o Double
                    val objetivoDouble = obj.optDouble("cantidadObjetivo", Double.NaN)
                        .takeIf { !it.isNaN() } ?: 1.0
                    val objetivo = kotlin.math.ceil(objetivoDouble).toInt().coerceAtLeast(1)

                    val unidad = obj.optString("unidadMedida", "")
                        .ifBlank { obj.optString("unidad", "") }
                    val colorHex    = obj.optString("color", "#ADADAD")
                    val descripcion = obj.optString("descripcion", "")
                    val fechaIniStr = obj.optString("fechaInicio", LocalDate.now().toString())
                    val fechaInicio = parseFecha(fechaIniStr) ?: LocalDate.now().toEpochDay()

                    // tipoMedicion
                    val aplicaPctReal = obj.optBoolean("aplicaPorcentajeRealCumplimiento", false)
                    val tipoMedicion  = if (aplicaPctReal) "PROPORCIONAL_SIN_TOPE" else "BINARIO"

                    // objetivoPorcentajeDias
                    val aplicaPctFrec  = obj.optBoolean("aplicaPorcentajeFrecuencia", false)
                    val objetivoPctDias: Int? = if (aplicaPctFrec) {
                        obj.optInt("porcentajeObjetivo", 0).takeIf { it > 0 }
                    } else null

                    // criterioCumplimiento
                    val critObj     = obj.optJSONObject("criterioCumplimiento")
                    val minimoTareas: Int? = if (critObj != null &&
                        critObj.optString("tipo").equals("minimum", ignoreCase = true)) {
                        critObj.optInt("valor", 1).takeIf { it > 0 }
                    } else null

                    val activo = obj.optBoolean("activoActualmente", true)

                    // Tareas (pueden ser strings o objects)
                    val tareasArr   = obj.optJSONArray("tareas")
                    val esCompuesto = tareasArr != null && tareasArr.length() > 0

                    // Pausas — estado actual del hábito (último periodo)
                    val pausasArr = obj.optJSONArray("periodosPausados")
                    var pausado         = false
                    var ultimaIni: Long? = null
                    var ultimaFin: Long? = null
                    if (pausasArr != null && pausasArr.length() > 0) {
                        val ultima  = pausasArr.getJSONObject(pausasArr.length() - 1)
                        val pIniStr = ultima.optString("fechaInicio", "")
                        val pFinStr = if (ultima.isNull("fechaFin")) "" else ultima.optString("fechaFin", "")
                        if (pIniStr.isNotBlank()) {
                            ultimaIni = parseFecha(pIniStr)
                            if (pFinStr.isBlank()) {
                                pausado   = true
                                ultimaFin = null
                            } else {
                                val finDate = parseFechaAsDate(pFinStr)
                                pausado   = finDate != null && finDate.isAfter(LocalDate.now())
                                ultimaFin = finDate?.toEpochDay()
                            }
                        }
                    }

                    // ── Insertar hábito ───────────────────────────────────────
                    val habitoId = insertHabito(
                        db, nombre, descripcion, catId, fechaInicio,
                        frecuencia, tipoObjetivo, objetivo, unidad,
                        esCompuesto, minimoTareas, activo, pausado,
                        ultimaIni, ultimaFin, objetivoPctDias, tipoMedicion, colorHex
                    )
                    mapaNombreAId[nombre] = habitoId
                    habitosImportados++

                    // ── Tareas específicas ────────────────────────────────────
                    if (esCompuesto && tareasArr != null) {
                        for (t in 0 until tareasArr.length()) {
                            val nombreTarea = when (val item = tareasArr.opt(t)) {
                                is String     -> item
                                is JSONObject -> item.optString("nombre", "Tarea ${t + 1}")
                                else          -> "Tarea ${t + 1}"
                            }
                            insertTarea(db, habitoId, nombreTarea, "")
                        }
                    }

                    // ── Todos los periodos pausados ───────────────────────────
                    if (pausasArr != null) {
                        for (p in 0 until pausasArr.length()) {
                            val pausa   = pausasArr.getJSONObject(p)
                            val pIniStr = pausa.optString("fechaInicio", "")
                            val pFinStr = if (pausa.isNull("fechaFin")) "" else pausa.optString("fechaFin", "")
                            val pIni    = parseFecha(pIniStr) ?: continue
                            val pFin    = if (pFinStr.isBlank()) null else parseFecha(pFinStr)
                            db.execSQL(
                                "INSERT INTO habitos_pausas (habitoId, fechaInicio, fechaFin) VALUES (?,?,?)",
                                arrayOf(habitoId, pIni, pFin)
                            )
                        }
                    }

                    // ── Versiones de definición ───────────────────────────────
                    val histFrecs = obj.optJSONArray("historialFrecuencias")
                    if (histFrecs != null && histFrecs.length() > 0) {
                        val fechasInsertadas = mutableSetOf<Long>()
                        for (v in 0 until histFrecs.length()) {
                            val ver      = histFrecs.getJSONObject(v)
                            val vFIStr   = ver.optString("fechaInicio", "")
                            val vFechaIni = parseFecha(vFIStr) ?: fechaInicio
                            if (vFechaIni in fechasInsertadas) continue
                            fechasInsertadas.add(vFechaIni)

                            val vFrecRaw = ver.optString("frecuencia",   frecuenciaRaw)
                            val vPeriodo = ver.optString("periodoTiempo", periodoTiempo)
                            val (vFrec, vTipoObj) = mapearFrecuencia(vFrecRaw, vPeriodo)

                            val vObjDouble = ver.optDouble("cantidadObjetivo", Double.NaN)
                                .takeIf { !it.isNaN() } ?: objetivoDouble
                            val vObjetivo  = kotlin.math.ceil(vObjDouble).toInt().coerceAtLeast(1)

                            // tipoMedicion en versiones usa "aplicaPorcentajeReal" (sin "Cumplimiento")
                            val vApReal   = ver.optBoolean("aplicaPorcentajeReal",
                                ver.optBoolean("aplicaPorcentajeRealCumplimiento", aplicaPctReal))
                            val vTipoMed  = if (vApReal) "PROPORCIONAL_SIN_TOPE" else "BINARIO"

                            // objetivoPorcentajeDias en versiones
                            val vUsaPct     = ver.optBoolean("usaPorcentaje", false)
                            val vPctObjeto  = ver.optInt("porcentajeObjetivo", 0)
                            val vObjPct: Int? = if (vUsaPct && vPctObjeto > 0) vPctObjeto
                            else if (ver.optBoolean("aplicaPorcentajeFrecuencia", false)) {
                                ver.optInt("porcentajeObjetivo", 0).takeIf { it > 0 }
                            } else null

                            insertVersion(db, habitoId, vFechaIni, vFrec, vTipoObj,
                                vObjetivo, unidad, esCompuesto, minimoTareas, vObjPct, vTipoMed)
                        }
                    } else {
                        // Sin historial de frecuencias → versión inicial desde fechaInicio
                        insertVersion(db, habitoId, fechaInicio, frecuencia, tipoObjetivo,
                            objetivo, unidad, esCompuesto, minimoTareas, objetivoPctDias, tipoMedicion)
                    }
                }

                // ── Punto 3: Importar historial ───────────────────────────────
                val hoy = LocalDate.now().toEpochDay()

                data class ClaveH(val habitoId: Long, val fecha: Long)
                val agregados = mutableMapOf<ClaveH, Pair<Int, Boolean>>()

                for (i in 0 until jsonHist.length()) {
                    val reg      = jsonHist.getJSONObject(i)
                    val hNombre  = reg.optString("habitoNombre", "")
                    val fechaStr = reg.optString("fecha", "")
                    val habitoId = mapaNombreAId[hNombre] ?: continue
                    val fecha    = parseFecha(fechaStr) ?: continue
                    if (fecha > hoy) continue

                    val valor      = reg.optInt("valorProgreso", 0)
                    val completado = reg.optBoolean("completado", false)
                    val clave      = ClaveH(habitoId, fecha)
                    val actual     = agregados[clave]
                    agregados[clave] = if (actual == null) Pair(valor, completado)
                    else Pair(actual.first + valor, actual.second || completado)
                }

                for ((clave, datos) in agregados) {
                    val cur = db.query(
                        "SELECT COUNT(*) FROM habitos_historial WHERE habitoId=${clave.habitoId} AND fecha=${clave.fecha}")
                    cur.moveToFirst(); val existe = cur.getInt(0) > 0; cur.close()
                    if (existe) continue

                    db.execSQL(
                        "INSERT OR IGNORE INTO habitos_historial (habitoId, fecha, valorProgreso, completado) VALUES (?,?,?,?)",
                        arrayOf(clave.habitoId, clave.fecha, datos.first, if (datos.second) 1L else 0L)
                    )
                    historialImportados++
                }

                db.setTransactionSuccessful()
                Log.d("IMPORT_HABITOS", "OK: $habitosImportados hábitos, $historialImportados registros historial")
            } finally {
                db.endTransaction()
            }

            AppDatabase.resetearInstancia()
            Resultado.Exito(habitosImportados, historialImportados)

        } catch (e: Exception) {
            Log.e("IMPORT_HABITOS", "Error durante importación", e)
            Resultado.Error(e.message ?: "Error desconocido durante la importación")
        }
    }

    // ── Helpers de inserción ─────────────────────────────────────────────────

    private fun insertHabito(
        db: SupportSQLiteDatabase,
        nombre: String, descripcion: String, catId: Long, fechaInicio: Long,
        frecuencia: String, tipoObjetivo: String, objetivo: Int, unidad: String,
        esCompuesto: Boolean, minimoTareas: Int?, activo: Boolean,
        pausado: Boolean, fechaInicioPausa: Long?, fechaFinPausa: Long?,
        objetivoPctDias: Int?, tipoMedicion: String, colorHex: String
    ): Long {
        // Para FRECUENCIA: vecesPorDia = objetivo, objetivoValor = null
        // Para CUANTITATIVO: vecesPorDia = 1, objetivoValor = objetivo
        val vecesPorDia  = if (tipoObjetivo == "FRECUENCIA") objetivo else 1
        val objetivoValor = if (tipoObjetivo == "CUANTITATIVO") objetivo else null

        val cols = listOf(
            "nombre","descripcion","categoriaId","fechaInicio",
            "frecuencia","tipoObjetivo","vecesPorDia",
            "objetivoValor","unidad",
            "esCompuestoPorTareas","criterioCumplimientoTareas","minimoTareasCumplimiento",
            "objetivoRachaSemanas","recordatoriosActivos","horaRecordatorio",
            "icono","colorHex","activo","fechaModificacion",
            "pausado","fechaInicioPausa","fechaFinPausa",
            "objetivoPorcentajeDias","diasSemana","puedeSuperar100","tipoMedicion"
        )
        val vals: Array<Any?> = arrayOf(
            nombre, descripcion.ifBlank { null }, catId, fechaInicio,
            frecuencia, tipoObjetivo, vecesPorDia.toLong(),
            objetivoValor?.toLong(), unidad.ifBlank { null },
            if (esCompuesto) 1L else 0L, "TODAS", minimoTareas?.toLong(),
            4L, 0L, null,
            "star", colorHex, if (activo) 1L else 0L, null,
            if (pausado) 1L else 0L, fechaInicioPausa, fechaFinPausa,
            objetivoPctDias?.toLong(), null, 0L, tipoMedicion
        )
        db.execSQL(
            "INSERT INTO habitos (${cols.joinToString(",") { "\"$it\"" }}) " +
            "VALUES (${cols.joinToString(",") { "?" }})",
            vals
        )
        val cur = db.query("SELECT last_insert_rowid()")
        cur.moveToFirst(); val id = cur.getLong(0); cur.close()
        return id
    }

    private fun insertTarea(db: SupportSQLiteDatabase, habitoId: Long, nombre: String, descripcion: String) {
        db.execSQL(
            "INSERT INTO habitos_tareas_especificas (habitoId, nombre, descripcion, completada) VALUES (?,?,?,0)",
            arrayOf(habitoId, nombre, descripcion.ifBlank { null })
        )
    }

    private fun insertVersion(
        db: SupportSQLiteDatabase,
        habitoId: Long, fechaInicio: Long,
        frecuencia: String, tipoObjetivo: String,
        objetivo: Int, unidad: String,
        esCompuesto: Boolean, minimoTareas: Int?,
        objetivoPctDias: Int?, tipoMedicion: String
    ) {
        val vecesPorDia   = if (tipoObjetivo == "FRECUENCIA") objetivo else 1
        val objetivoValor = if (tipoObjetivo == "CUANTITATIVO") objetivo else null

        db.execSQL("""
            INSERT INTO habitos_versiones
                (habitoId, fechaInicio, frecuencia, tipoObjetivo, vecesPorDia,
                 objetivoValor, unidad, esCompuestoPorTareas, minimoTareasCumplimiento,
                 objetivoPorcentajeDias, puedeSuperar100, diasSemana, tipoMedicion)
            VALUES (?,?,?,?,?,?,?,?,?,?,0,?,?)
        """.trimIndent(), arrayOf(
            habitoId, fechaInicio, frecuencia, tipoObjetivo, vecesPorDia.toLong(),
            objetivoValor?.toLong(),
            unidad.ifBlank { null },
            if (esCompuesto) 1L else 0L,
            minimoTareas?.toLong(),
            objetivoPctDias?.toLong(),
            null,
            tipoMedicion
        ))
    }

    private fun buscarOCrearCategoria(db: SupportSQLiteDatabase, nombre: String, color: String): Long {
        val nombreEsc = nombre.replace("'", "''")
        val cur = db.query("SELECT id FROM habitos_categorias WHERE nombre='$nombreEsc'")
        if (cur.moveToFirst()) { val id = cur.getLong(0); cur.close(); return id }
        cur.close()
        db.execSQL(
            "INSERT INTO habitos_categorias (nombre, icono, color) VALUES (?,?,?)",
            arrayOf(nombre, "label", color)
        )
        val cur2 = db.query("SELECT last_insert_rowid()")
        cur2.moveToFirst(); val id = cur2.getLong(0); cur2.close()
        return id
    }

    // ── Mapeo frecuencia ─────────────────────────────────────────────────────

    private fun mapearFrecuencia(frecuenciaRaw: String, periodoTiempo: String): Pair<String, String> =
        when (frecuenciaRaw.uppercase()) {
            "CUANTITATIVO" -> {
                val frec = when (periodoTiempo.uppercase()) {
                    "SEMANAL" -> "SEMANAL"
                    "MENSUAL" -> "MENSUAL"
                    else      -> "DIARIA"
                }
                Pair(frec, "CUANTITATIVO")
            }
            "SEMANAL" -> Pair("SEMANAL", "FRECUENCIA")
            "MENSUAL" -> Pair("MENSUAL", "FRECUENCIA")
            else      -> Pair("DIARIA",  "FRECUENCIA")  // DIARIO / cualquier otro
        }

    // ── Helpers de parsing ───────────────────────────────────────────────────

    private fun parsearComoArray(texto: String, clavesFallback: List<String>): JSONArray? {
        val trimmed = texto.trim()
        return if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            for (clave in clavesFallback) {
                if (obj.has(clave)) return obj.getJSONArray(clave)
            }
            // Fallback: primer valor que sea array
            val keys = obj.keys()
            while (keys.hasNext()) {
                val v = obj.opt(keys.next())
                if (v is JSONArray) return v
            }
            null
        }
    }

    // ── Helpers de fecha ─────────────────────────────────────────────────────

    private fun parseFecha(s: String): Long? = try {
        LocalDate.parse(s.take(10)).toEpochDay()
    } catch (e: Exception) { null }

    private fun parseFechaAsDate(s: String): LocalDate? = try {
        LocalDate.parse(s.take(10))
    } catch (e: Exception) { null }
}
