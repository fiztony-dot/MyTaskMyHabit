package com.example.mistareasapp.data.shopping

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lista_categorias_producto")
data class ListaCategoriaProducto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val colorHex: String,
    val orden: Int = 0
)
