package com.example.mistareasapp.core.network

import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Repositorio que traduce entre los modelos de la API (DTOs) y los modelos
 * locales de la app (Tarea, Categoria). Mantiene el mismo contrato que usaban
 * los DAOs de Room, para minimizar cambios en el ViewModel.
 */
object TareasApiRepository {

    // ═══════════════════════════════════════════
    //             CATEGORÍAS
    // ═══════════════════════════════════════════

    suspend fun obtenerCategorias(): List<Categoria> {
        return TareasApiService.getCategorias().map { it.toCategoria() }
    }

    suspend fun insertarCategoria(categoria: Categoria): Categoria {
        val dto = TareasApiService.createCategoria(categoria.titulo, categoria.icono)
        return dto.toCategoria()
    }

    suspend fun actualizarCategoria(categoria: Categoria) {
        TareasApiService.updateCategoria(
            id = categoria.id.toLong(),
            titulo = categoria.titulo,
            icono = categoria.icono,
            activa = categoria.activa
        )
    }

    suspend fun eliminarCategoria(categoria: Categoria) {
        TareasApiService.deleteCategoria(categoria.id.toLong())
    }

    // ═══════════════════════════════════════════
    //               TAREAS
    // ═══════════════════════════════════════════

    suspend fun obtenerTodas(): List<Tarea> {
        return TareasApiService.getTareas().map { it.toTarea() }
    }

    suspend fun obtenerPendientes(): List<Tarea> {
        return TareasApiService.getTareas(pendientes = true).map { it.toTarea() }
    }

    suspend fun obtenerTareaPorId(id: Int): Tarea? {
        return try {
            TareasApiService.getTarea(id.toLong()).toTarea()
        } catch (e: ApiException) {
            if (e.code == 404) null else throw e
        }
    }

    suspend fun insertar(tarea: Tarea): Long {
        val request = tarea.toCreateRequest()
        val dto = TareasApiService.createTarea(request)
        return dto.id
    }

    suspend fun actualizar(tarea: Tarea) {
        val request = tarea.toUpdateRequest()
        TareasApiService.updateTarea(tarea.id.toLong(), request)
    }

    suspend fun eliminar(tarea: Tarea) {
        TareasApiService.deleteTarea(tarea.id.toLong())
    }

    suspend fun completar(tarea: Tarea, completada: Boolean) {
        TareasApiService.completarTarea(tarea.id.toLong(), completada)
    }

    // ═══════════════════════════════════════════
    //            MAPPERS
    // ═══════════════════════════════════════════

    /** Mapa categoriaId → titulo, se actualiza al cargar categorías */
    private var categoriaMap: Map<Long, String> = emptyMap()

    fun actualizarMapaCategorias(categorias: List<Categoria>) {
        categoriaMap = categorias.associate { it.id.toLong() to it.titulo }
    }

    private fun CategoriaDto.toCategoria(): Categoria = Categoria(
        id = id.toInt(),
        titulo = titulo,
        icono = icono,
        fechaCreacion = parseTimestampToMillis(fechaCreacion),
        activa = activa
    )

    private fun TareaDto.toTarea(): Tarea = Tarea(
        id = id.toInt(),
        titulo = titulo,
        descripcion = descripcion,
        estaCompletada = estaCompletada,
        prioridad = try { Prioridad.valueOf(prioridad) } catch (_: Exception) { Prioridad.MEDIA },
        fechaCreacion = parseTimestampToMillis(fechaCreacion),
        fechaLimite = fechaLimite?.let { parseDate(it) },
        horaLimite = horaLimite?.let { parseTime(it) },
        categoria = categoriaId?.let { categoriaMap[it] },
        repeticion = repeticion,
        pendienteClasificar = pendienteClasificar,
        repeticionFin = repeticionFin?.let { parseDate(it) },
        repeticionVeces = repeticionVeces,
        repeticionContador = repeticionContador
    )

    private fun Tarea.toCreateRequest(): TareaCreateRequest {
        // Resolver categoriaId desde el nombre de categoría
        val catId = categoria?.let { nombre ->
            categoriaMap.entries.firstOrNull { it.value == nombre }?.key
        }
        return TareaCreateRequest(
            titulo = titulo,
            descripcion = descripcion,
            prioridad = prioridad.name,
            fechaLimite = fechaLimite?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            horaLimite = horaLimite?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            categoriaId = catId,
            repeticion = repeticion,
            pendienteClasificar = pendienteClasificar,
            repeticionFin = repeticionFin?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            repeticionVeces = repeticionVeces
        )
    }

    private fun Tarea.toUpdateRequest(): TareaUpdateRequest {
        val catId = categoria?.let { nombre ->
            categoriaMap.entries.firstOrNull { it.value == nombre }?.key
        }
        return TareaUpdateRequest(
            titulo = titulo,
            descripcion = descripcion,
            prioridad = prioridad.name,
            fechaLimite = fechaLimite?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            horaLimite = horaLimite?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            categoriaId = catId,
            repeticion = repeticion,
            pendienteClasificar = pendienteClasificar,
            estaCompletada = estaCompletada,
            repeticionFin = repeticionFin?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            repeticionVeces = repeticionVeces,
            repeticionContador = repeticionContador
        )
    }

    // --- Parsing helpers ---

    private fun parseTimestampToMillis(isoTimestamp: String?): Long {
        if (isoTimestamp == null) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(isoTimestamp).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            // API returns DATE as "YYYY-MM-DD" or full ISO timestamp
            LocalDate.parse(value.take(10))
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTime(value: String): LocalTime? {
        return try {
            LocalTime.parse(value)
        } catch (_: Exception) {
            null
        }
    }
}
