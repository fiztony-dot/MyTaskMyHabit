package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "tareas_habito_historial",
    indices = [
        Index(value = ["tareaId", "fecha"]),
        Index(value = ["habitoId", "fecha"])
    ]
)
data class TareaHabitoHistorial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tareaId: Long,
    val habitoId: Long,
    val fecha: LocalDate,
    val completada: Boolean
)
