package com.pchmn.pixelishsearch.ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pchmn.pixelishsearch.data.AppEntry
import com.pchmn.pixelishsearch.data.AppIconRequest


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    entry: AppEntry,
    highlighted: Boolean,
    onClick: () -> Unit,
    onAppInfo: () -> Unit,
    onAddToHomeScreen: () -> Unit,
    onHideFromRecents: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val backgroundColor = if (highlighted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    var menuExpanded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var iconCenterXPx by remember { mutableIntStateOf(0) }
    var menuLeftXPx by remember { mutableStateOf<Int?>(null) }

    val tipXDp = menuLeftXPx?.let {
        with(density) { (iconCenterXPx - it).toDp() }
    } ?: (APP_SLOT_WIDTH / 2)

    val calloutShape = remember(tipXDp) {
        CalloutShape(
            cornerRadius = 28.dp,
            tipWidth = 12.dp,
            tipHeight = 8.dp,
            tipX = tipXDp,
            tipCornerRadius = 2.dp,
            bottomInset = 8.dp,
        )
    }

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            iconCenterXPx =
                (coords.localToScreen(Offset.Zero).x + coords.size.width / 2f).toInt()
        },
    ) {
        Column(
            modifier = Modifier
                .width(APP_SLOT_WIDTH)
                .clip(shape)
                .background(backgroundColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                )
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
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = calloutShape,
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    menuLeftXPx = coords.localToScreen(Offset.Zero).x.toInt()
                },
        ) {
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onAppInfo()
                },
                contentPadding = PaddingValues(16.dp, top = 16.dp, end = 22.dp, bottom = 16.dp)
            )
            DropdownMenuItem(
                text = { Text("Add to home screen") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onAddToHomeScreen()
                },
                contentPadding = PaddingValues(16.dp, top = 16.dp, end = 22.dp, bottom = 16.dp)
            )
            DropdownMenuItem(
                text = { Text("Don't suggest app") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onHideFromRecents()
                },
                contentPadding = PaddingValues(16.dp, top = 16.dp, end = 22.dp, bottom = 16.dp)
            )
        }
    }
}
