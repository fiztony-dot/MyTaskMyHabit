package com.example.mistareasapp.core.notifications.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receptor de boot completado.
 * Antes reprogramaba notificaciones desde Room. Ahora las tareas viven en la API
 * y las notificaciones se programan al cargar la app (TareasApiViewModel.init).
 * Este receiver se mantiene registrado en el Manifest por si se necesita en el futuro.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BOOT_RECEIVER", "Boot completado — notificaciones se reprogramarán al abrir la app")
            // Las notificaciones de tareas se reprograman cuando el usuario abre la app
            // y TareasApiViewModel carga las tareas pendientes desde la API.
        }
    }
}
