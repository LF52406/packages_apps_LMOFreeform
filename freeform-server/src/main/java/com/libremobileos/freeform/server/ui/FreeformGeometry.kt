/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.libremobileos.freeform.server.ui

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * All dimensions are physical pixels. This helper never edits the app's density.
 *
 * Unlike a scaled thumbnail, freeform resizing must allow the logical aspect ratio
 * to change so the app can re-layout between portrait and wide floating windows.
 */
internal object FreeformGeometry {
    data class Size(val width: Int, val height: Int)
    data class Offset(val x: Int, val y: Int)

    private fun dp(value: Float, density: Float) = (value * density).roundToInt()

    fun limits(screenW: Int, screenH: Int, density: Float): Size {
        val margin = dp(12f, density)
        val verticalReserve = dp(96f, density)
        return Size(max(1, screenW - 2 * margin), max(1, screenH - verticalReserve))
    }

    fun initial(screenW: Int, screenH: Int, density: Float): Size {
        val portrait = screenH >= screenW
        val w = (screenW * if (portrait) 0.43f else 0.52f).roundToInt()
        val h = (screenH * if (portrait) 0.44f else 0.65f).roundToInt()
        return fit(Size(w, h), screenW, screenH, density)
    }

    /** Preserve both dimensions if possible when the display rotates. */
    fun fit(size: Size, screenW: Int, screenH: Int, density: Float): Size {
        val bounds = limits(screenW, screenH, density)
        val w = max(1, size.width)
        val h = max(1, size.height)
        val scale = min(1f, min(bounds.width.toFloat() / w, bounds.height.toFloat() / h))
        return Size(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt()))
    }

    /**
     * Independent width and height resize, measured from ACTION_DOWN.
     * The opposite top corner stays anchored by ScaleTouchListener.
     */
    fun resize(
        start: Size,
        deltaX: Float,
        deltaY: Float,
        isRightCorner: Boolean,
        screenW: Int,
        screenH: Int,
        density: Float,
    ): Size {
        val bounds = limits(screenW, screenH, density)
        val minW = min(bounds.width, max(dp(112f, density), (screenW * 0.22f).roundToInt()))
        val minH = min(bounds.height, max(dp(150f, density), (screenH * 0.18f).roundToInt()))
        val dx = (if (isRightCorner) deltaX else -deltaX).roundToInt()
        val newWidth = (start.width.toLong() + dx).coerceIn(minW.toLong(), bounds.width.toLong()).toInt()
        val newHeight = (start.height.toLong() + deltaY.roundToInt())
            .coerceIn(minH.toLong(), bounds.height.toLong()).toInt()
        return Size(newWidth, newHeight)
    }

    /** Compact live preview, with the same aspect ratio as its normal window. */
    fun mini(size: Size, screenW: Int, screenH: Int, density: Float): Size {
        val width = (screenW * 0.24f).roundToInt()
            .coerceIn(min(screenW, dp(84f, density)), max(1, min(screenW, dp(136f, density))))
        val aspect = size.height.toFloat() / max(1, size.width)
        val maxHeight = max(1, min(screenH / 3, dp(240f, density)))
        val height = (width * aspect).roundToInt().coerceIn(1, maxHeight)
        return Size(width, height)
    }

    /** Gravity.CENTER offsets; a decorated window stays reachable on either rotation. */
    fun clampCenter(
        x: Int, y: Int, outerW: Int, outerH: Int,
        screenW: Int, screenH: Int, density: Float,
    ): Offset {
        val horizontalMargin = dp(8f, density)
        val top = dp(32f, density)
        val bottom = dp(32f, density)
        val xRange = max(0, (screenW - outerW) / 2 - horizontalMargin)
        val minY = -(screenH / 2) + top + outerH / 2
        val maxY = screenH / 2 - bottom - outerH / 2
        val safeY = if (minY > maxY) 0 else y.coerceIn(minY, maxY)
        return Offset(x.coerceIn(-xRange, xRange), safeY)
    }

    /** Place the mini preview mostly off-screen but retain a tappable edge handle. */
    fun stashedX(screenW: Int, miniW: Int, side: Int, visibleEdgePx: Int): Int {
        val peek = visibleEdgePx.coerceIn(1, max(1, miniW))
        val offset = (screenW.toLong() + miniW - 2L * peek) / 2L
        return (if (side < 0) -offset else offset)
            .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }
}
