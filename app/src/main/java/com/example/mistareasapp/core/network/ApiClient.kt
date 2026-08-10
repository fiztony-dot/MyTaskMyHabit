package com.example.mistareasapp.core.network

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP configurado para la API de MyTaskMyHabit.
 * Incluye inyección automática del token JWT en cada petición.
 */
object ApiClient {

    // URL base configurable — apunta al backend en Render
    // En desarrollo local se puede cambiar a http://10.0.2.2:10000 (emulador)
    const val BASE_URL = "https://mytaskmyhabit-api.onrender.com"

    private var cachedToken: String? = null

    fun updateToken(token: String?) {
        cachedToken = token
    }

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
        }

        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
            cachedToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    /** Carga el token desde DataStore al arrancar la app */
    suspend fun initFromStorage(context: Context) {
        cachedToken = AuthManager.getToken(context)
    }
}
