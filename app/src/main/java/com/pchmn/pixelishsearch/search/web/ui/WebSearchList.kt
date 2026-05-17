package com.pchmn.pixelishsearch.search.web.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.core.ui.components.EntryList

@Composable
fun WebSearchList(
    suggestions: List<String>,
    @DrawableRes leadingIcon: Int,
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
