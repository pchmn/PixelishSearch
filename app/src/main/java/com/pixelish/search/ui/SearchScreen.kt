package com.pixelish.search.ui

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelish.search.data.AppEntry
import com.pixelish.search.data.ContactEntry

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val noIndication = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = noIndication,
                indication = null,
                onClick = onClose // tap au-dessus de la sheet → ferme
            )
    ) {
        // Bottom sheet : wallpaper visible et flouté derrière (FLAG_BLUR_BEHIND côté Window),
        // sheet teintée par la couleur dynamique du thème, légèrement translucide.
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                // se positionne au-dessus du clavier (qui inclut déjà la nav bar quand visible)
                .imePadding()
                // intercepte les taps : on ne ferme pas quand on touche la sheet
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                DragHandle()

                // Barre de recherche transparente (intégrée à la sheet)
                SearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    focusRequester = focusRequester
                )

                if (state.query.isBlank() && state.suggestedApps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SuggestedAppsRow(
                        apps = state.suggestedApps,
                        onClick = { entry -> launchApp(context, entry) }
                    )
                }

                val hasResults = state.apps.isNotEmpty() ||
                    state.contacts.isNotEmpty() ||
                    state.webSuggestions.isNotEmpty()
                if (hasResults) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.apps.forEach { app ->
                            AppRow(app = app, onClick = { launchApp(context, app) })
                        }
                        state.contacts.forEach { contact ->
                            ContactRow(
                                contact = contact,
                                onClick = { launchContact(context, contact) }
                            )
                        }
                        state.webSuggestions.forEach { suggestion ->
                            WebSuggestionRow(
                                text = suggestion,
                                onClick = { launchWebSearch(context, suggestion) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Info + Settings, alignés à droite, en bas de la sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { /* TODO: about */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "À propos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* TODO: settings */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Paramètres",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps, contacts, web…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            }
        )
        IconButton(onClick = { /* TODO: voice search */ }) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuggestedAppsRow(
    apps: List<AppEntry>,
    onClick: (AppEntry) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        apps.forEach { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onClick(app) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                AppIconImage(app, size = 56.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(app, size = 36.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ContactRow(contact: ContactEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                contact.phoneNumber?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSuggestionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AppIconImage(app: AppEntry, size: androidx.compose.ui.unit.Dp) {
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply { setImageDrawable(app.icon) }
        },
        modifier = Modifier.size(size)
    )
}

private fun launchApp(context: android.content.Context, app: AppEntry) {
    context.startActivity(app.launchIntent)
}

private fun launchContact(context: android.content.Context, contact: ContactEntry) {
    contact.phoneNumber?.let {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$it")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun launchWebSearch(context: android.content.Context, query: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
