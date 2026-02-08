package com.example.mistareasapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.network.TipoEntrada
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import com.example.mistareasapp.network.IAProcessor
import com.example.mistareasapp.network.IAResultHabito
import com.example.mistareasapp.network.IAResultTarea
import kotlinx.serialization.json.*

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
class IAViewModel : ViewModel() {
     fun procesarVoz(textoEscuchado: String, tipo: TipoEntrada, alTerminar: (Any?) -> Unit) {
        viewModelScope.launch {
            // 1. Llamamos a la IA (Lo que ya tienes en IAProcessor)
            val jsonResult = IAProcessor.procesarTexto(textoEscuchado, tipo)

            if (jsonResult != null) {
                try {
                    if (tipo == TipoEntrada.TAREA) {
                        // 1. Convertimos el JSON en el objeto raw (Ahora con fecha y hora)
                        val raw = jsonConfig.decodeFromString<IAResultTarea>(jsonResult)

                        // 2. Título limpio
                        val tituloLimpio = raw.titulo.trim().replaceFirstChar { it.uppercase() }

                        // 3. --- PARCHE 2: Cálculo de minutos (Copiado de tu MiApp) ---
                        var horaFinal = raw.hora // Usamos la hora que ya traía el JSON
                        val textoMinus = textoEscuchado.lowercase()
                        if (textoMinus.contains("en ") && (textoMinus.contains("minuto") || textoMinus.contains("min"))) {
                            val minutos = textoMinus.filter { it.isDigit() }.toIntOrNull() ?: 2
                            val calculoLocal = java.time.LocalTime.now().plusMinutes(minutos.toLong())
                            horaFinal = calculoLocal.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        }

                        // 4. --- PARCHE 3: Validación de fecha ---
                        val fechaFinal = try {
                            if (!raw.fecha.isNullOrBlank()) {
                                val fechaDate = java.time.LocalDate.parse(raw.fecha)
                                // Si la IA se equivoca y pone una fecha pasada, ponemos hoy
                                if (fechaDate.isBefore(java.time.LocalDate.now())) {
                                    java.time.LocalDate.now().toString()
                                } else raw.fecha
                            } else {
                                raw.fecha
                            }
                        } catch (e: Exception) {
                            raw.fecha
                        }

                        // 5. Devolvemos la tarea con TODO
                        // Usamos copy para mantener categoria y prioridad pero actualizar lo demas
                        alTerminar(raw.copy(
                            titulo = tituloLimpio,
                            fecha = fechaFinal,
                            hora = horaFinal
                        ))

                    } else if (tipo == TipoEntrada.HABITO) {
                        val habitResult = Json.decodeFromString<IAResultHabito>(jsonResult)
                        alTerminar(habitResult)
                    }
                } catch (e: Exception) {
                    Log.e("IA_VM", "Error procesando: ${e.message}")
                    alTerminar(null)
                }
            } else {
                alTerminar(null) // Error de red o API
            }
        }
    }
    private fun parsearTarea(jsonStr: String): IAResultTarea {
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        return IAResultTarea(
            titulo = element["titulo"]?.jsonPrimitive?.content ?: "Sin título",
            prioridad = element["prioridad"]?.jsonPrimitive?.content ?: "Media",
            categoria = element["categoria"]?.jsonPrimitive?.content ?: "General"
        )
    }
}