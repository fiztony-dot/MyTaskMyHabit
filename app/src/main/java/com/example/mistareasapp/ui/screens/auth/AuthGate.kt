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
import kotlinx.coroutines.launch

/**
 * Composable que actúa como puerta de entrada:
 * - Si hay un token guardado, lo carga en ApiClient y muestra la app.
 * - Si no hay token, muestra la pantalla de login.
 *
 * Se usa como wrapper en MainActivity, envolviendo a MisTareasApp().
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
