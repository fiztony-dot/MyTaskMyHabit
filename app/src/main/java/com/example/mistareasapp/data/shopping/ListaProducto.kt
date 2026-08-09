package com.example.mistareasapp.data.shopping

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lista_productos",
    foreignKeys = [
        ForeignKey(
            entity = ListaCategoriaProducto::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoriaId"])]
)
data class ListaProducto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoriaId: Long?,
    val aliases: String = "[]"
)
