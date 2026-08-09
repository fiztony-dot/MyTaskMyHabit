package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Histórico de definiciones de un hábito.
 * Cada registro representa la definición vigente desde [fechaInicio]
 * hasta el [fechaInicio] del siguiente registro (o la actualidad).
 * Se crea automáticamente: la primera entrada al crear el hábito,
 * y una nueva cada vez que cambia cualquier parámetro de definición.
 */
@Entity(
    tableName = "habitos_versiones",
    indices = [Index(value = ["habitoId", "fechaInicio"])]
)
data class HabitoVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    /** Fecha desde la que esta definición está vigente (inclusive). */
    val fechaInicio: LocalDate,
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val tipoObjetivo: TipoObjetivoHabito = TipoObjetivoHabito.FRECUENCIA,
    val vecesPorDia: Int = 1,
    val objetivoValor: Int? = null,
    val unidad: String? = null,
    val esCompuestoPorTareas: Boolean = false,
    val minimoTareasCumplimiento: Int? = null,
    val objetivoPorcentajeDias: Int? = null,
    val puedeSuperar100: Boolean = false,
    val diasSemana: String? = null,
    val tipoMedicion: TipoMedicion = TipoMedicion.PROPORCIONAL_CON_TOPE,
    val limiteMaximo: Double? = null,
    val tramosLimite: String? = null
)

// ─── Extensiones helper ─────────────────────────────────────────────────────

/** Crea un [HabitoVersion] a partir del estado actual de un [Habito]. */
fun Habito.toVersion(fechaVigencia: LocalDate = this.fechaInicio) = HabitoVersion(
    habitoId = id,
    fechaInicio = fechaVigencia,
    frecuencia = frecuencia,
    tipoObjetivo = tipoObjetivo,
    vecesPorDia = vecesPorDia,
    objetivoValor = objetivoValor,
    unidad = unidad,
    esCompuestoPorTareas = esCompuestoPorTareas,
    minimoTareasCumplimiento = minimoTareasCumplimiento,
    objetivoPorcentajeDias = objetivoPorcentajeDias,
    puedeSuperar100 = puedeSuperar100,
    diasSemana = diasSemana,
    tipoMedicion = tipoMedicion,
    limiteMaximo = limiteMaximo,
    tramosLimite = tramosLimite
)

/** True si los parámetros de definición que determinan el cumplimiento son distintos. */
fun Habito.tieneDefinicionDistintaA(otro: Habito): Boolean =
    frecuencia != otro.frecuencia ||
    tipoObjetivo != otro.tipoObjetivo ||
    vecesPorDia != otro.vecesPorDia ||
    objetivoValor != otro.objetivoValor ||
    esCompuestoPorTareas != otro.esCompuestoPorTareas ||
    minimoTareasCumplimiento != otro.minimoTareasCumplimiento ||
    objetivoPorcentajeDias != otro.objetivoPorcentajeDias ||
    tipoMedicion != otro.tipoMedicion ||
    diasSemana != otro.diasSemana ||
    limiteMaximo != otro.limiteMaximo ||
    tramosLimite != otro.tramosLimite

/** Devuelve los días de la semana aplicables a esta versión, o null si son todos. */
fun HabitoVersion.diasSemanaSet(): Set<DayOfWeek>? = diasSemana
    ?.split(",")
    ?.mapNotNull { token -> token.trim().toIntOrNull()?.let { n -> runCatching { DayOfWeek.of(n) }.getOrNull() } }
    ?.toSet()
    ?.takeIf { it.isNotEmpty() && it.size < 7 }
