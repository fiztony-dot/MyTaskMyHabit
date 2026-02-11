// Fichero: data/habits/Habito.kt
package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

// Archivo: data/habits/Habito.kt
@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String? = null,
    val categoriaId: Long = 0,
    val fechaInicio: LocalDate = LocalDate.now(),
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val vecesPorDia: Int = 1,
    val objetivoValor: Int? = null, // <--- Añadir este campo
    val unidad: String? = null,    // <--- Añadir este campo
    val objetivoRachaSemanas: Int = 4,
    val recordatoriosActivos: Boolean = false,
    val horaRecordatorio: LocalTime? = null,
    val icono: String = "favorite",
    val colorHex: String = "#FF0000",
    val activo: Boolean = true
)
