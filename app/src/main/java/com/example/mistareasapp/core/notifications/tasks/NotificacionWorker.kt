package com.example.mistareasapp.core.notifications.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Worker que muestra notificaciones de tareas.
 * Ya no consulta Room (tareas migradas a API). La validación de si la tarea
 * sigue pendiente se hace al programar la notificación (TareasApiViewModel).
 */
class NotificacionWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val idTarea = inputData.getInt("id_tarea", -1)
        val titulo = inputData.getString("titulo") ?: "Tarea pendiente"
        val tipo = inputData.getString("tipo") ?: ""
        val intervalo = inputData.getLong("intervalo", 0L)

        // Mostrar la notificación directamente
        mostrarNotificacion(titulo, idTarea, tipo)

        // Reprogramación automática para repeticiones
        if (tipo == "repeticion" && intervalo > 0) {
            val proximaRequest = OneTimeWorkRequestBuilder<NotificacionWorker>()
                .setInitialDelay(intervalo, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("notif_${idTarea}_repeticion")
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "notif_${idTarea}_repeticion",
                ExistingWorkPolicy.REPLACE,
                proximaRequest
            )
            Log.d("LOG-NOTIFICACION", "Repetición auto-programada para: $titulo")
        }

        return Result.success()
    }

    private fun mostrarNotificacion(titulo: String, idTarea: Int, tipo: String) {
        val channelId = "tareas_canal"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Recordatorio de Tarea")
            .setContentText(titulo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(idTarea + tipo.hashCode(), notification)
    }
}
