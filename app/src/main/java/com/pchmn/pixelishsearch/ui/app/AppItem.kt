package com.pchmn.pixelishsearch.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pchmn.pixelishsearch.data.AppEntry
import com.pchmn.pixelishsearch.data.AppIconRequest


@Composable
fun AppItem(
    entry: AppEntry,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val backgroundColor = if (highlighted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .width(APP_SLOT_WIDTH)
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = AppIconRequest(entry.packageName, entry.lastUpdateTime),
            contentDescription = entry.label,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}