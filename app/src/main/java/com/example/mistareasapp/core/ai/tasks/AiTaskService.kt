package com.example.mistareasapp.core.ai.tasks

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//Estructura de la Respuesta de la IA
@Serializable
data class TareaIA(
    val tarea: String,
    val fecha: String?=null,
    val hora: String? = null,
    val prioridad: String? = null
)
//Constantes de Configuración y Credenciales
object DatosIA {
    const val MI_LLAVE = "AIzaSyCcZTsOCkF6dpM-eTZ-DstBsCdGRq_YWcg"
}

object AiTaskService {

    suspend fun procesarTextoConGemini(client: HttpClient, textoEscuchado: String): TareaIA? {
        val apiKey = DatosIA.MI_LLAVE
        val ahora = LocalDateTime.now()
        val fechaHoy = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(ahora)
        val horaHoy = DateTimeFormatter.ofPattern("HH:mm").format(ahora)
        val anyoHoy = DateTimeFormatter.ofPattern("yyyy").format(ahora)

        val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash-lite:generateContent?key=$apiKey"

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Eres un experto en extracción de datos.
                                    CONTEXTO: Hoy es $fechaHoy, la hora actual es $horaHoy y el año es $anyoHoy.
                                    TAREA: Extraer la información de: "$textoEscuchado"
                                    ... (Copia aquí el resto de tus instrucciones del prompt original) ...
                                """.trimIndent())
                            }
                        }
                    }
                }
            }.toString())
        }

        if (response.status.isSuccess()) {
            val responseBody = response.bodyAsText()
            val jsonElement = Json.parseToJsonElement(responseBody)
            val textoIA = jsonElement.jsonObject["candidates"]
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content ?: ""

            val jsonLimpio = textoIA.replace("```json", "").replace("```", "").trim()
            return Json.decodeFromString<TareaIA>(jsonLimpio)
        }
        return null
    }
}