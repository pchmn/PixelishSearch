package com.pchmn.pixelishsearch.search.shortcuts.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.core.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Locally-stored recent-use record for an App shortcut. Self-sufficient for
 * *display* (label + parent-app label + icon); the launch Intent is re-resolved
 * from [ShortcutIndex] at tap time, and a record whose [key] is no longer in the
 * live index is filtered from the Recents strip (stale-filtering, like Settings
 * pages). See `docs/adr/0008`.
 */
@Serializable
data class ShortcutHistoryEntry(
    val packageName: String,
    val shortcutId: String,
    val shortLabel: String,
    val appLabel: String,
    val iconResId: Int = 0,
    val lastUpdateTime: Long = 0L,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry {
    val key: ShortcutKey get() = ShortcutKey(packageName, shortcutId)
}

private val Context.shortcutHistoryDataStore by preferencesDataStore(name = "shortcut_history")

class ShortcutHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<ShortcutHistoryEntry, ShortcutKey>(
    dataStore = context.applicationContext.shortcutHistoryDataStore,
    serializer = ShortcutHistoryEntry.serializer(),
    keyOf = { it.key },
    withUpdatedMetadata = { e, t, c -> e.copy(lastUsedEpochMillis = t, usageCount = c) },
    scope = scope,
) {
    val recents: StateFlow<List<ShortcutHistoryEntry>> = items

    suspend fun record(entry: ShortcutEntry) {
        if (entry.shortLabel.isBlank()) return
        upsert(
            ShortcutHistoryEntry(
                packageName = entry.packageName,
                shortcutId = entry.shortcutId,
                shortLabel = entry.shortLabel,
                appLabel = entry.appLabel,
                iconResId = entry.iconResId,
                lastUpdateTime = entry.lastUpdateTime,
            )
        )
    }
}
