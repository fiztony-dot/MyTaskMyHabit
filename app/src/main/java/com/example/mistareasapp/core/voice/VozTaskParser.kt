package com.example.mistareasapp.core.voice

import com.example.mistareasapp.data.tasks.Prioridad
import java.text.Normalizer
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class VozTaskData(
    val titulo: String,
    val fechaLimite: LocalDate? = null,
    val horaLimite: LocalTime? = null,
    val prioridad: Prioridad = Prioridad.MEDIA
)

private enum class FranjaDia {
    MANANA, TARDE, NOCHE
}

object VozTaskParser {

    fun parse(textoOriginal: String, ahora: LocalDateTime = LocalDateTime.now()): VozTaskData {
        val texto = normalizar(textoOriginal)

        val franja = extraerFranjaDia(texto)
        val fechaHoraRelativa = extraerFechaHoraRelativa(texto, ahora)
        val horaExplicita = extraerHoraExplicita(texto, franja)
        val fechaExplicita = extraerFechaExplicita(texto, ahora, horaExplicita)

        val horaPorFranja = if (fechaHoraRelativa == null && horaExplicita == null) {
            horaPorDefectoSegunFranja(franja)
        } else {
            null
        }

        val fechaFinal = fechaHoraRelativa?.toLocalDate() ?: fechaExplicita
        val horaFinal = fechaHoraRelativa?.toLocalTime()?.withSecond(0)?.withNano(0)
            ?: horaExplicita
            ?: horaPorFranja

        val prioridad = extraerPrioridad(texto)
        val tituloLimpio = limpiarTitulo(textoOriginal)

        return VozTaskData(
            titulo = if (tituloLimpio.isBlank()) {
                textoOriginal.trim().replaceFirstChar { it.uppercase() }
            } else {
                tituloLimpio
            },
            fechaLimite = fechaFinal,
            horaLimite = horaFinal,
            prioridad = prioridad
        )
    }

    private fun extraerFranjaDia(texto: String): FranjaDia? {
        return when {
            texto.contains("esta manana") ||
                    texto.contains("por la manana") ||
                    texto.contains("de la manana") -> FranjaDia.MANANA

            texto.contains("esta tarde") ||
                    texto.contains("por la tarde") ||
                    texto.contains("de la tarde") -> FranjaDia.TARDE

            texto.contains("esta noche") ||
                    texto.contains("por la noche") ||
                    texto.contains("de la noche") -> FranjaDia.NOCHE

            else -> null
        }
    }

    private fun extraerFechaHoraRelativa(texto: String, ahora: LocalDateTime): LocalDateTime? {
        val regex = Regex("""\ben\s+(\d+)\s+(minuto|minutos|hora|horas)\b""")
        val match = regex.find(texto) ?: return null

        val cantidad = match.groupValues[1].toLong()
        val unidad = match.groupValues[2]

        return when {
            unidad.startsWith("minuto") -> ahora.plusMinutes(cantidad)
            unidad.startsWith("hora") -> ahora.plusHours(cantidad)
            else -> null
        }
    }

    private fun extraerFechaExplicita(
        texto: String,
        ahora: LocalDateTime,
        horaExplicita: LocalTime?
    ): LocalDate? {
        val hoy = ahora.toLocalDate()

        return when {
            Regex("""\bpasado\s+manana\b""").containsMatchIn(texto) -> hoy.plusDays(2)

            Regex("""\besta\s+(manana|tarde|noche)\b""").containsMatchIn(texto) ||
                    Regex("""\bpor\s+la\s+(manana|tarde|noche)\b""").containsMatchIn(texto) -> hoy

            Regex("""\bmanana\b""").containsMatchIn(texto) -> hoy.plusDays(1)

            Regex("""\bhoy\b""").containsMatchIn(texto) -> hoy

            else -> {
                extraerFechaDiaMes(texto, hoy)
                    ?: extraerSoloDiaDelMes(texto, hoy)
                    ?: extraerDiaSemana(texto, ahora, horaExplicita)
                // OJO: sin fallback a hoy por tener hora, para evitar errores silenciosos.
            }
        }
    }

    private fun extraerFechaDiaMes(texto: String, hoy: LocalDate): LocalDate? {
        // 15/05 o 15-05
        val regexNumerica = Regex("""\b(\d{1,2})[/-](\d{1,2})\b""")
        regexNumerica.find(texto)?.let { match ->
            val dia = match.groupValues[1].toIntOrNull()
            val mes = match.groupValues[2].toIntOrNull()
            if (dia != null && mes != null) {
                return construirFechaFutura(dia, mes, hoy)
            }
        }

        // 15 de mayo / el 15 de mayo / quince de mayo
        val regexTexto = Regex("""\b(?:el\s+)?(\d{1,2}|[a-záéíóúñ]+(?:\s+y\s+[a-záéíóúñ]+)?)\s+de\s+([a-záéíóúñ]+)\b""")
        regexTexto.find(texto)?.let { match ->
            val diaRaw = normalizar(match.groupValues[1])
            val dia = diaRaw.toIntOrNull() ?: diaTextoANumero(diaRaw)
            val mesTexto = normalizar(match.groupValues[2])
            val mes = mesTextoANumero(mesTexto)

            if (dia != null && mes != null) {
                return construirFechaFutura(dia, mes, hoy)
            }
        }

        return null
    }

    private fun extraerSoloDiaDelMes(texto: String, hoy: LocalDate): LocalDate? {
        // "el 15"
        val regex = Regex("""\bel\s+(\d{1,2})\b""")
        val match = regex.find(texto) ?: return null
        val dia = match.groupValues[1].toIntOrNull() ?: return null

        val candidatos = listOf(
            hoy.withDayOfMonth(1),
            hoy.plusMonths(1).withDayOfMonth(1)
        )

        candidatos.forEach { base ->
            try {
                val fecha = base.withDayOfMonth(dia)
                if (!fecha.isBefore(hoy)) {
                    return fecha
                }
            } catch (_: DateTimeException) {
            }
        }

        return null
    }

    private fun construirFechaFutura(dia: Int, mes: Int, hoy: LocalDate): LocalDate? {
        val candidatos = listOf(hoy.year, hoy.year + 1)

        candidatos.forEach { anyo ->
            try {
                val fecha = LocalDate.of(anyo, mes, dia)
                if (!fecha.isBefore(hoy)) {
                    return fecha
                }
            } catch (_: DateTimeException) {
            }
        }

        return null
    }

    private fun mesTextoANumero(mes: String): Int? {
        val mapa = mapOf(
            "enero" to 1,
            "febrero" to 2,
            "marzo" to 3,
            "abril" to 4,
            "mayo" to 5,
            "junio" to 6,
            "julio" to 7,
            "agosto" to 8,
            "septiembre" to 9,
            "setiembre" to 9,
            "octubre" to 10,
            "noviembre" to 11,
            "diciembre" to 12
        )
        return mapa[mes]
    }

    private fun diaTextoANumero(dia: String): Int? {
        val d = dia.trim()

        val mapa = mapOf(
            "uno" to 1, "una" to 1,
            "dos" to 2,
            "tres" to 3,
            "cuatro" to 4,
            "cinco" to 5,
            "seis" to 6,
            "siete" to 7,
            "ocho" to 8,
            "nueve" to 9,
            "diez" to 10,
            "once" to 11,
            "doce" to 12,
            "trece" to 13,
            "catorce" to 14,
            "quince" to 15,
            "dieciseis" to 16,
            "diecisiete" to 17,
            "dieciocho" to 18,
            "diecinueve" to 19,
            "veinte" to 20,
            "veintiuno" to 21,
            "veintidos" to 22,
            "veintitres" to 23,
            "veinticuatro" to 24,
            "veinticinco" to 25,
            "veintiseis" to 26,
            "veintisiete" to 27,
            "veintiocho" to 28,
            "veintinueve" to 29,
            "treinta" to 30,
            "treinta y uno" to 31
        )

        return mapa[d]
    }

    private fun extraerDiaSemana(
        texto: String,
        ahora: LocalDateTime,
        horaExplicita: LocalTime?
    ): LocalDate? {
        val mapaDias = mapOf(
            "lunes" to DayOfWeek.MONDAY,
            "martes" to DayOfWeek.TUESDAY,
            "miercoles" to DayOfWeek.WEDNESDAY,
            "jueves" to DayOfWeek.THURSDAY,
            "viernes" to DayOfWeek.FRIDAY,
            "sabado" to DayOfWeek.SATURDAY,
            "domingo" to DayOfWeek.SUNDAY
        )

        val dia = mapaDias.entries.firstOrNull { texto.contains(it.key) }?.value ?: return null

        var fecha = ahora.toLocalDate().with(TemporalAdjusters.nextOrSame(dia))

        if (fecha == ahora.toLocalDate() && horaExplicita != null && horaExplicita.isBefore(ahora.toLocalTime())) {
            fecha = fecha.plusWeeks(1)
        }

        return fecha
    }

    private fun extraerHoraExplicita(texto: String, franja: FranjaDia?): LocalTime? {
        val regexNumerico = Regex("""\ba\s+las?\s+(\d{1,2})(?::(\d{2}))?(?:\s*horas?)?\b""")
        regexNumerico.find(texto)?.let { match ->
            val horaBase = match.groupValues[1].toIntOrNull()
            val minutos = match.groupValues[2].toIntOrNull() ?: 0

            if (horaBase != null && horaBase in 0..23 && minutos in 0..59) {
                val horaAjustada = ajustarHoraSegunFranja(horaBase, franja)
                return LocalTime.of(horaAjustada, minutos)
            }
        }

        val regexTexto = Regex("""\ba\s+las?\s+([a-záéíóúñ]+)(?:\s+y\s+(media|cuarto))?\b""")
        regexTexto.find(texto)?.let { match ->
            val horaTexto = normalizar(match.groupValues[1])
            val sufijo = normalizar(match.groupValues.getOrElse(2) { "" })

            val horaBase = palabraANumero(horaTexto)
            val minutos = when (sufijo) {
                "media" -> 30
                "cuarto" -> 15
                else -> 0
            }

            if (horaBase != null && horaBase in 0..23) {
                val horaAjustada = ajustarHoraSegunFranja(horaBase, franja)
                return LocalTime.of(horaAjustada, minutos)
            }
        }

        return null
    }

    private fun ajustarHoraSegunFranja(horaBase: Int, franja: FranjaDia?): Int {
        return when (franja) {
            FranjaDia.MANANA -> if (horaBase == 12) 0 else horaBase

            FranjaDia.TARDE -> when {
                horaBase in 1..11 -> horaBase + 12
                else -> horaBase
            }

            FranjaDia.NOCHE -> when {
                horaBase == 12 -> 0
                horaBase in 1..11 -> horaBase + 12
                else -> horaBase
            }

            null -> horaBase
        }
    }

    private fun horaPorDefectoSegunFranja(franja: FranjaDia?): LocalTime? {
        return when (franja) {
            FranjaDia.MANANA -> LocalTime.of(9, 0)
            FranjaDia.TARDE -> LocalTime.of(17, 0)
            FranjaDia.NOCHE -> LocalTime.of(21, 0)
            null -> null
        }
    }

    private fun palabraANumero(palabra: String): Int? {
        val mapa = mapOf(
            "cero" to 0,
            "una" to 1, "uno" to 1,
            "dos" to 2,
            "tres" to 3,
            "cuatro" to 4,
            "cinco" to 5,
            "seis" to 6,
            "siete" to 7,
            "ocho" to 8,
            "nueve" to 9,
            "diez" to 10,
            "once" to 11,
            "doce" to 12,
            "trece" to 13,
            "catorce" to 14,
            "quince" to 15,
            "dieciseis" to 16,
            "diecisiete" to 17,
            "dieciocho" to 18,
            "diecinueve" to 19,
            "veinte" to 20,
            "veintiuno" to 21, "veintiuna" to 21,
            "veintidos" to 22,
            "veintitres" to 23,
            "medianoche" to 0,
            "mediodia" to 12
        )
        return mapa[palabra]
    }

    private fun extraerPrioridad(texto: String): Prioridad {
        return when {
            texto.contains("urgente") ||
                    texto.contains("importante") ||
                    texto.contains("prioritario") ||
                    texto.contains("cuanto antes") ||
                    texto.contains("ya") -> Prioridad.ALTA

            texto.contains("sin prisa") ||
                    texto.contains("cuando puedas") -> Prioridad.BAJA

            else -> Prioridad.MEDIA
        }
    }

    private fun limpiarTitulo(textoOriginal: String): String {
        var limpio = normalizar(textoOriginal)

        val patrones = listOf(
            """\bpasado\s+manana\b""",
            """\bmanana\b""",
            """\bhoy\b""",
            """\besta\s+(manana|tarde|noche)\b""",
            """\bpor\s+la\s+(manana|tarde|noche)\b""",
            """\bde\s+la\s+(manana|tarde|noche)\b""",
            """\b(?:el\s+)?\d{1,2}[/-]\d{1,2}\b""",
            """\b(?:el\s+)?(\d{1,2}|[a-záéíóúñ]+(?:\s+y\s+[a-záéíóúñ]+)?)\s+de\s+[a-záéíóúñ]+\b""",
            """\bel\s+\d{1,2}\b""",
            """\blunes\b|\bmartes\b|\bmiercoles\b|\bjueves\b|\bviernes\b|\bsabado\b|\bdomingo\b""",
            """\ben\s+\d+\s+(minuto|minutos|hora|horas)\b""",
            """\ba\s+las?\s+\d{1,2}(?::\d{2})?(?:\s*horas?)?\b""",
            """\ba\s+las?\s+[a-záéíóúñ]+(?:\s+y\s+(media|cuarto))?\b""",
            """\burgente\b|\bimportante\b|\bprioritario\b|\bcuanto antes\b|\bya\b"""
        )

        patrones.forEach { patron ->
            limpio = limpio.replace(Regex(patron), " ")
        }

        limpio = limpio
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }

        return limpio
    }

    private fun normalizar(texto: String): String {
        return Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}

