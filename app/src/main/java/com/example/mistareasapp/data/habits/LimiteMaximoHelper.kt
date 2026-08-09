package com.example.mistareasapp.data.habits

import org.json.JSONArray
import org.json.JSONObject

data class TramoLimite(val desde: Double, val hasta: Double?, val porcentaje: Int)

/** Serializa lista de tramos a JSON string. */
fun serializarTramos(tramos: List<TramoLimite>): String {
    val arr = JSONArray()
    tramos.forEach { t ->
        arr.put(JSONObject().apply {
            put("desde", t.desde)
            if (t.hasta != null) put("hasta", t.hasta) else put("hasta", JSONObject.NULL)
            put("porcentaje", t.porcentaje)
        })
    }
    return arr.toString()
}

/** Parsea JSON string a lista de tramos. Devuelve lista vacía si hay error. */
fun parsearTramos(json: String): List<TramoLimite> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        TramoLimite(
            desde = o.getDouble("desde"),
            hasta = if (o.isNull("hasta")) null else o.getDouble("hasta"),
            porcentaje = o.getInt("porcentaje")
        )
    }
} catch (_: Exception) { emptyList() }

/**
 * Calcula el porcentaje (0-100+) de cumplimiento de un período de Límite Máximo.
 * El limite exacto siempre = 100. Los tramos definen rangos [desde, hasta) con hasta=null = abierto.
 */
fun calcularPorcentajeLimite(valor: Double, limiteMaximo: Double, tramosJson: String?): Int {
    if (limiteMaximo <= 0.0) return 100
    if (kotlin.math.abs(valor - limiteMaximo) < 0.0001) return 100
    if (tramosJson == null) return if (valor <= limiteMaximo) 100 else 0
    val tramos = parsearTramos(tramosJson).sortedBy { it.desde }
    for (t in tramos) {
        // Lower bound: inclusivo solo cuando desde == 0; otherwise exclusivo.
        // Upper bound: siempre inclusivo (hasta == null = abierto).
        val matchLower = if (t.desde == 0.0) valor >= t.desde else valor > t.desde
        val matchUpper = t.hasta == null || valor <= t.hasta
        if (matchLower && matchUpper) return t.porcentaje
    }
    // Fallback: si ningún tramo cubre el valor, respetar el límite absoluto
    return if (valor <= limiteMaximo) 100 else 0
}

/** Tramos por defecto: ≤ límite = 100%, > límite = 0%. */
fun tramosDefault(limiteMaximo: Double): List<TramoLimite> = listOf(
    TramoLimite(0.0, limiteMaximo, 100),
    TramoLimite(limiteMaximo, null, 0)
)

/** Tipos de bebida para la tabla UBE. */
enum class TipoBebidaUBE(val label: String, val mlPorUbe: Double) {
    CERVEZA_SIDRA("Cerveza / Sidra", 200.0),
    VINO("Vino", 100.0),
    DESTILADOS("Destilados", 50.0)
}

/** Convierte ml de un tipo de bebida a UBE. */
fun mlAUbe(ml: Double, tipo: TipoBebidaUBE): Double = ml / tipo.mlPorUbe
