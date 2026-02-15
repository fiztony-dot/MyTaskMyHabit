package com.example.mistareasapp.core.notifications.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mistareasapp.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                // Obtenemos todas las tareas pendientes
                val tareasActivas = db.tareaDao().obtenerTareasPendientesSincronas()
                tareasActivas.forEach { tarea ->
                    NotificationHelper.programarNotificacion(context, tarea)
                }
            }
        }
    }
}