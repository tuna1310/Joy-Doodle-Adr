package com.doodlestudio.app.domain.canvas.brush

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import com.doodlestudio.app.domain.canvas.StrokeInterpolator
import com.doodlestudio.app.domain.model.PointF

// ─────────────────────────────────────────────────────────────────────────────
// 1. Classic Neon Brush
// ─────────────────────────────────────────────────────────────────────────────
class ClassicNeonBrush : BrushEffect(
    id = "CLASSIC_NEON",
    displayNameEn = "Classic Neon",
    displayNameVi = "Neon cơ bản",
    defaultSize = 14f
) {
    override fun drawLive(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawStroke(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawStroke(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    private fun drawStroke(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            drawDot(canvas, points[0].x * scaleX, points[0].y * scaleY, color, strokeWidth, alpha)
            return
        }

        val segments = StrokeInterpolator.interpolateToSegments(points)
        if (segments.isEmpty()) return

        val path = Path().apply {
            moveTo(segments[0].startX * scaleX, segments[0].startY * scaleY)
            for (seg in segments) {
                quadTo(seg.controlX * scaleX, seg.controlY * scaleY, seg.endX * scaleX, seg.endY * scaleY)
            }
        }
        drawThreePassNeonPath(canvas, path, color, strokeWidth, alpha)
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        drawSampleFlower(canvas, width, height, 0xFFFF2D95.toInt(), 5f) { p, c, w ->
            drawThreePassNeonPath(canvas, p, c, w, 1f)
        }
    }

    private fun drawDot(canvas: Canvas, x: Float, y: Float, color: Int, strokeWidth: Float, alpha: Float) {
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        // Outer glow
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb((baseAlpha * 0.35f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 1.2f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, strokeWidth * 1.25f, outerPaint)

        // Inner glow
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb((baseAlpha * 0.75f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 0.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, strokeWidth * 0.7f, innerPaint)

        // White core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb((baseAlpha * 0.95f).toInt(), 255, 255, 255)
        }
        canvas.drawCircle(x, y, strokeWidth * 0.35f, corePaint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Rainbow Glow Brush
// ─────────────────────────────────────────────────────────────────────────────
class RainbowGlowBrush : BrushEffect(
    id = "RAINBOW_GLOW",
    displayNameEn = "Rainbow Glow",
    displayNameVi = "Neon cầu vồng",
    defaultSize = 14f
) {
    override fun drawLive(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawRainbow(canvas, points, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawRainbow(canvas, points, strokeWidth, alpha, scaleX, scaleY, seed)

    private fun drawRainbow(
        canvas: Canvas,
        points: List<PointF>,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            val hsv = floatArrayOf((seed % 360).toFloat(), 1f, 1f)
            val c = AndroidColor.HSVToColor(hsv)
            val p = ClassicNeonBrush()
            p.drawLive(canvas, points, c, strokeWidth, alpha, scaleX, scaleY, seed)
            return
        }

        val hsv = floatArrayOf(0f, 1f, 1f)
        val total = points.size
        for (i in 0 until total - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val hue = ((seed % 360) + i * 14f) % 360f
            hsv[0] = hue
            val segColor = AndroidColor.HSVToColor(hsv)

            val segPath = Path().apply {
                moveTo(p1.x * scaleX, p1.y * scaleY)
                lineTo(p2.x * scaleX, p2.y * scaleY)
            }
            drawThreePassNeonPath(canvas, segPath, segColor, strokeWidth, alpha)
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        drawSampleFlowerRainbow(canvas, width, height, 5f)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Calligraphy Ribbon Brush
// ─────────────────────────────────────────────────────────────────────────────
class CalligraphyRibbonBrush : BrushEffect(
    id = "CALLIGRAPHY_RIBBON",
    displayNameEn = "Ribbon",
    displayNameVi = "Thư pháp ruy băng",
    defaultSize = 16f
) {
    override fun drawLive(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawRibbon(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawRibbon(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    private fun drawRibbon(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(points[0].x * scaleX, points[0].y * scaleY, strokeWidth * 0.5f, paint)
            return
        }

        val total = points.size
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (i in 0 until total - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            // Taper at start and end of stroke
            val progress = i.toFloat() / total
            val taper = when {
                progress < 0.2f -> (progress / 0.2f).coerceIn(0.25f, 1f)
                progress > 0.8f -> ((1f - progress) / 0.2f).coerceIn(0.25f, 1f)
                else -> 1f
            }
            val currentWidth = strokeWidth * taper

            // Subtle glow under ribbon
            ribbonPaint.strokeWidth = currentWidth * 1.6f
            ribbonPaint.color = AndroidColor.argb((baseAlpha * 0.4f).toInt(), r, g, b)
            ribbonPaint.maskFilter = BlurMaskFilter((currentWidth * 0.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            canvas.drawLine(p1.x * scaleX, p1.y * scaleY, p2.x * scaleX, p2.y * scaleY, ribbonPaint)

            // Sharp ribbon body
            ribbonPaint.strokeWidth = currentWidth
            ribbonPaint.color = AndroidColor.argb(baseAlpha, r, g, b)
            ribbonPaint.maskFilter = null
            canvas.drawLine(p1.x * scaleX, p1.y * scaleY, p2.x * scaleX, p2.y * scaleY, ribbonPaint)
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        // Draw 3 curved calligraphic ribbon leaves/petals in cyan, orange, magenta
        val cx = width * 0.5f
        val cy = height * 0.55f

        fun drawRibbonLeaf(angleDeg: Float, len: Float, col: Int) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val ex = cx + (len * Math.cos(rad)).toFloat()
            val ey = cy + (len * Math.sin(rad)).toFloat()
            val ctrlX = cx + (len * 0.6f * Math.cos(rad - 0.4)).toFloat()
            val ctrlY = cy + (len * 0.6f * Math.sin(rad - 0.4)).toFloat()

            val path = Path().apply {
                moveTo(cx, cy)
                quadTo(ctrlX, ctrlY, ex, ey)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = col
                style = Paint.Style.STROKE
                strokeWidth = 6f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, paint)
        }

        drawRibbonLeaf(-140f, width * 0.35f, 0xFF00E5FF.toInt())
        drawRibbonLeaf(-90f, width * 0.4f, 0xFFFF2D95.toInt())
        drawRibbonLeaf(-40f, width * 0.35f, 0xFFFF9100.toInt())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Double Stroke Brush (Hollow outline)
// ─────────────────────────────────────────────────────────────────────────────
class DoubleStrokeBrush : BrushEffect(
    id = "DOUBLE_STROKE",
    displayNameEn = "Double Line",
    displayNameVi = "Nét đôi",
    defaultSize = 16f
) {
    override fun drawLive(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawDouble(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawDouble(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    private fun drawDouble(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                this.strokeWidth = 3f
            }
            canvas.drawCircle(points[0].x * scaleX, points[0].y * scaleY, strokeWidth * 0.6f, paint)
            return
        }

        val total = points.size
        val offsetDist = strokeWidth * 0.35f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = (strokeWidth * 0.28f).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Draw outer glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth * 1.4f
            maskFilter = BlurMaskFilter((strokeWidth * 0.6f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val centerPath = Path().apply {
            moveTo(points[0].x * scaleX, points[0].y * scaleY)
            for (i in 1 until total) {
                lineTo(points[i].x * scaleX, points[i].y * scaleY)
            }
        }
        canvas.drawPath(centerPath, glowPaint)

        // Draw 2 parallel side lines
        for (i in 0 until total - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dx = (p2.x - p1.x) * scaleX
            val dy = (p2.y - p1.y) * scaleY
            val len = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (len < 0.1f) continue

            val nx = (-dy / len) * offsetDist
            val ny = (dx / len) * offsetDist

            // Line A
            canvas.drawLine(
                p1.x * scaleX + nx, p1.y * scaleY + ny,
                p2.x * scaleX + nx, p2.y * scaleY + ny,
                linePaint
            )
            // Line B
            canvas.drawLine(
                p1.x * scaleX - nx, p1.y * scaleY - ny,
                p2.x * scaleX - nx, p2.y * scaleY - ny,
                linePaint
            )
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        drawSampleFlower(canvas, width, height, 0xFFFF5722.toInt(), 5f) { p, c, _ ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = c
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawPath(p, paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Thin Neon Pen Brush
// ─────────────────────────────────────────────────────────────────────────────
class ThinNeonPenBrush : BrushEffect(
    id = "THIN_NEON_PEN",
    displayNameEn = "Thin Neon",
    displayNameVi = "Neon mảnh",
    defaultSize = 6f
) {
    override fun drawLive(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawThin(canvas, points, color, strokeWidth.coerceAtMost(8f), alpha, scaleX, scaleY)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawThin(canvas, points, color, strokeWidth.coerceAtMost(8f), alpha, scaleX, scaleY)

    private fun drawThin(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.WHITE
            }
            canvas.drawCircle(points[0].x * scaleX, points[0].y * scaleY, strokeWidth * 0.6f, corePaint)
            return
        }

        val segments = StrokeInterpolator.interpolateToSegments(points)
        if (segments.isEmpty()) return

        val path = Path().apply {
            moveTo(segments[0].startX * scaleX, segments[0].startY * scaleY)
            for (seg in segments) {
                quadTo(seg.controlX * scaleX, seg.controlY * scaleY, seg.endX * scaleX, seg.endY * scaleY)
            }
        }

        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        // Crisp tight halo
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 2f
            this.color = AndroidColor.argb((baseAlpha * 0.65f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 0.8f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, haloPaint)

        // Sharp intense core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 0.7f
            this.color = AndroidColor.WHITE
        }
        canvas.drawPath(path, corePaint)
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        drawSampleFlower(canvas, width, height, 0xFF00E5FF.toInt(), 3f) { p, c, w ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = c
                style = Paint.Style.STROKE
                strokeWidth = w
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(p, paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Helper Drawing Functions
// ─────────────────────────────────────────────────────────────────────────────

fun drawThreePassNeonPath(
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
        this.color = AndroidColor.argb((baseAlpha * 0.35f).toInt(), r, g, b)
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
        this.color = AndroidColor.argb((baseAlpha * 0.75f).toInt(), r, g, b)
        maskFilter = BlurMaskFilter(innerRadius, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawPath(path, innerPaint)

    // Pass 3: Electric White-Hot Core
    val coreR = (r * 0.25f + 255 * 0.75f).toInt().coerceIn(0, 255)
    val coreG = (g * 0.25f + 255 * 0.75f).toInt().coerceIn(0, 255)
    val coreB = (b * 0.25f + 255 * 0.75f).toInt().coerceIn(0, 255)

    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.strokeWidth = strokeWidth * 0.55f
        this.color = AndroidColor.argb((baseAlpha * 0.95f).toInt(), coreR, coreG, coreB)
    }
    canvas.drawPath(path, corePaint)
}

/** Draws a sample 5-petal flower with stem onto thumbnail canvas */
fun drawSampleFlower(
    canvas: Canvas,
    width: Int,
    height: Int,
    color: Int,
    strokeWidth: Float,
    drawPathOp: (Path, Int, Float) -> Unit
) {
    val cx = width * 0.5f
    val cy = height * 0.42f
    val r = width * 0.22f

    // 5 petals
    for (i in 0 until 5) {
        val angle = Math.toRadians((i * 72.0) - 90.0)
        val px = cx + (r * Math.cos(angle)).toFloat()
        val py = cy + (r * Math.sin(angle)).toFloat()
        val path = Path().apply {
            addCircle(px, py, width * 0.12f, Path.Direction.CW)
        }
        drawPathOp(path, color, strokeWidth)
    }

    // Stem
    val stemPath = Path().apply {
        moveTo(cx, cy + r)
        lineTo(cx, height * 0.85f)
    }
    drawPathOp(stemPath, 0xFF4CAF50.toInt(), strokeWidth * 0.9f)
}

/** Draws a sample rainbow flower onto thumbnail canvas */
fun drawSampleFlowerRainbow(canvas: Canvas, width: Int, height: Int, strokeWidth: Float) {
    val cx = width * 0.5f
    val cy = height * 0.42f
    val r = width * 0.22f
    val colors = intArrayOf(
        0xFFFF1744.toInt(), 0xFFFF9100.toInt(), 0xFFFFEA00.toInt(),
        0xFF00E676.toInt(), 0xFF00B0FF.toInt()
    )

    for (i in 0 until 5) {
        val angle = Math.toRadians((i * 72.0) - 90.0)
        val px = cx + (r * Math.cos(angle)).toFloat()
        val py = cy + (r * Math.sin(angle)).toFloat()
        val path = Path().apply {
            addCircle(px, py, width * 0.12f, Path.Direction.CW)
        }
        drawThreePassNeonPath(canvas, path, colors[i], strokeWidth, 1f)
    }

    // Stem
    val stemPath = Path().apply {
        moveTo(cx, cy + r)
        lineTo(cx, height * 0.85f)
    }
    drawThreePassNeonPath(canvas, stemPath, 0xFF7C4DFF.toInt(), strokeWidth * 0.9f, 1f)
}

