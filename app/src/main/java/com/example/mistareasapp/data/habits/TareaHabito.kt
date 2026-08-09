// Fichero: data/habits/TareaHabito.kt
package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "habitos_tareas_especificas",
    foreignKeys = [
        ForeignKey(
            entity = Habito::class,
            parentColumns = ["id"],
            childColumns = ["habitoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TareaHabito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val nombre: String,
    val descripcion: String? = null,
    val completada: Boolean = false
)