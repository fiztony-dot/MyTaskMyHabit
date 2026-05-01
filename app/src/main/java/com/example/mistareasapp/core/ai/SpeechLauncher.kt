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
import com.example.mistareasapp.core.voice.VozTaskParser
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                scope.launch {
                    try {
                        Log.d("VOZ_DEBUG", "Entrada de voz: '$textoEscuchado'")
                        guardarTareaDesdeParser(textoEscuchado, viewModel, context)
                    } catch (e: Exception) {
                        Log.e("VOZ_DEBUG", "Error procesando voz: ${e.message}", e)
                        guardarTareaDesdeParser(textoEscuchado, viewModel, context)
                    }
                }
            }
        }
    }
}

suspend fun guardarTareaDesdeParser(
    texto: String,
    viewModel: TareasViewModel,
    context: Context
) {
    val resultado = VozTaskParser.parse(texto)

    withContext(Dispatchers.Main) {
        val tarea = Tarea(
            titulo = resultado.titulo,
            descripcion = "Creada por voz",
            prioridad = resultado.prioridad,
            fechaLimite = resultado.fechaLimite,
            horaLimite = resultado.horaLimite
        )

        viewModel.insertar(tarea)

        val resumen = buildString {
            append("Tarea creada")
            if (resultado.fechaLimite != null) {
                append(" · ${resultado.fechaLimite}")
            }
            if (resultado.horaLimite != null) {
                append(" · ${resultado.horaLimite}")
            }
        }

        Toast.makeText(context, resumen, Toast.LENGTH_SHORT).show()
    }
}
