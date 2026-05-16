package com.pchmn.pixelishsearch.search.apps.data

import android.content.Context
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.core.data.HistoryRepository
import com.pchmn.pixelishsearch.data.appHistoryDatastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class AppHistoryEntry(
    val packageName: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry


class AppHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<AppHistoryEntry, String>(
    dataStore = context.applicationContext.appHistoryDatastore,
    serializer = AppHistoryEntry.serializer(),
    scope = scope,
) {
    override fun keyOf(item: AppHistoryEntry) = item.packageName
    override fun withUpdatedMetadata(item: AppHistoryEntry, timestamp: Long, count: Int) =
        item.copy(lastUsedEpochMillis = timestamp, usageCount = count)

    val recents: StateFlow<List<AppHistoryEntry>> = items

    suspend fun record(packageName: String) {
        if (packageName.isBlank()) return
        upsert(AppHistoryEntry(packageName))
    }
}
