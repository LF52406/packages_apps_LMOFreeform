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

    @Test fun cornerResizePreservesAspectRatioAndMinimum() {
        val start = FreeformGeometry.Size(900, 1500)
        val grown = FreeformGeometry.resize(start, 250f, 400f, true, 1440, 3200, 3f)
        val shrunk = FreeformGeometry.resize(start, -9000f, -9000f, true, 1440, 3200, 3f)
        assertTrue(grown.width > start.width)
        assertTrue(shrunk.width > 25)
        assertTrue(shrunk.height > 25)
        assertEquals(start.width.toFloat() / start.height, grown.width.toFloat() / grown.height, 0.002f)
        assertEquals(start.width.toFloat() / start.height, shrunk.width.toFloat() / shrunk.height, 0.002f)
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
                    dx, dy, grown.width, grown.height + (54f * density).toInt(),
                    w, h, density
                )
                val xRange = ((w - grown.width) / 2 - (10f * density).toInt()).coerceAtLeast(0)
                assertTrue(kotlin.math.abs(clamped.x) <= xRange)
                assertTrue(grown.width <= FreeformGeometry.limits(w, h, density).width)
                assertTrue(grown.height <= FreeformGeometry.limits(w, h, density).height)
            }
        }
    }

    @Test fun miniKeepsContentAspectWithoutFixedPixels() {
        val size = FreeformGeometry.mini(FreeformGeometry.Size(900, 1500), 1440, 3200, 3f)
        assertTrue(size.width > 300)
        assertTrue(size.height > size.width)
    }
}
