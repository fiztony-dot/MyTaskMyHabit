package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Historial de periodos de pausa de un hábito.
 * Cada registro representa un intervalo [fechaInicio, fechaFin).
 * fechaFin = null significa que la pausa sigue activa.
 */
@Entity(
    tableName = "habitos_pausas",
    indices = [Index(value = ["habitoId"])]
)
data class HabitoPausa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate? = null
)
