/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.libremobileos.freeform.server.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeformGeometryTest {
    @Test fun initialSizesFitBothOrientations() {
        for ((w, h) in listOf(1440 to 3200, 3200 to 1440, 1080 to 2400)) {
            val size = FreeformGeometry.initial(w, h, 3f)
            val limit = FreeformGeometry.limits(w, h, 3f)
            assertTrue(size.width in 1..limit.width)
            assertTrue(size.height in 1..limit.height)
        }
    }

    @Test fun resizingAllowsPortraitToWideWithoutDistortion() {
        val start = FreeformGeometry.Size(420, 800)
        val widened = FreeformGeometry.resize(start, 450f, -180f, true, 1440, 3200, 3f)
        assertTrue(widened.width > start.width)
        assertTrue(widened.height < start.height)
        assertTrue(widened.width > widened.height)
        val shorter = FreeformGeometry.resize(start, 0f, -9000f, true, 1440, 3200, 3f)
        assertTrue(shorter.width > 0 && shorter.height > 0)
    }

    @Test fun leftAndRightCornersGrowSymmetrically() {
        val start = FreeformGeometry.Size(800, 1200)
        val a = FreeformGeometry.resize(start, 130f, 0f, true, 1440, 3200, 3f)
        val b = FreeformGeometry.resize(start, -130f, 0f, false, 1440, 3200, 3f)
        assertEquals(a, b)
    }

    @Test fun clampCenterRespectsScreenEdges() {
        val pos = FreeformGeometry.clampCenter(5000, -5000, 700, 1300, 1440, 3200, 3f)
        assertTrue(pos.x < 1000)
        assertTrue(pos.y > -1600)
    }

    @Test fun dragAndResizeStayInsideDisplayBounds() {
        for ((w, h) in listOf(1440 to 3200, 3200 to 1440, 1080 to 2400)) {
            val density = 3f
            val origin = FreeformGeometry.initial(w, h, density)
            for ((dx, dy) in listOf(
                20000 to 20000, -20000 to -20000, 0 to 0,
                1000 to -1000
            )) {
                val grown = FreeformGeometry.resize(
                    origin, dx.toFloat(), dy.toFloat(), true, w, h, density
                )
                val clamped = FreeformGeometry.clampCenter(
                    dx, dy, grown.width, grown.height + (40f * density).toInt(),
                    w, h, density
                )
                val xRange = ((w - grown.width) / 2 - (8f * density).toInt()).coerceAtLeast(0)
                assertTrue(kotlin.math.abs(clamped.x) <= xRange)
                assertTrue(grown.width <= FreeformGeometry.limits(w, h, density).width)
                assertTrue(grown.height <= FreeformGeometry.limits(w, h, density).height)
            }
        }
    }

    @Test fun leftAndRightCornersCanChangeAspectIndependently() {
        val start = FreeformGeometry.Size(550, 960)
        val right = FreeformGeometry.resize(start, 180f, 0f, true, 1440, 3200, 3f)
        val left = FreeformGeometry.resize(start, -180f, 0f, false, 1440, 3200, 3f)
        assertEquals(right, left)
        assertEquals(start.height, right.height)
    }

    @Test fun edgeStashLeavesVisibleHandleOnEitherSide() {
        val left = FreeformGeometry.stashedX(1440, 340, -1, 48)
        val right = FreeformGeometry.stashedX(1440, 340, 1, 48)
        assertEquals(-right, left)
        assertEquals(48, 1440 / 2 + 340 / 2 - right)
    }

    @Test fun miniKeepsContentAspectWithoutFixedPixels() {
        val size = FreeformGeometry.mini(FreeformGeometry.Size(900, 1500), 1440, 3200, 3f)
        assertTrue(size.width > 300)
        assertTrue(size.height > size.width)
    }
}
