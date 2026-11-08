package com.doodlestudio.app.domain.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

/**
 * Deterministic Connected Components Label Map Builder.
 * Segments line-art images into distinct closed regions.
 *
 * Guarantees:
 * 1. Deterministic: Scanning order and 4-connectivity queue order are invariant.
 * 2. ShortArray storage: Region IDs from 1 to 32767. Border is 0.
 * 3. Morphological dilation: Expands regions 1-2px under black borders to eliminate white halos.
 * 4. Deterministic arbitration: Lower regionId wins when 2 regions meet at a border pixel.
 * 5. Radial neighbor search: Finds nearest valid region when tapping on border pixels.
 */
object LabelMapBuilder {

    const val BORDER_ID: Short = 0
    const val DEFAULT_MAX_RADIUS = 24

    data class LabelMapResult(
        val width: Int,
        val height: Int,
        val labelMap: ShortArray,
        val regionCount: Int,
        val regionPixelCounts: IntArray,
        val version: Int = 1
    ) {
        fun getRegionId(x: Int, y: Int): Short {
            return if (x in 0 until width && y in 0 until height) {
                labelMap[y * width + x]
            } else {
                BORDER_ID
            }
        }

        fun findNearestRegionId(x: Int, y: Int, maxRadius: Int = DEFAULT_MAX_RADIUS): Short {
            val direct = getRegionId(x, y)
            val directCount = regionPixelCounts.getOrNull(direct.toInt()) ?: 0
            if (direct > BORDER_ID && directCount >= 1000) {
                return direct
            }

            // Radial search for nearest significant region (>= 1000 px)
            var fallbackId: Short = if (direct > BORDER_ID) direct else BORDER_ID
            for (r in 1..maxRadius) {
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        if (dx * dx + dy * dy <= r * r) {
                            val id = getRegionId(x + dx, y + dy)
                            if (id > BORDER_ID) {
                                val count = regionPixelCounts.getOrNull(id.toInt()) ?: 0
                                if (count >= 1000) {
                                    return id
                                } else if (fallbackId <= BORDER_ID) {
                                    fallbackId = id
                                }
                            }
                        }
                    }
                }
            }
            return fallbackId
        }
    }

    /**
     * Builds a deterministic LabelMap on Dispatchers.Default.
     */
    suspend fun buildFromPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        hasAlpha: Boolean = false,
        dilationPixels: Int = 1
    ): LabelMapResult = withContext(Dispatchers.Default) {
        buildFromPixelsSync(pixels, width, height, hasAlpha, dilationPixels)
    }

    /**
     * Synchronous implementation for unit testing and direct invocation.
     */
    fun buildFromPixelsSync(
        pixels: IntArray,
        width: Int,
        height: Int,
        hasAlpha: Boolean = false,
        dilationPixels: Int = 1
    ): LabelMapResult {
        require(width > 0 && height > 0) { "Dimensions must be positive" }
        require(pixels.size == width * height) { "Pixel array size (${pixels.size}) must match width*height (${width * height})" }

        val size = width * height
        val isBorderMask = BooleanArray(size)

        // 1. Identify line-art type: Neon (white/bright strokes on dark/transparent) vs Classic (dark on white)
        var whiteStrokeCount = 0
        var darkStrokeCount = 0
        val sampleStep = 8.coerceAtLeast(size / 10000)
        for (i in 0 until size step sampleStep) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            if (a > 100) {
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (lum > 128) whiteStrokeCount++ else darkStrokeCount++
            }
        }
        val isNeonLineArt = hasAlpha || (whiteStrokeCount > darkStrokeCount && whiteStrokeCount > 0)

        for (i in 0 until size) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            isBorderMask[i] = if (isNeonLineArt) {
                a > 100 && lum > 100
            } else if (hasAlpha) {
                a > 128 && lum < 128
            } else {
                lum < 128
            }
        }

        // 2. Connected Components Labeling (deterministic BFS)
        val labelMap = ShortArray(size)
        var currentRegionId: Short = 0
        val countsList = mutableListOf<Int>()
        countsList.add(0) // Index 0 corresponds to BORDER_ID

        val queue = ArrayDeque<Int>()

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val idx = rowOffset + x
                if (isBorderMask[idx] || labelMap[idx] != BORDER_ID) {
                    continue
                }

                // Start new region
                currentRegionId++
                check(currentRegionId < Short.MAX_VALUE) { "Exceeded maximum supported region count (32767)" }

                labelMap[idx] = currentRegionId
                queue.add(idx)
                var count = 0

                while (!queue.isEmpty()) {
                    val curIdx = queue.removeFirst()
                    count++
                    val curX = curIdx % width
                    val curY = curIdx / width

                    // Invariant 4-directional neighborhood: North, South, West, East
                    if (curY > 0) checkNeighbor(curIdx - width, isBorderMask, labelMap, currentRegionId, queue)
                    if (curY < height - 1) checkNeighbor(curIdx + width, isBorderMask, labelMap, currentRegionId, queue)
                    if (curX > 0) checkNeighbor(curIdx - 1, isBorderMask, labelMap, currentRegionId, queue)
                    if (curX < width - 1) checkNeighbor(curIdx + 1, isBorderMask, labelMap, currentRegionId, queue)
                }

                countsList.add(count)
            }
        }

        // 3. Morphological Dilation into border pixels (deterministic arbitration)
        if (dilationPixels > 0) {
            applyDeterministicDilation(labelMap, isBorderMask, width, height, dilationPixels)
        }

        return LabelMapResult(
            width = width,
            height = height,
            labelMap = labelMap,
            regionCount = currentRegionId.toInt(),
            regionPixelCounts = countsList.toIntArray(),
            version = 1
        )
    }

    private inline fun checkNeighbor(
        nIdx: Int,
        isBorderMask: BooleanArray,
        labelMap: ShortArray,
        regionId: Short,
        queue: ArrayDeque<Int>
    ) {
        if (!isBorderMask[nIdx] && labelMap[nIdx] == BORDER_ID) {
            labelMap[nIdx] = regionId
            queue.add(nIdx)
        }
    }

    private fun applyDeterministicDilation(
        labelMap: ShortArray,
        isBorderMask: BooleanArray,
        width: Int,
        height: Int,
        radius: Int
    ) {
        val size = width * height
        val dilatedLabels = labelMap.clone()

        for (step in 1..radius) {
            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    val idx = rowOffset + x
                    // Only dilate into actual line-art border pixels
                    if (isBorderMask[idx]) {
                        var bestNeighbor: Short = dilatedLabels[idx]

                        // Check 4-connected neighbors
                        if (y > 0) bestNeighbor = arbitrate(bestNeighbor, labelMap[idx - width])
                        if (y < height - 1) bestNeighbor = arbitrate(bestNeighbor, labelMap[idx + width])
                        if (x > 0) bestNeighbor = arbitrate(bestNeighbor, labelMap[idx - 1])
                        if (x < width - 1) bestNeighbor = arbitrate(bestNeighbor, labelMap[idx + 1])

                        dilatedLabels[idx] = bestNeighbor
                    }
                }
            }
            // Copy back for next dilation step
            System.arraycopy(dilatedLabels, 0, labelMap, 0, size)
        }
    }

    /**
     * Deterministic arbitration rule: Lower non-zero regionId wins.
     */
    private fun arbitrate(current: Short, candidate: Short): Short {
        if (candidate <= BORDER_ID) return current
        if (current <= BORDER_ID) return candidate
        return if (candidate < current) candidate else current
    }
}

