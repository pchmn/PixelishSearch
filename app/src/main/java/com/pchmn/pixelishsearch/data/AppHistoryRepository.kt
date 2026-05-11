package com.pchmn.pixelishsearch.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class AppHistoryEntry(
    val packageName: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry


class AppHistoryRepository(context: Context) : HistoryRepository<AppHistoryEntry, String>(
    dataStore = context.applicationContext.appHistoryDatastore,
    serializer = AppHistoryEntry.serializer(),
) {
    override fun keyOf(item: AppHistoryEntry) = item.packageName
    override fun withUpdatedMetadata(item: AppHistoryEntry, timestamp: Long, count: Int) =
        item.copy(lastUsedEpochMillis = timestamp, usageCount = count)

    val recents: Flow<List<AppHistoryEntry>> = items

    suspend fun record(packageName: String) {
        if (packageName.isBlank()) return
        upsert(AppHistoryEntry(packageName))
    }
}
