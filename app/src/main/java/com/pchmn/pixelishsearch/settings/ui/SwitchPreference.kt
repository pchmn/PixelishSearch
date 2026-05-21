package com.pchmn.pixelishsearch.settings.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable

@Composable
fun SwitchPreference(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String? = null,
    isFirst: Boolean,
    isLast: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    PreferenceRow(
        isFirst = isFirst,
        isLast = isLast,
        leadingIcon = icon,
        title = title,
        subtitle = subtitle,
        ending = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        onClick = { onCheckedChange(!checked) })
}