package com.pchmn.pixelishsearch.core.ui.components

import android.app.Activity
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

@Composable
@ExperimentalMaterial3Api
fun BottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberSheetState(
        initialValue = SheetValue.Expanded,
        skipPartiallyExpanded = true
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Compact drag handle: same look as BottomSheetDefaults.DragHandle (32×4dp,
        // extraLarge, onSurfaceVariant @ 40%) but with 8dp vertical padding instead of
        // the default 22dp, to tighten the gap above the search field.
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        },
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize(),
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

        LaunchedEffect(Unit) {
            val sheetWindow = view.findDialogWindow() ?: (view.context as Activity).window
            WindowCompat.getInsetsController(sheetWindow, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !isDark
            }
        }

        content()
    }
}

@Composable
@ExperimentalMaterial3Api
internal fun rememberSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = Hidden,
    skipHiddenState: Boolean = false,
    positionalThreshold: Dp = 56.dp,
    velocityThreshold: Dp = 125.dp,
): SheetState {
    val density = LocalDensity.current
    val positionalThresholdToPx = { with(density) { positionalThreshold.toPx() } }
    val velocityThresholdToPx = { with(density) { velocityThreshold.toPx() } }
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        skipHiddenState,
        saver =
            SheetState.Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                positionalThreshold = positionalThresholdToPx,
                velocityThreshold = velocityThresholdToPx,
                confirmValueChange = confirmValueChange,
                skipHiddenState = skipHiddenState,
            ),
    ) {
        SheetState(
            skipPartiallyExpanded,
            positionalThresholdToPx,
            velocityThresholdToPx,
            initialValue,
            confirmValueChange,
            skipHiddenState,
        )
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