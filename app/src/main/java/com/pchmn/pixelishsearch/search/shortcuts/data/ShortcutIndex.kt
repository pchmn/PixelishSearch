package com.pchmn.pixelishsearch.search.shortcuts.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.net.Uri
import com.pchmn.pixelishsearch.search.apps.data.normalizeForSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser

/** Stable identity of an App shortcut: the declaring package + its shortcut id. */
data class ShortcutKey(val packageName: String, val shortcutId: String)

/**
 * A static (manifest-declared) app shortcut surfaced by [ShortcutIndex].
 *
 * [packageName] is the *declaring* app — the one whose `shortcuts.xml` we parsed,
 * against whose resources [shortLabel]/[iconResId] resolve, and whose label is
 * [appLabel]. [launchIntent] is the fully built, ready-to-fire intent for the
 * shortcut's `<intent>`. See `docs/adr/0008`.
 */
data class ShortcutEntry(
    val packageName: String,
    val shortcutId: String,
    val shortLabel: String,
    val appLabel: String,
    val iconResId: Int,
    val launchIntent: Intent,
    // Coil icon cache-key suffix: a new app version (new shortcut icon) invalidates it.
    val lastUpdateTime: Long,
) {
    val key: ShortcutKey get() = ShortcutKey(packageName, shortcutId)
    val normalizedLabel: String = shortLabel.normalizeForSearch()
}

/**
 * In-memory index of *static* app shortcuts, built by parsing the
 * `android.app.shortcuts` meta-data XML of every launcher activity — no
 * default-launcher role required. Mirrors [SettingsPageIndex][com.pchmn.pixelishsearch.search.settings.data.SettingsPageIndex]:
 * preloaded async on app start, refreshed by `PackageReceiver`, no disk cache
 * (shortcuts only ever render on a non-blank query, never at first frame).
 *
 * Only launchable shortcuts are kept: we fire the parsed intent ourselves (we
 * can't use `LauncherApps.startShortcut` without the launcher role), so a target
 * activity that isn't exported, or an action that doesn't resolve, is dropped at
 * index time. See `docs/adr/0008`.
 */
object ShortcutIndex {

    private const val MIN_QUERY_LENGTH = 2
    private const val SHORTCUTS_META = "android.app.shortcuts"
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    private val _entries = MutableStateFlow<List<ShortcutEntry>>(emptyList())
    val entries: StateFlow<List<ShortcutEntry>> = _entries.asStateFlow()

    fun preload(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { _entries.value = discover(context) }
    }

    /** Re-parse from PackageManager. Triggered by `PackageReceiver`. */
    fun refresh(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { _entries.value = discover(context) }
    }

    fun find(key: ShortcutKey): ShortcutEntry? =
        _entries.value.firstOrNull { it.key == key }

    /**
     * `startsWith → contains` tiering on the (accent-insensitive) shortLabel,
     * each tier re-sorted by usage frequency via [scoreOf] (ADR-0006).
     */
    fun search(
        query: String,
        limit: Int = 3,
        scoreOf: (ShortcutKey) -> Float = { 0f },
    ): List<ShortcutEntry> {
        val q = query.normalizeForSearch()
        if (q.length < MIN_QUERY_LENGTH) return emptyList()
        val all = _entries.value
        if (all.isEmpty()) return emptyList()

        val startsWith = mutableListOf<ShortcutEntry>()
        val contains = mutableListOf<ShortcutEntry>()
        for (entry in all) {
            when {
                entry.normalizedLabel.startsWith(q) -> startsWith += entry
                entry.normalizedLabel.contains(q) -> contains += entry
            }
        }
        val byScoreDesc = compareByDescending<ShortcutEntry> { scoreOf(it.key) }
        return (startsWith.sortedWith(byScoreDesc) + contains.sortedWith(byScoreDesc))
            .take(limit)
    }

    /**
     * Enumerate launcher activities with their meta-data, and for each that
     * references `android.app.shortcuts`, parse and collect every launchable
     * static shortcut. Labels/icons resolve against the *declaring* app's
     * resources; per-package lookups (resources, label, version) are memoized.
     */
    private fun discover(context: Context): List<ShortcutEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val out = ArrayList<ShortcutEntry>()
        val seen = HashSet<ShortcutKey>()
        val resByPkg = HashMap<String, Resources?>()
        val labelByPkg = HashMap<String, String>()
        val updateByPkg = HashMap<String, Long>()

        for (info in activities) {
            val act = info.activityInfo ?: continue
            val pkg = act.packageName
            if (pkg == context.packageName) continue

            val parser = act.loadXmlMetaData(pm, SHORTCUTS_META) ?: continue
            val res = resByPkg.getOrPut(pkg) {
                runCatching { pm.getResourcesForApplication(pkg) }.getOrNull()
            }
            if (res == null) {
                parser.close()
                continue
            }
            val appLabel = labelByPkg.getOrPut(pkg) { info.loadLabel(pm).toString() }
            val lastUpdate = updateByPkg.getOrPut(pkg) {
                runCatching { pm.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)
            }

            try {
                parseShortcuts(parser, pm, res, pkg, appLabel, lastUpdate, seen, out)
            } catch (_: Exception) {
                // A malformed XML in one app must not sink the whole index.
            } finally {
                parser.close()
            }
        }
        return out
    }

    private fun parseShortcuts(
        parser: XmlResourceParser,
        pm: PackageManager,
        res: Resources,
        pkg: String,
        appLabel: String,
        lastUpdate: Long,
        seen: HashSet<ShortcutKey>,
        out: MutableList<ShortcutEntry>,
    ) {
        var inShortcut = false
        var id: String? = null
        var enabled = true
        var shortLabel: String? = null
        var iconResId = 0
        var action: String? = null
        var data: String? = null
        var targetPackage: String? = null
        var targetClass: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "shortcut" -> {
                        inShortcut = true
                        id = parser.getAttributeValue(ANDROID_NS, "shortcutId")
                        enabled = parser.getAttributeBooleanValue(ANDROID_NS, "enabled", true)
                        val slRes = parser.getAttributeResourceValue(ANDROID_NS, "shortcutShortLabel", 0)
                        shortLabel = if (slRes != 0) {
                            runCatching { res.getString(slRes) }.getOrNull()
                        } else {
                            parser.getAttributeValue(ANDROID_NS, "shortcutShortLabel")
                        }
                        iconResId = parser.getAttributeResourceValue(ANDROID_NS, "icon", 0)
                        // Reset intent accumulators for this shortcut.
                        action = null; data = null; targetPackage = null; targetClass = null
                    }
                    // A shortcut may declare several <intent> (a synthetic back
                    // stack); the last one is the one actually launched, so we
                    // simply let each overwrite the accumulators.
                    "intent" -> if (inShortcut) {
                        action = parser.getAttributeValue(ANDROID_NS, "action")
                        data = parser.getAttributeValue(ANDROID_NS, "data")
                        targetPackage = parser.getAttributeValue(ANDROID_NS, "targetPackage")
                        targetClass = parser.getAttributeValue(ANDROID_NS, "targetClass")
                    }
                }

                XmlPullParser.END_TAG -> if (parser.name == "shortcut" && inShortcut) {
                    inShortcut = false
                    buildShortcut(
                        pm, pkg, appLabel, lastUpdate,
                        id, enabled, shortLabel, iconResId,
                        action, data, targetPackage, targetClass,
                    )?.let { entry ->
                        if (seen.add(entry.key)) out += entry
                    }
                }
            }
            event = parser.next()
        }
    }

    /**
     * Build the launchable [ShortcutEntry], or null if the shortcut is disabled,
     * incomplete, or not launchable by us. Launchability: an explicit component
     * must be exported + enabled (the system launches manifest shortcuts with a
     * privilege we don't have); an action-only intent must resolve.
     */
    private fun buildShortcut(
        pm: PackageManager,
        pkg: String,
        appLabel: String,
        lastUpdate: Long,
        id: String?,
        enabled: Boolean,
        shortLabel: String?,
        iconResId: Int,
        action: String?,
        data: String?,
        targetPackage: String?,
        targetClass: String?,
    ): ShortcutEntry? {
        if (!enabled) return null
        if (id.isNullOrBlank()) return null
        if (shortLabel.isNullOrBlank()) return null

        val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (action != null) intent.action = action
        if (data != null) intent.data = Uri.parse(data)
        when {
            targetPackage != null && targetClass != null ->
                intent.setClassName(targetPackage, targetClass)
            targetPackage != null -> intent.setPackage(targetPackage)
        }

        val launchable = if (targetPackage != null && targetClass != null) {
            val ai = runCatching {
                pm.getActivityInfo(ComponentName(targetPackage, targetClass), 0)
            }.getOrNull()
            ai != null && ai.exported && ai.enabled
        } else {
            intent.resolveActivity(pm) != null
        }
        if (!launchable) return null

        return ShortcutEntry(
            packageName = pkg,
            shortcutId = id,
            shortLabel = shortLabel,
            appLabel = appLabel,
            iconResId = iconResId,
            launchIntent = intent,
            lastUpdateTime = lastUpdate,
        )
    }
}
