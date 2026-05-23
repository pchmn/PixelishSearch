package com.pchmn.pixelishsearch.search.settings.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.core.ui.components.EntryRow
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageEntry

enum class RowType { RESULT, RECENT }

@Composable
fun SettingsPageRow(
    entry: SettingsPageEntry,
    iconRequest: AppIconRequest?,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    type: RowType = RowType.RESULT,
    onDelete: (() -> Unit)? = null,
) {
    EntryRow(
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        onDelete = onDelete,
        leading = {
            AsyncImage(
                model = iconRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
        },
    ) {
        Text(
            text = entry.label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        if (type == RowType.RECENT) {
            Text(
                text = " · ${stringResource(R.string.preferences_title)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
