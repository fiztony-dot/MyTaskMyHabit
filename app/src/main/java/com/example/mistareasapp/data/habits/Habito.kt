// Fichero: data/habits/Habito.kt
package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Archivo: data/habits/Habito.kt
@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String? = null,
    val categoriaId: Long = 0,
    val fechaInicio: LocalDate = LocalDate.now(),
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val tipoObjetivo: TipoObjetivoHabito = TipoObjetivoHabito.FRECUENCIA,
    val vecesPorDia: Int = 1,
    val objetivoValor: Int? = null,
    val unidad: String? = null,
    val esCompuestoPorTareas: Boolean = false,
    val criterioCumplimientoTareas: CriterioCumplimientoTareas = CriterioCumplimientoTareas.TODAS,
    val minimoTareasCumplimiento: Int? = null,
    val objetivoRachaSemanas: Int = 4,
    val recordatoriosActivos: Boolean = false,
    val horaRecordatorio: LocalTime? = null,
    val icono: String = "favorite",
    val colorHex: String = "#FF0000",
    val activo: Boolean = true,
    val fechaModificacion: LocalDate? = null,
    val pausado: Boolean = false,
    val fechaInicioPausa: LocalDate? = null,
    val fechaFinPausa: LocalDate? = null,
    val objetivoPorcentajeDias: Int? = null,
    // Días de la semana en que aplica el hábito (solo para frecuencia DIARIA).
    // Cadena "1,3,5" = lunes, miércoles, viernes. Null = todos los días.
    val diasSemana: String? = null,
    // Legacy — mantenido para compatibilidad de DB, no usar en lógica nueva.
    val puedeSuperar100: Boolean = false,
    // Tipo de medición del cumplimiento (ver TipoMedicion).
    val tipoMedicion: TipoMedicion = TipoMedicion.PROPORCIONAL_CON_TOPE,
    // Dificultad subjetiva 1-5, usada en la fórmula del % general ponderado.
    val dificultad: Int = 3,
    // Límite Máximo: valor máximo permitido por periodo.
    val limiteMaximo: Double? = null,
    // Límite Máximo: tramos JSON [{"desde":0,"hasta":10,"porcentaje":120}, ...].
    val tramosLimite: String? = null,
    // Límite Máximo: activa tabla de equivalencias UBE al registrar.
    val ubeActivo: Boolean = false,
    // Orden visual dentro de la categoría (menor = antes).
    val orden: Int = 0
)

/** Devuelve el conjunto de DayOfWeek en que aplica este hábito, o null si aplica todos los días. */
fun Habito.diasSemanaSet(): Set<DayOfWeek>? = diasSemana
    ?.split(",")
    ?.mapNotNull { token -> token.trim().toIntOrNull()?.let { n -> runCatching { DayOfWeek.of(n) }.getOrNull() } }
    ?.toSet()
    ?.takeIf { it.isNotEmpty() && it.size < 7 }
