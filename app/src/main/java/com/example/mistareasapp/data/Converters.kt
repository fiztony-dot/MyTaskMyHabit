package com.example.mistareasapp.data

import androidx.room.TypeConverter
import com.example.mistareasapp.data.habits.CriterioCumplimientoTareas
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.data.tasks.Prioridad
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

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }
    }
    // Añadir esto dentro de la clase Converters en data/Converters.kt
    @TypeConverter
    fun fromFrecuencia(frecuencia: FrecuenciaHabito): String {
        return frecuencia.name
    }

    @TypeConverter
    fun toFrecuencia(valor: String): FrecuenciaHabito {
        return FrecuenciaHabito.valueOf(valor)
    }

    @TypeConverter
    fun fromTipoObjetivo(tipoObjetivo: TipoObjetivoHabito): String {
        return tipoObjetivo.name
    }

    @TypeConverter
    fun toTipoObjetivo(valor: String): TipoObjetivoHabito {
        return TipoObjetivoHabito.valueOf(valor)
    }

    @TypeConverter
    fun fromCriterioCumplimiento(criterio: CriterioCumplimientoTareas): String {
        return criterio.name
    }

    @TypeConverter
    fun toCriterioCumplimiento(valor: String): CriterioCumplimientoTareas {
        return CriterioCumplimientoTareas.valueOf(valor)
    }
}