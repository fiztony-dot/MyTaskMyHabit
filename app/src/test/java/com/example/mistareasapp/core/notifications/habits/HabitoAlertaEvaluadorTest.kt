package com.example.mistareasapp.core.notifications.habits

import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class HabitoAlertaEvaluadorTest {
    private val ahora = LocalDateTime.of(2026, 9, 12, 18, 15)

    @Test
    fun diarioSinRegistroNotificaYConRegistroNo() {
        val habito = habito(FrecuenciaHabito.DIARIA)
        assertEquals(TipoAlertaHabito.DIARIA, HabitoAlertaEvaluador.evaluar(habito, ahora, null, emptyList())?.tipo)
        assertNull(HabitoAlertaEvaluador.evaluar(habito, ahora, historial(ahora.toLocalDate()), emptyList()))
    }

    @Test
    fun semanalEnRiesgoCuandoNoQuedanDiasSuficientes() {
        val habito = habito(FrecuenciaHabito.SEMANAL).copy(vecesPorDia = 5)
        val historial = listOf(historial(LocalDate.of(2026, 9, 8)), historial(LocalDate.of(2026, 9, 9)))
        val resultado = HabitoAlertaEvaluador.evaluar(habito, ahora, null, historial)
        assertNotNull(resultado)
        assertEquals(2, resultado?.cumplidos)
        assertEquals(2, resultado?.diasRestantes)
    }

    @Test
    fun mensualEnRiesgoYLimiteMaximoNoAplica() {
        val mensual = habito(FrecuenciaHabito.MENSUAL).copy(vecesPorDia = 5)
        val historial = listOf(historial(LocalDate.of(2026, 9, 1)), historial(LocalDate.of(2026, 9, 2)))
        assertNotNull(HabitoAlertaEvaluador.evaluar(mensual, LocalDateTime.of(2026, 9, 29, 18, 0), null, historial))
        assertNull(HabitoAlertaEvaluador.evaluar(mensual.copy(tipoObjetivo = TipoObjetivoHabito.LIMITE_MAXIMO), ahora, null, historial))
    }

    private fun habito(frecuencia: FrecuenciaHabito) = Habito(
        nombre = "Lectura",
        frecuencia = frecuencia,
        alertaActivada = true,
        horaAlerta = LocalTime.of(18, 0)
    )

    private fun historial(fecha: LocalDate) = HabitoHistorial(habitoId = 1, fecha = fecha, completado = true)
}