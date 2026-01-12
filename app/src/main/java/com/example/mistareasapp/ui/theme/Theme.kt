package com.example.mistareasapp.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// --- ESQUEMA OSCURO (El que usa tu App según las fotos) ---
private val DarkColorScheme = darkColorScheme(
    primary = BlancoTexto, //botones principales, color  texto título  TopAppBar, iconos seleccionados BottomBar
    onPrimary = Fondo, //Define el color que se dibuja encima del color primary. Es el color de "contraste".

    // Las barras (TopBar/BottomBar) y Cards leerán este color por defecto
    surface = GrisSuperficie, // Topbar
    onSurface = BlancoTexto,
    surfaceContainer = GrisSuperficie, // Bottombar

    // El fondo del Scaffold y NavHost leerá este
    background = Fondo,
    onBackground = BlancoTexto,

    // Colores secundarios para elementos de acento
    secondary = GrisBotonAdd,
    onSecondary = Color.Black,

    // ✅ Este controla el color del "óvalo/chicle" de selección
    secondaryContainer = Color(0xFF3D3D43),

    error = PrioridadAlta,
    outline = GrisDetalles
)

// --- ESQUEMA CLARO (Opcional, por si algún día lo activas) ---
private val LightScheme = lightColorScheme(
    primary = Color(0xFF415F91),
    background = Color(0xFFF9F9FF),
    surface = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20)
)

@Composable
fun MisTareasAppTheme(
    // Forzamos darkTheme a true para que siempre se vea el diseño gris "Premium"
    darkTheme: Boolean = true,
    // Desactivamos dynamicColor para que Android no cambie tus grises por colores del fondo de pantalla
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de tener el archivo Typography.kt creado
        content = content
    )
}

