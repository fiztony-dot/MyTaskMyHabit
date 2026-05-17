package com.example.mistareasapp.core.notifications.tasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date

object NotificationHelper {

    private const val CINCOHORAS_EN_MS = 300 * 60 * 1000L
    private const val UN_DIA_MS = 24 * 60 * 60 * 1000L
    private const val TRES_DIAS_MS = 3 * 24 * 60 * 60 * 1000L

    fun programarNotificacion(context: Context, tarea: Tarea) {
        val fecha = tarea.fechaLimite ?: return
        val hora = tarea.horaLimite ?: LocalTime.of(9, 0)
        val fechaHoraLimite = LocalDateTime.of(fecha, hora)
        val delayMs = Duration.between(LocalDateTime.now(), fechaHoraLimite).toMillis()

        if (delayMs <= 0) return

        val triggerTime = System.currentTimeMillis() + delayMs
        val intervaloRepeticion = when (tarea.prioridad) {
            Prioridad.ALTA -> CINCOHORAS_EN_MS
            Prioridad.MEDIA -> UN_DIA_MS
            Prioridad.BAJA -> TRES_DIAS_MS
        }

        programarAlarma(context, tarea.id, tarea.titulo, triggerTime, "principal", 0L)
        programarAlarma(context, tarea.id, tarea.titulo, triggerTime + intervaloRepeticion, "repeticion", intervaloRepeticion)
    }

    fun programarAlarma(context: Context, idTarea: Int, titulo: String, triggerTime: Long, tipo: String, intervalo: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("LOG-NOTIFICACION", "❌ Sin permiso para alarmas exactas. Ve a Ajustes > Apps > Acceso especial > Alarmas exactas")
            return
        }

        val pendingIntent = crearPendingIntent(context, idTarea, titulo, tipo, intervalo) ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("LOG-NOTIFICACION", "⏰ Alarma '$tipo' programada para tarea $idTarea ('$titulo') a las ${Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e("LOG-NOTIFICACION", "❌ SecurityException al programar alarma: ${e.message}")
        }
    }

    fun cancelarNotificacion(context: Context, tareaId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf("principal", "repeticion").forEach { tipo ->
            val pi = crearPendingIntent(context, tareaId, "", tipo, 0L, noCreate = true)
            if (pi != null) alarmManager.cancel(pi)
        }

        // Cancela también jobs de WorkManager que pudieran quedar
        WorkManager.getInstance(context).cancelAllWorkByTag("notif_$tareaId")
    }

    private fun crearPendingIntent(
        context: Context,
        idTarea: Int,
        titulo: String,
        tipo: String,
        intervalo: Long,
        noCreate: Boolean = false
    ): PendingIntent? {
        val intent = Intent(context, NotificacionReceiver::class.java).apply {
            putExtra("id_tarea", idTarea)
            putExtra("titulo", titulo)
            putExtra("tipo", tipo)
            putExtra("intervalo", intervalo)
        }
        val requestCode = idTarea * 10 + if (tipo == "principal") 0 else 1
        val flags = (if (noCreate) PendingIntent.FLAG_NO_CREATE else PendingIntent.FLAG_UPDATE_CURRENT) or
                PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
