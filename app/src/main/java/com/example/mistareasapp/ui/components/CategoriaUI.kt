package com.example.mistareasapp.ui.components

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
        // --- Grupo Original ---
        "shopping_cart" -> Color(0xFF4CAF50) // Verde (Súper)
        "work" -> Color(0xFF2196F3)          // Azul (Trabajo)
        "home" -> Color(0xFFFF9800)          // Naranja (Casa)
        "star" -> Color(0xFFFFC107)          // Ámbar (Destacados)
        "event" -> Color(0xFFE91E63)         // Rosa (Eventos)
        "settings" -> Color(0xFF607D8B)      // Gris Azulado (Ajustes)
        "person" -> Color(0xFF00BCD4)        // Cian (Personal)
        "code" -> Color(0xFF9C27B0)          // Morado (Programación)
        "lightbulb" -> Color(0xFFFFEB3B)     // Amarillo (Ideas)
        "restaurant" -> Color(0xFFFF5722)    // Naranja Rojizo (Comida)
        "directions_car" -> Color(0xFFF44336) // Rojo (Coche)
        "fitness_center" -> Color(0xFF009688) // Turquesa (Gym)
        "payments" -> Color(0xFF2E7D32)       // Verde Oscuro (Dinero)
        "medical_services" -> Color(0xFFEF5350) // Rojo Suave (Salud)
        "school" -> Color(0xFF3F51B5)         // Índigo (Estudios)
        "pet_page" -> Color(0xFF795548)       // Marrón (Mascotas)
        "favorite" -> Color(0xFFD81B60)       // Magenta (Favoritos)
        "build" -> Color(0xFF455A64)          // Gris Hierro (Herramientas)
        "call" -> Color(0xFF00C853)           // Verde Brillante (Llamadas)
        "list" -> Color(0xFF616161)           // Gris medio (General)
        else -> Color(0xFF757575)            // Gris por defecto
    }
}