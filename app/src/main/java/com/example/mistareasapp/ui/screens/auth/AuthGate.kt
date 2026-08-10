package com.example.mistareasapp.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mistareasapp.core.network.ApiClient
import com.example.mistareasapp.core.network.AuthManager
import com.example.mistareasapp.core.network.TareasApiService
import kotlinx.coroutines.launch

/**
 * Composable que actúa como puerta de entrada:
 * - Si hay un token guardado, lo verifica contra la API (GET /auth/me).
 *   Si es válido → muestra la app. Si no → muestra login.
 * - Si no hay token → muestra login.
 */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }

    LaunchedEffect(Unit) {
        val token = AuthManager.getToken(context)
        if (token != null) {
            ApiClient.updateToken(token)
            // Verificar que el token sigue siendo válido
            try {
                TareasApiService.me()
                authState = AuthState.Authenticated
            } catch (e: Exception) {
                // Token inválido o expirado — limpiar y pedir login
                AuthManager.clearSession(context)
                ApiClient.updateToken(null)
                authState = AuthState.NotAuthenticated
            }
        } else {
            authState = AuthState.NotAuthenticated
        }
    }

    when (authState) {
        AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthState.NotAuthenticated -> {
            LoginScreen(
                onLoginSuccess = {
                    authState = AuthState.Authenticated
                }
            )
        }
        AuthState.Authenticated -> {
            content()
        }
    }
}

private enum class AuthState {
    Loading, NotAuthenticated, Authenticated
}
