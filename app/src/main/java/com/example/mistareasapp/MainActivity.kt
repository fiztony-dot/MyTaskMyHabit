package com.example.mistareasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MisTareasApp()

            // Usamos LaunchedEffect para que esto solo ocurra UNA VEZ
            // y cuando la interfaz ya sea visible.
            LaunchedEffect(Unit) {
                // Primero lo más ligero: Notificaciones
                checkNotificationPermission()

              // Esperamos un segundo antes de lanzar lo de la batería
                // para evitar que el móvil se colapse con dos diálogos
                delay(1000)
                solicitarIgnorarOptimizacionBateria()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun solicitarIgnorarOptimizacionBateria() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("BATERIA", "Fallo al abrir: ${e.message}")
            }
        }
    }
}