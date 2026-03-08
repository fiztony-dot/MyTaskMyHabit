package com.example.mistareasapp.core.notifications.tasks

import android.content.Context
import androidx.work.*
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val CINCO_MINUTOS_MS = 5 * 60 * 1000L
    private const val HORA_EN_MS = 60 * 60 * 1000L
    private const val CINCOHORAS_EN_MS = 300 * 60 * 1000L
    private const val UN_DIA_MS = 1 * 24 * 60 * 60 * 1000L
    private const val TRES_DIAS_MS = 3 * 24 * 60 * 60 * 1000L

    fun programarNotificacion(context: Context, tarea: Tarea) {
        val fecha = tarea.fechaLimite ?: return
        val hora = tarea.horaLimite ?: LocalTime.of(9, 0)
        val fechaHoraLimite = LocalDateTime.of(fecha, hora)
        val ahora = LocalDateTime.now()

        val delayBase = Duration.between(ahora, fechaHoraLimite).toMillis()

        // Determinamos el intervalo según la prioridad que ya tienes en Tarea.kt
        val tiempoRepeticion = when (tarea.prioridad) {
            Prioridad.ALTA -> CINCOHORAS_EN_MS
            Prioridad.MEDIA -> UN_DIA_MS
            Prioridad.BAJA -> TRES_DIAS_MS
        }

        // A. ALARMA PRINCIPAL (Solo si es a futuro)
        if (delayBase > 0) {
            programarWork(context, tarea, delayBase, "principal")
        }

        // B. ALARMA DE REPETICIÓN
        // Si la tarea es urgente (ALTA/MEDIA), programamos el bucle de avisos
        val delayRepeticion = if (delayBase > 0) delayBase + tiempoRepeticion else tiempoRepeticion
        programarWork(context, tarea, delayRepeticion, "repeticion", tiempoRepeticion)
    }

    private fun programarWork(context: Context, tarea: Tarea, delay: Long, tipo: String, intervalo: Long = 0L) {
        val data = workDataOf(
            "titulo" to tarea.titulo,
            "id_tarea" to tarea.id,
            "tipo" to tipo,
            "intervalo" to intervalo
        )

        val request = OneTimeWorkRequestBuilder<NotificacionWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("notif_${tarea.id}") // Tag común para poder cancelar todo lo de esta tarea
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "notif_${tarea.id}_$tipo",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelarNotificacion(context: Context, tareaId: Int) {
        // Esto detiene tanto la principal como las repeticiones
        WorkManager.getInstance(context).cancelAllWorkByTag("notif_$tareaId")
    }
}