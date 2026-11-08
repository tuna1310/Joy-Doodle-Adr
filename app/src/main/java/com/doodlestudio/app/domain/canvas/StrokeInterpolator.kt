package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.PointF

/**
 * Pure Kotlin stroke smoothing and interpolation.
 * Uses Quadratic Bézier curve approximation between successive touch points
 * to produce smooth curves and eliminate jagged angle artifacts.
 */
object StrokeInterpolator {

    data class QuadSegment(
        val startX: Float,
        val startY: Float,
        val controlX: Float,
        val controlY: Float,
        val endX: Float,
        val endY: Float
    )

    /**
     * Converts raw touch points into a sequence of quadratic bezier segments.
     * Guaranteed to handle empty, 1-point, 2-point, and multi-point lists without crashing.
     */
    fun interpolateToSegments(points: List<PointF>): List<QuadSegment> {
        if (points.size < 2) return emptyList()

        if (points.size == 2) {
            val p0 = points[0]
            val p1 = points[1]
            return listOf(
                QuadSegment(
                    startX = p0.x, startY = p0.y,
                    controlX = (p0.x + p1.x) / 2f, controlY = (p0.y + p1.y) / 2f,
                    endX = p1.x, endY = p1.y
                )
            )
        }

        val segments = mutableListOf<QuadSegment>()
        var prevX = points[0].x
        var prevY = points[0].y

        for (i in 1 until points.size) {
            val curX = points[i].x
            val curY = points[i].y
            val midX = (prevX + curX) / 2f
            val midY = (prevY + curY) / 2f

            val startX = if (i == 1) prevX else (points[i - 2].x + prevX) / 2f
            val startY = if (i == 1) prevY else (points[i - 2].y + prevY) / 2f

            segments.add(
                QuadSegment(
                    startX = startX,
                    startY = startY,
                    controlX = prevX,
                    controlY = prevY,
                    endX = midX,
                    endY = midY
                )
            )

            prevX = curX
            prevY = curY
        }

        // Connect the last midpoint to the final endpoint
        val lastIdx = points.size - 1
        val lastMidX = (points[lastIdx - 1].x + points[lastIdx].x) / 2f
        val lastMidY = (points[lastIdx - 1].y + points[lastIdx].y) / 2f
        segments.add(
            QuadSegment(
                startX = lastMidX,
                startY = lastMidY,
                controlX = points[lastIdx].x,
                controlY = points[lastIdx].y,
                endX = points[lastIdx].x,
                endY = points[lastIdx].y
            )
        )

        return segments
    }

    /**
     * Samples points along a quadratic bezier segment at parameter t in [0.0, 1.0].
     * Formula: B(t) = (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
     */
    fun sampleQuad(segment: QuadSegment, t: Float): PointF {
        val clampedT = t.coerceIn(0f, 1f)
        val oneMinusT = 1f - clampedT
        val x = oneMinusT * oneMinusT * segment.startX + 2f * oneMinusT * clampedT * segment.controlX + clampedT * clampedT * segment.endX
        val y = oneMinusT * oneMinusT * segment.startY + 2f * oneMinusT * clampedT * segment.controlY + clampedT * clampedT * segment.endY
        return PointF(x, y)
    }

    /**
     * Downsamples points that are closer than minDistance to reduce memory & JSON payload size.
     */
    fun downsamplePoints(points: List<PointF>, minDistance: Float = 2.0f): List<PointF> {
        if (points.size <= 2) return points
        val result = mutableListOf<PointF>()
        result.add(points.first())
        var lastPoint = points.first()

        val minDistanceSq = minDistance * minDistance
        for (i in 1 until points.size - 1) {
            val p = points[i]
            val dx = p.x - lastPoint.x
            val dy = p.y - lastPoint.y
            if (dx * dx + dy * dy >= minDistanceSq) {
                result.add(p)
                lastPoint = p
            }
        }
        result.add(points.last())
        return result
    }
}

