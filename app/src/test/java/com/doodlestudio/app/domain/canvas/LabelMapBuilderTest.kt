package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.generator.TemplateArtGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class LabelMapBuilderTest {

    @Test
    fun testClosedBoxSegmentation() {
        val w = 50
        val h = 50
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() } // White background

        // Draw black border box from (10, 10) to (40, 40)
        for (x in 10..40) {
            pixels[10 * w + x] = 0xFF000000.toInt()
            pixels[40 * w + x] = 0xFF000000.toInt()
        }
        for (y in 10..40) {
            pixels[y * w + 10] = 0xFF000000.toInt()
            pixels[y * w + 40] = 0xFF000000.toInt()
        }

        val result = LabelMapBuilder.buildFromPixelsSync(pixels, w, h, hasAlpha = false, dilationPixels = 0)

        // Must segment into 2 regions: 1 inside the box, 1 outside the box
        assertEquals("Must segment into 2 regions", 2, result.regionCount)

        val idOutside = result.getRegionId(5, 5)
        val idInside = result.getRegionId(25, 25)
        val idBorder = result.getRegionId(10, 10)

        assertEquals("Border pixel must have BORDER_ID (0)", LabelMapBuilder.BORDER_ID, idBorder)
        assertTrue("Outside region must be > 0", idOutside > 0)
        assertTrue("Inside region must be > 0", idInside > 0)
        assertNotEquals("Inside and outside must be distinct regions", idOutside, idInside)
    }

    @Test
    fun testBothImageTypes_OpaqueAndTransparentAlpha() {
        val w = 20
        val h = 20

        // Type 1: Opaque white background with black line
        val opaquePixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }
        for (x in 0 until w) opaquePixels[10 * w + x] = 0xFF000000.toInt()
        val opaqueResult = LabelMapBuilder.buildFromPixelsSync(opaquePixels, w, h, hasAlpha = false, dilationPixels = 0)
        assertEquals("Line must be border in opaque", LabelMapBuilder.BORDER_ID, opaqueResult.getRegionId(5, 10))
        assertTrue("Background must be region", opaqueResult.getRegionId(5, 5) > 0)

        // Type 2: Transparent background (alpha = 0) with dark line (alpha = 255, lum < 128)
        val transparentPixels = IntArray(w * h) { 0x00000000 } // Transparent
        for (x in 0 until w) transparentPixels[10 * w + x] = 0xFF000000.toInt() // Opaque black
        val transResult = LabelMapBuilder.buildFromPixelsSync(transparentPixels, w, h, hasAlpha = true, dilationPixels = 0)
        assertEquals("Line must be border in transparent", LabelMapBuilder.BORDER_ID, transResult.getRegionId(5, 10))
        assertTrue("Transparent canvas must be region", transResult.getRegionId(5, 5) > 0)
    }

    @Test
    fun testDeterministicDilationArbitration() {
        val w = 30
        val h = 10
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }

        // Two regions separated by a 2-pixel wide black vertical wall at x = 14, 15
        for (y in 0 until h) {
            pixels[y * w + 14] = 0xFF000000.toInt()
            pixels[y * w + 15] = 0xFF000000.toInt()
        }

        val result = LabelMapBuilder.buildFromPixelsSync(pixels, w, h, hasAlpha = false, dilationPixels = 1)
        assertEquals(2, result.regionCount)

        // Region 1 is on the left (x < 14), Region 2 is on the right (x > 15)
        val leftId = result.getRegionId(5, 5)
        val rightId = result.getRegionId(25, 5)
        assertEquals("Left region must be 1", 1.toShort(), leftId)
        assertEquals("Right region must be 2", 2.toShort(), rightId)

        // At x = 14 (neighbor to Region 1), lower regionId (1) should win
        val dilatedAt14 = result.getRegionId(14, 5)
        assertEquals("Lower regionId (1) must win arbitration", 1.toShort(), dilatedAt14)
    }

    @Test
    fun testRadialNeighborSearchOnBorder() {
        val w = 40
        val h = 40
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }

        // Draw a thick horizontal border from y = 18 to y = 22
        for (y in 18..22) {
            for (x in 0 until w) {
                pixels[y * w + x] = 0xFF000000.toInt()
            }
        }

        val result = LabelMapBuilder.buildFromPixelsSync(pixels, w, h, hasAlpha = false, dilationPixels = 0)
        assertEquals("Border pixel directly at (20, 20)", LabelMapBuilder.BORDER_ID, result.getRegionId(20, 20))

        // Radial search around border tap (20, 19) should find the top region
        val nearestId = result.findNearestRegionId(20, 19, maxRadius = 5)
        assertTrue("Radial search must find a valid neighboring region", nearestId > 0)

        // Far out of bounds or completely isolated border should return BORDER_ID safely
        val outsideCanvas = result.findNearestRegionId(-100, -100, maxRadius = 2)
        assertEquals(LabelMapBuilder.BORDER_ID, outsideCanvas)
    }

    @Test
    fun testBitmapBoundariesDoNotThrowException() {
        val w = 10
        val h = 10
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }
        val result = LabelMapBuilder.buildFromPixelsSync(pixels, w, h, hasAlpha = false, dilationPixels = 1)

        // Edge coordinates must not crash
        assertEquals(1.toShort(), result.getRegionId(0, 0))
        assertEquals(1.toShort(), result.getRegionId(w - 1, h - 1))
        assertEquals(LabelMapBuilder.BORDER_ID, result.getRegionId(-1, 0))
        assertEquals(LabelMapBuilder.BORDER_ID, result.getRegionId(w, h))
    }

    @Test
    fun testBaselineRegionCountsAndNoLeakOver60PercentFor10Templates() {
        val assetsDir = File("src/main/assets/templates")
        if (!assetsDir.exists()) {
            TemplateArtGenerator.generateAll(assetsDir)
        }

        val totalCanvasPixels = 1024 * 1024
        val maxAllowedAreaPixels = (totalCanvasPixels * 0.60).toInt() // 60% threshold

        // Baselines established at Phase P2 (real measured counts from first run)
        val expectedBaselines = mapOf(
            "mandala_blossom_8" to 36,
            "mandala_sun_wheel" to 62,
            "mandala_star_shield" to 22,
            "flower_lotus_zen" to 101,
            "flower_daisy_ring" to 420,
            "flower_tulip_trio" to 134,
            "animal_geometric_owl" to 15,
            "animal_butterfly_monarch" to 16,
            "animal_cat_poly" to 9,
            "geo_sacred_hex" to 31
        )

        for (tmpl in TemplateArtGenerator.templates) {
            val canvas = TemplateArtGenerator.render(tmpl)
            val pixels = IntArray(1024 * 1024)
            for (i in 0 until 1024 * 1024) {
                pixels[i] = if (canvas.buffer[i] != 0.toByte()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }

            val result = LabelMapBuilder.buildFromPixelsSync(pixels, 1024, 1024, hasAlpha = false, dilationPixels = 1)
            System.err.println("P2_REAL_COUNT: \"${tmpl.id}\" to ${result.regionCount}, maxArea=${result.regionPixelCounts.maxOrNull()}")

            val baseline = expectedBaselines[tmpl.id]
            if (baseline != null) {
                assertEquals("Region count for ${tmpl.id} must match P2 baseline", baseline, result.regionCount)
            }

            // Check no single internal region exceeds 60% of total canvas area (leak detection)
            for (regionId in 1..result.regionCount) {
                val regionPixels = result.regionPixelCounts[regionId]
                assertTrue(
                    "Region $regionId of ${tmpl.id} has $regionPixels pixels, exceeding 60% ($maxAllowedAreaPixels) - potential line leak!",
                    regionPixels <= maxAllowedAreaPixels
                )
            }
        }
    }
}

