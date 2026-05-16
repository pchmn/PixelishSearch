package com.pchmn.pixelishsearch.settings.data

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.pchmn.pixelishsearch.data.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * User preferences. For now only stores whether contact search is enabled.
 * Default is false: the user must opt in explicitly (which also triggers the
 * READ_CONTACTS permission request).
 */
class SettingsRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val contactSearchKey = booleanPreferencesKey("contact_search_enabled")

    val contactSearchEnabled: StateFlow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[contactSearchKey] ?: false }
        .stateIn(scope, SharingStarted.Companion.Eagerly, false)

    suspend fun setContactSearchEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[contactSearchKey] = enabled }
    }
}