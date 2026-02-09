package com.example.mistareasapp.viewmodel.Habits

import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import java.time.LocalDate

// Esto une el Hábito con su progreso de un día concreto
data class HabitoConProgreso(
    val habito: Habito,
    val progreso: HabitoHistorial? = null
) {
    // Propiedades de ayuda para la UI
    val estaCompletado: Boolean = progreso?.completado ?: false
    val valorActual: Int = progreso?.valorProgreso ?: 0
}