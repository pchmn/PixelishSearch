package com.pchmn.pixelishsearch.search.apps.data

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pchmn.pixelishsearch.data.hiddenAppsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Persists the set of package names the user has opted out of the recent
 * suggestions for. Hidden apps remain searchable — they're only filtered
 * out of the default (query-blank) suggestion strip.
 */
class HiddenAppsRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.applicationContext.hiddenAppsDataStore
    private val key = stringSetPreferencesKey("packages")

    val hidden: StateFlow<Set<String>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[key].orEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    suspend fun hide(packageName: String) {
        if (packageName.isBlank()) return
        dataStore.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() + packageName
        }
    }

    suspend fun unhide(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[key].orEmpty()
            if (packageName !in current) return@edit
            prefs[key] = current - packageName
        }
    }
}
