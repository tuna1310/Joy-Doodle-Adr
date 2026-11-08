package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeInterpolatorTest {

    @Test
    fun testEmptyAndSinglePointReturnsEmptySegments() {
        val empty = StrokeInterpolator.interpolateToSegments(emptyList())
        assertTrue("Empty point list must return empty segments", empty.isEmpty())

        val single = StrokeInterpolator.interpolateToSegments(listOf(PointF(10f, 20f)))
        assertTrue("Single point list must return empty segments", single.isEmpty())
    }

    @Test
    fun testTwoPointsProducesSingleSegment() {
        val points = listOf(PointF(0f, 0f), PointF(100f, 100f))
        val segments = StrokeInterpolator.interpolateToSegments(points)

        assertEquals("Two points must produce 1 segment", 1, segments.size)
        val seg = segments[0]
        assertEquals(0f, seg.startX, 0.001f)
        assertEquals(0f, seg.startY, 0.001f)
        assertEquals(100f, seg.endX, 0.001f)
        assertEquals(100f, seg.endY, 0.001f)
        assertEquals(50f, seg.controlX, 0.001f)
        assertEquals(50f, seg.controlY, 0.001f)
    }

    @Test
    fun testMultiPointsProducesConnectedSmoothSegments() {
        val points = listOf(
            PointF(0f, 0f),
            PointF(50f, 100f),
            PointF(100f, 0f),
            PointF(150f, 100f)
        )
        val segments = StrokeInterpolator.interpolateToSegments(points)

        assertTrue("Must produce at least 3 segments for 4 points", segments.size >= 3)

        // Check continuity: end of segment i must equal start of segment i+1
        for (i in 0 until segments.size - 1) {
            assertEquals("Segment $i endX must match segment ${i + 1} startX",
                segments[i].endX, segments[i + 1].startX, 0.001f)
            assertEquals("Segment $i endY must match segment ${i + 1} startY",
                segments[i].endY, segments[i + 1].startY, 0.001f)
        }

        // Final endpoint must reach the last point
        val lastSeg = segments.last()
        assertEquals(150f, lastSeg.endX, 0.001f)
        assertEquals(100f, lastSeg.endY, 0.001f)
    }

    @Test
    fun testSampleQuadEndpointsAndMidpoint() {
        val segment = StrokeInterpolator.QuadSegment(
            startX = 0f, startY = 0f,
            controlX = 50f, controlY = 100f,
            endX = 100f, endY = 0f
        )

        val pStart = StrokeInterpolator.sampleQuad(segment, 0f)
        assertEquals(0f, pStart.x, 0.001f)
        assertEquals(0f, pStart.y, 0.001f)

        val pEnd = StrokeInterpolator.sampleQuad(segment, 1f)
        assertEquals(100f, pEnd.x, 0.001f)
        assertEquals(0f, pEnd.y, 0.001f)

        // At t = 0.5: B(0.5) = 0.25*0 + 0.5*50 + 0.25*100 = 50; Y = 0.25*0 + 0.5*100 + 0.25*0 = 50
        val pMid = StrokeInterpolator.sampleQuad(segment, 0.5f)
        assertEquals(50f, pMid.x, 0.001f)
        assertEquals(50f, pMid.y, 0.001f)
    }

    @Test
    fun testDownsamplePointsRemovesJitter() {
        val rawPoints = listOf(
            PointF(10f, 10f),
            PointF(10.1f, 10.2f), // sub-pixel jitter
            PointF(10.2f, 10.1f), // sub-pixel jitter
            PointF(20f, 20f),     // significant move
            PointF(20.3f, 20.1f), // sub-pixel jitter
            PointF(50f, 50f)      // endpoint
        )

        val downsampled = StrokeInterpolator.downsamplePoints(rawPoints, minDistance = 2.0f)
        assertTrue("Downsampled points count (${downsampled.size}) must be less than raw (${rawPoints.size})",
            downsampled.size < rawPoints.size)
        assertEquals("First point must be preserved", rawPoints.first(), downsampled.first())
        assertEquals("Last point must be preserved", rawPoints.last(), downsampled.last())
    }
}

