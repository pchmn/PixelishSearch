package com.pchmn.pixelishsearch.preferences.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.search.settings.data.settingsTiles
import com.pchmn.pixelishsearch.update.data.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    viewModel: PreferencesViewModel,
    onBack: () -> Unit,
    onOpenTiles: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The system permission dialog must be launched from composition. The VM
    // owns the decision and the resulting state; the screen only fires the
    // dialog and reports the outcome back.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onContactsPermissionResult(granted) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.preferences_title),
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
            PreferencesGroup(title = stringResource(R.string.preferences_section_search)) {
                SwitchPreference(
                    icon = R.drawable.ic_contacts,
                    title = stringResource(R.string.preferences_contact_search_title),
                    subtitle = stringResource(R.string.preferences_contact_search_subtitle),
                    isFirst = true,
                    isLast = false,
                    checked = uiState.effectiveContactSearch,
                    onCheckedChange = { newValue ->
                        if (newValue && !uiState.hasContactsPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            viewModel.setContactSearch(newValue)
                        }
                    },
                )
                NavigationPreference(
                    isFirst = false,
                    isLast = true,
                    icon = R.drawable.ic_location_chip,
                    title = stringResource(R.string.preferences_tiles_title),
                    subtitle = stringResource(
                        R.string.preferences_tiles_count,
                        settingsTiles.size - uiState.disabledTileIds.size,
                    ),
                    onClick = onOpenTiles,
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PreferencesGroup(title = stringResource(R.string.preferences_section_appearance)) {
                    LanguagePreference(
                        currentTag = uiState.currentLanguageTag,
                        onSelect = viewModel::onLanguageSelected,
                    )
                }
            }

            PreferencesGroup(title = stringResource(R.string.preferences_section_about)) {
                UpdateCheckPreference(
                    available = uiState.updateAvailable,
                    currentVersion = uiState.currentVersion,
                    checkState = uiState.updateCheck,
                    onCheck = viewModel::onCheckForUpdates,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UpdateCheckPreference(
    available: UpdateInfo?,
    currentVersion: String,
    checkState: CheckUiState,
    onCheck: () -> Unit,
) {
    val subtitle = when {
        available != null -> stringResource(
            R.string.update_settings_available,
            available.versionName,
        )

        checkState is CheckUiState.Checking -> stringResource(R.string.update_status_checking)
        checkState is CheckUiState.UpToDate -> stringResource(R.string.update_status_up_to_date)
        checkState is CheckUiState.Failed -> stringResource(R.string.update_status_check_failed)
        else -> stringResource(R.string.update_settings_current_version, currentVersion)
    }

    val isChecking = checkState is CheckUiState.Checking

    PreferenceRow(
        isFirst = true,
        isLast = true,
        leadingIcon = R.drawable.ic_deployed_code_update,
        title = stringResource(R.string.update_settings_title),
        subtitle = subtitle,
        ending = {
            if (isChecking) {
                Spacer(Modifier.width(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        onClick = { if (!isChecking) onCheck() },
    )
}

@Composable
private fun LanguagePreference(
    currentTag: String?,
    onSelect: (String?) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    val options: List<Pair<String?, String>> = listOf(
        null to stringResource(R.string.preferences_language_system),
        "en" to stringResource(R.string.language_en),
        "fr" to stringResource(R.string.language_fr),
        "es" to stringResource(R.string.language_es),
        "de" to stringResource(R.string.language_de),
        "it" to stringResource(R.string.language_it),
    )
    val currentLabel = options.firstOrNull { it.first == currentTag }?.second ?: options[0].second

    PreferenceRow(
        isFirst = true,
        isLast = true,
        leadingIcon = R.drawable.ic_language,
        title = stringResource(R.string.preferences_language_title),
        subtitle = currentLabel,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.preferences_language_title)) },
            text = {
                Column {
                    options.forEach { (tag, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .selectable(
                                    selected = tag == currentTag,
                                    onClick = {
                                        onSelect(tag)
                                        showDialog = false
                                    },
                                )
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = tag == currentTag,
                                onClick = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = label, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 10.dp, top = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun PreferencesGroup(title: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (title != null) {
            SectionHeader(title)
        }
        content()
    }
}

@Composable
private fun NavigationPreference(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String? = null,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    PreferenceRow(
        isFirst = isFirst,
        isLast = isLast,
        leadingIcon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick
    )
}
