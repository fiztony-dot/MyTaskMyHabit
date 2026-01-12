package com.example.mistareasapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Entity(tableName = "categorias_table")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val icono: String = "list", // Valor por defecto
    val fechaCreacion: Long = System.currentTimeMillis(),
    val activa: Boolean = true // Usar Boolean es más estándar que String?
)
