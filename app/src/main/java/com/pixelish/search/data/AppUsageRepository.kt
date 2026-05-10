package com.pixelish.search.data

import android.content.Context
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
import kotlin.math.exp

data class UsageStat(
    val count: Int,
    val lastLaunchEpochMillis: Long,
)

/**
 * Persiste un compteur d'ouverture par package + le timestamp de la dernière
 * ouverture. Le score combine les deux avec un decay exponentiel : une app très
 * utilisée puis abandonnée finit par redescendre.
 */
object AppUsageRepository {

    // Constante de demi-vie : un launch « vaut » la moitié au bout de 14 jours,
    // un quart au bout de 28, etc.
    private const val HALF_LIFE_DAYS = 14.0
    private const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_usage")
    private val USAGE_KEY = stringSetPreferencesKey("usage_stats")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: DataStore<Preferences>

    private val _stats = MutableStateFlow<Map<String, UsageStat>>(emptyMap())
    val stats: StateFlow<Map<String, UsageStat>> = _stats.asStateFlow()

    fun init(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.dataStore
        scope.launch {
            dataStore.data.collect { prefs ->
                _stats.value = (prefs[USAGE_KEY] ?: emptySet())
                    .mapNotNull(::decode)
                    .toMap()
            }
        }
    }

    fun recordLaunch(packageName: String) {
        if (!::dataStore.isInitialized) return
        scope.launch {
            dataStore.edit { prefs ->
                val current = (prefs[USAGE_KEY] ?: emptySet())
                    .mapNotNull(::decode)
                    .toMap()
                    .toMutableMap()
                val prev = current[packageName]
                current[packageName] = UsageStat(
                    count = (prev?.count ?: 0) + 1,
                    lastLaunchEpochMillis = System.currentTimeMillis(),
                )
                prefs[USAGE_KEY] = current.entries
                    .map { (pkg, stat) -> encode(pkg, stat) }
                    .toSet()
            }
        }
    }

    fun scoreOf(
        packageName: String,
        stats: Map<String, UsageStat> = _stats.value,
        now: Long = System.currentTimeMillis(),
    ): Float {
        val s = stats[packageName] ?: return 0f
        val daysSince = (now - s.lastLaunchEpochMillis).toDouble() / MILLIS_PER_DAY
        return (s.count * exp(-daysSince / HALF_LIFE_DAYS)).toFloat()
    }

    private fun encode(packageName: String, stat: UsageStat): String =
        "$packageName|${stat.count}|${stat.lastLaunchEpochMillis}"

    private fun decode(raw: String): Pair<String, UsageStat>? {
        val parts = raw.split("|")
        if (parts.size != 3) return null
        val count = parts[1].toIntOrNull() ?: return null
        val ts = parts[2].toLongOrNull() ?: return null
        return parts[0] to UsageStat(count, ts)
    }
}
