// Fichero: data/habits/HabitoHistorial.kt
package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "habitos_historial", // Asegúrate de que el nombre coincida con el DAO
    foreignKeys = [
        ForeignKey(
            entity = Habito::class,
            parentColumns = ["id"],
            childColumns = ["habitoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitoHistorial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val fecha: LocalDate = LocalDate.now(),
    val valorProgreso: Int = 0,
    val completado: Boolean = false,
    // Para hábitos de Límite Máximo: valor registrado con decimales.
    val valorProgresoDecimal: Double = 0.0
)