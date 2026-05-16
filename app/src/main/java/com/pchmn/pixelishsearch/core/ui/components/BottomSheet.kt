package com.pchmn.pixelishsearch.core.ui.components

import android.app.Activity
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

@Composable
@ExperimentalMaterial3Api
fun BottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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

        content()
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