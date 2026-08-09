package com.example.mistareasapp.data.habits

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habitos_metricas",
    foreignKeys = [
        ForeignKey(
            entity = Habito::class,
            parentColumns = ["id"],
            childColumns = ["habitoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitoId")]
)
data class HabitoMetricas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val rachaActual: Int = 0,
    val mejorRacha: Int = 0,
    val totalCompletados: Int = 0
)