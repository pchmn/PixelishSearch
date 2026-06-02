package com.pchmn.pixelishsearch.search.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.core.ui.components.EntryRow
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import com.pchmn.pixelishsearch.search.calendar.data.CalendarEventEntry
import com.pchmn.pixelishsearch.search.calendar.utils.formatEventWhen

@Composable
fun CalendarResultRow(
    event: CalendarEventEntry,
    iconRequest: AppIconRequest?,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    EntryRow(
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        leading = { CalendarLeadingIcon(iconRequest) },
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        ) {
            Text(
                text = event.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatEventWhen(context, event.begin, event.allDay),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarLeadingIcon(iconRequest: AppIconRequest?) {
    val modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
    if (iconRequest != null) {
        AsyncImage(
            model = iconRequest,
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
