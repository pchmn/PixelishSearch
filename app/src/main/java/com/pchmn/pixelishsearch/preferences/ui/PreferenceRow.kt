package com.pchmn.pixelishsearch.preferences.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pchmn.pixelishsearch.core.ui.components.ClickableRow

@Composable
fun PreferenceRow(
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    @DrawableRes leadingIcon: Int? = null,
    ending: @Composable (() -> Unit)? = null,
    title: String,
    subtitle: String? = null,
) {
    ClickableRow(
        padding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        outerRadius = 24.dp,
        bgColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (ending != null) {
            Spacer(Modifier.width(32.dp))
            ending()
        }
    }
}
