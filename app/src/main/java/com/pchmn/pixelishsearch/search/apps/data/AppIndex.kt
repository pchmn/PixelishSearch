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
    val normalizedLabel: String = label.normalizeForSearch()
}

internal fun String.normalizeForSearch(): String {
    val sb = StringBuilder(length)
    for (c in this) {
        when (c) {
            'à', 'á', 'â', 'ã', 'ä', 'å', 'À', 'Á', 'Â', 'Ã', 'Ä', 'Å' -> sb.append('a')
            'è', 'é', 'ê', 'ë', 'È', 'É', 'Ê', 'Ë' -> sb.append('e')
            'ì', 'í', 'î', 'ï', 'Ì', 'Í', 'Î', 'Ï' -> sb.append('i')
            'ò', 'ó', 'ô', 'õ', 'ö', 'Ò', 'Ó', 'Ô', 'Õ', 'Ö' -> sb.append('o')
            'ù', 'ú', 'û', 'ü', 'Ù', 'Ú', 'Û', 'Ü' -> sb.append('u')
            'ç', 'Ç' -> sb.append('c')
            '\'', '’', '‘', '`', '´' -> Unit
            else -> sb.append(c.lowercaseChar())
        }
    }
    return sb.toString()
}

/**
 * Singleton that keeps the apps index in memory.
 * Preloaded on app start and on phone boot.
 */
object AppIndex {

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    /**
     * Phase A — hydrate the in-memory list from the persisted cache. Cheap: a
     * single DataStore read, no PackageManager label resolution, so it is safe
     * on the cold-start critical path (dispatched from `Application.onCreate`).
     * The UI renders real names + Coil disk-cached icons within ms without ever
     * touching PackageManager.
     *
     * Phase B ([refresh]) is deliberately *not* chained here: resolving each
     * launcher app's label loads that app's resources, which contends with the
     * first-frame composition on ART locks (see docs/performance-analysis.md,
     * ADR-0009). The cold-start path defers it past the first frame from
     * `MainActivity`; nothing needs it before the user types 2 chars.
     */
    fun preloadFromCache(context: Context, scope: CoroutineScope) {
        scope.launch {
            // Async section: the coroutine may hop dispatcher threads on
            // suspension, so we use beginAsyncSection (cross-thread safe).
            val readCookie = ASYNC_COOKIE_READ_CACHE
            Trace.beginAsyncSection("AppIndex.phaseA.readCache", readCookie)
            val cached = try { AppIndexCacheRepository(context).read() }
                finally { Trace.endAsyncSection("AppIndex.phaseA.readCache", readCookie) }

            if (cached.isNotEmpty() && _apps.value.isEmpty()) {
                trace("AppIndex.phaseA.hydrate") {
                    _apps.value = cached.map { it.toAppEntry() }
                }
            }
        }
    }

    private const val ASYNC_COOKIE_READ_CACHE = 1
    private const val ASYNC_COOKIE_WRITE_CACHE = 2

    /**
     * Phase B — authoritative re-enumeration from PackageManager. Resolving each
     * launcher app's label loads its resources, so this is the expensive,
     * contention-prone half kept *off* the cold-start critical path. Persists
     * only if the list changed (and updates the in-memory list to match).
     * Triggered after the first frame from `MainActivity` on cold start, by
     * [PackageReceiver] on app install / remove / update, and by [BootReceiver]
     * on boot — so the next user tap reflects the current state without waiting
     * for the process to restart.
     */
    fun refresh(context: Context, scope: CoroutineScope) {
        scope.launch {
            val cacheRepo = AppIndexCacheRepository(context)
            val fresh = trace("AppIndex.phaseB.enumerate") { enumerate(context) }
            val freshCached = fresh.map { it.toCached() }
            if (freshCached != cacheRepo.read()) {
                _apps.value = fresh
                val writeCookie = ASYNC_COOKIE_WRITE_CACHE
                Trace.beginAsyncSection("AppIndex.phaseB.writeCache", writeCookie)
                try { cacheRepo.write(freshCached) }
                finally { Trace.endAsyncSection("AppIndex.phaseB.writeCache", writeCookie) }
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
        val q = query.normalizeForSearch()

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
