package com.pchmn.pixelishsearch.search.settings.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.Normalizer

/**
 * In-memory index of system Settings sub-pages discovered dynamically on the
 * device.
 *
 * Strategy: `pm.queryIntentActivities` against `ACTION_MAIN` in the Settings
 * package, then keep only activities that declare their own `labelRes` (i.e.
 * not just inheriting the parent "Settings" label). Each kept entry is
 * launched by [ComponentName] — there's no public `Settings.ACTION_*` constant
 * for many OEM-specific pages (Pixel "Modes", "Satellite", "One-handed", …)
 * so binding by component is the only way to reach them.
 *
 * Search is purely in-memory: `contains` match on the (localized) label,
 * accent-insensitive. Hits whose label *starts* with the query rank first.
 */
object SettingsPageIndex {

    private const val MIN_QUERY_LENGTH = 2
    private const val SETTINGS_PKG = "com.android.settings"

    @Volatile
    private var entries: List<SettingsPageEntry> = emptyList()

    /**
     * Cache key for the Settings app launcher icon — shared by every row in
     * the search UI (all entries live in the same package by construction).
     */
    @Volatile
    var iconRequest: AppIconRequest? = null
        private set

    fun preload(scope: CoroutineScope, context: Context) {
        scope.launch(Dispatchers.IO) {
            entries = discover(context)
            iconRequest = resolveIcon(context)
        }
    }

    fun search(query: String, limit: Int = 4): List<SettingsPageEntry> {
        val needle = query.trim().normalize()
        if (needle.length < MIN_QUERY_LENGTH) return emptyList()
        val all = entries
        if (all.isEmpty()) return emptyList()
        return all
            .asSequence()
            .mapNotNull { entry ->
                val norm = entry.label.normalize()
                when {
                    norm.startsWith(needle) -> 0 to entry
                    norm.contains(needle) -> 1 to entry
                    else -> null
                }
            }
            .sortedBy { it.first }
            .map { it.second }
            .take(limit)
            .toList()
    }

    /**
     * Discover every Settings sub-page reachable via `ACTION_MAIN` on this
     * device. Filtering rules:
     *  - activity must be `exported` (sanity — Android 11+ visibility makes
     *    non-exported activities unreachable from us anyway),
     *  - activity must declare its own `labelRes` (otherwise the loaded label
     *    falls back to the app label "Settings" and we'd surface dozens of
     *    indistinguishable rows),
     *  - the loaded label must differ from the Settings app label (defensive,
     *    catches the rare case where `labelRes` resolves to the parent label),
     *  - deduplicate by [ComponentName].
     */
    private fun discover(context: Context): List<SettingsPageEntry> {
        val pm = context.packageManager
        val appLabel = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(SETTINGS_PKG, 0)).toString().trim()
        }.getOrNull().orEmpty()

        val intent = Intent(Intent.ACTION_MAIN).setPackage(SETTINGS_PKG)
        val matches = pm.queryIntentActivities(intent, 0)

        val seen = HashSet<ComponentName>(matches.size)
        return matches.mapNotNull { info ->
            val act = info.activityInfo ?: return@mapNotNull null
            if (!act.exported) return@mapNotNull null
            if (act.labelRes == 0) return@mapNotNull null
            val label = info.loadLabel(pm).toString().trim()
            if (label.isEmpty() || label == appLabel) return@mapNotNull null
            val component = ComponentName(act.packageName, act.name)
            if (!seen.add(component)) return@mapNotNull null
            SettingsPageEntry(label, component)
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
            ?: SETTINGS_PKG
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
