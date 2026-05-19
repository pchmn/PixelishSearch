package com.pchmn.pixelishsearch.search.settings.ui

import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.core.ui.components.EntryList
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageEntry
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageIndex

@Composable
fun SettingsPageList(
    pages: List<SettingsPageEntry>,
    onClick: (SettingsPageEntry) -> Unit,
) {
    val iconRequest = SettingsPageIndex.iconRequest
    EntryList(entries = pages) {
        pages.forEachIndexed { index, entry ->
            SettingsPageRow(
                entry = entry,
                iconRequest = iconRequest,
                isFirst = index == 0,
                isLast = index == pages.lastIndex,
                onClick = { onClick(entry) },
            )
        }
    }
}
