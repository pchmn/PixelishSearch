package com.pchmn.pixelishsearch.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class WebSearchHistoryEntry(
    val query: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry

class WebSearchHistoryRepository(context: Context) : HistoryRepository<WebSearchHistoryEntry, String>(
    dataStore = context.applicationContext.searchHistoryDataStore,
    serializer = WebSearchHistoryEntry.serializer(),
    maxEntries = 50,
) {
    override fun keyOf(item: WebSearchHistoryEntry) = item.query.lowercase()
    override fun withUpdatedMetadata(item: WebSearchHistoryEntry, timestamp: Long, count: Int) =
        item.copy(lastUsedEpochMillis = timestamp, usageCount = count)

    val history: Flow<List<WebSearchHistoryEntry>> = items

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        upsert(WebSearchHistoryEntry(trimmed))
    }
}
