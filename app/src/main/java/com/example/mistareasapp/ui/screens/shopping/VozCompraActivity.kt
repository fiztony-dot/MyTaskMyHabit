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
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.shopping.ListaItem
import com.example.mistareasapp.data.shopping.ListaProducto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

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
            val dao = AppDatabase.getDatabase(applicationContext).listaCompraDao()
            val lugares = dao.obtenerLugaresSync()
            val productos = dao.obtenerTodosProductosSync()

            val textoTrimmed = texto.trim()
            val primeraPalabra = textoTrimmed.substringBefore(" ")
            val lugarDetectado = lugares.firstOrNull { normalizar(it.nombre) == normalizar(primeraPalabra) }

            val lugarDestino = if (lugarDetectado != null) lugarDetectado
                               else lugares.firstOrNull { it.esDefault } ?: lugares.firstOrNull()
            if (lugarDestino == null) { withContext(Dispatchers.Main) { finish() }; return@launch }

            val textoProductos = if (lugarDetectado != null) textoTrimmed.substringAfter(" ").trim()
                                 else textoTrimmed
            val nombresProductos = textoProductos
                .split(Regex("\\s+y\\s+", RegexOption.IGNORE_CASE))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (nombresProductos.isEmpty()) { withContext(Dispatchers.Main) { finish() }; return@launch }

            val itemsExistentes = dao.obtenerItemsPendientesSync(lugarDestino.id)
            val productosIdsEnLista = itemsExistentes.map { it.productoId }.toSet()

            var añadidos = 0
            var duplicados = 0

            nombresProductos.forEach { nombre ->
                val nombreCap = capitalizar(nombre)
                val normalizado = normalizar(nombre)
                val existente = productos.firstOrNull { p ->
                    normalizar(p.nombre) == normalizado ||
                    p.aliases.split(",").any { normalizar(it.trim()) == normalizado }
                }
                val esNuevo = existente == null
                val resolvedId = existente?.id ?: dao.insertarProducto(
                    ListaProducto(nombre = nombreCap, categoriaId = null)
                )
                if (!esNuevo && resolvedId in productosIdsEnLista) {
                    duplicados++
                    return@forEach
                }
                dao.insertarItem(
                    ListaItem(productoId = resolvedId, lugarId = lugarDestino.id, cantidad = "1", unidad = "")
                )
                añadidos++
            }

            val mensaje = when {
                añadidos > 0 && duplicados > 0 -> "Añadidos $añadidos a ${lugarDestino.nombre} ($duplicados ya estaban)"
                añadidos > 0 -> "Añadidos $añadidos producto(s) a ${lugarDestino.nombre}"
                else -> "Todos los productos ya estaban en ${lugarDestino.nombre}"
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@VozCompraActivity, mensaje, Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)
            }
        }
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    private fun capitalizar(texto: String): String =
        texto.trim().replaceFirstChar { it.uppercase() }
}
