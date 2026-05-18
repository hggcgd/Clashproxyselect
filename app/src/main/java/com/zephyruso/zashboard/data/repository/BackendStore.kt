package com.zephyruso.zashboard.data.repository

import android.content.Context
import com.zephyruso.zashboard.data.model.Backend
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.backendDataStore by preferencesDataStore(name = "cps_backend")

class BackendStore(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val backendKey = stringPreferencesKey("current_backend")

    val backendFlow: Flow<Backend?> = context.backendDataStore.data.map { preferences ->
        preferences[backendKey]?.let { encoded ->
            runCatching { json.decodeFromString(Backend.serializer(), encoded) }.getOrNull()
        }
    }

    suspend fun save(backend: Backend) {
        context.backendDataStore.edit { preferences ->
            preferences[backendKey] = json.encodeToString(backend)
        }
    }

    suspend fun clear() {
        context.backendDataStore.edit { preferences ->
            preferences.remove(backendKey)
        }
    }
}
