package com.example.mistareasapp.data.shopping

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lista_lugares")
data class ListaLugar(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val esDefault: Boolean = false
)
