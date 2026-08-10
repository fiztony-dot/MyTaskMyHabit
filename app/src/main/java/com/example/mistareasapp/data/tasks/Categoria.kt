package com.example.mistareasapp.data.tasks

data class Categoria(
    val id: Int = 0,
    val titulo: String,
    val icono: String = "list",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val activa: Boolean = true
)
