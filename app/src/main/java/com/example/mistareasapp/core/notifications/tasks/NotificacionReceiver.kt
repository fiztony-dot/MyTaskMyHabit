package com.example.mistareasapp.core.notifications.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificacionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val idTarea = intent.getIntExtra("id_tarea", -1)
        val titulo = intent.getStringExtra("titulo") ?: "Tarea pendiente"
        val tipo = intent.getStringExtra("tipo") ?: "principal"
        val intervalo = intent.getLongExtra("intervalo", 0L)

        if (idTarea == -1) return

        mostrarNotificacion(context, titulo, idTarea, tipo)

        if (tipo == "repeticion" && intervalo > 0) {
            val siguiente = System.currentTimeMillis() + intervalo
            NotificationHelper.programarAlarma(context, idTarea, titulo, siguiente, "repeticion", intervalo)
            Log.d("LOG-NOTIFICACION", "🔁 Repetición programada para: $titulo")
        }
    }

    private fun mostrarNotificacion(context: Context, titulo: String, idTarea: Int, tipo: String) {
        val channelId = "tareas_canal"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Recordatorio de Tarea")
            .setContentText(titulo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        val notifId = idTarea * 10 + if (tipo == "principal") 0 else 1
        notificationManager.notify(notifId, notification)
        Log.d("LOG-NOTIFICACION", "✅ Notificación mostrada: $titulo")
    }
}
