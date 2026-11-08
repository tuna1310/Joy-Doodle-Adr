package com.doodlestudio.app.domain.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegionFillerTest {

    private lateinit var labelMapResult: LabelMapBuilder.LabelMapResult
    private lateinit var regionFiller: RegionFiller

    @Before
    fun setUp() {
        // Create a 10x10 synthetic test image with 3 distinct regions
        // Region 1: left half (x = 0..3)
        // Region 2: center column (x = 4..5)
        // Region 3: right half (x = 7..9)
        // Wall 1 at x = 4, Wall 2 at x = 6
        val w = 10
        val h = 10
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }
        for (y in 0 until h) {
            pixels[y * w + 4] = 0xFF000000.toInt() // Wall 1
            pixels[y * w + 6] = 0xFF000000.toInt() // Wall 2
        }

        labelMapResult = LabelMapBuilder.buildFromPixelsSync(pixels, w, h, hasAlpha = false, dilationPixels = 0)
        regionFiller = RegionFiller(labelMapResult, HistoryManager(maxHistorySize = 50))
    }

    @Test
    fun testFillRegionRecolorsPixelsAndUpdatesColorTable() {
        val redColor = 0xFFFF0000.toInt()
        val targetRegion = 1

        val action = regionFiller.fill(targetRegion, redColor)
        assertNotNull("Fill action must be generated", action)
        assertEquals(targetRegion, action?.regionId)
        assertEquals(redColor, action?.color)
        assertEquals(0, action?.prevColor)

        assertEquals("Color table must be updated", redColor, regionFiller.getRegionColor(targetRegion))

        // Check that pixels in region 1 are colored red
        for (y in 0 until labelMapResult.height) {
            for (x in 0..3) {
                val idx = y * labelMapResult.width + x
                assertEquals("Pixel at ($x, $y) must be red", redColor, regionFiller.fillPixels[idx])
            }
        }
    }

    @Test
    fun testFillSameColorIsIgnored() {
        val blueColor = 0xFF0000FF.toInt()
        val targetRegion = 2

        val action1 = regionFiller.fill(targetRegion, blueColor)
        assertNotNull(action1)
        assertTrue(regionFiller.canUndo.value)

        // Filling again with identical color must be ignored
        val action2 = regionFiller.fill(targetRegion, blueColor)
        assertNull("Redundant fill with same color must return null", action2)
        assertEquals(1, regionFiller.getActionLog().size)
    }

    @Test
    fun testUndoRedoAtLeast30Steps() {
        val testColors = listOf(
            0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
            0xFFFFFF00.toInt(), 0xFFFF00FF.toInt(), 0xFF00FFFF.toInt()
        )

        // Perform 32 consecutive fills across available regions
        val steps = 32
        for (step in 1..steps) {
            val regionId = (step % 3) + 1
            val color = testColors[step % testColors.size]
            val action = regionFiller.fill(regionId, color)
            assertNotNull("Step $step fill must succeed", action)
        }

        assertTrue("Can undo must be true", regionFiller.canUndo.value)
        assertFalse("Can redo must be false after new action", regionFiller.canRedo.value)

        // Undo all 32 steps
        for (step in steps downTo 1) {
            assertTrue("Must be able to undo at step $step", regionFiller.canUndo.value)
            val undone = regionFiller.undo()
            assertNotNull("Undone action at step $step must not be null", undone)
        }

        assertFalse("Can undo must be false after fully rewinding", regionFiller.canUndo.value)
        assertTrue("Can redo must be true after undoing", regionFiller.canRedo.value)

        // Verify all regions are back to initial 0 (uncolored)
        for (r in 1..labelMapResult.regionCount) {
            assertEquals("Region $r must be uncolored after full undo", 0, regionFiller.getRegionColor(r))
        }

        // Redo all 32 steps
        for (step in 1..steps) {
            assertTrue("Must be able to redo at step $step", regionFiller.canRedo.value)
            val redone = regionFiller.redo()
            assertNotNull("Redone action at step $step must not be null", redone)
        }

        assertTrue("Can undo must be true after full redo", regionFiller.canUndo.value)
        assertFalse("Can redo must be false after full redo", regionFiller.canRedo.value)
    }

    @Test
    fun testResetClearsEverything() {
        regionFiller.fill(1, 0xFFFF0000.toInt())
        regionFiller.fill(2, 0xFF00FF00.toInt())
        assertTrue(regionFiller.canUndo.value)

        regionFiller.reset()

        assertFalse(regionFiller.canUndo.value)
        assertFalse(regionFiller.canRedo.value)
        assertEquals(0, regionFiller.getActionLog().size)
        assertEquals(0, regionFiller.getRegionColor(1))
        assertEquals(0, regionFiller.getRegionColor(2))
    }
}

