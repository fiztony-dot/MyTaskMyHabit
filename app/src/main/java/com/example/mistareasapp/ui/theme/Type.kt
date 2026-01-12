package com.example.mistareasapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Definición de la tipografía personalizada para la App
val Typography = Typography(

    // ESTILO PARA EL TÍTULO DE LAS TAREAS
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,       // Bajamos de 22sp a 20sp para que no sea tan gigante
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // ESTILO PARA LAS DESCRIPCIONES (Texto intermedio)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // ESTILO PARA LAS FECHAS Y PRIORIDADES (Texto pequeño)
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // OPCIONAL: Estilo para etiquetas (Tags de prioridad)
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)