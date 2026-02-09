// Fichero: data/habits/CategoriaHabito.kt
package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habitos_categorias")
data class CategoriaHabito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val icono: String = "category",
    val color: String = "#757575"
)