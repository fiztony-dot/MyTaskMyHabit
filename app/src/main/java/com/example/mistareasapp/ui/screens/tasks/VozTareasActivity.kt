package com.example.mistareasapp.ui.screens.tasks

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.mistareasapp.core.voice.VozTaskParser
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.tasks.Tarea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VozTareasActivity : ComponentActivity() {

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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre de la tarea (y fecha/hora opcional)")
        }
        try {
            vozLauncher.launch(intent)
        } catch (_: Exception) {
            finish()
        }
    }

    private fun procesarTexto(texto: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val datos = VozTaskParser.parse(texto)
            val dao = AppDatabase.getDatabase(applicationContext).tareaDao()
            dao.insertar(
                Tarea(
                    titulo = datos.titulo,
                    descripcion = null,
                    prioridad = datos.prioridad,
                    fechaLimite = datos.fechaLimite,
                    horaLimite = datos.horaLimite,
                    pendienteClasificar = true
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@VozTareasActivity,
                    "Tarea añadida: ${datos.titulo}",
                    Toast.LENGTH_LONG
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)
            }
        }
    }
}
