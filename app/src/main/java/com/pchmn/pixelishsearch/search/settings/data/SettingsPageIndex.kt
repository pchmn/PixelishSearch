package com.pchmn.pixelishsearch.search.settings.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.Normalizer

/**
 * In-memory index of system Settings pages resolvable by intent on this device.
 *
 * Each entry in [settingsPages] is run through `PackageManager.resolveActivity`
 * at preload time. Entries that don't resolve (the action isn't declared by the
 * Settings APK on this device / Android version) are dropped. Labels come from
 * `ResolveInfo.loadLabel`, so they follow the device locale automatically.
 *
 * Search is purely in-memory: a `contains` match on the localized label plus a
 * `startsWith` match on the English keywords, accent-insensitive.
 */
object SettingsPageIndex {

    private const val MIN_QUERY_LENGTH = 2

    @Volatile
    private var entries: List<SettingsPageEntry> = emptyList()

    /**
     * Cache key for the Settings app launcher icon — shared by every row in the
     * search UI. Resolved once at preload (basically every entry in
     * [settingsPages] points at the same package, so we'd otherwise store the
     * same pair 30+ times).
     */
    @Volatile
    var iconRequest: AppIconRequest? = null
        private set

    /**
     * Resolve every page against the device's PackageManager. Runs once at app
     * start on Dispatchers.IO — PM lookups are cheap individually but ~35 of
     * them adds up enough to keep off the main thread.
     */
    fun preload(scope: CoroutineScope, context: Context) {
        scope.launch(Dispatchers.IO) {
            entries = resolveAll(context)
            iconRequest = resolveIcon(context)
        }
    }

    fun search(query: String, limit: Int = 4): List<SettingsPageEntry> {
        val needle = query.trim().normalize()
        if (needle.length < MIN_QUERY_LENGTH) return emptyList()
        val all = entries
        if (all.isEmpty()) return emptyList()
        return all
            .filter { entry ->
                entry.label.normalize().contains(needle) ||
                        entry.keywords.any { it.normalize().startsWith(needle) }
            }
            .take(limit)
    }

    private fun resolveAll(context: Context): List<SettingsPageEntry> {
        val pm = context.packageManager
        val seenActions = HashSet<String>(settingsPages.size)
        return settingsPages.mapNotNull { page ->
            if (!seenActions.add(page.action)) return@mapNotNull null
            val info = pm.resolveActivity(Intent(page.action), 0) ?: return@mapNotNull null
            val label = info.loadLabel(pm).toString().trim()
            if (label.isEmpty()) return@mapNotNull null
            SettingsPageEntry(page.action, label, page.keywords)
        }
    }

    /**
     * Resolve the canonical Settings package via `ACTION_SETTINGS`, then read
     * its `lastUpdateTime` so Coil invalidates the cached icon after an OS
     * update changes it. Falls back to AOSP `com.android.settings` if the
     * action somehow doesn't resolve (shouldn't happen in practice).
     */
    private fun resolveIcon(context: Context): AppIconRequest? {
        val pm = context.packageManager
        val pkg = pm.resolveActivity(Intent(android.provider.Settings.ACTION_SETTINGS), 0)
            ?.activityInfo?.packageName
            ?: "com.android.settings"
        val lastUpdate = try {
            pm.getPackageInfo(pkg, 0).lastUpdateTime
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        return AppIconRequest(pkg, lastUpdate)
    }

    private fun String.normalize(): String =
        Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")

    private val DIACRITICS = Regex("\\p{Mn}+")
}
