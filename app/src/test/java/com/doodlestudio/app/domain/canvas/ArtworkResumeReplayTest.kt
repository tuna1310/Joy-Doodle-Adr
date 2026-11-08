package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkResumeReplayTest {

    @Test
    fun testResumeSessionAndAppendNewActions() {
        // Initial session: User colored 2 regions and drew 1 stroke
        val session1Actions = listOf(
            EditAction.Fill(regionId = 1, color = 0xFFFF0000.toInt(), timestampMs = 1000L),
            EditAction.Fill(regionId = 2, color = 0xFF00FF00.toInt(), timestampMs = 2000L),
            EditAction.Stroke(listOf(PointF(10f, 10f), PointF(50f, 50f)), 0xFF0000FF.toInt(), 12f, timestampMs = 3000L)
        )
        val initialLog = ActionLog(
            version = 1,
            artworkId = "resume_test_art",
            templateId = "flower_lotus_zen",
            actions = session1Actions,
            totalDurationMs = 2000L
        )

        // Save session 1
        val serializedLog = ActionLogSerializer.serialize(initialLog)

        // Simulate App closed, re-opened, and artwork resumed from Library
        val reloadedLog = ActionLogSerializer.deserialize(serializedLog)
        assertEquals("resume_test_art", reloadedLog.artworkId)
        assertEquals(3, reloadedLog.actions.size)

        // Reconstruct HistoryManager and replay
        val historyManager = HistoryManager(maxHistorySize = 50)
        for (action in reloadedLog.actions) {
            historyManager.recordAction(action)
        }
        assertEquals(3, historyManager.size)
        assertTrue(historyManager.canUndo.value)

        // Continue editing: User draws 2 more strokes in session 2
        val session2Action1 = EditAction.Stroke(
            points = listOf(PointF(60f, 60f), PointF(80f, 80f)),
            color = 0xFFFFC107.toInt(),
            strokeWidth = 16f,
            timestampMs = 4000L
        )
        val session2Action2 = EditAction.Erase(
            points = listOf(PointF(20f, 20f), PointF(30f, 30f)),
            strokeWidth = 24f,
            timestampMs = 5000L
        )
        historyManager.recordAction(session2Action1)
        historyManager.recordAction(session2Action2)

        assertEquals(5, historyManager.size)

        // Undo the last erase action
        val undone = historyManager.undo()
        assertNotNull(undone)
        assertTrue("Undone action must be Erase", undone is EditAction.Erase)
        assertEquals(4, historyManager.size)

        // Package updated ActionLog
        val updatedActions = historyManager.getActionLog()
        val finalLog = ActionLog(
            version = 1,
            artworkId = reloadedLog.artworkId,
            templateId = reloadedLog.templateId,
            actions = updatedActions,
            totalDurationMs = 4000L
        )

        assertEquals(4, finalLog.actions.size)
        assertEquals(1, (finalLog.actions[0] as EditAction.Fill).regionId)
        assertEquals(2, (finalLog.actions[1] as EditAction.Fill).regionId)
        assertEquals(16f, (finalLog.actions[3] as EditAction.Stroke).strokeWidth, 0.001f)
    }
}

