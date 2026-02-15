package com.example.mistareasapp

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.data.TareasDatabase
import com.example.mistareasapp.data.Prioridad
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class VoiceTaskActivity : ComponentActivity() {

    private val client = HttpClient(OkHttp)
    private val apiKey = DatosIA.MI_LLAVE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu nueva tarea...")
        }
        speechLauncher.launch(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Close HttpClient to prevent resource leaks
        client.close()
    }

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)

            spokenText?.let { texto ->
                procesarYGuardarConIA(texto)
            }
        } else {
            finish()
        }
    }

    private fun procesarYGuardarConIA(textoEscuchado: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Preparar datos de contexto para la IA
                val ahora = java.time.LocalDateTime.now()
                val fechaHoy = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(ahora)
                val horaHoy = DateTimeFormatter.ofPattern("HH:mm").format(ahora)

                // 2. Llamada a Gemini (Copiada de tu MisTareasApp)
                val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash-lite:generateContent?key=$apiKey"

                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        putJsonArray("contents") {
                            addJsonObject {
                                putJsonArray("parts") {
                                    addJsonObject {
                                        put("text", """
                                            Extraer JSON de: "$textoEscuchado". Hoy es $fechaHoy, $horaHoy.
                                            Formato: {"tarea": "string", "fecha": "YYYY-MM-DD o null", "hora": "HH:mm o null", "prioridad": "ALTA|MEDIA|BAJA"}
                                        """.trimIndent())
                                    }
                                }
                            }
                        }
                    }.toString())
                }

                if (response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    val jsonElement = Json.parseToJsonElement(body)
                    val textoIA = jsonElement.jsonObject["candidates"]?.jsonArray?.get(0)
                        ?.jsonObject?.get("content")?.jsonObject?.get("parts")
                        ?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                    val jsonLimpio = textoIA.replace("```json", "").replace("```", "").trim()
                    val objetoTarea = Json.decodeFromString<TareaIA>(jsonLimpio)

                    // 3. Crear la tarea procesada
                    val nuevaTarea = Tarea(
                        titulo = objetoTarea.tarea.replaceFirstChar { it.uppercase() },
                        descripcion = "Dictado desde Widget",
                        prioridad = when(objetoTarea.prioridad?.uppercase()) {
                            "ALTA" -> Prioridad.ALTA
                            "BAJA" -> Prioridad.BAJA
                            else -> Prioridad.MEDIA
                        },
                        fechaLimite = objetoTarea.fecha?.let { LocalDate.parse(it) },
                        horaLimite = objetoTarea.hora?.let { LocalTime.parse(it) }
                    )

                    TareasDatabase.getDatabase(applicationContext).tareaDao().insertar(nuevaTarea)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VoiceTaskActivity, "IA: ${nuevaTarea.titulo} guardada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("VOICE_IA", "Error procesando IA: ${e.message}")
                // Plan B: Guardar sin procesar si falla la IA
                val db = TareasDatabase.getDatabase(applicationContext)
                db.tareaDao().insertar(Tarea(titulo = textoEscuchado.replaceFirstChar { it.uppercase() }, descripcion = "Voz (Error IA)"))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VoiceTaskActivity, "Guardado simple", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}