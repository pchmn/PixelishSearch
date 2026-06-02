package com.pchmn.pixelishsearch.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.PixelishSearchApp
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.core.ui.components.BottomSheet
import com.pchmn.pixelishsearch.core.ui.components.rememberSheetState
import com.pchmn.pixelishsearch.search.apps.ui.AppList
import com.pchmn.pixelishsearch.search.calendar.ui.CalendarResultList
import com.pchmn.pixelishsearch.search.contacts.ui.ContactResultList
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageIndex
import com.pchmn.pixelishsearch.search.settings.ui.SettingsPageList
import com.pchmn.pixelishsearch.search.settings.ui.SettingsTileGrid
import com.pchmn.pixelishsearch.search.shortcuts.ui.ShortcutResultList
import com.pchmn.pixelishsearch.search.web.ui.WebSearchList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onClose: () -> Unit
) {
    val sheetState = rememberSheetState(
        initialValue = SheetValue.Expanded,
        skipPartiallyExpanded = true
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as PixelishSearchApp
    val updateInfo by app.updates.available.collectAsStateWithLifecycle()

    val displayedApps = if (uiState.query.isBlank()) {
        uiState.appRecents
    } else {
        uiState.appResults
    }.take(4)

    BottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            SearchField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                onGeminiClick = viewModel::onGeminiClick,
                onLensClick = viewModel::onLensClick,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.onSearchSubmit(uiState.query) }
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppList(
                apps = displayedApps,
                highlightFirst = uiState.query.isNotBlank(),
                onAppClick = viewModel::onAppClick,
                onAppInfo = viewModel::onAppInfo,
                onAddToHomeScreen = viewModel::onPinAppShortcut,
                onHideFromRecents = { entry -> viewModel.hideAppFromRecents(entry.packageName) },
            )

            if (displayedApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (uiState.query.isBlank()) {
                if (uiState.fusedRecents.isNotEmpty()) {
                    FusedRecentsList(
                        entries = uiState.fusedRecents,
                        iconRequest = SettingsPageIndex.iconRequest,
                        onContactClick = viewModel::onRecentContactClick,
                        onContactDelete = viewModel::removeRecentContact,
                        onPageClick = viewModel::onRecentSettingsPageClick,
                        onPageDelete = viewModel::removeRecentSettingsPage,
                        onShortcutClick = viewModel::onRecentShortcutClick,
                        onShortcutDelete = viewModel::removeRecentShortcut,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                WebSearchList(
                    suggestions = uiState.webRecents.take(3),
                    leadingIcon = R.drawable.ic_schedule,
                    onClick = viewModel::onWebSuggestionClick,
                    onDelete = viewModel::removeSearchHistory,
                )
            } else {
                if (uiState.tileResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsTileGrid(
                        tiles = uiState.tileResults,
                        onClick = viewModel::onTileTap,
                    )
                }

                if (uiState.webResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = stringResource(R.string.search_section_web))
                    WebSearchList(
                        suggestions = uiState.webResults.take(if (displayedApps.isNotEmpty() || uiState.contactResults.isNotEmpty() || uiState.calendarResults.isNotEmpty() || uiState.shortcutResults.isNotEmpty()) 3 else 5),
                        leadingIcon = R.drawable.ic_search,
                        onClick = viewModel::onWebSuggestionClick,
                    )
                }

                if (uiState.shortcutResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = stringResource(R.string.search_section_shortcuts))
                    ShortcutResultList(
                        shortcuts = uiState.shortcutResults,
                        onClick = viewModel::onShortcutClick,
                    )
                }

                if (uiState.contactResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = stringResource(R.string.search_section_contacts))
                    ContactResultList(
                        contacts = uiState.contactResults,
                        onContactClick = viewModel::onContactClick,
                        onMessageClick = viewModel::onContactMessage,
                        onCallClick = viewModel::onContactCall,
                    )
                }

                if (uiState.calendarResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = stringResource(R.string.search_section_calendar))
                    CalendarResultList(
                        events = uiState.calendarResults,
                        onEventClick = viewModel::onCalendarEventClick,
                    )
                }

                if (uiState.settingsPageResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = stringResource(R.string.search_section_settings))
                    SettingsPageList(
                        pages = uiState.settingsPageResults,
                        onClick = viewModel::onSettingsPageClick,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (updateInfo != null) {
                    IconButton(onClick = viewModel::onOpenUpdate) {
                        Icon(
                            painter = painterResource(R.drawable.ic_deployed_code_update),
                            contentDescription = stringResource(R.string.update_available_badge),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                IconButton(onClick = viewModel::onOpenPreferences) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.preferences_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
    )
}
