package com.pchmn.pixelishsearch.search.apps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.search.apps.data.AppEntry

val APP_SLOT_WIDTH = 88.dp

@Composable
fun AppList(
    apps: List<AppEntry>,
    highlightFirst: Boolean,
    onAppClick: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onAddToHomeScreen: (AppEntry) -> Unit,
    onHideFromRecents: (AppEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Always 4 fixed-width slots — keeps the layout stable even if
        // fewer than 4 apps match.
        repeat(4) { index ->
            Box(
                modifier = Modifier.width(APP_SLOT_WIDTH),
                contentAlignment = Alignment.TopCenter,
            ) {
                apps.getOrNull(index)?.let { entry ->
                    AppItem(
                        entry = entry,
                        highlighted = highlightFirst && index == 0,
                        onClick = { onAppClick(entry) },
                        onAppInfo = { onAppInfo(entry) },
                        onAddToHomeScreen = { onAddToHomeScreen(entry) },
                        onHideFromRecents = { onHideFromRecents(entry) },
                    )
                }
            }
        }
    }
}