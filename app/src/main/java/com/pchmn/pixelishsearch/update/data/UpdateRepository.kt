package com.pchmn.pixelishsearch.update.data

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A downloaded, ready-to-install release. `apkPath` points to a file in the
 * app's external files dir (no permission needed, and FileProvider can expose
 * it to the package installer).
 */
@Serializable
data class UpdateInfo(
    val versionName: String,
    val changelog: String,
    val apkPath: String,
    val downloadedAt: Long,
)

class UpdateRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.applicationContext.updateDataStore
    private val key = stringPreferencesKey("available")
    private val json = Json { ignoreUnknownKeys = true }

    val available: StateFlow<UpdateInfo?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[key]?.let { raw ->
                runCatching { json.decodeFromString(UpdateInfo.serializer(), raw) }.getOrNull()
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setAvailable(info: UpdateInfo) {
        val encoded = json.encodeToString(UpdateInfo.serializer(), info)
        dataStore.edit { it[key] = encoded }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
