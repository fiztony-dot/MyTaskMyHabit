package com.example.mistareasapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "tareas_table")
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val descripcion: String?,
    val estaCompletada: Boolean = false,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaLimite: LocalDate? = null,
    val horaLimite: LocalTime? = null,
    val categoria: String? = null,
    val repeticion: String = "Sin repetición"
) {
    // AÑADE ESTO AQUÍ:
    fun toComparableDateTime(): java.time.LocalDateTime {
        val fecha = this.fechaLimite ?: java.time.LocalDate.MAX
        // Si no hay hora, usamos el final del día (23:59),
        // pero si hay fecha de HOY y queremos que venza, debe tener hora asignada.
        val hora = this.horaLimite ?: java.time.LocalTime.MIDNIGHT
        return java.time.LocalDateTime.of(fecha, hora)
    }
    // Propiedad inteligente para el filtrado automático
    val estaVencida: Boolean
        get() {
            if (estaCompletada) return false
            val fecha = fechaLimite ?: return false
            val hoy = java.time.LocalDate.now()

            return when {
                fecha.isBefore(hoy) -> true
                fecha.isEqual(hoy) -> {
                    // Si no hay hora, usamos el final del día (23:59)
                    // Así, si son las 19:00, la tarea no vence hasta medianoche.
                    // Si quieres que venza YA si no hay hora, usa LocalTime.now().minusMinutes(1)
                    val hora = horaLimite ?: java.time.LocalTime.MAX
                    java.time.LocalTime.now().isAfter(hora)
                }
                else -> false
            }
        }
}
