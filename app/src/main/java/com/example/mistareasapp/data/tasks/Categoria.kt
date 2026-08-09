package com.example.mistareasapp.data.tasks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias_table")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val icono: String = "list", // Valor por defecto
    val fechaCreacion: Long = System.currentTimeMillis(),
    val activa: Boolean = true // Usar Boolean es más estándar que String?
)
