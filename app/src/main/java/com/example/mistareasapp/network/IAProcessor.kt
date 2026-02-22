package com.example.mistareasapp.network

import android.util.Log
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

// 1. Definición de modelos para la IA (SIN asteriscos)
@Serializable
data class IAResultTarea(
    val titulo: String,
    val fecha: String? = null,
    val hora: String? = null,
    val prioridad: String = "Media",
    val categoria: String = "General"
)

@Serializable
data class IAResultHabito(
    val nombre: String,
    val frecuencia: String,
    val objetivo: String
)

enum class TipoEntrada { TAREA, HABITO }

// 2. El procesador que hace la llamada a Gemini
object IAProcessor {
    // 1. Definimos la clave primero como una constante
    //private const val API_KEY = "AIzaSyANEu3pDzqEtBAUPCzjAT44UgwNYX76ENg"
    private const val API_KEY = "AIzaSyBRltwfm7SaPzQpChZ_4gV5zTuxu4aAftM"
    private const val URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Esto evita que la app explote si Google manda campos nuevos
                isLenient = true
            })
        }
    }

    suspend fun procesarTexto(textoEscuchado: String, tipo: TipoEntrada): String? {
        val ahora = LocalDateTime.now()
        val fechaHoy = ahora.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val horaHoy = ahora.format(DateTimeFormatter.ofPattern("HH:mm"))
        val anyoHoy = ahora.year.toString()
        val fechaManana = LocalDate.now().plusDays(1).toString()

        val prompt = when (tipo) {
            TipoEntrada.TAREA -> {
                """
                Eres un experto en extracción de datos.
                CONTEXTO: Hoy es $fechaHoy, la hora actual es $horaHoy y el año es $anyoHoy.
                                           
                TAREA: Extraer la información de: "$textoEscuchado"
                                            
                INSTRUCCIONES CRÍTICAS:
                1. "fecha":
                   - Si dice "mañana", usa exactamente: ${java.time.LocalDate.now().plusDays(1)}.
                   - Si menciona día/mes, usa el año $anyoHoy y devuélvelo como YYYY-MM-DD.
                   - Si dice "en X minutos/horas" o solo indica una hora, la fecha es $fechaHoy.
                   - Solo si es una tarea sin ninguna referencia de tiempo, pon null.
                2. "hora":
                   - Si dice "en X minutos", calcula la hora sumando a $horaHoy.
                   - Si no especifica hora, pon null.
                   - Formato HH:mm (24h).
                3. JSON (estrictamente numérico):
                {
                  "titulo": "acción sin palabras temporales",
                  "fecha": "YYYY-MM-DD o null",
                  "hora": "HH:mm o null",
                  "prioridad": "ALTA|MEDIA|BAJA"
                }
            
                EJEMPLOS:
                - "Mañana a las 5": {"fecha": "${java.time.LocalDate.now().plusDays(1)}", "hora": "17:00"}
                - "En 2 min": {"fecha": "$fechaHoy", "hora": "Calcula según $horaHoy"}
                """.trimIndent()
            }
            TipoEntrada.HABITO -> {
                """
                Analiza: "${'$'}textoEscuchado"
                Genera un JSON con: {"nombre": "...", "frecuencia": "Diaria/Semanal", "objetivo": "..."}
                Responde solo el JSON sin formato markdown.
                """.trimIndent()
            }
        }

        return try {
            val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash-lite:generateContent?key=$API_KEY"

            val response: io.ktor.client.statement.HttpResponse = client.post(url) {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(buildJsonObject {
                    putJsonArray("contents") {
                        addJsonObject {
                            putJsonArray("parts") {
                                addJsonObject {
                                    put("text", prompt)
                                }
                            }
                        }
                    }
                })
            }

            val responseBody = response.bodyAsText()
            Log.d("IA_DEBUG", "🔵 RESPUESTA RECIBIDA - Status: ${response.status.value}, Body: $responseBody")

            val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

            // Validación: verificar si hay error en la respuesta
            if (jsonResponse.containsKey("error")) {
                val errorObj = jsonResponse["error"]?.jsonObject
                val errorMessage = errorObj?.get("message")?.jsonPrimitive?.content ?: "Error desconocido"
                val errorStatus = errorObj?.get("status")?.jsonPrimitive?.content ?: "UNKNOWN"
                Log.e("IA_DEBUG", "❌ ERROR DE API GOOGLE [$errorStatus]: $errorMessage")
                return null
            }

            Log.d("IA_DEBUG", "✅ Respuesta exitosa, buscando candidates...")

            val textoExtraido = jsonResponse["candidates"]?.jsonArray?.getOrNull(0)
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.getOrNull(0)
                ?.jsonObject?.get("text")?.jsonPrimitive?.content

            Log.d("IA_DEBUG", "📄 Texto extraído: $textoExtraido")

            // 3. LIMPIEZA PARA ASEGURAR EL PARSEO
            val jsonLimpio = textoExtraido?.let { texto ->
                Log.d("IA_DEBUG", "🔍 Buscando JSON en texto...")
                val inicio = texto.indexOf("{")
                val fin = texto.lastIndexOf("}")
                Log.d("IA_DEBUG", "  Inicio: $inicio, Fin: $fin")
                if (inicio != -1 && fin != -1 && fin > inicio) {
                    val resultado = texto.substring(inicio, fin + 1)
                    Log.d("IA_DEBUG", "✨ JSON encontrado: $resultado")
                    resultado
                } else {
                    Log.e("IA_DEBUG", "❌ No se encontró JSON en: $texto")
                    null
                }
            }

            Log.d("IA_DEBUG", "🎯 JSON FINAL A RETORNAR: $jsonLimpio")
            jsonLimpio

        } catch (e: Exception) {
            Log.e("IA_DEBUG", "💥 EXCEPCIÓN EN IAProcessor: ${e::class.simpleName} - ${e.message}")
            Log.e("IA_DEBUG", "Stack trace: ", e)
            null
        }
    }
}
