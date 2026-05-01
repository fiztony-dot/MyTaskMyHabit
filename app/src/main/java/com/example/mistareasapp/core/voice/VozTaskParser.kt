package com.example.mistareasapp.core.voice

import com.example.mistareasapp.data.tasks.Prioridad
import java.text.Normalizer
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

object VozTaskParser {

    fun parse(textoOriginal: String, ahora: LocalDateTime = LocalDateTime.now()): VozTaskData {
        val texto = normalizar(textoOriginal)

        val fechaHoraRelativa = extraerFechaHoraRelativa(texto, ahora)
        val horaExplicita = extraerHoraExplicita(texto)
        val fechaExplicita = extraerFechaExplicita(texto, ahora, horaExplicita)

        val fechaFinal = fechaHoraRelativa?.toLocalDate() ?: fechaExplicita
        val horaFinal = fechaHoraRelativa?.toLocalTime()?.withSecond(0)?.withNano(0) ?: horaExplicita
        val prioridad = extraerPrioridad(texto)
        val tituloLimpio = limpiarTitulo(textoOriginal)

        return VozTaskData(
            titulo = if (tituloLimpio.isBlank()) textoOriginal.trim().replaceFirstChar { it.uppercase() } else tituloLimpio,
            fechaLimite = fechaFinal,
            horaLimite = horaFinal,
            prioridad = prioridad
        )
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
        return when {
            texto.contains("pasado manana") -> ahora.toLocalDate().plusDays(2)
            texto.contains("manana") -> ahora.toLocalDate().plusDays(1)
            texto.contains("hoy") -> ahora.toLocalDate()
            else -> extraerDiaSemana(texto, ahora, horaExplicita)
        }
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

    private fun extraerHoraExplicita(texto: String): LocalTime? {
        val regexNumerico = Regex("""\ba\s+las?\s+(\d{1,2})(?::(\d{2}))?(?:\s*horas?)?\b""")
        regexNumerico.find(texto)?.let { match ->
            val hora = match.groupValues[1].toIntOrNull()
            val minutos = match.groupValues[2].toIntOrNull() ?: 0
            if (hora != null && hora in 0..23 && minutos in 0..59) {
                return LocalTime.of(hora, minutos)
            }
        }

        val regexTexto = Regex("""\ba\s+las?\s+([a-záéíóúñ]+)(?:\s+y\s+(media|cuarto))?\b""")
        regexTexto.find(texto)?.let { match ->
            val horaTexto = normalizar(match.groupValues[1])
            val sufijo = normalizar(match.groupValues.getOrElse(2) { "" })

            val hora = palabraANumero(horaTexto)
            val minutos = when (sufijo) {
                "media" -> 30
                "cuarto" -> 15
                else -> 0
            }

            if (hora != null && hora in 0..23) {
                return LocalTime.of(hora, minutos)
            }
        }

        return null
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
