package com.example.mistareasapp.viewmodel.Habits

import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.HabitoVersion
import com.example.mistareasapp.data.habits.TipoMedicion
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.data.habits.diasSemanaSet
import java.time.LocalDate

data class HabitoConProgreso(
    val habito: Habito,
    val progreso: HabitoHistorial? = null,
    val valorPeriodo: Int = 0,
    val completadosPeriodo: Int = 0,
    /** Porcentaje histórico individual (mismo cálculo que en tarjeta semanal). */
    val porcentajeHistorico: Float = 0f,
    /** Días activos desde el inicio del hábito hasta hoy, sin contar pausas. */
    val diasVidaEfectivos: Int = 1,
    /** Número real de tareas definidas para hábitos por tareas; 0 para el resto. */
    val totalTareas: Int = 0,
    /** Para hábitos de Límite Máximo: valor acumulado decimal del periodo. */
    val valorPeriodoDecimal: Double = 0.0
) {
    val estaCompletado: Boolean = progreso?.completado ?: false
    val valorActual: Int = progreso?.valorProgreso ?: 0
}

data class HabitoConHistorialSemanal(
    val habito: Habito,
    val historialSemana: Map<LocalDate, HabitoHistorial?>,
    val progresoHoy: HabitoHistorial?,
    /** Porcentaje histórico desde inicio hasta hoy. */
    val porcentajeHistorico: Float = 0f,
    /** Días activos desde el inicio del hábito hasta hoy, sin contar pausas. */
    val diasVidaEfectivos: Int = 1,
    /** Versión de definición vigente al inicio de la semana mostrada. */
    val versionActiva: HabitoVersion? = null,
    /** Para hábitos MENSUAL: acumulado del mes hasta la fecha seleccionada (valor o días completados). */
    val progresoMesActual: Int = 0,
    /** Para hábitos MENSUAL LIMITE_MAXIMO: acumulado decimal del mes hasta la fecha seleccionada. */
    val progresoMesDecimal: Double = 0.0
) {
    val estaCompletadoHoy: Boolean get() = progresoHoy?.completado ?: false

    fun completadoEnFecha(fecha: LocalDate): Boolean = historialSemana[fecha]?.completado ?: false

    /**
     * Porcentaje de la semana en curso.
     * Solo significativo para hábitos SEMANALES (simples y cuantitativos).
     */
    fun porcentajeSemanal(hoy: LocalDate = LocalDate.now()): Float {
        val aplicables = habito.diasSemanaSet()
        val diasConsiderados = historialSemana.keys.filter { fecha ->
            !fecha.isAfter(hoy) && (aplicables == null || fecha.dayOfWeek in aplicables)
        }
        if (diasConsiderados.isEmpty()) return 0f

        val raw: Float =
            if (habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO &&
                habito.frecuencia == FrecuenciaHabito.SEMANAL
            ) {
                val objetivo = habito.objetivoValor ?: habito.vecesPorDia
                val totalValor = diasConsiderados.sumOf { historialSemana[it]?.valorProgreso ?: 0 }
                if (objetivo > 0) totalValor.toFloat() / objetivo else 0f
            } else if (habito.frecuencia == FrecuenciaHabito.SEMANAL) {
                // Simple semanal: días completados / objetivo semanal
                val completados = diasConsiderados.count { historialSemana[it]?.completado == true }
                val obj = habito.vecesPorDia
                if (obj > 0) completados.toFloat() / obj else 0f
            } else {
                val completados = diasConsiderados.count { historialSemana[it]?.completado == true }
                completados.toFloat() / diasConsiderados.size
            }

        return aplicarTipoMedicion(raw, habito.tipoMedicion)
    }
}

/** Aplica el tipo de medición al porcentaje raw. */
fun aplicarTipoMedicion(raw: Float, tipo: TipoMedicion): Float = when (tipo) {
    TipoMedicion.BINARIO -> if (raw >= 1f) 1f else 0f
    TipoMedicion.PROPORCIONAL_CON_TOPE -> raw.coerceIn(0f, 1f)
    TipoMedicion.PROPORCIONAL_SIN_TOPE -> raw.coerceAtLeast(0f)
}

data class EstadisticasHabito(
    val rachaActual: Int = 0,
    val mejorRacha: Int = 0,
    val totalCompletados: Int = 0,
    val completadosSemana: Int = 0,
    val diasEnSemana: Int = 1,
    val completadosMes: Int = 0,
    val diasEnMes: Int = 1,
    val completadosAno: Int = 0,
    val diasEnAno: Int = 1,
    val porcentajeHistorico: Float = 0f
) {
    val porcentajeCumplimiento: Float
        get() = porcentajeHistorico
}
