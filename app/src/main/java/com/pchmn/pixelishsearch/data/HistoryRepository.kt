package com.pchmn.pixelishsearch.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.exp

interface HistoryEntry {
    val lastUsedEpochMillis: Long
    val usageCount: Int

    companion object {
        private const val HALF_LIFE_DAYS = 14.0
        private const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24
    }

    // Exponential time decay: a launch is worth half after 14 days,
    // a quarter after 28, etc. So heavily used apps that are then
    // abandoned eventually drop back down.
    fun score(now: Long = System.currentTimeMillis()): Float {
        val daysSince = (now - lastUsedEpochMillis).toDouble() / MILLIS_PER_DAY
        return (usageCount * exp(-daysSince / HALF_LIFE_DAYS)).toFloat()
    }
}

abstract class HistoryRepository<T : HistoryEntry, K>(
    private val dataStore: DataStore<Preferences>,
    private val serializer: KSerializer<T>,
    private val maxEntries: Int = 20,
) {
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }

    protected abstract fun keyOf(item: T): K
    protected abstract fun withUpdatedMetadata(item: T, timestamp: Long, count: Int): T

    val items: Flow<List<T>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[key]
                ?.let { runCatching { json.decodeFromString(ListSerializer(serializer), it) }.getOrNull() }
                ?.sortedByDescending { it.lastUsedEpochMillis }
                .orEmpty()
        }

    protected suspend fun upsert(item: T) {
        dataStore.edit { prefs ->
            val current = prefs[key]
                ?.let { runCatching { json.decodeFromString(ListSerializer(serializer), it) }.getOrNull() }
                .orEmpty()
            val existing = current.find { keyOf(it) == keyOf(item) }
            val now = System.currentTimeMillis()
            val newCount = (existing?.usageCount ?: 0) + 1
            val finalItem = withUpdatedMetadata(item, now, newCount)

            val merged = (current.filterNot { keyOf(it) == keyOf(item) } + finalItem)
                .sortedByDescending { it.lastUsedEpochMillis }
                .take(maxEntries)
            prefs[key] = json.encodeToString(ListSerializer(serializer), merged)
        }
    }

    suspend fun remove(item: T) {
        dataStore.edit { prefs ->
            val current = prefs[key]
                ?.let { runCatching { json.decodeFromString(ListSerializer(serializer), it) }.getOrNull() }
                .orEmpty()
            val targetKey = keyOf(item)
            val filtered = current.filterNot { keyOf(it) == targetKey }
            if (filtered.size == current.size) return@edit
            prefs[key] = json.encodeToString(ListSerializer(serializer), filtered)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}