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

private val DarkColorScheme = darkColorScheme(
    primary              = M3Primary,
    onPrimary            = M3OnPrimary,
    primaryContainer     = M3PrimaryContainer,
    onPrimaryContainer   = M3Primary,

    secondary            = M3Secondary,
    onSecondary          = M3OnSecondary,
    secondaryContainer   = M3SecondaryContainer,
    onSecondaryContainer = Color(0xFFE8DEF8),

    background           = M3Background,
    onBackground         = M3OnSurface,

    surface              = M3Surface,
    onSurface            = M3OnSurface,
    surfaceVariant       = M3SurfaceVariant,
    onSurfaceVariant     = M3OnSurfaceVariant,
    surfaceContainer     = M3SurfaceContainer,

    outline              = M3Outline,
    outlineVariant       = Color(0xFF49454F),

    error                = Color(0xFFF2B8B5),
    onError              = Color(0xFF601410),
)

private val LightScheme = lightColorScheme(
    primary    = Color(0xFF6750A4),
    background = Color(0xFFFFFBFE),
    surface    = Color(0xFFFFFBFE),
    onSurface  = Color(0xFF1C1B1F)
)

@Composable
fun MisTareasAppTheme(
    darkTheme: Boolean = true,
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
        typography = Typography,
        content = content
    )
}
