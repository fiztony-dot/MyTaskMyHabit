package com.example.mistareasapp.core.notifications.habits

import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class HabitoAlertaResultado(
    val tipo: TipoAlertaHabito,
    val cumplidos: Int = 0,
    val objetivo: Int = 0,
    val diasRestantes: Int = 0
)

enum class TipoAlertaHabito { DIARIA, SEMANAL, MENSUAL }

object HabitoAlertaEvaluador {
    fun evaluar(
        habito: Habito,
        ahora: LocalDateTime,
        progresoHoy: HabitoHistorial?,
        historial: List<HabitoHistorial>
    ): HabitoAlertaResultado? {
        if (!habito.alertaActivada || habito.horaAlerta == null ||
            ahora.toLocalTime().isBefore(habito.horaAlerta) ||
            habito.tipoObjetivo.name == "LIMITE_MAXIMO"
        ) return null

        return when (habito.frecuencia) {
            FrecuenciaHabito.DIARIA -> if (progresoHoy?.completado != true) {
                HabitoAlertaResultado(TipoAlertaHabito.DIARIA)
            } else null
            FrecuenciaHabito.SEMANAL -> evaluarPeriodo(
                habito, ahora.toLocalDate(), historial,
                ahora.toLocalDate().with(java.time.DayOfWeek.MONDAY),
                ahora.toLocalDate().with(java.time.DayOfWeek.SUNDAY),
                TipoAlertaHabito.SEMANAL
            )
            FrecuenciaHabito.MENSUAL -> evaluarPeriodo(
                habito, ahora.toLocalDate(), historial,
                ahora.toLocalDate().withDayOfMonth(1),
                ahora.toLocalDate().withDayOfMonth(ahora.toLocalDate().lengthOfMonth()),
                TipoAlertaHabito.MENSUAL
            )
        }
    }

    private fun evaluarPeriodo(
        habito: Habito,
        hoy: LocalDate,
        historial: List<HabitoHistorial>,
        inicio: LocalDate,
        fin: LocalDate,
        tipo: TipoAlertaHabito
    ): HabitoAlertaResultado? {
        val cumplidos = historial.asSequence()
            .filter { it.completado && it.fecha in inicio..hoy }
            .map { it.fecha }
            .distinct()
            .count()
        val diasRestantes = (ChronoUnit.DAYS.between(hoy, fin) + 1).toInt().coerceAtLeast(0)
        val objetivo = habito.objetivoPorcentajeDias?.let { ceil((fin.toEpochDay() - inicio.toEpochDay() + 1) * it / 100.0).toInt() }
            ?: (habito.objetivoValor ?: habito.vecesPorDia).coerceAtLeast(1)
        return if (cumplidos + diasRestantes < objetivo) {
            HabitoAlertaResultado(tipo, cumplidos, objetivo, diasRestantes)
        } else null
    }

    fun estaEnVentanaDeQuinceMinutos(horaAlerta: LocalTime, ahora: LocalDateTime): Boolean {
        val inicio = ahora.minusMinutes(15)
        val hoy = ahora.toLocalDate()
        return sequenceOf(hoy, hoy.minusDays(1))
            .map { LocalDateTime.of(it, horaAlerta) }
            .any { !it.isBefore(inicio) && !it.isAfter(ahora) }
    }
}