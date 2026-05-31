package com.example.mistareasapp.viewmodel.Habits

import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.diasSemanaSet
import java.time.LocalDate

data class HabitoConProgreso(
    val habito: Habito,
    val progreso: HabitoHistorial? = null,
    val valorPeriodo: Int = 0,       // acumulado en el periodo (semana/mes/día)
    val completadosPeriodo: Int = 0  // veces completado en el periodo (hábitos de frecuencia)
) {
    val estaCompletado: Boolean = progreso?.completado ?: false
    val valorActual: Int = progreso?.valorProgreso ?: 0
}

data class HabitoConHistorialSemanal(
    val habito: Habito,
    val historialSemana: Map<LocalDate, HabitoHistorial?>,
    val progresoHoy: HabitoHistorial?
) {
    val estaCompletadoHoy: Boolean get() = progresoHoy?.completado ?: false

    fun completadoEnFecha(fecha: LocalDate): Boolean = historialSemana[fecha]?.completado ?: false

    fun porcentajeSemanal(hoy: LocalDate = LocalDate.now()): Float {
        val aplicables = habito.diasSemanaSet()
        val diasConsiderados = historialSemana.keys.filter { fecha ->
            !fecha.isAfter(hoy) && (aplicables == null || fecha.dayOfWeek in aplicables)
        }
        if (diasConsiderados.isEmpty()) return 0f
        val completados = diasConsiderados.count { historialSemana[it]?.completado == true }
        return completados.toFloat() / diasConsiderados.size
    }
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
    val diasEnAno: Int = 1
) {
    val porcentajeCumplimiento: Float
        get() = if (diasEnAno > 0) completadosAno.toFloat() / diasEnAno else 0f
}
