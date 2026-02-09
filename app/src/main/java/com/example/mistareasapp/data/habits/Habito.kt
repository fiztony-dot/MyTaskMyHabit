// Fichero: data/habits/Habito.kt
package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String? = null,
    val categoriaId: Long = 0, // Relación con CategoriaHabito
    val fechaInicio: LocalDate = LocalDate.now(),
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val vecesPorDia: Int = 1, // "vecesPorDia" de tu nota
    val objetivoRachaSemanas: Int = 4, // "objetivoRachaSemanas" de tu nota
    val recordatoriosActivos: Boolean = false,
    val horaRecordatorio: LocalTime? = null,
    val icono: String = "favorite",
    val colorHex: String = "#FF0000",
    val activo: Boolean = true
)
