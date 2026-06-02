package com.pchmn.pixelishsearch.search.ui

import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.core.ui.components.EntryList
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.search.contacts.ui.ContactRecentRow
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageEntry
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageHistoryEntry
import com.pchmn.pixelishsearch.search.settings.ui.RowType
import com.pchmn.pixelishsearch.search.settings.ui.SettingsPageRow
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutHistoryEntry
import com.pchmn.pixelishsearch.search.shortcuts.ui.ShortcutRow

/**
 * Blank-state recents block fusing contacts + settings pages + app shortcuts
 * into a single ranked list. Dispatch on [RecentEntity] subtype to render the
 * right row.
 */
@Composable
fun FusedRecentsList(
    entries: List<RecentEntity>,
    iconRequest: AppIconRequest?,
    onContactClick: (ContactHistoryEntry) -> Unit,
    onContactDelete: (ContactHistoryEntry) -> Unit,
    onPageClick: (SettingsPageHistoryEntry) -> Unit,
    onPageDelete: (SettingsPageHistoryEntry) -> Unit,
    onShortcutClick: (ShortcutHistoryEntry) -> Unit,
    onShortcutDelete: (ShortcutHistoryEntry) -> Unit,
) {
    EntryList(entries = entries) {
        entries.forEachIndexed { index, entity ->
            val isFirst = index == 0
            val isLast = index == entries.lastIndex
            when (entity) {
                is RecentEntity.Contact -> ContactRecentRow(
                    contact = entity.entry,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = { onContactClick(entity.entry) },
                    onDelete = { onContactDelete(entity.entry) },
                )

                is RecentEntity.SettingsPage -> SettingsPageRow(
                    type = RowType.RECENT,
                    entry = SettingsPageEntry(
                        label = entity.entry.label,
                        component = entity.entry.component,
                    ),
                    iconRequest = iconRequest,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = { onPageClick(entity.entry) },
                    onDelete = { onPageDelete(entity.entry) },
                )

                is RecentEntity.Shortcut -> ShortcutRow(
                    shortLabel = entity.entry.shortLabel,
                    appLabel = entity.entry.appLabel,
                    packageName = entity.entry.packageName,
                    iconResId = entity.entry.iconResId,
                    lastUpdateTime = entity.entry.lastUpdateTime,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = { onShortcutClick(entity.entry) },
                    onDelete = { onShortcutDelete(entity.entry) },
                )
            }
        }
    }
}
