package com.pchmn.pixelishsearch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SearchHistoryEntry(
    val query: String,
    val lastSearchEpochMillis: Long,
)

/**
 * Persists queries run through Google Search.
 * Deduplicated by query (case-insensitive): re-running a query just updates
 * the timestamp.
 */
object SearchHistoryRepository {

    private const val MAX_ENTRIES = 50

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")
    private val HISTORY_KEY = stringSetPreferencesKey("queries")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: DataStore<Preferences>

    private val _history = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    val history: StateFlow<List<SearchHistoryEntry>> = _history.asStateFlow()

    fun init(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.dataStore
        scope.launch {
            dataStore.data.collect { prefs ->
                _history.value = (prefs[HISTORY_KEY] ?: emptySet())
                    .mapNotNull(::decode)
                    .sortedByDescending { it.lastSearchEpochMillis }
            }
        }
    }

    fun record(query: String) {
        if (!::dataStore.isInitialized) return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            dataStore.edit { prefs ->
                val current = (prefs[HISTORY_KEY] ?: emptySet())
                    .mapNotNull(::decode)
                    .associateBy { it.query.lowercase() }
                    .toMutableMap()
                current[trimmed.lowercase()] = SearchHistoryEntry(
                    query = trimmed,
                    lastSearchEpochMillis = System.currentTimeMillis(),
                )
                prefs[HISTORY_KEY] = current.values
                    .sortedByDescending { it.lastSearchEpochMillis }
                    .take(MAX_ENTRIES)
                    .map(::encode)
                    .toSet()
            }
        }
    }

    private fun encode(entry: SearchHistoryEntry): String {
        val q = URLEncoder.encode(entry.query, StandardCharsets.UTF_8.name())
        return "${entry.lastSearchEpochMillis}|$q"
    }

    private fun decode(raw: String): SearchHistoryEntry? {
        val sep = raw.indexOf('|')
        if (sep <= 0) return null
        val ts = raw.substring(0, sep).toLongOrNull() ?: return null
        val q = runCatching {
            URLDecoder.decode(raw.substring(sep + 1), StandardCharsets.UTF_8.name())
        }.getOrNull() ?: return null
        if (q.isBlank()) return null
        return SearchHistoryEntry(q, ts)
    }
}
