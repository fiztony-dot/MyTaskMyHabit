package com.example.mistareasapp.core.notifications.tasks

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificacionWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val titulo = inputData.getString("titulo") ?: "Tarea pendiente"
        // 1. Extraemos el ID que pasaremos desde MiApp.kt
        val idTarea = inputData.getInt("id_tarea", -1)

        val channelId = "tareas_canal"

        Log.d("WORKER_DEBUG", "¡Worker activado para la tarea: $titulo!")

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para avisos de tareas de voz"
                enableLights(true)
                enableVibration(true)
                setImportance(NotificationManager.IMPORTANCE_HIGH)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¡Recordatorio de Tarea!")
            .setContentText(titulo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // <--- CRUCIAL
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Para que se vea en pantalla bloqueada
            .setAutoCancel(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    notificationManager.notify(idTarea.takeIf { it != -1 } ?: System.currentTimeMillis().toInt(), notification)
                    Log.d("WORKER_DEBUG", "Notificación enviada con éxito")
                }
            } else {
                notificationManager.notify(idTarea.takeIf { it != -1 } ?: System.currentTimeMillis().toInt(), notification)
            }
        } catch (e: Exception) {
            Log.e("WORKER_DEBUG", "Error al lanzar notificación: ${e.message}")
            return Result.failure()
        }

        return Result.success()
    }
}