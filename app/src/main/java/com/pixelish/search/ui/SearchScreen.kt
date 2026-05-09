package com.pixelish.search.ui

import android.app.Activity
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

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
        // La window du dialog interne du ModalBottomSheet ne suit pas automatiquement
        // le thème système. On force ici les icônes status/nav bar à suivre le thème :
        //   - thème clair → icônes sombres (isAppearanceLight* = true)
        //   - thème sombre → icônes blanches (isAppearanceLight* = false)
        val view = LocalView.current

        SideEffect {
            val sheetWindow = view.findDialogWindow() ?: (view.context as Activity).window
            WindowCompat.getInsetsController(sheetWindow, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !isDark
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            var query by remember { mutableStateOf("") }
            var expanded by remember { mutableStateOf(true) }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(expanded) {
                if (expanded) focusRequester.requestFocus()
            }

            val collapsedContainer = if (isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            }

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        modifier = Modifier.focusRequester(focusRequester),
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { expanded = false },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = {
                            Text(
                                text = if (expanded) "Search web and more" else "",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { /* TODO voice */ }) {
                                    Icon(Icons.Outlined.Mic, contentDescription = "Voice search")
                                }
                                IconButton(onClick = { /* TODO lens */ }) {
                                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Image search")
                                }
                            }
                        },
                        colors = inputFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                colors = SearchBarDefaults.colors(
                    containerColor = Color.Transparent,
                    dividerColor = Color.Transparent,
//                inputFieldColors =
//                    inputFieldColors(
//                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.primary,
//                        disabledContainerColor = MaterialTheme.colorScheme.primary,
//                    )
                ),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if(expanded) 8.dp else 16.dp),
            ) {
                // TODO: contenu suggestions (historique, web, apps...) — vide pour l'instant
            }
        }
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
