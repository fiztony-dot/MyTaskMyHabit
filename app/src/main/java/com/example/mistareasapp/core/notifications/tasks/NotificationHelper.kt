package com.example.mistareasapp.core.notifications.tasks

import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import androidx.work.*

object NotificationHelper {
    private const val CINCO_MINUTOS_MS = 5 * 60 * 1000L
    private const val HORA_EN_MS = 60 * 60 * 1000L
    private const val TRES_DIAS_MS = 3 * 24 * 60 * 60 * 1000L

    private fun programarTareaEnWorkManager(
        context: android.content.Context,
        tarea: Tarea,
        delayMs: Long,
        tipo: String,
        intervalo: Long = 0L
    ) {
        val data = workDataOf(
            "titulo" to tarea.titulo,
            "id_tarea" to tarea.id,
            "tipo" to tipo,
            "intervalo" to intervalo
        )

        val request = OneTimeWorkRequestBuilder<NotificacionWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("notif_${tarea.id}_$tipo")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "notif_${tarea.id}_$tipo",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun programarNotificacion(context: android.content.Context, tarea: Tarea) {
        val fecha = tarea.fechaLimite ?: return
        val hora = tarea.horaLimite ?: LocalTime.of(9, 0)
        val fechaHoraLimite = LocalDateTime.of(fecha, hora)
        val ahora = LocalDateTime.now()

        val delayBase = Duration.between(ahora, fechaHoraLimite).toMillis()

        val tiempoRepeticion = when (tarea.prioridad) {
            Prioridad.ALTA -> CINCO_MINUTOS_MS
            Prioridad.MEDIA -> HORA_EN_MS
            Prioridad.BAJA -> TRES_DIAS_MS
            else -> 0L
        }

        // A. AVISO PRINCIPAL
        if (delayBase > 0) {
            programarTareaEnWorkManager(context, tarea, delayBase, "principal")
            android.util.Log.d("LOG-NOTIFICACION", "✅ ALARMA PRINCIPAL: '${tarea.titulo}' a las $hora")
        }

        // B. AVISO DE REPETICIÓN (Bucle)
        if (tiempoRepeticion > 0) {
            val proximoAvisoRepeticion = if (delayBase > 0) delayBase + tiempoRepeticion else tiempoRepeticion

            // Programamos si el momento de repetición es futuro
            if (proximoAvisoRepeticion > 0) {
                programarTareaEnWorkManager(context, tarea, proximoAvisoRepeticion, "repeticion", tiempoRepeticion)
                android.util.Log.d("LOG-NOTIFICACION", "➕ REPETICIÓN PROGRAMADA para: '${tarea.titulo}'")
            }
        }
    }
}