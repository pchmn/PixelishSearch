package com.pchmn.pixelishsearch.ui

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material.icons.outlined.Phone
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.data.AppEntry
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.data.ContactEntry
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.launchAndDismiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                                    modifier = Modifier.size(24.dp),
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
                                    modifier = Modifier.size(24.dp),
                                )
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
                    uiState.suggestedApps
                } else {
                    uiState.apps
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
                    if (uiState.recentContacts.isNotEmpty()) {
                        RecentContactList(
                            contacts = uiState.recentContacts.take(2),
                            onClick = { entry ->
                                replayContactAction(context, entry)
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SuggestionList(
                        suggestions = uiState.searchHistory.take(3),
                        leadingIcon = Icons.Outlined.Schedule,
                        onClick = { suggestion ->
                            viewModel.onSearchLaunched(suggestion)
                            launchGoogleSearch(context, suggestion)
                        },
                    )
                } else {
                    if (uiState.webSuggestions.isNotEmpty()) {
                        SectionHeader(title = "Web Search")
                        SuggestionList(
                            suggestions = uiState.webSuggestions.take(if (displayedApps.isNotEmpty() || uiState.contacts.isNotEmpty()) 3 else 5),
                            leadingIcon = Icons.Outlined.Search,
                            onClick = { suggestion ->
                                viewModel.onSearchLaunched(suggestion)
                                launchGoogleSearch(context, suggestion)
                            },
                        )
                    }

                    if (uiState.contacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "Contacts")
                        val smsIcon = remember(context) {
                            resolveAppIcon(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
                        }
                        val callIcon = remember(context) {
                            resolveAppIcon(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
                        }
                        ContactList(
                            contacts = uiState.contacts,
                            smsIcon = smsIcon,
                            callIcon = callIcon,
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
) {
    if (suggestions.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            SuggestionItem(
                text = suggestion,
                leadingIcon = leadingIcon,
                isFirst = index == 0,
                isLast = index == suggestions.lastIndex,
                onClick = { onClick(suggestion) },
            )
        }
    }
}

@Composable
private fun SearchRowItem(
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val outer = 28.dp
    val inner = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outer else inner,
        topEnd = if (isFirst) outer else inner,
        bottomStart = if (isLast) outer else inner,
        bottomEnd = if (isLast) outer else inner,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(modifier = Modifier.width(16.dp))
        content()
    }
}

@Composable
private fun SuggestionItem(
    text: String,
    leadingIcon: ImageVector,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    SearchRowItem(
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
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

@Composable
private fun ContactList(
    contacts: List<ContactEntry>,
    smsIcon: ImageBitmap?,
    callIcon: ImageBitmap?,
    onContactClick: (ContactEntry) -> Unit,
    onMessageClick: (ContactEntry) -> Unit,
    onCallClick: (ContactEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        contacts.forEachIndexed { index, contact ->
            ContactItem(
                contact = contact,
                isFirst = index == 0,
                isLast = index == contacts.lastIndex,
                smsIcon = smsIcon,
                callIcon = callIcon,
                onClick = { onContactClick(contact) },
                onMessageClick = { onMessageClick(contact) },
                onCallClick = { onCallClick(contact) },
            )
        }
    }
}

@Composable
private fun ContactItem(
    contact: ContactEntry,
    isFirst: Boolean,
    isLast: Boolean,
    smsIcon: ImageBitmap?,
    callIcon: ImageBitmap?,
    onClick: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: () -> Unit,
) {
    val outer = 28.dp
    val inner = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outer else inner,
        topEnd = if (isFirst) outer else inner,
        bottomStart = if (isLast) outer else inner,
        bottomEnd = if (isLast) outer else inner,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(name = contact.name, photoUri = contact.photoUri)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = contact.name,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (contact.phoneNumber != null) {
            Spacer(modifier = Modifier.width(8.dp))
            ActionIconButton(
                bitmap = smsIcon,
                fallbackIcon = Icons.AutoMirrored.Outlined.Message,
                contentDescription = "Send message",
                onClick = onMessageClick,
            )
            Spacer(modifier = Modifier.width(16.dp))
            ActionIconButton(
                bitmap = callIcon,
                fallbackIcon = Icons.Outlined.Phone,
                contentDescription = "Call",
                onClick = onCallClick,
            )
        }
    }
}

@Composable
private fun ContactAvatar(
    name: String,
    photoUri: Uri?,
    size: Dp = 48.dp,
) {
    val photo = rememberContactPhoto(photoUri)
    val avatarModifier = Modifier
        .size(size)
        .clip(CircleShape)
    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
    } else {
        Box(
            modifier = avatarModifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull { it.isLetter() }?.uppercase() ?: "?",
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun rememberContactPhoto(uri: Uri?): ImageBitmap? {
    if (uri == null) return null
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
private fun ActionIconButton(
    bitmap: ImageBitmap?,
    fallbackIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val baseModifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .clickable(onClick = onClick)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = baseModifier,
        )
    } else {
        Box(
            modifier = baseModifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RecentContactList(
    contacts: List<ContactHistoryEntry>,
    onClick: (ContactHistoryEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        contacts.forEachIndexed { index, contact ->
            RecentContactItem(
                contact = contact,
                isFirst = index == 0,
                isLast = index == contacts.lastIndex,
                onClick = { onClick(contact) },
            )
        }
    }
}

@Composable
private fun RecentContactItem(
    contact: ContactHistoryEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    SearchRowItem(
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        leading = {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 32.dp)
        },
    ) {
        Text(
            text = contact.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = " · ${contact.action.label()}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun ContactAction.label(): String = when (this) {
    ContactAction.CARD -> "Contacts"
    ContactAction.MESSAGE -> "Message"
    ContactAction.CALL -> "Phone"
}

private fun replayContactAction(context: Context, entry: ContactHistoryEntry) {
    val phone = entry.phoneNumber
    when (entry.action) {
        ContactAction.MESSAGE -> if (phone != null) launchSms(context, phone) else openContactById(context, entry.id)
        ContactAction.CALL -> if (phone != null) launchDialer(context, phone) else openContactById(context, entry.id)
        ContactAction.CARD -> openContactById(context, entry.id)
    }
}

private fun resolveAppIcon(context: Context, intent: Intent): ImageBitmap? {
    val pm = context.packageManager
    val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
    return runCatching {
        resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
    }.getOrNull()
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
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
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
    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
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
