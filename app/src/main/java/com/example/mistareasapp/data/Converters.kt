// Fichero: Converters.kt
package com.example.mistareasapp.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {

    @TypeConverter
    fun fromPrioridad(prioridad: Prioridad): String {
        return prioridad.name
    }

    @TypeConverter
    fun toPrioridad(nombre: String): Prioridad {
        return Prioridad.valueOf(nombre)
    }

    @TypeConverter
    fun fromLocalDate(fecha: LocalDate?): Long? {
        return fecha?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    // --- NUEVOS CONVERSORES PARA LA HORA ---

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }
    }
}