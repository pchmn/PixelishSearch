package com.pchmn.pixelishsearch.ui.dropdown

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class CalloutShape(
    private val cornerRadius: Dp,
    private val tipWidth: Dp,
    private val tipHeight: Dp,
    private val tipX: Dp,
    private val tipCornerRadius: Dp = 0.dp,
    private val bottomInset: Dp = 0.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val tw = with(density) { tipWidth.toPx() }
        val th = with(density) { tipHeight.toPx() }
        val tcr = with(density) { tipCornerRadius.toPx() }.coerceAtMost(th)
        val bi = with(density) { bottomInset.toPx() }
        val bottom = size.height - bi
        val rawTipX = with(density) { tipX.toPx() }
        val tip = rawTipX.coerceIn(r + tw / 2f, size.width - r - tw / 2f)
        val apexInsetX = (tw * tcr) / (2f * th)

        val path = Path().apply {
            moveTo(r, th)
            lineTo(tip - tw / 2f, th)
            lineTo(tip - apexInsetX, tcr)
            quadraticTo(tip, 0f, tip + apexInsetX, tcr)
            lineTo(tip + tw / 2f, th)
            lineTo(size.width - r, th)
            arcTo(
                rect = Rect(size.width - 2f * r, th, size.width, th + 2f * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(size.width, bottom - r)
            arcTo(
                rect = Rect(size.width - 2f * r, bottom - 2f * r, size.width, bottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(r, bottom)
            arcTo(
                rect = Rect(0f, bottom - 2f * r, 2f * r, bottom),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(0f, th + r)
            arcTo(
                rect = Rect(0f, th, 2f * r, th + 2f * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            close()
        }
        return Outline.Generic(path)
    }
}