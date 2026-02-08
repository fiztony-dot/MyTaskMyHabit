package com.example.mistareasapp.core.notifications.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
// IMPORTANTE: Estos imports deben coincidir con tus rutas reales
import com.example.mistareasapp.data.tasks.TareasDatabase
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class NotificacionWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val idTarea = inputData.getInt("id_tarea", -1)
        val titulo = inputData.getString("titulo") ?: "Tarea pendiente"
        val tipo = inputData.getString("tipo") ?: ""
        val intervalo = inputData.getLong("intervalo", 0L)

        // 1. CONEXIÓN A TU BASE DE DATOS REAL
        // Cambiado AppDatabase por TareasDatabase que es el tuyo
        val db = TareasDatabase.getDatabase(applicationContext)

        val tareaEnDb = runBlocking {
            db.tareaDao().obtenerTareaPorIdSincrona(idTarea)
        }

        // 2. VERIFICACIÓN DE ESTADO
        // Si la tarea no existe o ya está marcada como completada, frenamos en seco
        if (tareaEnDb == null || tareaEnDb.estaCompletada == true) {
            Log.d("LOG-NOTIFICACION", "⏹️ Deteniendo avisos: Tarea completada o inexistente")
            return Result.success()
        }

        // 3. LANZAR LA NOTIFICACIÓN
        mostrarNotificacion(titulo, idTarea, tipo)

        // 4. REPROGRAMACIÓN AUTOMÁTICA
        // Si es una repetición y la tarea sigue pendiente, el Worker se vuelve a llamar a sí mismo
        if (tipo == "repeticion" && intervalo > 0) {
            val proximaRequest = OneTimeWorkRequestBuilder<NotificacionWorker>()
                .setInitialDelay(intervalo, TimeUnit.MILLISECONDS)
                .setInputData(inputData) // Pasa los mismos datos al siguiente aviso
                .addTag("notif_${idTarea}_repeticion")
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "notif_${idTarea}_repeticion",
                ExistingWorkPolicy.REPLACE,
                proximaRequest
            )
            Log.d("LOG-NOTIFICACION", "🔁 Repetición auto-programada para: $titulo")
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