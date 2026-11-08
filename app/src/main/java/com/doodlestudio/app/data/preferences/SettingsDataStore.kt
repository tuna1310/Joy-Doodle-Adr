package com.doodlestudio.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "doodle_settings")

class SettingsDataStore(private val context: Context) {

    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: true
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "auto"
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}

