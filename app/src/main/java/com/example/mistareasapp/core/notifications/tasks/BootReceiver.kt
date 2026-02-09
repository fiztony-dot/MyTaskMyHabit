// BootReceiver.kt
package com.example.mistareasapp.core.notifications.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mistareasapp.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(Dispatchers.IO)
            val db = AppDatabase.getDatabase(context)

            scope.launch {
                // Obtenemos todas las tareas no completadas que tengan fecha
                val tareas = db.tareaDao().obtenerTodas().first()
                tareas.filter { !it.estaCompletada && it.fechaLimite != null }.forEach { tarea ->
                    NotificationHelper.programarNotificacion(context, tarea)
                }
            }
        }
    }
}