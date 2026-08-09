package com.example.mistareasapp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Auth ---

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(val username: String, val id: Int = 0)

@Serializable
data class MeResponse(val user: MeUser)

@Serializable
data class MeUser(val id: Int, val username: String, @SerialName("nombreVisible") val nombreVisible: String? = null)

// --- Categorías ---

@Serializable
data class CategoriaDto(
    val id: Long,
    val titulo: String,
    val icono: String = "list",
    @SerialName("fecha_creacion") val fechaCreacion: String? = null,
    val activa: Boolean = true
)

@Serializable
data class CategoriaCreateRequest(val titulo: String, val icono: String = "list")

@Serializable
data class CategoriaUpdateRequest(
    val titulo: String? = null,
    val icono: String? = null,
    val activa: Boolean? = null
)

// --- Tareas ---

@Serializable
data class TareaDto(
    val id: Long,
    val titulo: String,
    val descripcion: String? = null,
    @SerialName("esta_completada") val estaCompletada: Boolean = false,
    val prioridad: String = "MEDIA",
    @SerialName("fecha_creacion") val fechaCreacion: String? = null,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    @SerialName("hora_limite") val horaLimite: String? = null,
    @SerialName("categoria_id") val categoriaId: Long? = null,
    val repeticion: String = "Sin repetición",
    @SerialName("pendiente_clasificar") val pendienteClasificar: Boolean = false,
    @SerialName("repeticion_fin") val repeticionFin: String? = null,
    @SerialName("repeticion_veces") val repeticionVeces: Int? = null,
    @SerialName("repeticion_contador") val repeticionContador: Int = 0
)

@Serializable
data class TareaCreateRequest(
    val titulo: String,
    val descripcion: String? = null,
    val prioridad: String = "MEDIA",
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    @SerialName("hora_limite") val horaLimite: String? = null,
    @SerialName("categoria_id") val categoriaId: Long? = null,
    val repeticion: String = "Sin repetición",
    @SerialName("pendiente_clasificar") val pendienteClasificar: Boolean = false,
    @SerialName("repeticion_fin") val repeticionFin: String? = null,
    @SerialName("repeticion_veces") val repeticionVeces: Int? = null
)

@Serializable
data class TareaUpdateRequest(
    val titulo: String? = null,
    val descripcion: String? = null,
    val prioridad: String? = null,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    @SerialName("hora_limite") val horaLimite: String? = null,
    @SerialName("categoria_id") val categoriaId: Long? = null,
    val repeticion: String? = null,
    @SerialName("pendiente_clasificar") val pendienteClasificar: Boolean? = null,
    @SerialName("esta_completada") val estaCompletada: Boolean? = null,
    @SerialName("repeticion_fin") val repeticionFin: String? = null,
    @SerialName("repeticion_veces") val repeticionVeces: Int? = null,
    @SerialName("repeticion_contador") val repeticionContador: Int? = null
)

@Serializable
data class CompletarRequest(@SerialName("esta_completada") val estaCompletada: Boolean)

// --- Wrappers de respuesta ---

@Serializable
data class DataResponse<T>(val data: T)

@Serializable
data class DeleteResponse(val data: DeleteResult)

@Serializable
data class DeleteResult(val deleted: Boolean, val id: Long)

@Serializable
data class ErrorResponse(val error: String)
