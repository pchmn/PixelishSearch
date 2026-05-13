package com.pchmn.pixelishsearch.ui.dropdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.ui.app.APP_SLOT_WIDTH

private val CornerRadius = 28.dp
private val TipWidth = 12.dp
private val TipHeight = 8.dp
private val TipCornerRadius = 2.dp
private val BottomInset = 8.dp
private val MinTipX = CornerRadius + TipWidth / 2

@Composable
fun DropdownMenuWithArrow(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset(y = 0.dp, x = 0.dp),
    anchorXCenter: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    var menuLeftXPx by remember { mutableStateOf<Int?>(null) }
    var autoOffsetX by remember { mutableStateOf(0.dp) }

    val tipXDp = menuLeftXPx?.let {
        with(density) { (anchorXCenter - it).toDp() }
    } ?: (APP_SLOT_WIDTH / 2)

    // If the anchor sits inside the corner zone (tipXDp < MinTipX), the CalloutShape
    // would clamp the arrow away from the anchor. Shift the menu left by the missing
    // delta so the tip lands exactly at the anchor center without being clamped.
    LaunchedEffect(menuLeftXPx, anchorXCenter, density) {
        val left = menuLeftXPx ?: return@LaunchedEffect
        val naturalLeftPx = left - with(density) { autoOffsetX.roundToPx() }
        val naturalTipDp = with(density) { (anchorXCenter - naturalLeftPx).toDp() }
        val target = (naturalTipDp - MinTipX).coerceAtMost(0.dp)
        if (target != autoOffsetX) {
            autoOffsetX = target
        }
    }

    val calloutShape = remember(tipXDp) {
        CalloutShape(
            cornerRadius = CornerRadius,
            tipWidth = TipWidth,
            tipHeight = TipHeight,
            tipX = tipXDp,
            tipCornerRadius = TipCornerRadius,
            bottomInset = BottomInset,
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
        offset = DpOffset(x = offset.x + autoOffsetX, y = offset.y),
    ) {
        content()
    }
}