package com.pchmn.pixelishsearch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.launchAndDismiss
import com.pchmn.pixelishsearch.launchAppInfo
import com.pchmn.pixelishsearch.launchContactDetails
import com.pchmn.pixelishsearch.launchDialer
import com.pchmn.pixelishsearch.launchGoogleSearch
import com.pchmn.pixelishsearch.launchSms
import com.pchmn.pixelishsearch.pinAppShortcut
import com.pchmn.pixelishsearch.ui.app.AppList
import com.pchmn.pixelishsearch.ui.bottomsheet.BottomSheet
import com.pchmn.pixelishsearch.ui.contact.ContactRecentList
import com.pchmn.pixelishsearch.ui.contact.ContactResultList
import com.pchmn.pixelishsearch.ui.contact.replayContactAction
import com.pchmn.pixelishsearch.ui.websearch.WebSearchList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        com.pchmn.pixelishsearch.geminiIntent(context)?.let {
                            context.launchAndDismiss(it)
                        }
                    },
                    onLensClick = {
                        com.pchmn.pixelishsearch.lensIntent(context)?.let {
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
                        leadingIcon = Icons.Outlined.Schedule,
                        onClick = { suggestion ->
                            viewModel.onSearchLaunched(suggestion)
                            launchGoogleSearch(context, suggestion)
                        },
                        onDelete = viewModel::removeSearchHistory,
                    )
                } else {
                    if (uiState.webResults.isNotEmpty()) {
                        SectionHeader(title = "Web Search")
                        WebSearchList(
                            suggestions = uiState.webResults.take(if (displayedApps.isNotEmpty() || uiState.contactResults.isNotEmpty()) 3 else 5),
                            leadingIcon = Icons.Outlined.Search,
                            onClick = { suggestion ->
                                viewModel.onSearchLaunched(suggestion)
                                launchGoogleSearch(context, suggestion)
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.contactResults.isNotEmpty()) {
                        SectionHeader(title = "Contacts")
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
