/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.libremobileos.freeform.server.ui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pixel-space geometry shared by initial placement, resizing and display rotations.
 *
 * Ratios are design defaults inspired by HyperOS freeform windows, not proprietary constants.
 * Never let the window collapse to an unusable 25px surface or change aspect ratio mid-drag.
 */
internal object FreeformGeometry {
    data class Size(val width: Int, val height: Int)
    data class Offset(val x: Int, val y: Int)

    private fun dp(value: Float, density: Float) = (value * density).roundToInt()

    fun limits(screenW: Int, screenH: Int, density: Float): Size {
        val margin = dp(16f, density)
        val verticalReserve = dp(128f, density)
        return Size(max(1, screenW - 2 * margin), max(1, screenH - verticalReserve))
    }

    fun initial(screenW: Int, screenH: Int, density: Float): Size {
        val portrait = screenH >= screenW
        val desiredW = (screenW * if (portrait) 0.70f else 0.43f).roundToInt()
        val desiredH = (screenH * if (portrait) 0.60f else 0.66f).roundToInt()
        return fit(Size(desiredW, desiredH), screenW, screenH, density)
    }

    fun fit(size: Size, screenW: Int, screenH: Int, density: Float): Size {
        val bounds = limits(screenW, screenH, density)
        val w = max(1, size.width)
        val h = max(1, size.height)
        val scale = min(1f, min(bounds.width.toFloat() / w, bounds.height.toFloat() / h))
        return Size(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt()))
    }

    /** Resize relative to ACTION_DOWN: no incremental rounding drift or sudden squares. */
    fun resize(
        start: Size,
        deltaX: Float,
        deltaY: Float,
        isRightCorner: Boolean,
        screenW: Int,
        screenH: Int,
        density: Float,
    ): Size {
        val w = max(1, start.width)
        val h = max(1, start.height)
        val sx = 1f + (if (isRightCorner) deltaX else -deltaX) / w
        val sy = 1f + deltaY / h
        val wantedScale = if (abs(sx - 1f) > abs(sy - 1f)) sx else sy
        val maxSize = limits(screenW, screenH, density)
        val minW = min(maxSize.width, max(dp(180f, density), (screenW * 0.32f).roundToInt()))
        val minH = min(maxSize.height, max(dp(240f, density), (screenH * 0.25f).roundToInt()))
        val minScale = max(minW.toFloat() / w, minH.toFloat() / h)
        val maxScale = min(maxSize.width.toFloat() / w, maxSize.height.toFloat() / h)
        val scale = wantedScale.coerceIn(min(minScale, maxScale), maxScale)
        return Size(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt()))
    }

    fun mini(size: Size, screenW: Int, screenH: Int, density: Float): Size {
        val width = min(dp(138f, density), max(dp(92f, density), (screenW * 0.30f).roundToInt()))
            .coerceAtMost(max(1, screenW - dp(24f, density)))
        val aspect = max(0.4f, size.height.toFloat() / max(1, size.width))
        val height = (width * aspect).roundToInt()
            .coerceAtMost(max(1, min(screenH / 3, dp(240f, density))))
        return Size(width, max(1, height))
    }

    /** LayoutParams use Gravity.CENTER. Keep the entire decorated window on screen. */
    fun clampCenter(
        x: Int, y: Int, outerW: Int, outerH: Int,
        screenW: Int, screenH: Int, density: Float,
    ): Offset {
        val horizontalMargin = dp(10f, density)
        val top = dp(32f, density)
        val bottom = dp(32f, density)
        val xRange = max(0, (screenW - outerW) / 2 - horizontalMargin)
        val minY = -(screenH / 2) + top + outerH / 2
        val maxY = screenH / 2 - bottom - outerH / 2
        val safeY = if (minY > maxY) 0 else y.coerceIn(minY, maxY)
        return Offset(x.coerceIn(-xRange, xRange), safeY)
    }
}
