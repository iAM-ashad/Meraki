package com.iamashad.meraki.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.dataStore by preferencesDataStore(name = "settings")

object ThemePreference {
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_enabled")

    // Save dynamic color preference
    suspend fun setDynamicColor(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    // Observe dynamic color preference
    fun isDynamicColorEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: false // Default to false (disabled)
        }
    }
}