package com.pchmn.pixelishsearch.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.ui.dropdown.DropdownMenuWithArrow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryRow(
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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
    var leadingBounds by remember { mutableStateOf<IntRect?>(null) }

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
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnchorBox { bounds ->
                leadingBounds = bounds
                leading()
            }
            Spacer(modifier = Modifier.width(16.dp))
            content()
        }
        if (onDelete != null) {
            leadingBounds?.let { bounds ->
                DropdownMenuWithArrow(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    anchorBounds = bounds,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(com.pchmn.pixelishsearch.R.string.action_delete)) },
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
                        contentPadding = PaddingValues(16.dp)
                    )
                }
            }
        }
    }
}