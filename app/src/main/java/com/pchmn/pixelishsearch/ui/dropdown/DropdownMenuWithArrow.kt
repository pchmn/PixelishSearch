package com.pchmn.pixelishsearch.ui.dropdown

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

private val CornerRadius = 28.dp
private val TipWidth = 12.dp
private val TipHeight = 8.dp
private val TipCornerRadius = 2.dp
private val BottomInset = 8.dp
private val MinTipX = CornerRadius + TipWidth / 2

private val DropdownMenuVerticalPadding = 8.dp
private const val ExpandedScaleTarget = 1f
private const val ClosedScaleTarget = 0.8f
private const val ExpandedAlphaTarget = 1f
private const val ClosedAlphaTarget = 0f

@Composable
fun DropdownMenuWithArrow(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorBounds: IntRect,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    if (!expandedState.currentState && !expandedState.targetState) return

    val density = LocalDensity.current
    val offsetXPx = with(density) { offset.x.roundToPx() }
    val offsetYPx = with(density) { offset.y.roundToPx() }
    val minTipPx = with(density) { MinTipX.roundToPx() }

    val anchorCenterXPx = anchorBounds.left + anchorBounds.width / 2

    // Composition-time best guess for tipX (no clamping info yet). Matches the final value in the
    // common case (menu fits in the window). When the position provider must clamp the menu to a
    // screen edge, it overwrites this state during layout; the corrected shape appears on the next
    // frame — invisible since the open animation starts at alpha = 0.
    val initialTipPx = run {
        val naturalLeft = anchorBounds.left + offsetXPx
        val natural = anchorCenterXPx - naturalLeft
        if (natural < minTipPx) minTipPx else natural
    }
    val tipXPxState = remember(anchorBounds, offsetXPx) { mutableIntStateOf(initialTipPx) }
    val transformOriginState = remember { mutableStateOf(TransformOrigin(0.5f, 0f)) }

    val positionProvider = remember(anchorBounds, offsetXPx, offsetYPx, minTipPx) {
        CalloutPopupPositionProvider(
            screenAnchorBounds = anchorBounds,
            offsetX = offsetXPx,
            offsetY = offsetYPx,
            minTipPx = minTipPx,
            tipXPxState = tipXPxState,
            transformOriginState = transformOriginState,
        )
    }

    val tipXDp = with(density) { tipXPxState.intValue.toDp() }
    val calloutShape = remember(tipXDp) {
        CalloutShape(
            cornerRadius = CornerRadius,
            tipWidth = TipWidth,
            tipHeight = TipHeight,
            tipX = tipXDp,
            tipCornerRadius = TipCornerRadius,
            bottomInset = BottomInset,
        )
    }

    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = positionProvider,
        properties = properties,
    ) {
        DropdownMenuContent(
            expandedState = expandedState,
            transformOrigin = transformOriginState.value,
            scrollState = scrollState,
            shape = calloutShape,
            content = content,
        )
    }
}

private class CalloutPopupPositionProvider(
    private val screenAnchorBounds: IntRect,
    private val offsetX: Int,
    private val offsetY: Int,
    private val minTipPx: Int,
    private val tipXPxState: MutableIntState,
    private val transformOriginState: MutableState<TransformOrigin>,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorCenterX = screenAnchorBounds.left + screenAnchorBounds.width / 2
        val naturalLeft = screenAnchorBounds.left + offsetX
        // If the tip would land in the left-corner zone, shift the menu further left.
        val tipBeforeShift = anchorCenterX - naturalLeft
        val cornerShift = if (tipBeforeShift < minTipPx) minTipPx - tipBeforeShift else 0
        val afterCornerLeft = naturalLeft - cornerShift
        // Clamp to the window so the menu always fits.
        val maxLeft = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val clampedLeft = afterCornerLeft.coerceIn(0, maxLeft)
        val clampedTop = (screenAnchorBounds.bottom + offsetY).coerceIn(
            0,
            (windowSize.height - popupContentSize.height).coerceAtLeast(0),
        )

        val finalTipPx = anchorCenterX - clampedLeft
        if (tipXPxState.intValue != finalTipPx) {
            tipXPxState.intValue = finalTipPx
        }
        val pivotX = if (popupContentSize.width == 0) {
            0.5f
        } else {
            (finalTipPx.toFloat() / popupContentSize.width).coerceIn(0f, 1f)
        }
        val newOrigin = TransformOrigin(pivotX, 0f)
        if (transformOriginState.value != newOrigin) {
            transformOriginState.value = newOrigin
        }
        return IntOffset(clampedLeft, clampedTop)
    }
}

@Composable
private fun DropdownMenuContent(
    expandedState: MutableTransitionState<Boolean>,
    transformOrigin: TransformOrigin,
    scrollState: ScrollState,
    shape: Shape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = rememberTransition(expandedState, label = "DropdownMenu")
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 120) },
        label = "scale",
    ) { expanded -> if (expanded) ExpandedScaleTarget else ClosedScaleTarget }
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 80) },
        label = "alpha",
    ) { expanded -> if (expanded) ExpandedAlphaTarget else ClosedAlphaTarget }

    val isInspecting = LocalInspectionMode.current
    Surface(
        modifier = Modifier.graphicsLayer {
            scaleX = if (!isInspecting) scale
            else if (expandedState.targetState) ExpandedScaleTarget else ClosedScaleTarget
            scaleY = scaleX
            this.alpha = if (!isInspecting) alpha
            else if (expandedState.targetState) ExpandedAlphaTarget else ClosedAlphaTarget
            this.transformOrigin = transformOrigin
        },
        shape = shape,
        color = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = DropdownMenuVerticalPadding)
                .width(IntrinsicSize.Max)
                .verticalScroll(scrollState),
            content = content,
        )
    }
}
