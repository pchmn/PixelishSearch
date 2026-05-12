package com.pchmn.pixelishsearch.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.launchAndDismiss
import com.pchmn.pixelishsearch.launchContactDetails
import com.pchmn.pixelishsearch.launchDialer
import com.pchmn.pixelishsearch.launchGoogleSearch
import com.pchmn.pixelishsearch.launchSms
import com.pchmn.pixelishsearch.ui.app.AppList
import com.pchmn.pixelishsearch.ui.contact.ContactRecentList
import com.pchmn.pixelishsearch.ui.contact.ContactResultList
import com.pchmn.pixelishsearch.ui.websearch.WebSearchList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        containerColor = if (isDark) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            .compositeOver(MaterialTheme.colorScheme.surface)
            .copy(0.6f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            .compositeOver(MaterialTheme.colorScheme.surface).copy(0.6f),
        scrimColor = Color.White.copy(alpha = 0.1f),
    ) {
        // The ModalBottomSheet's internal dialog window doesn't automatically follow
        // the system theme. Force the status/nav bar icons to match the theme:
        //   - light theme → dark icons (isAppearanceLight* = true)
        //   - dark theme → white icons (isAppearanceLight* = false)
        val view = LocalView.current

        SideEffect {
            val sheetWindow = view.findDialogWindow() ?: (view.context as Activity).window
            WindowCompat.getInsetsController(sheetWindow, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !isDark
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val focusRequester = remember { FocusRequester() }
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
            ) {
                TextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Search web and more",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = com.pchmn.pixelishsearch.R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        } else {
                            Row {
                                IconButton(onClick = {
                                    com.pchmn.pixelishsearch.geminiIntent(context)?.let {
                                        context.launchAndDismiss(it)
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(id = com.pchmn.pixelishsearch.R.drawable.gemini_icon),
                                        contentDescription = "Gemini",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                IconButton(onClick = {
                                    com.pchmn.pixelishsearch.lensIntent(context)?.let {
                                        context.launchAndDismiss(it)
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(id = com.pchmn.pixelishsearch.R.drawable.google_lens_icon),
                                        contentDescription = "Google Lens",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val query = uiState.query.trim()
                            if (query.isNotEmpty()) {
                                viewModel.onSearchLaunched(query)
                                launchGoogleSearch(context, query)
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                val displayedApps = if (uiState.query.isBlank()) {
                    uiState.appRecents
                } else {
                    uiState.appResults
                }.take(4)

                AppList(
                    apps = displayedApps,
                    highlightFirst = uiState.query.isNotBlank(),
                    onAppClick = { entry ->
                        viewModel.onAppLaunched(entry.packageName)
                        context.launchAndDismiss(entry.launchIntent)
                    },
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
                    }

                    if (uiState.contactResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
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

private fun replayContactAction(context: Context, entry: ContactHistoryEntry) {
    val phone = entry.phoneNumber
    when (entry.action) {
        ContactAction.MESSAGE -> if (phone != null) launchSms(
            context,
            phone
        ) else launchContactDetails(context, entry.id)

        ContactAction.CALL -> if (phone != null) launchDialer(
            context,
            phone
        ) else launchContactDetails(context, entry.id)

        ContactAction.CARD -> launchContactDetails(context, entry.id)
    }
}

private fun View.findDialogWindow(): Window? {
    var p: ViewParent? = parent
    while (p != null) {
        if (p is DialogWindowProvider) return p.window
        p = p.parent
    }
    return null
}
