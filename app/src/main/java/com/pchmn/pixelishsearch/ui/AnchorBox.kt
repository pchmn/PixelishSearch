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


/**
 * Box that exposes its horizontal center (in screen-space px) to its [content].
 * Useful for positioning a callout popup aligned with the anchor.
 *
 * @param content Receives `xCenter`: the anchor's horizontal center in screen-space px.
 */
@Composable
fun AnchorBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.(xCenter: Int) -> Unit,
) {
    var xCenter by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            xCenter =
                (coords.localToScreen(Offset.Zero).x + coords.size.width / 2f).toInt()
        },
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints
    ) {
        xCenter?.let {
            content(it)
        }
    }
}