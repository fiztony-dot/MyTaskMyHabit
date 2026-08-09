package com.example.mistareasapp.ui.theme

import androidx.compose.ui.graphics.Color

// --- PALETA MATERIAL YOU DARK ---
val M3Background       = Color(0xFF0D0C12)   // casi negro con toque púrpura
val M3Surface          = Color(0xFF1E1B2E)   // tarjetas: púrpura oscuro visible
val M3SurfaceVariant   = Color(0xFF2A2640)   // superficies elevadas
val M3CardHabito       = Color(0xFFADADAD)   // cards de hábitos — gris neutro
val M3SurfaceContainer = Color(0xFF161327)   // bottom nav / top bar

val M3Primary          = Color(0xFFD0BCFF)   // lavanda
val M3OnPrimary        = Color(0xFF381E72)
val M3PrimaryContainer = Color(0xFF4F378B)

val M3Secondary        = Color(0xFFCCC2DC)
val M3OnSecondary      = Color(0xFF332D41)
val M3SecondaryContainer = Color(0xFF4A4458)

val M3OnSurface        = Color(0xFFE6E0E9)
val M3OnSurfaceVariant = Color(0xFF938F99)
val M3Outline          = Color(0xFF79747E)

// --- PRIORIDADES (mantenemos semántica, ajustadas para dark) ---
val PrioridadAlta      = Color(0xFFCF6679)
val PrioridadMedia     = Color(0xFFFFB74D)
val PrioridadBaja      = Color(0xFF81C784)
val PrioridadCompletada = Color(0xFF454548)

// Legacy aliases (usados en código existente que no tocamos)
val Fondo              = M3Background
val GrisSuperficie     = M3Surface
val GrisBotonAdd       = M3OnSurfaceVariant
val ColorCard          = M3Surface
val BlancoTexto        = M3OnSurface
val GrisDetalles       = M3OnSurfaceVariant
