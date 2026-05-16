package com.pchmn.pixelishsearch.search.apps.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persisted snapshot of the app launcher index. Stored as a JSON list in a
 * dedicated DataStore so that on cold start we can rebuild AppEntry objects
 * without calling PackageManager at all.
 *
 * `activityClassName` is the resolved launcher activity (ComponentName.className).
 * Storing it explicitly lets us reconstruct the launch Intent in pure Kotlin —
 * no `pm.getLaunchIntentForPackage` call needed at restore time.
 */
@Serializable
data class CachedAppEntry(
    val label: String,
    val packageName: String,
    val activityClassName: String,
    val lastUpdateTime: Long,
)

class AppIndexCacheRepository(context: Context) {

    private val dataStore = context.applicationContext.appIndexCacheDataStore
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CachedAppEntry.serializer())

    suspend fun read(): List<CachedAppEntry> {
        val raw = dataStore.data.first()[key] ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun write(entries: List<CachedAppEntry>) {
        val encoded = json.encodeToString(serializer, entries)
        dataStore.edit { it[key] = encoded }
    }
}

fun CachedAppEntry.toAppEntry(): AppEntry = AppEntry(
    label = label,
    packageName = packageName,
    launchIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(packageName, activityClassName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    },
    lastUpdateTime = lastUpdateTime,
)

fun AppEntry.toCached(): CachedAppEntry = CachedAppEntry(
    label = label,
    packageName = packageName,
    activityClassName = launchIntent.component?.className.orEmpty(),
    lastUpdateTime = lastUpdateTime,
)
