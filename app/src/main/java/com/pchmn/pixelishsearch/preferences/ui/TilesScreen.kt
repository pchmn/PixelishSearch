package com.pchmn.pixelishsearch.preferences.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.search.settings.data.SettingsTile
import com.pchmn.pixelishsearch.search.settings.data.settingsTiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TilesScreen(
    viewModel: PreferencesViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val disabledIds = uiState.disabledTileIds

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.preferences_tiles_title),
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onBack,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.preferences_tiles_subtitle),
                modifier = Modifier.padding(start = 26.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                settingsTiles.forEachIndexed { index, tile ->
                    TileToggleRow(
                        isFirst = index == 0,
                        isLast = index == settingsTiles.lastIndex,
                        tile = tile,
                        checked = tile.id.name !in disabledIds,
                        onCheckedChange = { enabled -> viewModel.onTileToggled(tile.id, enabled) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TileToggleRow(
    tile: SettingsTile,
    checked: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        isFirst = isFirst,
        isLast = isLast,
        icon = tile.iconRes,
        title = stringResource(tile.labelRes),
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}
