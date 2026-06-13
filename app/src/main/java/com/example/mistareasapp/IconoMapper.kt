package com.example.mistareasapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Catálogo ampliado de emojis para categorías, organizado por grupos. */
val EMOJIS_CATEGORIA = listOf(
    // Ejercicio / Deporte
    "🤸", "🏃", "🚴", "🏊", "🏋️", "⚽", "🎾", "🏀", "🥊", "🧗", "🎿", "🏄", "🏇", "🤺", "🥋",
    // Salud / Medicina
    "🩺", "💊", "🏥", "🩻", "💉", "🧬", "🫀", "🧠", "🦷", "🩹", "🫁", "🌡️",
    // Bienestar / Mente
    "🧘", "😴", "🛁", "🌸", "🍵", "☕", "🌅", "🌙", "🕯️", "🛀", "🎭", "🌺",
    // Aprendizaje / Cultura
    "📚", "📖", "✍️", "🎓", "🔬", "🎨", "🎸", "🎵", "📐", "🔭", "🖊️", "🎤",
    // Naturaleza
    "🌱", "🌳", "🌺", "🍃", "🌊", "⛰️", "🌤️", "🌿", "🍀", "🌻", "🦋",
    // Alimentación
    "🍎", "🥦", "🥗", "🍽️", "🥤", "💧", "🫐", "🥕", "🧃",
    // Objetos / Hogar
    "💼", "🏠", "🛒", "⚙️", "💡", "🔧", "📱", "💻", "📅", "🗓️", "🧹", "🔑",
    // Finanzas / Trabajo
    "💰", "📊", "📈", "🏦", "💳", "🤝",
    // Miscelánea
    "⭐", "🔥", "💪", "❤️", "👤", "🎯", "🚀", "✅", "🎁", "🏆", "🌍", "🕊️"
)

val EMOJIS_HABITO = listOf(
    "💊", "💪", "🏃", "🧘", "📚", "🚫",
    "💧", "🌿", "❤️", "😴", "🎵", "🎨",
    "🧠", "⚽", "🚗", "🍎", "☕", "🏠",
    "💼", "🐾", "🔥", "✍️", "🎯", "💰",
    "🚴", "🏊", "🌅", "📖", "🎸", "🌱",
    "🍽️", "💻", "⭐", "🏋️", "🧹", "🛒",
    "⚙️", "💡", "🔧", "📞", "🌙", "☀️"
)

fun iconoAEmoji(nombre: String?): String {
    if (nombre == null) return "⭐"
    if (nombre.any { it.code > 127 }) return nombre  // ya es emoji
    return when (nombre) {
        "work"               -> "💼"
        "person"             -> "👤"
        "warning"            -> "⚠️"
        "favorite"           -> "❤️"
        "home"               -> "🏠"
        "shopping_cart"      -> "🛒"
        "star"               -> "⭐"
        "event"              -> "📅"
        "settings"           -> "⚙️"
        "lightbulb"          -> "💡"
        "restaurant"         -> "🍽️"
        "directions_car"     -> "🚗"
        "fitness_center"     -> "🤸️"
        "payments"           -> "💰"
        "medical_services"   -> "🩺️"
        "school"             -> "📚"
        "pets", "pet_page"   -> "🐾"
        "build"              -> "🔧"
        "call"               -> "📞"
        "code"               -> "💻"
        "self_improvement"   -> "🧘"
        "directions_run"     -> "🏃"
        "local_drink"        -> "💧"
        "bedtime"            -> "😴"
        "book"               -> "📖"
        "music_note"         -> "🎵"
        "brush"              -> "🎨"
        "eco"                -> "🌿"
        "monitor_heart"      -> "❤️"
        "emoji_food_beverage"-> "☕"
        "sports_soccer"      -> "⚽"
        "psychology"         -> "🧠"
        "medication"         -> "💊"
        "smoking_rooms"      -> "🚬"
        "no_drinks"          -> "🚫"
        else                 -> "⭐"
    }
}

/** Devuelve el icono efectivo del hábito: el de su categoría si existe, o el propio como fallback. */
fun iconoEfectivoHabito(
    habitoCategoriaId: Long,
    habitoIcono: String,
    categorias: List<com.example.mistareasapp.data.habits.CategoriaHabito>
): String = categorias.firstOrNull { it.id == habitoCategoriaId }?.icono ?: habitoIcono

fun obtenerIconoPorNombre(nombre: String?): ImageVector {
    return when (nombre) {
        "work"              -> Icons.Default.Work
        "person"            -> Icons.Default.Person
        "warning"           -> Icons.Default.Warning
        "favorite"          -> Icons.Default.Favorite
        "home"              -> Icons.Default.Home
        "shopping_cart"     -> Icons.Default.ShoppingCart
        "star"              -> Icons.Default.Star
        "event"             -> Icons.Default.Event
        "settings"          -> Icons.Default.Settings
        "lightbulb"         -> Icons.Default.Lightbulb
        "restaurant"        -> Icons.Default.Restaurant
        "directions_car"    -> Icons.Default.DirectionsCar
        "fitness_center"    -> Icons.Default.FitnessCenter
        "payments"          -> Icons.Default.Payments
        "medical_services"  -> Icons.Default.MedicalServices
        "school"            -> Icons.Default.School
        "pets"              -> Icons.Default.Pets
        "pet_page"          -> Icons.Default.Pets
        "build"             -> Icons.Default.Build
        "call"              -> Icons.Default.Call
        "code"              -> Icons.Default.Code
        "self_improvement"  -> Icons.Default.SelfImprovement
        "directions_run"    -> Icons.Default.DirectionsRun
        "local_drink"       -> Icons.Default.LocalDrink
        "bedtime"           -> Icons.Default.Bedtime
        "book"              -> Icons.Default.Book
        "music_note"        -> Icons.Default.MusicNote
        "brush"             -> Icons.Default.Brush
        "eco"               -> Icons.Default.Eco
        "monitor_heart"     -> Icons.Default.MonitorHeart
        "emoji_food_beverage" -> Icons.Default.EmojiFoodBeverage
        "sports_soccer"     -> Icons.Default.SportsSoccer
        "psychology"        -> Icons.Default.Psychology
        "medication"        -> Icons.Default.Medication
        "smoking_rooms"     -> Icons.Default.SmokingRooms
        "no_drinks"         -> Icons.Default.NoDrinks
        else                -> Icons.Default.RadioButtonUnchecked
    }
}