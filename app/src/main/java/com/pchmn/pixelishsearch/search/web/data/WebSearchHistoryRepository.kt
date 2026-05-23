package com.pchmn.pixelishsearch.search.web.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.core.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class WebSearchHistoryEntry(
    val query: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")

class WebSearchHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<WebSearchHistoryEntry, String>(
    dataStore = context.applicationContext.searchHistoryDataStore,
    serializer = WebSearchHistoryEntry.serializer(),
    keyOf = { it.query.lowercase() },
    withUpdatedMetadata = { e, t, c -> e.copy(lastUsedEpochMillis = t, usageCount = c) },
    scope = scope,
    maxEntries = 50,
) {
    val history: StateFlow<List<WebSearchHistoryEntry>> = items

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        upsert(WebSearchHistoryEntry(trimmed))
    }
}
