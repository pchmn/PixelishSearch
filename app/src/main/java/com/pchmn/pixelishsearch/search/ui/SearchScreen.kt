package com.pchmn.pixelishsearch.search.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.pchmn.pixelishsearch.core.data.launchAndDismiss
import com.pchmn.pixelishsearch.core.ui.components.BottomSheet
import com.pchmn.pixelishsearch.core.ui.components.rememberSheetState
import com.pchmn.pixelishsearch.search.apps.data.geminiIntent
import com.pchmn.pixelishsearch.search.apps.data.launchAppInfo
import com.pchmn.pixelishsearch.search.apps.data.lensIntent
import com.pchmn.pixelishsearch.search.apps.data.pinAppShortcut
import com.pchmn.pixelishsearch.search.apps.ui.AppList
import com.pchmn.pixelishsearch.search.contacts.data.ContactAction
import com.pchmn.pixelishsearch.search.contacts.data.launchContactDetails
import com.pchmn.pixelishsearch.search.contacts.data.launchDialer
import com.pchmn.pixelishsearch.search.contacts.data.launchSms
import com.pchmn.pixelishsearch.search.contacts.ui.ContactRecentList
import com.pchmn.pixelishsearch.search.contacts.ui.ContactResultList
import com.pchmn.pixelishsearch.search.contacts.utils.replayContactAction
import com.pchmn.pixelishsearch.search.settings.data.launchSettingsTile
import com.pchmn.pixelishsearch.search.settings.ui.SettingsTileGrid
import com.pchmn.pixelishsearch.search.web.data.launchGoogleSearch
import com.pchmn.pixelishsearch.search.web.ui.WebSearchList
import com.pchmn.pixelishsearch.settings.SettingsActivity
import com.pchmn.pixelishsearch.update.UpdateActivity

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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
            ) {
                SearchField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    onGeminiClick = {
                        geminiIntent(context)?.let {
                            context.launchAndDismiss(it)
                        }
                    },
                    onLensClick = {
                        lensIntent(context)?.let {
                            context.launchAndDismiss(it)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val query = uiState.query.trim()
                            if (query.isNotEmpty()) {
                                viewModel.onSearchLaunched(query)
                                launchGoogleSearch(context, query)
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppList(
                    apps = displayedApps,
                    highlightFirst = uiState.query.isNotBlank(),
                    onAppClick = { entry ->
                        viewModel.onAppLaunched(entry.packageName)
                        context.launchAndDismiss(entry.launchIntent)
                    },
                    onAppInfo = { entry -> launchAppInfo(context, entry.packageName) },
                    onAddToHomeScreen = { entry -> pinAppShortcut(context, entry) },
                    onHideFromRecents = { entry -> viewModel.hideAppFromRecents(entry.packageName) },
                )

                if (displayedApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.query.isBlank()) {
                    if (uiState.contactRecents.isNotEmpty()) {
                        ContactRecentList(
                            contacts = uiState.contactRecents.take(2),
                            onClick = { entry ->
                                replayContactAction(context, entry)
                            },
                            onDelete = viewModel::removeRecentContact,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    WebSearchList(
                        suggestions = uiState.webRecents.take(3),
                        leadingIcon = R.drawable.ic_schedule,
                        onClick = { suggestion ->
                            viewModel.onSearchLaunched(suggestion)
                            launchGoogleSearch(context, suggestion)
                        },
                        onDelete = viewModel::removeSearchHistory,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (updateInfo != null) {
                            IconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(context, UpdateActivity::class.java)
                                    )
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_deployed_code_update),
                                    contentDescription = stringResource(R.string.update_available_badge),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                context.startActivity(
                                    Intent(context, SettingsActivity::class.java)
                                )
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.settings_title),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                } else {
                    if (uiState.tileResults.isNotEmpty()) {
                        SettingsTileGrid(
                            tiles = uiState.tileResults,
                            onClick = { tile ->
                                launchSettingsTile(context, tile.id)
                                onClose()
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.webResults.isNotEmpty()) {
                        SectionHeader(title = stringResource(R.string.search_section_web))
                        WebSearchList(
                            suggestions = uiState.webResults.take(if (displayedApps.isNotEmpty() || uiState.contactResults.isNotEmpty()) 3 else 5),
                            leadingIcon = R.drawable.ic_search,
                            onClick = { suggestion ->
                                viewModel.onSearchLaunched(suggestion)
                                launchGoogleSearch(context, suggestion)
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.contactResults.isNotEmpty()) {
                        SectionHeader(title = stringResource(R.string.search_section_contacts))
                        ContactResultList(
                            contacts = uiState.contactResults,
                            onContactClick = { contact ->
                                viewModel.onContactUsed(contact, ContactAction.CARD)
                                launchContactDetails(context, contact.id)
                            },
                            onMessageClick = { contact ->
                                contact.phoneNumber?.let {
                                    viewModel.onContactUsed(contact, ContactAction.MESSAGE)
                                    launchSms(context, it)
                                }
                            },
                            onCallClick = { contact ->
                                contact.phoneNumber?.let {
                                    viewModel.onContactUsed(contact, ContactAction.CALL)
                                    launchDialer(context, it)
                                }
                            },
                        )
                    }
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
