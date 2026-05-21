package com.pchmn.pixelishsearch.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.core.ui.components.dropdown.DropdownMenuWithArrow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryRow(
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var leadingBounds by remember { mutableStateOf<IntRect?>(null) }

    Box {
        ClickableRow(
            padding = padding,
            bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            isFirst = isFirst,
            isLast = isLast,
            onClick = onClick,
            onLongClick = { menuExpanded = true }) {
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
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
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