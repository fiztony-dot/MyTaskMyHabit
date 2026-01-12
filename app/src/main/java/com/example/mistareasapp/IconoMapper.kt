package com.example.mistareasapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun obtenerIconoPorNombre(nombre: String?): ImageVector {
    return when (nombre) {
        "work" -> Icons.Default.Work
        "person" -> Icons.Default.Person
        "warning" -> Icons.Default.Warning
        "favorite" -> Icons.Default.Favorite
        "home" -> Icons.Default.Home
        "shopping_cart" -> Icons.Default.ShoppingCart
        else -> Icons.Default.List // Icono genérico por defecto
    }
}