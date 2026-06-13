package com.example.mistareasapp.data.habits

enum class TipoObjetivoHabito {
    FRECUENCIA,
    CUANTITATIVO
}

enum class CriterioCumplimientoTareas {
    TODAS,
    PARCIAL
}

/**
 * Tipo de medición de cumplimiento del hábito:
 * - BINARIO: 0% o 100% por periodo (cumplió o no cumplió).
 * - PROPORCIONAL_CON_TOPE: % real, máximo 100%.
 * - PROPORCIONAL_SIN_TOPE: % real sin límite superior.
 */
enum class TipoMedicion {
    BINARIO,
    PROPORCIONAL_CON_TOPE,
    PROPORCIONAL_SIN_TOPE
}

