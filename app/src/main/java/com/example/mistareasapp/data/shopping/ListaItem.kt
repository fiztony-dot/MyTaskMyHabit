package com.example.mistareasapp.data.shopping

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lista_items",
    foreignKeys = [
        ForeignKey(
            entity = ListaProducto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ListaLugar::class,
            parentColumns = ["id"],
            childColumns = ["lugarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productoId"]), Index(value = ["lugarId"])]
)
data class ListaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productoId: Long,
    val lugarId: Long,
    val cantidad: String = "1",
    val unidad: String = "",
    val marcadoComprado: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val tipo: TipoItemCompra = TipoItemCompra.URGENTE,
    val tienda: TiendaItem = TiendaItem.NINGUNA
)
