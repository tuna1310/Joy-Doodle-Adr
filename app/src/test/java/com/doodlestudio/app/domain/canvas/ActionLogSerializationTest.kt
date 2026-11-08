package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionLogSerializationTest {

    @Test
    fun testRoundTripSerializationWithMixedActions() {
        val baseTime = 1700000000000L
        val originalLog = ActionLog(
            version = 1,
            artworkId = "artwork_test_42",
            templateId = "flower_lotus_zen",
            isBlankCanvas = false,
            canvasWidth = 1024,
            canvasHeight = 1024,
            backgroundColor = 0xFFFFFFFF.toInt(),
            actions = listOf(
                EditAction.Fill(
                    regionId = 3,
                    color = 0xFFFF5722.toInt(),
                    prevColor = 0,
                    normalizedX = 0.5f,
                    normalizedY = 0.5f,
                    timestampMs = baseTime
                ),
                EditAction.Stroke(
                    points = listOf(PointF(10f, 20f), PointF(30f, 40f), PointF(50f, 60f)),
                    color = 0xFF4CAF50.toInt(),
                    strokeWidth = 12f,
                    alpha = 0.8f,
                    toolType = "NEON",
                    timestampMs = baseTime + 500L
                ),
                EditAction.Erase(
                    points = listOf(PointF(100f, 100f), PointF(120f, 120f)),
                    strokeWidth = 24f,
                    timestampMs = baseTime + 1200L
                )
            ),
            totalDurationMs = 1200L,
            createdAt = baseTime
        )

        // Serialize to JSON
        val jsonString = ActionLogSerializer.serialize(originalLog)
        assertNotNull("Serialized string must not be null", jsonString)
        assertTrue("JSON must contain artworkId", jsonString.contains("artwork_test_42"))
        assertTrue("JSON must contain flower_lotus_zen", jsonString.contains("flower_lotus_zen"))
        assertTrue("JSON must contain NEON", jsonString.contains("NEON"))

        // Deserialize back
        val decoded = ActionLogSerializer.deserialize(jsonString)
        assertEquals(originalLog.version, decoded.version)
        assertEquals(originalLog.artworkId, decoded.artworkId)
        assertEquals(originalLog.templateId, decoded.templateId)
        assertEquals(originalLog.isBlankCanvas, decoded.isBlankCanvas)
        assertEquals(originalLog.canvasWidth, decoded.canvasWidth)
        assertEquals(originalLog.canvasHeight, decoded.canvasHeight)
        assertEquals(originalLog.backgroundColor, decoded.backgroundColor)
        assertEquals(originalLog.actions.size, decoded.actions.size)
        assertEquals(originalLog.totalDurationMs, decoded.totalDurationMs)

        // Verify action 1 (Fill)
        val action1 = decoded.actions[0] as EditAction.Fill
        assertEquals(3, action1.regionId)
        assertEquals(0xFFFF5722.toInt(), action1.color)
        assertEquals(baseTime, action1.timestampMs)

        // Verify action 2 (Stroke)
        val action2 = decoded.actions[1] as EditAction.Stroke
        assertEquals(3, action2.points.size)
        assertEquals(0xFF4CAF50.toInt(), action2.color)
        assertEquals(12f, action2.strokeWidth, 0.001f)
        assertEquals(0.8f, action2.alpha, 0.001f)
        assertEquals("NEON", action2.toolType)
        assertEquals(baseTime + 500L, action2.timestampMs)

        // Verify action 3 (Erase)
        val action3 = decoded.actions[2] as EditAction.Erase
        assertEquals(2, action3.points.size)
        assertEquals(24f, action3.strokeWidth, 0.001f)
        assertEquals(baseTime + 1200L, action3.timestampMs)
    }

    @Test
    fun testPayloadByteSizeIsCompact() {
        val actions = (1..50).map { i ->
            EditAction.Fill(
                regionId = i,
                color = 0xFF000000.toInt() or (i * 5000),
                prevColor = 0,
                normalizedX = i * 0.01f,
                normalizedY = i * 0.01f,
                timestampMs = 1700000000000L + i * 100L
            )
        }
        val log = ActionLog(
            version = 1,
            artworkId = "compact_test",
            canvasWidth = 1024,
            canvasHeight = 1024,
            actions = actions
        )

        val size = ActionLogSerializer.estimateByteSize(log)
        // 50 actions should be well below 20KB
        assertTrue("50 actions JSON payload size ($size bytes) must be < 20KB", size < 20_000)
    }
}

