package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionLogPlayerTest {

    private lateinit var sampleLog: ActionLog
    private lateinit var player: ActionLogPlayer
    private val t0 = 1000000L

    @Before
    fun setUp() {
        val actions = listOf(
            EditAction.Fill(1, 0xFF111111.toInt(), timestampMs = t0),             // t = 0
            EditAction.Stroke(listOf(PointF(0f, 0f)), 0xFF222222.toInt(), 10f, timestampMs = t0 + 1000L), // t = 1000ms
            EditAction.Fill(2, 0xFF333333.toInt(), timestampMs = t0 + 2000L),     // t = 2000ms
            EditAction.Stroke(listOf(PointF(10f, 10f)), 0xFF444444.toInt(), 10f, timestampMs = t0 + 3000L), // t = 3000ms
            EditAction.Erase(listOf(PointF(5f, 5f)), 20f, timestampMs = t0 + 4000L) // t = 4000ms
        )
        sampleLog = ActionLog(
            version = 1,
            artworkId = "test_player",
            actions = actions,
            totalDurationMs = 4000L
        )
        player = ActionLogPlayer(sampleLog)
    }

    @Test
    fun testActionCountAndDuration() {
        assertEquals(5, player.actionCount)
        assertEquals(t0, player.startTimeMs)
        assertEquals(t0 + 4000L, player.endTimeMs)
        assertEquals(4000L, player.rawDurationMs)
    }

    @Test
    fun testGetActionsUpToRelativeTime() {
        // At t = 0ms: should include action 1
        val actionsAt0 = player.getActionsUpToRelativeTime(0L)
        assertEquals(1, actionsAt0.size)

        // At t = 1500ms: should include action 1 & 2
        val actionsAt1500 = player.getActionsUpToRelativeTime(1500L)
        assertEquals(2, actionsAt1500.size)

        // At t = 3000ms: should include actions 1, 2, 3, 4
        val actionsAt3000 = player.getActionsUpToRelativeTime(3000L)
        assertEquals(4, actionsAt3000.size)

        // At t = 5000ms (past end): should include all 5 actions
        val actionsAt5000 = player.getActionsUpToRelativeTime(5000L)
        assertEquals(5, actionsAt5000.size)
    }

    @Test
    fun testGetActionsUpToStep() {
        assertEquals(0, player.getActionsUpToStep(0).size)
        assertEquals(1, player.getActionsUpToStep(1).size)
        assertEquals(3, player.getActionsUpToStep(3).size)
        assertEquals(5, player.getActionsUpToStep(5).size)
        assertEquals(5, player.getActionsUpToStep(10).size) // Clamped to max size
    }

    @Test
    fun testGetActionsForTimelapseFrames() {
        val totalFrames = 5

        // Frame 0: Start of timelapse
        val frame0 = player.getActionsForTimelapseFrame(0, totalFrames)
        assertEquals("Frame 0 must contain at least 1 action", 1, frame0.size)

        // Frame 4 (last frame): Must contain all actions
        val frameLast = player.getActionsForTimelapseFrame(totalFrames - 1, totalFrames)
        assertEquals("Final frame must contain all actions", 5, frameLast.size)

        // Monotonic progression: each subsequent frame must have >= actions than previous
        var prevCount = 0
        for (f in 0 until totalFrames) {
            val count = player.getActionsForTimelapseFrame(f, totalFrames).size
            assertTrue("Action count in frame $f ($count) must be >= previous frame ($prevCount)", count >= prevCount)
            prevCount = count
        }
    }
}

