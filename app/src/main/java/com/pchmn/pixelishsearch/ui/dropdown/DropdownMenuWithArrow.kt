package com.pchmn.pixelishsearch.ui.dropdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.ui.app.APP_SLOT_WIDTH

@Composable
fun DropdownMenuWithArrow(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorXCenter: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    var menuLeftXPx by remember { mutableStateOf<Int?>(null) }

    val tipXDp = menuLeftXPx?.let {
        with(density) { (anchorXCenter - it).toDp() }
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

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = calloutShape,
        modifier = Modifier
            .onGloballyPositioned { coords ->
                menuLeftXPx = coords.localToScreen(Offset.Zero).x.toInt()
            },
    ) {
        content()
    }
}