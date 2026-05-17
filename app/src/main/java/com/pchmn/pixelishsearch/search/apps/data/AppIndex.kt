package com.pchmn.pixelishsearch.search.apps.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Trace
import androidx.tracing.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppEntry(
    val label: String,
    val packageName: String,
    val launchIntent: Intent,
    // Used as the Coil cache key suffix so icon updates invalidate automatically.
    val lastUpdateTime: Long,
) {
    // Lowercased, accent-stripped label for fast matching
    val normalizedLabel: String = label.lowercase()
        .replace("[àáâãäå]".toRegex(), "a")
        .replace("[èéêë]".toRegex(), "e")
        .replace("[ìíîï]".toRegex(), "i")
        .replace("[òóôõö]".toRegex(), "o")
        .replace("[ùúûü]".toRegex(), "u")
        .replace("[ç]".toRegex(), "c")
}

/**
 * Singleton that keeps the apps index in memory.
 * Preloaded on app start and on phone boot.
 */
object AppIndex {

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    fun preload(context: Context, scope: CoroutineScope) {
        if (_isLoaded.value) return
        scope.launch {
            // Async section: the coroutine may hop dispatcher threads on suspension,
            // so we use beginAsyncSection (cross-thread safe) instead of trace { }.
            val preloadCookie = ASYNC_COOKIE_PRELOAD
            Trace.beginAsyncSection("AppIndex.preload", preloadCookie)
            try {
                val cacheRepo = AppIndexCacheRepository(context)

                // Phase A — hydrate from the persisted cache so the UI can render
                // the AppRow with real names + Coil disk-cached icons within ms,
                // without waiting on PackageManager.
                val readCookie = ASYNC_COOKIE_READ_CACHE
                Trace.beginAsyncSection("AppIndex.phaseA.readCache", readCookie)
                val cached = try { cacheRepo.read() }
                    finally { Trace.endAsyncSection("AppIndex.phaseA.readCache", readCookie) }

                if (cached.isNotEmpty() && _apps.value.isEmpty()) {
                    trace("AppIndex.phaseA.hydrate") {
                        _apps.value = cached.map { it.toAppEntry() }
                    }
                }

                // Phase B — authoritative enumeration. Picks up newly installed /
                // uninstalled / renamed apps. Cheap now that icons are loaded by
                // Coil on demand.
                val fresh = trace("AppIndex.phaseB.enumerate") { enumerate(context) }
                val freshCached = fresh.map { it.toCached() }
                if (freshCached != cached) {
                    _apps.value = fresh
                    val writeCookie = ASYNC_COOKIE_WRITE_CACHE
                    Trace.beginAsyncSection("AppIndex.phaseB.writeCache", writeCookie)
                    try { cacheRepo.write(freshCached) }
                    finally { Trace.endAsyncSection("AppIndex.phaseB.writeCache", writeCookie) }
                }

                _isLoaded.value = true
            } finally {
                Trace.endAsyncSection("AppIndex.preload", preloadCookie)
            }
        }
    }

    private const val ASYNC_COOKIE_PRELOAD = 1
    private const val ASYNC_COOKIE_READ_CACHE = 2
    private const val ASYNC_COOKIE_WRITE_CACHE = 3

    /**
     * Re-enumerates from PackageManager and persists if the list changed.
     * Triggered by [PackageReceiver] when an app is installed / removed /
     * updated, so the next user tap reflects the new state without waiting
     * for the process to be restarted.
     */
    fun refresh(context: Context, scope: CoroutineScope) {
        scope.launch {
            val cacheRepo = AppIndexCacheRepository(context)
            val fresh = enumerate(context)
            val freshCached = fresh.map { it.toCached() }
            if (freshCached != cacheRepo.read()) {
                _apps.value = fresh
                cacheRepo.write(freshCached)
            }
        }
    }

    private fun enumerate(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(mainIntent, 0)
            .mapNotNull { ri ->
                try {
                    val pkg = ri.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null

                    val launchIntent =
                        pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null

                    val lastUpdateTime = runCatching {
                        pm.getPackageInfo(pkg, 0).lastUpdateTime
                    }.getOrDefault(0L)

                    AppEntry(
                        label = ri.loadLabel(pm).toString(),
                        packageName = pkg,
                        launchIntent = launchIntent.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                        lastUpdateTime = lastUpdateTime,
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Fuzzy search: starts with apps whose label starts with the query,
     * then those that contain it. Matches Pixel Launcher's behavior.
     *
     * `scoreOf` lets each group be re-sorted by usage frequency. Ties
     * (typically score = 0) are broken by the alpha order inherited from `_apps`.
     */
    fun search(
        query: String,
        limit: Int = 8,
        scoreOf: (String) -> Float = { 0f },
    ): List<AppEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
            .replace("[àáâãäå]".toRegex(), "a")
            .replace("[èéêë]".toRegex(), "e")
            .replace("[ìíîï]".toRegex(), "i")
            .replace("[òóôõö]".toRegex(), "o")
            .replace("[ùúûü]".toRegex(), "u")
            .replace("[ç]".toRegex(), "c")

        val all = _apps.value
        val startsWith = mutableListOf<AppEntry>()
        val contains = mutableListOf<AppEntry>()

        for (entry in all) {
            when {
                entry.normalizedLabel.startsWith(q) -> startsWith += entry
                entry.normalizedLabel.contains(q) -> contains += entry
            }
        }

        val byScoreDesc = compareByDescending<AppEntry> { scoreOf(it.packageName) }
        return (startsWith.sortedWith(byScoreDesc) + contains.sortedWith(byScoreDesc))
            .take(limit)
    }
}
