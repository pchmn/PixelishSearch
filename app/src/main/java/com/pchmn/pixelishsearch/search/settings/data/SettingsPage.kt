package com.pchmn.pixelishsearch.search.settings.data

import android.content.ComponentName

/**
 * A Settings sub-page surfaced by [SettingsPageIndex]. Discovered dynamically
 * at preload by querying `ACTION_MAIN` activities in the Settings package —
 * the label comes from `ResolveInfo.loadLabel` (already localized), and the
 * component is launched directly so OEM-specific pages without a public
 * `Settings.ACTION_*` constant are still reachable (Pixel "Modes", "Satellite",
 * "One-handed", etc.). The icon is shared across all entries via
 * [SettingsPageIndex.iconRequest].
 */
data class SettingsPageEntry(
    val label: String,
    val component: ComponentName,
)
