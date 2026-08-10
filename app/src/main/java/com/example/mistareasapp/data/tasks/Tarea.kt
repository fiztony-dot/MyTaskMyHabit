package com.example.mistareasapp.data.tasks

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Tarea(
    val id: Int = 0,
    val titulo: String,
    val descripcion: String? = null,
    val estaCompletada: Boolean = false,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaLimite: LocalDate? = null,
    val horaLimite: LocalTime? = null,
    val categoria: String? = null,
    val repeticion: String = "Sin repetición",
    val pendienteClasificar: Boolean = false,
    val repeticionFin: LocalDate? = null,
    val repeticionVeces: Int? = null,
    val repeticionContador: Int = 0
) {
    fun toComparableDateTime(): LocalDateTime {
        val fecha = this.fechaLimite ?: LocalDate.MAX
        val hora = this.horaLimite ?: LocalTime.MIDNIGHT
        return LocalDateTime.of(fecha, hora)
    }

    val estaVencida: Boolean
        get() {
            if (estaCompletada) return false
            val fecha = fechaLimite ?: return false
            val hoy = LocalDate.now()

            return when {
                fecha.isBefore(hoy) -> true
                fecha.isEqual(hoy) -> {
                    val hora = horaLimite ?: LocalTime.MAX
                    LocalTime.now().isAfter(hora)
                }
                else -> false
            }
        }
}
