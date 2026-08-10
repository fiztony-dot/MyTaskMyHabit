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

/**
 * Composable que actúa como puerta de entrada:
 * - Si hay un token guardado en DataStore, confía en él y muestra la app directamente
 *   (sin verificar contra la API — el token tiene 30 días de expiración).
 * - Si no hay token → muestra login.
 * - Si un endpoint devuelve 401 durante el uso, el ViewModel/ApiService limpiará
 *   el token y la app redirigirá al login.
 */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }

    LaunchedEffect(Unit) {
        val token = AuthManager.getToken(context)
        if (token != null) {
            // Confiar en el token local — no verificar contra la API en cada arranque
            ApiClient.updateToken(token)
            authState = AuthState.Authenticated
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
