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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Serializable
data class SearchHistoryEntry(
    val query: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry

class SearchHistoryRepository(context: Context) : HistoryRepository<SearchHistoryEntry, String>(
    dataStore = context.applicationContext.searchHistoryDataStore,
    serializer = SearchHistoryEntry.serializer(),
    maxEntries = 50,
) {
    override fun keyOf(item: SearchHistoryEntry) = item.query.lowercase()
    override fun withUpdatedMetadata(item: SearchHistoryEntry, timestamp: Long, count: Int) =
        item.copy(lastUsedEpochMillis = timestamp, usageCount = count)

    val history: Flow<List<SearchHistoryEntry>> = items

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        upsert(SearchHistoryEntry(trimmed))
    }
}
