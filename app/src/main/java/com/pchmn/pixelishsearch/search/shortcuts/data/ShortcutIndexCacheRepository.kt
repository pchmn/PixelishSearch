package com.pchmn.pixelishsearch.search.shortcuts.data

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.shortcutIndexCacheDataStore by preferencesDataStore(name = "shortcut_index_cache")

/**
 * Persisted snapshot of [ShortcutIndex]. Lets cold start hydrate the static
 * shortcuts — and therefore the blank-state recent-shortcuts block (see
 * [SearchViewModel][com.pchmn.pixelishsearch.search.ui.SearchViewModel]) — on
 * the first frame, without re-parsing every launcher app's `shortcuts.xml` or
 * loading its `.arsc`. The expensive re-parse runs in the background after the
 * first frame and rewrites this cache. See ADR-0008 / ADR-0009.
 *
 * [launchIntentUri] round-trips the built launch Intent via [Intent.toUri] /
 * [Intent.parseUri]. [iconResId] is just the int resource id; Coil resolves the
 * actual drawable on demand through `ShortcutIconFetcher`.
 */
@Serializable
data class CachedShortcutEntry(
    val packageName: String,
    val shortcutId: String,
    val shortLabel: String,
    val appLabel: String,
    val iconResId: Int,
    val launchIntentUri: String,
    val lastUpdateTime: Long,
)

class ShortcutIndexCacheRepository(context: Context) {

    private val dataStore = context.applicationContext.shortcutIndexCacheDataStore
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CachedShortcutEntry.serializer())

    suspend fun read(): List<CachedShortcutEntry> {
        val raw = dataStore.data.first()[key] ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun write(entries: List<CachedShortcutEntry>) {
        val encoded = json.encodeToString(serializer, entries)
        dataStore.edit { it[key] = encoded }
    }
}

/** Returns null if the persisted Intent URI no longer parses (skip the entry). */
fun CachedShortcutEntry.toShortcutEntry(): ShortcutEntry? {
    val intent = runCatching { Intent.parseUri(launchIntentUri, 0) }.getOrNull() ?: return null
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return ShortcutEntry(
        packageName = packageName,
        shortcutId = shortcutId,
        shortLabel = shortLabel,
        appLabel = appLabel,
        iconResId = iconResId,
        launchIntent = intent,
        lastUpdateTime = lastUpdateTime,
    )
}

fun ShortcutEntry.toCached(): CachedShortcutEntry = CachedShortcutEntry(
    packageName = packageName,
    shortcutId = shortcutId,
    shortLabel = shortLabel,
    appLabel = appLabel,
    iconResId = iconResId,
    launchIntentUri = launchIntent.toUri(0),
    lastUpdateTime = lastUpdateTime,
)
