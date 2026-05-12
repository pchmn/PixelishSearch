package com.pchmn.pixelishsearch.ui

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NorthWest
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.data.AppEntry
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.launchAndDismiss
import androidx.core.net.toUri
import com.pchmn.pixelishsearch.ui.contact.ContactRecentList
import com.pchmn.pixelishsearch.ui.contact.ContactResultList

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
        containerColor = if(isDark) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            .compositeOver(MaterialTheme.colorScheme.surface).copy(0.6f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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

                AppRow(
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
                    SuggestionList(
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
                        SuggestionList(
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
                                openContactById(context, contact.id)
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

private val AppSlotWidth = 88.dp

@Composable
private fun AppRow(
    apps: List<AppEntry>,
    highlightFirst: Boolean,
    onAppClick: (AppEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Always 4 fixed-width slots — keeps the layout stable even if
        // fewer than 4 apps match.
        repeat(4) { index ->
            Box(
                modifier = Modifier.width(AppSlotWidth),
                contentAlignment = Alignment.TopCenter,
            ) {
                apps.getOrNull(index)?.let { entry ->
                    AppItem(
                        entry = entry,
                        highlighted = highlightFirst && index == 0,
                        onClick = { onAppClick(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    entry: AppEntry,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val backgroundColor = if (highlighted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .width(AppSlotWidth)
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bitmap = remember(entry.packageName) {
            entry.icon.toBitmap().asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = entry.label,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<String>,
    leadingIcon: ImageVector,
    onClick: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    EntryList(entries = suggestions) {
        suggestions.forEachIndexed { index, suggestion ->
            SuggestionItem(
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

@Composable
private fun SuggestionItem(
    text: String,
    leadingIcon: ImageVector,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    EntryRow(
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        onDelete = onDelete,
        leading = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.NorthWest,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
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
        ContactAction.MESSAGE -> if (phone != null) launchSms(context, phone) else openContactById(context, entry.id)
        ContactAction.CALL -> if (phone != null) launchDialer(context, phone) else openContactById(context, entry.id)
        ContactAction.CARD -> openContactById(context, entry.id)
    }
}

private fun openContactById(context: Context, contactId: Long) {
    val uri = ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI,
        contactId,
    )
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
        // No contacts app available — silently ignore.
    }
}

private fun launchSms(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumber".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun launchGoogleSearch(context: Context, query: String) {
    val googleApp = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, query)
        setPackage("com.google.android.googlequicksearchbox")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.launchAndDismiss(googleApp)
        return
    } catch (_: ActivityNotFoundException) {
        // Google app unavailable, fall back to the browser.
    }

    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
    val fallback = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$encoded".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.launchAndDismiss(fallback)
}

private fun View.findDialogWindow(): Window? {
    var p: ViewParent? = parent
    while (p != null) {
        if (p is DialogWindowProvider) return p.window
        p = p.parent
    }
    return null
}
