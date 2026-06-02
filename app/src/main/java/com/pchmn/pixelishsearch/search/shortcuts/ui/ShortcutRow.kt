package com.pchmn.pixelishsearch.search.shortcuts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pchmn.pixelishsearch.core.ui.components.EntryRow
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutIconRequest

/**
 * One App shortcut row, shared by the query-results section and the blank-state
 * Recents strip. Renders "shortLabel · App" (the parent-app suffix disambiguates
 * homonymous shortcuts), with the shortcut's own icon on a white disc badged
 * with the parent app's icon — or, when the shortcut declares no icon, just the
 * parent app's icon. Both go through Coil.
 */
@Composable
fun ShortcutRow(
    shortLabel: String,
    appLabel: String,
    packageName: String,
    iconResId: Int,
    lastUpdateTime: Long,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    EntryRow(
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        onDelete = onDelete,
        leading = {
            ShortcutLeadingIcon(
                packageName = packageName,
                iconResId = iconResId,
                lastUpdateTime = lastUpdateTime,
            )
        },
    ) {
        Text(
            text = shortLabel,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = " · $appLabel",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Shortcut glyph on a white disc, badged bottom-right with the parent app's icon
 * so the row reads as "this app's shortcut". The badge is only drawn when the
 * shortcut ships its own icon; otherwise the leading slot *is* the app icon and a
 * badge would be redundant.
 */
@Composable
private fun ShortcutLeadingIcon(
    packageName: String,
    iconResId: Int,
    lastUpdateTime: Long,
) {
    if (iconResId == 0) {
        AsyncImage(
            model = AppIconRequest(packageName, lastUpdateTime),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
        )
        return
    }

    Box(modifier = Modifier.size(32.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ShortcutIconRequest(packageName, iconResId, lastUpdateTime),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        AsyncImage(
            model = AppIconRequest(packageName, lastUpdateTime),
            contentDescription = null,
            modifier = Modifier
                .size(15.dp)
                .align(Alignment.BottomEnd)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape),
        )
    }
}
