package com.example.mistareasapp.ui.screens.tasks

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VozTareaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val texto = intent.getStringExtra("texto")?.trim() ?: return
        if (texto.isBlank() || texto.length > 300) {
            setResult(Activity.RESULT_FIRST_USER, "No he entendido la tarea", null)
            Toast.makeText(context, "Texto inválido (vacío o más de 300 caracteres)", Toast.LENGTH_SHORT).show()
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultado = ComandoTarea(context).procesar(texto)
                val (codigo, mensajeVoz, mensajeToast) = resultado.fold(
                    onSuccess = { r ->
                        Triple(Activity.RESULT_OK, r.mensajeVoz, r.mensajeVoz)
                    },
                    onFailure = { e ->
                        val msg = e.message ?: "No se pudo crear la tarea"
                        Triple(Activity.RESULT_FIRST_USER, msg, msg)
                    }
                )
                Handler(Looper.getMainLooper()).post {
                    pendingResult.setResult(codigo, mensajeVoz, null)
                    Toast.makeText(context, mensajeToast, Toast.LENGTH_LONG).show()
                    pendingResult.finish()
                }
            } catch (e: Exception) {
                val msg = "No se pudo crear la tarea"
                Handler(Looper.getMainLooper()).post {
                    pendingResult.setResult(Activity.RESULT_FIRST_USER, msg, null)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    pendingResult.finish()
                }
            }
        }
    }
}
