package com.pchmn.pixelishsearch.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableRow(
    bgColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    outerRadius: Dp = 28.dp,
    innerRadius: Dp = 6.dp,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outerRadius else innerRadius,
        topEnd = if (isFirst) outerRadius else innerRadius,
        bottomStart = if (isLast) outerRadius else innerRadius,
        bottomEnd = if (isLast) outerRadius else innerRadius,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}