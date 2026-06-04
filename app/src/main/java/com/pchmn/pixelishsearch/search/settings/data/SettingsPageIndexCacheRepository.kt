package com.pchmn.pixelishsearch.search.settings.data

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.settingsPageIndexCacheDataStore by preferencesDataStore(name = "settings_page_index_cache")

/**
 * Persisted snapshot of [SettingsPageIndex]. Lets cold start hydrate the
 * Settings sub-pages — and therefore the blank-state recent-settings block (see
 * [SearchViewModel][com.pchmn.pixelishsearch.search.ui.SearchViewModel]) — on
 * the first frame, without loading the Settings `.arsc` + its RRO overlay
 * cascade. The expensive re-discovery runs in the background after the first
 * frame and rewrites this cache. See ADR-0009.
 *
 * [component] is the [ComponentName] flattened via [ComponentName.flattenToString].
 */
@Serializable
data class CachedSettingsPageEntry(
    val label: String,
    val component: String,
)

class SettingsPageIndexCacheRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsPageIndexCacheDataStore
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CachedSettingsPageEntry.serializer())

    suspend fun read(): List<CachedSettingsPageEntry> {
        val raw = dataStore.data.first()[key] ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun write(entries: List<CachedSettingsPageEntry>) {
        val encoded = json.encodeToString(serializer, entries)
        dataStore.edit { it[key] = encoded }
    }
}

/** Returns null if the persisted component string no longer parses (skip it). */
fun CachedSettingsPageEntry.toEntry(): SettingsPageEntry? {
    val cn = ComponentName.unflattenFromString(component) ?: return null
    return SettingsPageEntry(label, cn)
}

fun SettingsPageEntry.toCached(): CachedSettingsPageEntry =
    CachedSettingsPageEntry(label, component.flattenToString())
