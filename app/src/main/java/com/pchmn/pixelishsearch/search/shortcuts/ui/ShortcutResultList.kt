package com.pchmn.pixelishsearch.search.shortcuts.ui

import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.core.ui.components.EntryList
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutEntry

@Composable
fun ShortcutResultList(
    shortcuts: List<ShortcutEntry>,
    onClick: (ShortcutEntry) -> Unit,
) {
    EntryList(entries = shortcuts) {
        shortcuts.forEachIndexed { index, entry ->
            ShortcutRow(
                shortLabel = entry.shortLabel,
                appLabel = entry.appLabel,
                packageName = entry.packageName,
                iconResId = entry.iconResId,
                lastUpdateTime = entry.lastUpdateTime,
                isFirst = index == 0,
                isLast = index == shortcuts.lastIndex,
                onClick = { onClick(entry) },
            )
        }
    }
}
