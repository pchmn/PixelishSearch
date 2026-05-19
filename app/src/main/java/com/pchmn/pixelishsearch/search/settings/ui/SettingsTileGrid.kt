package com.pchmn.pixelishsearch.search.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.search.settings.data.SettingsTile
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileResult

/**
 * Two-column grid of quick-toggle chips. Each chip is exactly half the
 * available width (minus the inter-chip gap). A solo last row leaves the
 * second slot empty so the chip still occupies its half — never expands to
 * full width.
 */
@Composable
fun SettingsTileGrid(
    tiles: List<SettingsTileResult>,
    onClick: (SettingsTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsTileChip(
                    tile = pair[0].tile,
                    isActive = pair[0].isActive,
                    onClick = { onClick(pair[0].tile) },
                    modifier = Modifier.weight(1f),
                )
                if (pair.size > 1) {
                    SettingsTileChip(
                        tile = pair[1].tile,
                        isActive = pair[1].isActive,
                        onClick = { onClick(pair[1].tile) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsTileChip(
    tile: SettingsTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (isActive) 24.dp else 48.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.4f
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(
                if (!isActive && tile.inactiveIconRes != null) tile.inactiveIconRes else tile.iconRes
            ),
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(tile.labelRes),
            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            fontWeight = FontWeight.Medium
        )
    }
}
