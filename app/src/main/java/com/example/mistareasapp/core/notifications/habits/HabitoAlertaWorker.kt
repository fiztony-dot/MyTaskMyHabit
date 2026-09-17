package com.example.mistareasapp.core.notifications.habits

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mistareasapp.MainActivity
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDateTime

class HabitoAlertaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val ahora = LocalDateTime.now()
            val hoy = ahora.toLocalDate()
            val dao = AppDatabase.getDatabase(applicationContext).habitoDao()
            dao.obtenerHabitosConAlertaActiva().forEach { habito ->
                val hora = habito.horaAlerta ?: return@forEach
                if (!HabitoAlertaEvaluador.estaEnVentanaDeQuinceMinutos(hora, ahora)) return@forEach

                // Load only the historial needed for the evaluation window,
                // not the full history (which could be thousands of rows per habit).
                val historial = when (habito.frecuencia) {
                    FrecuenciaHabito.SEMANAL -> {
                        val inicio = hoy.with(DayOfWeek.MONDAY)
                        dao.obtenerHistorialEntreFechas(habito.id, inicio, hoy)
                    }
                    FrecuenciaHabito.MENSUAL -> {
                        val inicio = hoy.withDayOfMonth(1)
                        dao.obtenerHistorialEntreFechas(habito.id, inicio, hoy)
                    }
                    FrecuenciaHabito.DIARIA -> {
                        val entrada = dao.obtenerProgresoDiario(habito.id, hoy)
                        if (entrada != null) listOf(entrada) else emptyList()
                    }
                }

                val resultado = HabitoAlertaEvaluador.evaluar(
                    habito,
                    ahora,
                    historial.lastOrNull { it.fecha == hoy },
                    historial
                ) ?: return@forEach
                mostrarNotificacion(habito.id, habito.nombre, resultado)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun mostrarNotificacion(idHabito: Long, nombre: String, resultado: HabitoAlertaResultado) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CANAL, "Hábitos", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_HABITO_ALERTA_ID, idHabito)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            idHabito.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val texto = when (resultado.tipo) {
            TipoAlertaHabito.DIARIA -> "Aún no has registrado $nombre hoy"
            TipoAlertaHabito.SEMANAL -> "En riesgo: llevas ${resultado.cumplidos}/${resultado.objetivo} $nombre esta semana y quedan ${resultado.diasRestantes} días"
            TipoAlertaHabito.MENSUAL -> "En riesgo: llevas ${resultado.cumplidos}/${resultado.objetivo} $nombre este mes y quedan ${resultado.diasRestantes} días"
        }
        manager.notify(idHabito.toInt(), NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(nombre)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build())
    }

    companion object {
        const val CANAL = "habitos_canal"
        const val NOMBRE_TRABAJO = "alertas_habitos_periodicas"
    }
}