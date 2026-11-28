package com.example.mistareasapp.ui.screens.shopping

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VozCompraActivity : ComponentActivity() {

    private val vozLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val texto = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (texto != null) procesarTexto(texto) else finish()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el lugar (opcional) y los productos separados por 'y'")
        }
        try {
            vozLauncher.launch(intent)
        } catch (_: Exception) {
            finish()
        }
    }

    private fun procesarTexto(texto: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resultado = ComandoCompra(this@VozCompraActivity).procesar(texto)
            withContext(Dispatchers.Main) {
                val mensaje = resultado.fold(
                    onSuccess = { r ->
                        when {
                            r.nombresAñadidos.isNotEmpty() && r.duplicados > 0 ->
                                "Añadidos ${r.nombresAñadidos.size} a ${r.lugarNombre} (${r.duplicados} ya estaban)"
                            r.nombresAñadidos.isNotEmpty() ->
                                "Añadidos ${r.nombresAñadidos.size} producto(s) a ${r.lugarNombre}"
                            else ->
                                "Todos los productos ya estaban en ${r.lugarNombre}"
                        }
                    },
                    onFailure = { it.message ?: "Error" }
                )
                Toast.makeText(this@VozCompraActivity, mensaje, Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)
            }
        }
    }
}
