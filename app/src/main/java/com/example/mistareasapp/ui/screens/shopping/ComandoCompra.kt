package com.example.mistareasapp.ui.screens.shopping

import android.content.Context
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.shopping.ListaItem
import com.example.mistareasapp.data.shopping.ListaProducto
import java.text.Normalizer

data class ResultadoComandoCompra(
    val lugarNombre: String,
    val nombresAñadidos: List<String>,
    val duplicados: Int
)

class ComandoCompra(private val context: Context) {

    suspend fun procesar(texto: String): Result<ResultadoComandoCompra> {
        if (texto.isBlank()) return Result.failure(IllegalArgumentException("Texto vacío"))
        if (texto.length > 300) return Result.failure(IllegalArgumentException("Texto demasiado largo"))

        val dao = AppDatabase.getDatabase(context.applicationContext).listaCompraDao()
        val lugares = dao.obtenerLugaresSync()
        val productos = dao.obtenerTodosProductosSync()

        val textoTrimmed = texto.trim()
        val primeraPalabra = textoTrimmed.substringBefore(" ")
        val lugarDetectado = lugares.firstOrNull { normalizar(it.nombre) == normalizar(primeraPalabra) }

        val lugarDestino = lugarDetectado
            ?: lugares.firstOrNull { it.esDefault }
            ?: lugares.firstOrNull()
            ?: return Result.failure(Exception("No hay lugares definidos"))

        val textoProductos = if (lugarDetectado != null) textoTrimmed.substringAfter(" ").trim()
                             else textoTrimmed
        val nombresProductos = textoProductos
            .split(Regex("\\s+y\\s+", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (nombresProductos.isEmpty()) return Result.failure(Exception("Sin productos reconocidos"))

        val itemsExistentes = dao.obtenerItemsPendientesSync(lugarDestino.id)
        val productosIdsEnLista = itemsExistentes.map { it.productoId }.toSet()

        val añadidos = mutableListOf<String>()
        var duplicados = 0

        nombresProductos.forEach { nombre ->
            val nombreCap = nombre.trim().replaceFirstChar { it.uppercase() }
            val normalizado = normalizar(nombre)
            val existente = productos.firstOrNull { p ->
                normalizar(p.nombre) == normalizado ||
                p.aliases.split(",").any { normalizar(it.trim()) == normalizado }
            }
            val resolvedId = existente?.id ?: dao.insertarProducto(
                ListaProducto(nombre = nombreCap, categoriaId = null)
            )
            if (existente != null && resolvedId in productosIdsEnLista) {
                duplicados++
                return@forEach
            }
            dao.insertarItem(
                ListaItem(productoId = resolvedId, lugarId = lugarDestino.id, cantidad = "1", unidad = "")
            )
            añadidos += nombreCap
        }

        return Result.success(ResultadoComandoCompra(lugarDestino.nombre, añadidos, duplicados))
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
