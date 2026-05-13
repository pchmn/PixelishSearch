package com.pchmn.pixelishsearch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect


/**
 * Box that exposes its bounds (in screen-space px) to its [content].
 * Useful for positioning a callout popup aligned with the anchor.
 *
 * @param content Receives `bounds`: the anchor's bounds in screen-space px.
 */
@Composable
fun AnchorBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.(bounds: IntRect) -> Unit,
) {
    var bounds by remember { mutableStateOf<IntRect?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            val topLeft = coords.localToScreen(Offset.Zero)
            bounds = IntRect(
                left = topLeft.x.toInt(),
                top = topLeft.y.toInt(),
                right = (topLeft.x + coords.size.width).toInt(),
                bottom = (topLeft.y + coords.size.height).toInt(),
            )
        },
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints
    ) {
        bounds?.let {
            content(it)
        }
    }
}
