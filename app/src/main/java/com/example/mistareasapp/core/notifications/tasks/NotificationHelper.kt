package com.example.mistareasapp.core.notifications.tasks

import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea

object NotificationHelper {
    private fun programarTareaEnWorkManager(
        context: android.content.Context,
        tarea: Tarea,
        delayMs: Long,
        tipo: String
    ) {
        val data = androidx.work.workDataOf(
            "titulo" to tarea.titulo,
            "id_tarea" to tarea.id,
            "tipo" to tipo // <-- AÑADE ESTA LÍNEA en NotificationHelper.kt
        )

        val request = androidx.work.OneTimeWorkRequestBuilder<NotificacionWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(data)
            // BORRA O COMENTA ESTA LÍNEA:
            // .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("notif_${tarea.id}_$tipo")
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "notif_${tarea.id}_$tipo",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }
    private const val CINCO_MINUTOS_MS = 5 * 60 * 1000L // 5 minutos
    private const val HORA_EN_MS = 3_600_000L // 60 minutos
    private const val DIA_EN_MS = 86_400_000L // 24 horas
    private const val TRES_DIAS_MS = 259_200_000L // 3 días

    fun programarNotificacion(context: android.content.Context, tarea: Tarea) {
        val fecha = tarea.fechaLimite ?: return
        val hora = tarea.horaLimite ?: java.time.LocalTime.of(9, 0)
        val fechaHoraLimite = java.time.LocalDateTime.of(fecha, hora)
        val ahora = java.time.LocalDateTime.now()

        val delayBase = java.time.Duration.between(ahora, fechaHoraLimite).toMillis()

        // 1. DETERMINAMOS EL INTERVALO DE REPETICIÓN SEGÚN TU SOLICITUD
        val tiempoRepeticion = when (tarea.prioridad) {
            Prioridad.ALTA -> CINCO_MINUTOS_MS
            Prioridad.MEDIA -> HORA_EN_MS
            Prioridad.BAJA -> TRES_DIAS_MS
            else -> 0L
        }

        if (delayBase > 0) {
            // A. AVISO PRINCIPAL (A la hora de la tarea)
            programarTareaEnWorkManager(context, tarea, delayBase, "principal")
            android.util.Log.d("LOG-NOTIFICACION", "✅ ALARMA PRINCIPAL: '${tarea.titulo}' a las $hora")

            // B. AVISO DE REPETICIÓN (Según prioridad)
            if (tiempoRepeticion > 0) {
                programarTareaEnWorkManager(context, tarea, delayBase + tiempoRepeticion, "repeticion")
                val info = when(tarea.prioridad) {
                    Prioridad.ALTA -> "60 min"
                    Prioridad.MEDIA -> "24 horas"
                    Prioridad.BAJA -> "3 días"
                    else -> ""
                }
                android.util.Log.d("LOG-NOTIFICACION", "➕ REPETICIÓN PROGRAMADA (cada $info) para: '${tarea.titulo}'")
            }

        } else {
            android.util.Log.d("LOG-NOTIFICACION", "❌ NO PROGRAMADA: '${tarea.titulo}' ya pasó.")
        }
    }
}