package com.example.mistareasapp.ui.components.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// El modelo visual
data class EstiloCategoria(
    val icono: ImageVector,
    val color: Color
)

fun obtenerEstiloCategoria(nombre: String?): EstiloCategoria {
    // .trim() quita espacios al principio o final
    // .lowercase() asegura que no importe si es "Trabajo" o "trabajo"
    val busqueda = nombre?.trim()?.lowercase() ?: ""

    return when (busqueda) {
        "trabajo"  -> EstiloCategoria(Icons.Default.Work, Color(0xFF2196F3))
        "personal" -> EstiloCategoria(Icons.Default.Person, Color(0xFF4CAF50))
        "urgente"  -> EstiloCategoria(Icons.Default.Warning, Color(0xFFF44336))
        "salud"    -> EstiloCategoria(Icons.Default.Favorite, Color(0xFFE91E63))
        "hogar"    -> EstiloCategoria(Icons.Default.Home, Color(0xFFFF9800))
        else       -> EstiloCategoria(Icons.Default.Label, Color.Companion.Cyan) // <-- CAMBIA ESTO A CYAN PARA PROBAR
    }
}

/**
 * BLOQUE 1: TRADUCTOR DE ICONOS (Los iconos que sabe pintar la aplicacion)
 * Su función no es crear iconos nuevos, sino servir de puente entre un nombre (String) que
 * guardas en la base de datos y un recurso gráfico (ImageVector) que Android puede dibujar en
 * pantalla.
 */
fun obtenerIcono(nombre: String): ImageVector {
    return when (nombre) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "work" -> Icons.Default.Work
        "home" -> Icons.Default.Home
        "star" -> Icons.Default.Star
        "event" -> Icons.Default.Event
        "settings" -> Icons.Default.Settings
        "person" -> Icons.Default.Person
        "lightbulb" -> Icons.Default.Lightbulb
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "fitness_center" -> Icons.Default.FitnessCenter
        "payments" -> Icons.Default.Payments
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "pet_page" -> Icons.Default.Pets
        "favorite" -> Icons.Default.Favorite
        "build" -> Icons.Default.Build
        "call" -> Icons.Default.Call
        "code" -> Icons.Default.Code
        else -> Icons.Default.List
    }
}

/**
 * BLOQUE 2: TRADUCTOR DE COLORES (RESTAURADO)
 * Asocia cada icono con su color vibrante original.
 */
fun obtenerColorIcono(nombreIcono: String?): Color {
    return when (nombreIcono) {
        "shopping_cart"    -> Color(0xFF81C784) // Verde suave
        "work"             -> Color(0xFF90CAF9) // Azul claro
        "home"             -> Color(0xFFFFCC80) // Ámbar suave
        "star"             -> Color(0xFFFFD54F) // Amarillo apagado
        "event"            -> Color(0xFFF48FB1) // Rosa suave
        "settings"         -> Color(0xFF90A4AE) // Gris azulado suave
        "person"           -> Color(0xFF80DEEA) // Cian suave
        "code"             -> Color(0xFFCE93D8) // Lavanda / Morado suave
        "lightbulb"        -> Color(0xFFFFF176) // Amarillo muy suave
        "restaurant"       -> Color(0xFFFFAB91) // Salmón suave
        "directions_car"   -> Color(0xFFEF9A9A) // Rojo suave
        "fitness_center"   -> Color(0xFF80CBC4) // Turquesa suave
        "payments"         -> Color(0xFFA5D6A7) // Verde claro
        "medical_services" -> Color(0xFFEF9A9A) // Rojo suave (salud)
        "school"           -> Color(0xFF9FA8DA) // Índigo suave
        "pet_page"         -> Color(0xFFBCAAA4) // Marrón suave
        "favorite"         -> Color(0xFFF48FB1) // Rosa suave
        "build"            -> Color(0xFFB0BEC5) // Gris claro
        "call"             -> Color(0xFFA5D6A7) // Verde claro
        "list"             -> Color(0xFF9E9E9E) // Gris medio
        else               -> Color(0xFF757575)
    }
}