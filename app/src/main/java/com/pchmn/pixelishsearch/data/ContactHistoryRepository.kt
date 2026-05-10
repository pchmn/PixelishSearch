package com.pchmn.pixelishsearch.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class ContactAction { CARD, MESSAGE, CALL }

data class ContactHistoryEntry(
    val id: Long,
    val name: String,
    val photoUri: Uri?,
    val phoneNumber: String?,
    val action: ContactAction,
    val lastUsedEpochMillis: Long,
)

/**
 * Persists opened contacts (card, message or call) so they can be
 * suggested later when the search field is empty.
 * Deduplicated by contactId — the latest action overwrites the previous one.
 */
object ContactHistoryRepository {

    private const val MAX_ENTRIES = 20

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "contact_history")
    private val HISTORY_KEY = stringSetPreferencesKey("contacts")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: DataStore<Preferences>

    private val _recents = MutableStateFlow<List<ContactHistoryEntry>>(emptyList())
    val recents: StateFlow<List<ContactHistoryEntry>> = _recents.asStateFlow()

    fun init(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.dataStore
        scope.launch {
            dataStore.data.collect { prefs ->
                _recents.value = (prefs[HISTORY_KEY] ?: emptySet())
                    .mapNotNull(::decode)
                    .sortedByDescending { it.lastUsedEpochMillis }
            }
        }
    }

    fun record(
        id: Long,
        name: String,
        photoUri: Uri?,
        phoneNumber: String?,
        action: ContactAction,
    ) {
        if (!::dataStore.isInitialized) return
        if (name.isBlank()) return
        scope.launch {
            dataStore.edit { prefs ->
                val current = (prefs[HISTORY_KEY] ?: emptySet())
                    .mapNotNull(::decode)
                    .associateBy { it.id }
                    .toMutableMap()
                current[id] = ContactHistoryEntry(
                    id = id,
                    name = name,
                    photoUri = photoUri,
                    phoneNumber = phoneNumber,
                    action = action,
                    lastUsedEpochMillis = System.currentTimeMillis(),
                )
                prefs[HISTORY_KEY] = current.values
                    .sortedByDescending { it.lastUsedEpochMillis }
                    .take(MAX_ENTRIES)
                    .map(::encode)
                    .toSet()
            }
        }
    }

    private fun encode(entry: ContactHistoryEntry): String {
        val n = URLEncoder.encode(entry.name, StandardCharsets.UTF_8.name())
        val ph = entry.phoneNumber
            ?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
            .orEmpty()
        val pu = entry.photoUri
            ?.let { URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.name()) }
            .orEmpty()
        return "${entry.lastUsedEpochMillis}|${entry.id}|${entry.action.name}|$n|$ph|$pu"
    }

    private fun decode(raw: String): ContactHistoryEntry? {
        val parts = raw.split("|", limit = 6)
        if (parts.size < 6) return null
        val ts = parts[0].toLongOrNull() ?: return null
        val id = parts[1].toLongOrNull() ?: return null
        val action = runCatching { ContactAction.valueOf(parts[2]) }.getOrNull() ?: return null
        val name = runCatching {
            URLDecoder.decode(parts[3], StandardCharsets.UTF_8.name())
        }.getOrNull() ?: return null
        if (name.isBlank()) return null
        val phoneNumber = parts[4].takeIf { it.isNotEmpty() }?.let {
            runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.getOrNull()
        }
        val photoUri = parts[5].takeIf { it.isNotEmpty() }?.let {
            runCatching {
                Uri.parse(URLDecoder.decode(it, StandardCharsets.UTF_8.name()))
            }.getOrNull()
        }
        return ContactHistoryEntry(id, name, photoUri, phoneNumber, action, ts)
    }
}
