package com.pchmn.pixelishsearch.search.apps.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.core.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class AppHistoryEntry(
    val packageName: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry

private val Context.appHistoryDatastore by preferencesDataStore(name = "app_history")

class AppHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<AppHistoryEntry, String>(
    dataStore = context.applicationContext.appHistoryDatastore,
    serializer = AppHistoryEntry.serializer(),
    keyOf = { it.packageName },
    withUpdatedMetadata = { e, t, c -> e.copy(lastUsedEpochMillis = t, usageCount = c) },
    scope = scope,
) {
    val recents: StateFlow<List<AppHistoryEntry>> = items

    suspend fun record(packageName: String) {
        if (packageName.isBlank()) return
        upsert(AppHistoryEntry(packageName))
    }

    /**
     * Rank [apps] by usage score DESC, then alpha. Drives the blank-state
     * recents strip.
     */
    fun ranked(apps: List<AppEntry>): List<AppEntry> {
        val history = byKey.value
        val now = System.currentTimeMillis()
        return apps.sortedWith(
            compareByDescending<AppEntry> { history[it.packageName]?.score(now) ?: 0f }
                .thenBy { it.label.lowercase() }
        )
    }
}
