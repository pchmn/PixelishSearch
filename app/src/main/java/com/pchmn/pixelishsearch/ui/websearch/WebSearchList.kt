package com.pchmn.pixelishsearch.ui.websearch

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.pchmn.pixelishsearch.ui.EntryList

@Composable
fun WebSearchList(
    suggestions: List<String>,
    leadingIcon: ImageVector,
    onClick: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    EntryList(entries = suggestions) {
        suggestions.forEachIndexed { index, suggestion ->
            WebSearchRow(
                text = suggestion,
                leadingIcon = leadingIcon,
                isFirst = index == 0,
                isLast = index == suggestions.lastIndex,
                onClick = { onClick(suggestion) },
                onDelete = onDelete?.let { { it(suggestion) } },
            )
        }
    }
}