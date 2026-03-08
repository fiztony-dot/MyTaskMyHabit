package com.example.mistareasapp.core.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.ui.navigation.Rutas
import com.example.mistareasapp.core.notifications.tasks.NotificationHelper
import com.example.mistareasapp.network.IAProcessor
import com.example.mistareasapp.network.IAResultTarea
import com.example.mistareasapp.network.TipoEntrada
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime


@Composable
fun crearSpeechLauncher(
    navController: NavController,
    viewModel: TareasViewModel,
    context: Context,
    scope: CoroutineScope
): ManagedActivityResultLauncher<Intent, ActivityResult> {

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.get(0)?.let { textoEscuchado ->

                val tipo = if (navController.currentBackStackEntry?.destination?.route == Rutas.PantallaHabitos.ruta) {
                    TipoEntrada.HABITO
                } else {
                    TipoEntrada.TAREA
                }

                scope.launch {
                    try {
                        Log.d("IA_DEBUG", "🎤 ENTRADA: texto='$textoEscuchado', tipo=$tipo")
                        val jsonResultado = IAProcessor.procesarTexto(textoEscuchado, tipo)

                        Log.d("IA_DEBUG", """
                            ✅ RESULTADO DE IA:
                            TEXTO: "$textoEscuchado"
                            TIPO: $tipo
                            JSON OBTENIDO: $jsonResultado
                        """.trimIndent())

                        when (tipo) {
                            TipoEntrada.TAREA -> {
                                if (jsonResultado != null) {
                                    Log.d("IA_DEBUG", "📋 Intentando parsear como TAREA...")
                                    val objetoTarea = Json.decodeFromString<IAResultTarea>(jsonResultado)
                                    Log.d("IA_DEBUG", "✨ Tarea parseada: ${objetoTarea.titulo}")

                                    val nuevaTarea = Tarea(
                                        id = 0,
                                        titulo = objetoTarea.titulo,
                                        descripcion = textoEscuchado,
                                        prioridad = when (objetoTarea.prioridad.uppercase()) {
                                            "ALTA" -> Prioridad.ALTA
                                            "BAJA" -> Prioridad.BAJA
                                            else -> Prioridad.MEDIA
                                        },
                                        fechaLimite = objetoTarea.fecha?.let { LocalDate.parse(it) },
                                        horaLimite = objetoTarea.hora?.let { LocalTime.parse(it) },
                                        fechaCreacion = System.currentTimeMillis()
                                    )

                                    viewModel.insertar(nuevaTarea)
                                    NotificationHelper.programarNotificacion(context, nuevaTarea)
                                    Toast.makeText(context, "Tarea: ${objetoTarea.titulo}", Toast.LENGTH_SHORT).show()

                                } else {
                                    Log.w("IA_DEBUG", "⚠️ jsonResultado es NULL, llamando guardarTareaSimple")
                                    guardarTareaSimple(textoEscuchado, viewModel, context)
                                }
                            }

                            TipoEntrada.HABITO -> {
                                Toast.makeText(context, "Hábito detectado: $textoEscuchado", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("IA_DEBUG", "Error procesando voz: ${e.message}")
                        guardarTareaSimple(textoEscuchado, viewModel, context)
                    }
                }
            }
        }
    }
}
public suspend fun guardarTareaSimple(texto: String, viewModel: TareasViewModel, context: android.content.Context) {
    Log.d("IA_DEBUG", "💾 GUARDADO SIMPLE: '$texto'")
    withContext(Dispatchers.Main) {
        val tareaBasica = Tarea(
            titulo = texto.replaceFirstChar { it.uppercase() },
            descripcion = "Voz (IA no disponible)",
            prioridad = Prioridad.MEDIA
        )
        viewModel.insertar(tareaBasica)
        Toast.makeText(context, "Guardado simple (IA falló)", Toast.LENGTH_SHORT).show()
    }
}

