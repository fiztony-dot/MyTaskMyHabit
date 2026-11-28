package com.example.mistareasapp.ui.screens.tasks

import android.content.Context
import com.example.mistareasapp.core.network.ApiClient
import com.example.mistareasapp.core.network.TareasApiRepository
import com.example.mistareasapp.core.voice.VozTaskParser
import com.example.mistareasapp.data.tasks.Tarea
import kotlinx.coroutines.withTimeout
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ResultadoComandoTarea(
    val titulo: String,
    val mensajeVoz: String
)

class ComandoTarea(private val context: Context) {

    suspend fun procesar(texto: String): Result<ResultadoComandoTarea> {
        if (texto.isBlank()) return Result.failure(IllegalArgumentException("Texto vacío"))
        if (texto.length > 300) return Result.failure(IllegalArgumentException("Texto demasiado largo"))

        return try {
            ApiClient.initFromStorage(context)
            withTimeout(8_000L) {
                val datos = VozTaskParser.parse(texto)
                TareasApiRepository.insertar(
                    Tarea(
                        titulo = datos.titulo,
                        descripcion = null,
                        prioridad = datos.prioridad,
                        fechaLimite = datos.fechaLimite,
                        horaLimite = datos.horaLimite,
                        pendienteClasificar = true
                    )
                )
                val mensajeVoz = construirMensajeVoz(datos.titulo, datos)
                Result.success(ResultadoComandoTarea(titulo = datos.titulo, mensajeVoz = mensajeVoz))
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(Exception("No se pudo crear la tarea, sin respuesta del servidor"))
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("connect", ignoreCase = true) == true ||
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "No hay conexión"
                else -> "No se pudo crear la tarea"
            }
            Result.failure(Exception(msg))
        }
    }

    private fun construirMensajeVoz(titulo: String, datos: com.example.mistareasapp.core.voice.VozTaskData): String {
        val partes = mutableListOf("Tarea creada: $titulo")
        if (datos.fechaLimite != null) {
            val fechaStr = datos.fechaLimite.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es")))
            if (datos.horaLimite != null) {
                val horaStr = datos.horaLimite.format(DateTimeFormatter.ofPattern("H:mm"))
                partes.add("$fechaStr a las $horaStr")
            } else {
                partes.add(fechaStr)
            }
        }
        return partes.joinToString(", ")
    }
}
