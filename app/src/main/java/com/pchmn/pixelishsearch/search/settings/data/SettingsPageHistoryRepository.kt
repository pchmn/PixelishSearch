package com.pchmn.pixelishsearch.search.settings.data

import android.content.ComponentName
import android.content.Context
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.core.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class SettingsPageHistoryEntry(
    @Serializable(with = ComponentNameSerializer::class)
    val component: ComponentName,
    val label: String,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry

class SettingsPageHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<SettingsPageHistoryEntry, ComponentName>(
    dataStore = context.applicationContext.settingsPageHistoryDataStore,
    serializer = SettingsPageHistoryEntry.serializer(),
    scope = scope,
) {
    override fun keyOf(item: SettingsPageHistoryEntry) = item.component
    override fun withUpdatedMetadata(item: SettingsPageHistoryEntry, timestamp: Long, count: Int) =
        item.copy(lastUsedEpochMillis = timestamp, usageCount = count)

    val recents: StateFlow<List<SettingsPageHistoryEntry>> = items

    suspend fun record(component: ComponentName, label: String) {
        if (label.isBlank()) return
        upsert(SettingsPageHistoryEntry(component = component, label = label))
    }
}
