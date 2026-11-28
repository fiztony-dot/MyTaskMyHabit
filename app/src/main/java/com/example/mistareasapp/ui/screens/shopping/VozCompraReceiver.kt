package com.example.mistareasapp.ui.screens.shopping

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

class VozCompraReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val texto = intent.getStringExtra("texto")?.trim() ?: return
        if (texto.isBlank() || texto.length > 300) {
            val mensajeError = "Texto inválido"
            setResult(Activity.RESULT_FIRST_USER, mensajeError, null)
            Toast.makeText(context, "Texto inválido (vacío o más de 300 caracteres)", Toast.LENGTH_SHORT).show()
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultado = ComandoCompra(context).procesar(texto)
                val (codigo, mensajeVoz, mensajeToast) = resultado.fold(
                    onSuccess = { r ->
                        val productosStr = r.nombresAñadidos.joinToString(" y ") { it.lowercase() }
                        when {
                            r.nombresAñadidos.isNotEmpty() && r.duplicados > 0 -> Triple(
                                Activity.RESULT_OK,
                                "Añadido a ${r.lugarNombre}: $productosStr",
                                "Añadido a ${r.lugarNombre}: ${r.nombresAñadidos.joinToString(", ")} (${r.duplicados} ya estaban)"
                            )
                            r.nombresAñadidos.isNotEmpty() -> Triple(
                                Activity.RESULT_OK,
                                "Añadido a ${r.lugarNombre}: $productosStr",
                                "Añadido a ${r.lugarNombre}: ${r.nombresAñadidos.joinToString(", ")}"
                            )
                            else -> Triple(
                                Activity.RESULT_OK,
                                "Todo ya estaba en ${r.lugarNombre}",
                                "Todos los productos ya estaban en ${r.lugarNombre}"
                            )
                        }
                    },
                    onFailure = { e ->
                        val msg = when {
                            e.message?.contains("lugares") == true -> "No hay lugares configurados"
                            e.message?.contains("productos") == true -> "No he entendido ningún producto"
                            else -> "Error: ${e.message}"
                        }
                        Triple(Activity.RESULT_FIRST_USER, msg, msg)
                    }
                )
                Handler(Looper.getMainLooper()).post {
                    pendingResult.setResult(codigo, mensajeVoz, null)
                    Toast.makeText(context, mensajeToast, Toast.LENGTH_LONG).show()
                    pendingResult.finish()
                }
            } catch (e: Exception) {
                val msg = "Error: ${e.message}"
                Handler(Looper.getMainLooper()).post {
                    pendingResult.setResult(Activity.RESULT_FIRST_USER, msg, null)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    pendingResult.finish()
                }
            }
        }
    }
}
