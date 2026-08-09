package com.example.mistareasapp.core.network

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Servicio que encapsula todas las llamadas a la API REST de Tareas.
 */
object TareasApiService {

    private val client get() = ApiClient.client

    // ═══════════════════════════════════════════
    //                AUTH
    // ═══════════════════════════════════════════

    suspend fun login(username: String, password: String): LoginResponse {
        val response = client.post("/auth/login") {
            setBody(LoginRequest(username, password))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException(401, "Usuario o contraseña incorrectos")
        }
        if (!response.status.isSuccess()) {
            throw ApiException(response.status.value, "Error de login: ${response.status}")
        }
        return response.body()
    }

    suspend fun me(): MeResponse {
        val response = client.get("/auth/me")
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException(401, "Token inválido o expirado")
        }
        return response.body()
    }

    // ═══════════════════════════════════════════
    //             CATEGORÍAS
    // ═══════════════════════════════════════════

    suspend fun getCategorias(): List<CategoriaDto> {
        val response = client.get("/api/categorias")
        checkResponse(response.status)
        return response.body<DataResponse<List<CategoriaDto>>>().data
    }

    suspend fun createCategoria(titulo: String, icono: String = "list"): CategoriaDto {
        val response = client.post("/api/categorias") {
            setBody(CategoriaCreateRequest(titulo, icono))
        }
        checkResponse(response.status)
        return response.body<DataResponse<CategoriaDto>>().data
    }

    suspend fun updateCategoria(id: Long, titulo: String?, icono: String?, activa: Boolean?): CategoriaDto {
        val response = client.put("/api/categorias/$id") {
            setBody(CategoriaUpdateRequest(titulo, icono, activa))
        }
        checkResponse(response.status)
        return response.body<DataResponse<CategoriaDto>>().data
    }

    suspend fun deleteCategoria(id: Long) {
        val response = client.delete("/api/categorias/$id")
        checkResponse(response.status)
    }

    // ═══════════════════════════════════════════
    //               TAREAS
    // ═══════════════════════════════════════════

    suspend fun getTareas(pendientes: Boolean = false): List<TareaDto> {
        val response = client.get("/api/tareas") {
            if (pendientes) parameter("pendientes", "true")
        }
        checkResponse(response.status)
        return response.body<DataResponse<List<TareaDto>>>().data
    }

    suspend fun getTarea(id: Long): TareaDto {
        val response = client.get("/api/tareas/$id")
        checkResponse(response.status)
        return response.body<DataResponse<TareaDto>>().data
    }

    suspend fun createTarea(request: TareaCreateRequest): TareaDto {
        val response = client.post("/api/tareas") {
            setBody(request)
        }
        if (response.status != HttpStatusCode.Created && !response.status.isSuccess()) {
            throw ApiException(response.status.value, "Error creando tarea")
        }
        return response.body<DataResponse<TareaDto>>().data
    }

    suspend fun updateTarea(id: Long, request: TareaUpdateRequest): TareaDto {
        val response = client.put("/api/tareas/$id") {
            setBody(request)
        }
        checkResponse(response.status)
        return response.body<DataResponse<TareaDto>>().data
    }

    suspend fun completarTarea(id: Long, completada: Boolean): TareaDto {
        val response = client.patch("/api/tareas/$id/completar") {
            setBody(CompletarRequest(completada))
        }
        checkResponse(response.status)
        return response.body<DataResponse<TareaDto>>().data
    }

    suspend fun deleteTarea(id: Long) {
        val response = client.delete("/api/tareas/$id")
        checkResponse(response.status)
    }

    // ═══════════════════════════════════════════

    private fun checkResponse(status: HttpStatusCode) {
        if (status == HttpStatusCode.Unauthorized) {
            throw ApiException(401, "No autorizado — inicia sesión de nuevo")
        }
        if (!status.isSuccess()) {
            throw ApiException(status.value, "Error del servidor: $status")
        }
    }
}

class ApiException(val code: Int, message: String) : Exception(message)
