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
        val idTarea = inputData.getInt("id_tarea", -1)

        // 1. RECOGER EL TIPO (Esto te faltaba)
        val tipo = inputData.getString("tipo") ?: ""

        val channelId = "tareas_canal"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ... (Tu bloque de NotificationChannel está perfecto, déjalo igual) ...

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¡Recordatorio de Tarea!")
            .setContentText(titulo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        // 2. CALCULAR ID ÚNICO (Para que no se pisen)
        val finalNotificationId = if (idTarea != -1) {
            idTarea + tipo.hashCode()
        } else {
            System.currentTimeMillis().toInt()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    // 3. USAR EL ID CALCULADO
                    notificationManager.notify(finalNotificationId, notification)
                    Log.d("WORKER_DEBUG", "Notificación enviada con éxito")
                }
            } else {
                // 3. USAR EL ID CALCULADO TAMBIÉN AQUÍ
                notificationManager.notify(finalNotificationId, notification)
            }
        } catch (e: Exception) {
            Log.e("WORKER_DEBUG", "Error al lanzar notificación: ${e.message}")
            return Result.failure()
        }

        return Result.success()
    }
}