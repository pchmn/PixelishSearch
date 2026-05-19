package com.pchmn.pixelishsearch.search.settings.data

import android.content.Context
import java.text.Normalizer

/**
 * Filters the static list of quick-toggle tiles against a search query.
 * Matches against the localized label (contains) and the per-tile English
 * keyword list (startsWith). Accent-insensitive.
 *
 * Minimum query length is enforced by the caller — keep this object pure.
 */
object SettingsTileRepository {

    private const val MIN_QUERY_LENGTH = 2

    fun search(context: Context, query: String, limit: Int = 4): List<SettingsTileResult> {
        val needle = query.trim().normalize()
        if (needle.length < MIN_QUERY_LENGTH) return emptyList()
        return settingsTiles
            .filter { tile ->
                val label = context.getString(tile.labelRes).normalize()
                label.contains(needle) ||
                        tile.keywords.any { it.normalize().startsWith(needle) }
            }
            .take(limit)
            .map { tile -> SettingsTileResult(tile, tile.id.isActive(context)) }
    }

    private fun String.normalize(): String =
        Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")

    private val DIACRITICS = Regex("\\p{Mn}+")
}
