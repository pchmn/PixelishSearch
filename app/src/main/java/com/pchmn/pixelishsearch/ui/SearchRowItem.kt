package com.pchmn.pixelishsearch.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchRowItem(
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val outer = 28.dp
    val inner = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outer else inner,
        topEnd = if (isFirst) outer else inner,
        bottomStart = if (isLast) outer else inner,
        bottomEnd = if (isLast) outer else inner,
    )
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .then(
                    if (onDelete != null) {
                        Modifier.combinedClickable(
                            onClick = onClick,
                            onLongClick = { menuExpanded = true },
                        )
                    } else {
                        Modifier.clickable(onClick = onClick)
                    }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(modifier = Modifier.width(16.dp))
            content()
        }
        if (onDelete != null) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(28.dp),
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}