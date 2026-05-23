package com.pchmn.pixelishsearch.search.settings.data

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
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

private val Context.settingsPageHistoryDataStore by preferencesDataStore(name = "settings_page_history")

class SettingsPageHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<SettingsPageHistoryEntry, ComponentName>(
    dataStore = context.applicationContext.settingsPageHistoryDataStore,
    serializer = SettingsPageHistoryEntry.serializer(),
    keyOf = { it.component },
    withUpdatedMetadata = { e, t, c -> e.copy(lastUsedEpochMillis = t, usageCount = c) },
    scope = scope,
) {
    val recents: StateFlow<List<SettingsPageHistoryEntry>> = items

    suspend fun record(component: ComponentName, label: String) {
        if (label.isBlank()) return
        upsert(SettingsPageHistoryEntry(component = component, label = label))
    }
}
