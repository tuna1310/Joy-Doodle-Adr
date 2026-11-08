package com.doodlestudio.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolutionPolicyTest {

    @Test
    fun testLowRamDeviceAlwaysReturnsLowResolution() {
        // Any device marked as low-RAM (Android Go) must use 512px
        assertEquals(512, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 512, isLowRamDevice = true))
        assertEquals(512, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 128, isLowRamDevice = true))
        assertEquals(512, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 64, isLowRamDevice = true))
    }

    @Test
    fun testVeryConstrainedHeapReturnsLowResolution() {
        // Devices with <= 96MB heap
        assertEquals(512, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 64, isLowRamDevice = false))
        assertEquals(512, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 96, isLowRamDevice = false))
    }

    @Test
    fun testMidRangeHeapReturnsMediumResolution() {
        // Devices with 97MB to 192MB heap
        assertEquals(768, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 128, isLowRamDevice = false))
        assertEquals(768, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 192, isLowRamDevice = false))
    }

    @Test
    fun testStandardDeviceReturnsHighResolution() {
        // Standard modern phones (> 192MB heap, e.g. 256MB, 384MB, 512MB)
        assertEquals(1024, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 256, isLowRamDevice = false))
        assertEquals(1024, ResolutionPolicy.determineCanvasResolution(memoryClassInMb = 512, isLowRamDevice = false))
    }
}

