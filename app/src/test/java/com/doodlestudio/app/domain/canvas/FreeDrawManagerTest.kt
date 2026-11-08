package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.EditAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FreeDrawManagerTest {

    private lateinit var historyManager: HistoryManager
    private lateinit var freeDrawManager: FreeDrawManager

    @Before
    fun setUp() {
        historyManager = HistoryManager(maxHistorySize = 50)
        freeDrawManager = FreeDrawManager(historyManager)
    }

    @Test
    fun testDrawingStrokeCreatesValidEditActionStroke() {
        freeDrawManager.setBrushTool(BrushTool.PEN)
        freeDrawManager.setBrushSize(16f)

        freeDrawManager.onTouchDown(10f, 10f)
        freeDrawManager.onTouchMove(20f, 20f)
        freeDrawManager.onTouchMove(30f, 30f)
        val action = freeDrawManager.onTouchUp()

        assertNotNull("Stroke action must be returned on touchUp", action)
        assertTrue("Action must be an EditAction.Stroke", action is EditAction.Stroke)

        val stroke = action as EditAction.Stroke
        assertEquals("PEN", stroke.toolType)
        assertEquals(16f, stroke.strokeWidth, 0.001f)
        assertTrue("Stroke points must contain points", stroke.points.size >= 2)
        assertEquals(1, freeDrawManager.actions.size)
        assertTrue(historyManager.canUndo.value)
    }

    @Test
    fun testEraserToolCreatesEditActionErase() {
        freeDrawManager.setBrushTool(BrushTool.ERASER)
        freeDrawManager.setBrushSize(20f)

        freeDrawManager.onTouchDown(50f, 50f)
        freeDrawManager.onTouchMove(60f, 60f)
        val action = freeDrawManager.onTouchUp()

        assertNotNull("Eraser action must be returned", action)
        assertTrue("Action must be an EditAction.Erase", action is EditAction.Erase)

        val erase = action as EditAction.Erase
        assertEquals(30f, erase.strokeWidth, 0.001f) // Eraser is 1.5x brush size
    }

    @Test
    fun testUndoRedoAtLeast30StepsInFreeDraw() {
        freeDrawManager.setBrushTool(BrushTool.MARKER)

        // Draw 32 strokes
        for (i in 1..32) {
            freeDrawManager.onTouchDown(i.toFloat(), i.toFloat())
            freeDrawManager.onTouchMove((i + 5).toFloat(), (i + 5).toFloat())
            freeDrawManager.onTouchUp()
        }

        assertEquals(32, freeDrawManager.actions.size)
        assertEquals(32, historyManager.size)

        // Undo 30 steps
        for (step in 1..30) {
            val undone = freeDrawManager.undo()
            assertNotNull("Undone action at step $step must not be null", undone)
        }

        assertEquals(2, freeDrawManager.actions.size)
        assertTrue(historyManager.canRedo.value)

        // Redo 30 steps
        for (step in 1..30) {
            val redone = freeDrawManager.redo()
            assertNotNull("Redone action at step $step must not be null", redone)
        }

        assertEquals(32, freeDrawManager.actions.size)
    }

    @Test
    fun testClearResetsEverything() {
        freeDrawManager.onTouchDown(10f, 10f)
        freeDrawManager.onTouchUp()
        assertEquals(1, freeDrawManager.actions.size)

        freeDrawManager.clear()
        assertEquals(0, freeDrawManager.actions.size)
        assertEquals(0, freeDrawManager.currentPoints.size)
        assertFalse(historyManager.canUndo.value)
    }

    @Test
    fun testSinglePointTapCreatesValidStroke() {
        // Tap down without drag (single point / dot)
        freeDrawManager.setBrushTool(BrushTool.NEON)
        freeDrawManager.setBrushSize(14f)

        freeDrawManager.onTouchDown(55f, 88f)
        val action = freeDrawManager.onTouchUp()

        assertNotNull("Single point tap must produce a valid action", action)
        assertTrue("Action must be EditAction.Stroke", action is EditAction.Stroke)

        val stroke = action as EditAction.Stroke
        assertEquals(1, stroke.points.size)
        assertEquals(55f, stroke.points[0].x, 0.001f)
        assertEquals(88f, stroke.points[0].y, 0.001f)
        assertEquals("NEON", stroke.toolType)
        assertEquals(1, freeDrawManager.actions.size)
    }

    @Test
    fun testLongStrokeWithManyPoints() {
        // Continuous long stroke with 100 points
        freeDrawManager.setBrushTool(BrushTool.NEON)
        freeDrawManager.setBrushSize(12f)

        freeDrawManager.onTouchDown(0f, 0f)
        for (i in 1..100) {
            freeDrawManager.onTouchMove(i * 3f, i * 2f)
        }
        val action = freeDrawManager.onTouchUp()

        assertNotNull("Long stroke must return an action", action)
        assertTrue(action is EditAction.Stroke)

        val stroke = action as EditAction.Stroke
        assertTrue("Long stroke should have multiple points", stroke.points.size > 20)
        assertEquals(0f, stroke.points.first().x, 0.001f)
        assertEquals(0f, stroke.points.first().y, 0.001f)
        assertEquals(300f, stroke.points.last().x, 0.001f)
        assertEquals(200f, stroke.points.last().y, 0.001f)
    }

    @Test
    fun testChangeToolMidway() {
        // Stroke 1 with PEN
        freeDrawManager.setBrushTool(BrushTool.PEN)
        freeDrawManager.onTouchDown(10f, 10f)
        freeDrawManager.onTouchMove(20f, 20f)
        val action1 = freeDrawManager.onTouchUp() as EditAction.Stroke
        assertEquals("PEN", action1.toolType)

        // Change tool midway to NEON
        freeDrawManager.setBrushTool(BrushTool.NEON)
        freeDrawManager.onTouchDown(30f, 30f)
        freeDrawManager.onTouchMove(40f, 40f)
        val action2 = freeDrawManager.onTouchUp() as EditAction.Stroke
        assertEquals("NEON", action2.toolType)

        // Change tool midway to RAINBOW
        freeDrawManager.setBrushTool(BrushTool.RAINBOW)
        freeDrawManager.onTouchDown(50f, 50f)
        freeDrawManager.onTouchMove(60f, 60f)
        val action3 = freeDrawManager.onTouchUp() as EditAction.Stroke
        assertEquals("RAINBOW", action3.toolType)

        assertEquals(3, freeDrawManager.actions.size)
        assertEquals("PEN", (freeDrawManager.actions[0] as EditAction.Stroke).toolType)
        assertEquals("NEON", (freeDrawManager.actions[1] as EditAction.Stroke).toolType)
        assertEquals("RAINBOW", (freeDrawManager.actions[2] as EditAction.Stroke).toolType)
    }

    @Test
    fun testDrawAfterUndo() {
        freeDrawManager.setBrushTool(BrushTool.NEON)

        // Draw stroke 1
        freeDrawManager.onTouchDown(10f, 10f)
        freeDrawManager.onTouchMove(20f, 20f)
        freeDrawManager.onTouchUp()

        // Draw stroke 2
        freeDrawManager.onTouchDown(30f, 30f)
        freeDrawManager.onTouchMove(40f, 40f)
        freeDrawManager.onTouchUp()
        assertEquals(2, freeDrawManager.actions.size)

        // Undo stroke 2
        freeDrawManager.undo()
        assertEquals(1, freeDrawManager.actions.size)

        // Draw stroke 3 after undo
        freeDrawManager.onTouchDown(50f, 50f)
        freeDrawManager.onTouchMove(60f, 60f)
        freeDrawManager.onTouchUp()

        assertEquals(2, freeDrawManager.actions.size)
        // Redo stack should now be cleared because a new action was recorded
        assertFalse(historyManager.canRedo.value)
    }
}

