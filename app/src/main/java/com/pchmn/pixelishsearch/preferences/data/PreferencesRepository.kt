package com.pchmn.pixelishsearch.preferences.data

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * User preferences.
 *
 * Tile visibility is persisted as the *disabled* set — empty means all tiles
 * visible. This way, tiles added to `settingsTiles` in a future release are
 * automatically visible to existing users (no data migration). See
 * `docs/adr/0001-store-disabled-tile-ids.md`.
 */
class PreferencesRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.applicationContext.preferencesDataStore
    private val contactSearchKey = booleanPreferencesKey("contact_search_enabled")
    private val disabledTilesKey = stringSetPreferencesKey("disabled_tile_ids")

    val contactSearchEnabled: StateFlow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[contactSearchKey] ?: false }
        .stateIn(scope, SharingStarted.Companion.Eagerly, false)

    val disabledTileIds: StateFlow<Set<String>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[disabledTilesKey] ?: emptySet() }
        .stateIn(scope, SharingStarted.Companion.Eagerly, emptySet())

    suspend fun setContactSearchEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[contactSearchKey] = enabled }
    }

    suspend fun setTileEnabled(id: SettingsTileId, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[disabledTilesKey] ?: emptySet()
            prefs[disabledTilesKey] = if (enabled) current - id.name else current + id.name
        }
    }
}
