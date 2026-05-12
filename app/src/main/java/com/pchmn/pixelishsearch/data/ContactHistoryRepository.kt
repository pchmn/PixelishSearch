package com.pchmn.pixelishsearch.data

import android.content.Context
import android.net.Uri
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

class ContactHistoryRepository(
    context: Context,
    scope: CoroutineScope,
) : HistoryRepository<ContactHistoryEntry, Long>(
    dataStore = context.applicationContext.contactHistoryDataStore,
    serializer = ContactHistoryEntry.serializer(),
    scope = scope,
) {
    override fun keyOf(item: ContactHistoryEntry) = item.id
    override fun withUpdatedMetadata(item: ContactHistoryEntry, timestamp: Long, count: Int) =
        item.copy(lastUsedEpochMillis = timestamp, usageCount = count)

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
