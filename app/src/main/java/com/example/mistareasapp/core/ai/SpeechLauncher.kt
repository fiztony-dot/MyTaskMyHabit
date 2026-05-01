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
import com.example.mistareasapp.data.tasks.Prioridad
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
                        guardarTareaSimple(textoEscuchado, viewModel, context)
                    } catch (e: Exception) {
                        Log.e("VOZ_DEBUG", "Error procesando voz: ${e.message}", e)
                        guardarTareaSimple(textoEscuchado, viewModel, context)
                    }
                }
            }
        }
    }
}

suspend fun guardarTareaSimple(
    texto: String,
    viewModel: TareasViewModel,
    context: Context
) {
    withContext(Dispatchers.Main) {
        val tareaBasica = Tarea(
            titulo = texto.replaceFirstChar { it.uppercase() },
            descripcion = "Creada por voz",
            prioridad = Prioridad.MEDIA
        )
        viewModel.insertar(tareaBasica)
        Toast.makeText(context, "Tarea creada por voz", Toast.LENGTH_SHORT).show()
    }
}
