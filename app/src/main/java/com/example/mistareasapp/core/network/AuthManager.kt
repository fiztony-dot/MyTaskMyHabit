package com.example.mistareasapp.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Gestiona el almacenamiento y recuperación del token JWT.
 * Usa DataStore (cifrado por defecto en disco interno de la app).
 */
object AuthManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USERNAME_KEY = stringPreferencesKey("username")

    fun tokenFlow(context: Context): Flow<String?> =
        context.authDataStore.data.map { prefs -> prefs[TOKEN_KEY] }

    suspend fun getToken(context: Context): String? =
        context.authDataStore.data.first()[TOKEN_KEY]

    suspend fun getUsername(context: Context): String? =
        context.authDataStore.data.first()[USERNAME_KEY]

    suspend fun saveSession(context: Context, token: String, username: String) {
        context.authDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USERNAME_KEY] = username
        }
    }

    suspend fun clearSession(context: Context) {
        context.authDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USERNAME_KEY)
        }
    }

    suspend fun isLoggedIn(context: Context): Boolean =
        getToken(context) != null
}
