package com.huntersystem.app

import org.junit.Assert.assertEquals
import org.junit.Test

class HunterMinimalIconResolutionTest {
    @Test fun rasterSizeScalesWithMeasuredPhysicalPixelsWithoutExcessiveMemory() {
        assertEquals(128,hunterSvgRasterSize(32))
        assertEquals(192,hunterSvgRasterSize(80))
        assertEquals(256,hunterSvgRasterSize(110))
        assertEquals(384,hunterSvgRasterSize(150))
        assertEquals(512,hunterSvgRasterSize(220))
        assertEquals(1024,hunterSvgRasterSize(300))
        assertEquals(1024,hunterSvgRasterSize(1000))
    }
}
