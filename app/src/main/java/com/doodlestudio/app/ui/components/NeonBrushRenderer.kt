package com.doodlestudio.app.ui.components

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.doodlestudio.app.domain.canvas.StrokeInterpolator
import com.doodlestudio.app.domain.model.PointF

/**
 * High-performance Neon Brush Renderer implementing 3-pass luminous glow:
 * 1. Outer diffused glow (wide radius + BlurMaskFilter)
 * 2. Inner intense glow (medium radius + BlurMaskFilter)
 * 3. Electric white-hot core (narrow, blended 70% toward pure white)
 *
 * Supports Bitmap stroke caching for 60fps interactive drawing.
 */
object NeonBrushRenderer {

    /**
     * Draws a single stroke onto the target Canvas with 3-pass neon glow.
     */
    fun drawNeonStroke(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float = 1.0f,
        toolType: String = "NEON",
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ) {
        if (points.isEmpty()) return

        if (points.size == 1) {
            val p = points[0]
            drawNeonDot(canvas, p.x * scaleX, p.y * scaleY, color, strokeWidth, alpha, toolType)
            return
        }

        val segments = StrokeInterpolator.interpolateToSegments(points)
        if (segments.isEmpty()) return

        val path = Path().apply {
            moveTo(segments[0].startX * scaleX, segments[0].startY * scaleY)
            for (seg in segments) {
                quadTo(
                    seg.controlX * scaleX,
                    seg.controlY * scaleY,
                    seg.endX * scaleX,
                    seg.endY * scaleY
                )
            }
        }

        when (toolType) {
            "ERASER" -> {
                val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    this.strokeWidth = strokeWidth * 1.6f
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
                canvas.drawPath(path, erasePaint)
            }

            "RAINBOW" -> {
                drawRainbowPath(canvas, points, strokeWidth, alpha, scaleX, scaleY)
            }

            "SPARKLE", "STARS" -> {
                drawThreePassGlowPath(canvas, path, color, strokeWidth, alpha)
                drawSparkleStars(canvas, points, color, strokeWidth, scaleX, scaleY)
            }

            else -> { // "NEON", "PEN", etc.
                drawThreePassGlowPath(canvas, path, color, strokeWidth, alpha)
            }
        }
    }

    private fun drawThreePassGlowPath(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        alpha: Float
    ) {
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        // Pass 1: Diffused Outer Glow
        val outerRadius = (strokeWidth * 1.2f).coerceAtLeast(1f)
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 2.5f
            this.color = AndroidColor.argb((baseAlpha * 0.32f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter(outerRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, outerPaint)

        // Pass 2: Vivid Inner Glow
        val innerRadius = (strokeWidth * 0.5f).coerceAtLeast(1f)
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 1.4f
            this.color = AndroidColor.argb((baseAlpha * 0.70f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter(innerRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, innerPaint)

        // Pass 3: Electric White Hot Core (Blended 70% toward pure white)
        val coreR = (r * 0.3f + 255 * 0.7f).toInt().coerceIn(0, 255)
        val coreG = (g * 0.3f + 255 * 0.7f).toInt().coerceIn(0, 255)
        val coreB = (b * 0.3f + 255 * 0.7f).toInt().coerceIn(0, 255)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 0.55f
            this.color = AndroidColor.argb((baseAlpha * 0.95f).toInt(), coreR, coreG, coreB)
        }
        canvas.drawPath(path, corePaint)
    }

    private fun drawNeonDot(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        toolType: String
    ) {
        if (toolType == "ERASER") {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            canvas.drawCircle(x, y, strokeWidth * 0.8f, paint)
            return
        }

        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        // Outer glow
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb((baseAlpha * 0.32f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 1.2f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, strokeWidth * 1.25f, outerPaint)

        // Inner glow
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb((baseAlpha * 0.70f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 0.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, strokeWidth * 0.7f, innerPaint)

        // Core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb((baseAlpha * 0.95f).toInt(), 255, 255, 255)
        }
        canvas.drawCircle(x, y, strokeWidth * 0.3f, corePaint)
    }

    private fun drawRainbowPath(
        canvas: Canvas,
        points: List<PointF>,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        val hsv = floatArrayOf(0f, 1f, 1f)
        val total = points.size
        for (i in 0 until total - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val hue = (i * 12f) % 360f
            hsv[0] = hue
            val segColor = AndroidColor.HSVToColor(hsv)

            val segPath = Path().apply {
                moveTo(p1.x * scaleX, p1.y * scaleY)
                lineTo(p2.x * scaleX, p2.y * scaleY)
            }
            drawThreePassGlowPath(canvas, segPath, segColor, strokeWidth, alpha)
        }
    }

    private fun drawSparkleStars(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            this.strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }

        // Draw small sparkle cross stars periodically along the stroke
        for (i in points.indices step 4) {
            val p = points[i]
            val cx = p.x * scaleX
            val cy = p.y * scaleY
            val starRadius = strokeWidth * 0.7f

            canvas.drawLine(cx - starRadius, cy, cx + starRadius, cy, starPaint)
            canvas.drawLine(cx, cy - starRadius, cx, cy + starRadius, starPaint)
        }
    }
}
