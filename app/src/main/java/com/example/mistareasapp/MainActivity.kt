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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mistareasapp.core.backup.BackupScheduler
import com.example.mistareasapp.core.notifications.habits.HabitoAlertaNavigation
import com.example.mistareasapp.core.notifications.habits.HabitoAlertaScheduler
import com.example.mistareasapp.ui.screens.SplashScreen
import com.example.mistareasapp.ui.screens.auth.AuthGate
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackupScheduler.registrar(this)
        HabitoAlertaScheduler.registrar(this)
        recibirAlertaHabito(intent)

        setContent {
            var mostrarSplash by remember { mutableStateOf(true) }

            if (mostrarSplash) {
                SplashScreen(onComplete = { mostrarSplash = false })
            } else {
                AuthGate {
                    MisTareasApp()
                }

                // Solicitar permisos solo después de que el splash haya terminado
                LaunchedEffect(Unit) {
                    checkNotificationPermission()
                    delay(1000)
                    solicitarIgnorarOptimizacionBateria()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recibirAlertaHabito(intent)
    }

    private fun recibirAlertaHabito(intent: Intent?) {
        intent?.getLongExtra(EXTRA_HABITO_ALERTA_ID, -1L)?.takeIf { it >= 0L }?.let {
            HabitoAlertaNavigation.setPending(it)
        }
    }

    companion object {
        const val EXTRA_HABITO_ALERTA_ID = "habito_alerta_id"
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