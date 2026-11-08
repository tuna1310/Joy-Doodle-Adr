package com.doodlestudio.app.util

object ResolutionPolicy {
    const val RESOLUTION_HIGH = 1024
    const val RESOLUTION_MEDIUM = 768
    const val RESOLUTION_LOW = 512

    /**
     * Determines optimal working canvas resolution based on device RAM configuration.
     * Pure function for 100% deterministic unit testing without Android SDK dependencies.
     *
     * - Default standard: 1024x1024 (devices with > 192MB heap)
     * - Mid-range / constrained: 768x768 (devices with 97MB - 192MB heap)
     * - Low-RAM / Android Go: 512x512 (isLowRamDevice = true or <= 96MB heap)
     *
     * @param memoryClassInMb Value from ActivityManager.memoryClass (available heap per app)
     * @param isLowRamDevice Value from ActivityManager.isLowRamDevice() (Android Go flag)
     * @return Canvas resolution in square pixels (width = height)
     */
    fun determineCanvasResolution(memoryClassInMb: Int, isLowRamDevice: Boolean): Int {
        return when {
            isLowRamDevice || memoryClassInMb <= 96 -> RESOLUTION_LOW
            memoryClassInMb <= 192 -> RESOLUTION_MEDIUM
            else -> RESOLUTION_HIGH
        }
    }
}

