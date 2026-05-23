package com.pchmn.pixelishsearch.search.contacts.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.preferencesDataStore
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.core.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class ContactHistoryEntry(
    val id: Long,
    val name: String,
    @Serializable(with = UriSerializer::class)
    val photoUri: Uri? = null,
    val phoneNumber: String? = null,
    val action: ContactAction,
    override val lastUsedEpochMillis: Long = 0L,
    override val usageCount: Int = 0,
) : HistoryEntry

enum class ContactAction { CARD, MESSAGE, CALL }

private val Context.contactHistoryDataStore by preferencesDataStore(name = "contact_history")

class ContactHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<ContactHistoryEntry, Long>(
    dataStore = context.applicationContext.contactHistoryDataStore,
    serializer = ContactHistoryEntry.serializer(),
    keyOf = { it.id },
    withUpdatedMetadata = { e, t, c -> e.copy(lastUsedEpochMillis = t, usageCount = c) },
    scope = scope,
) {
    val recents: StateFlow<List<ContactHistoryEntry>> = items

    suspend fun record(
        id: Long,
        name: String,
        photoUri: Uri?,
        phoneNumber: String?,
        action: ContactAction,
    ) {
        if (name.isBlank()) return
        upsert(
            ContactHistoryEntry(
                id = id,
                name = name,
                photoUri = photoUri,
                phoneNumber = phoneNumber,
                action = action,
            )
        )
    }
}
