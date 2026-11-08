package com.doodlestudio.app.domain.canvas.brush

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import com.doodlestudio.app.domain.canvas.StrokeInterpolator
import com.doodlestudio.app.domain.model.PointF

// ─────────────────────────────────────────────────────────────────────────────
// 6. Crayon / Chalk Spray Brush
// ─────────────────────────────────────────────────────────────────────────────
class CrayonChalkBrush : BrushEffect(
    id = "CRAYON_CHALK",
    displayNameEn = "Crayon",
    displayNameVi = "Phấn sáp",
    defaultSize = 18f
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
    ) = drawCrayon(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawCrayon(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    private fun drawCrayon(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) {
        if (points.isEmpty()) return
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        if (points.size == 1) {
            val p = points[0]
            val x = p.x * scaleX
            val y = p.y * scaleY
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.argb((baseAlpha * 0.85f).toInt(), r, g, b)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(x, y, strokeWidth * 0.5f, dotPaint)
            var rnd = (seed xor 0x5DEECE66DL) and 0xFFFFFFFFFFFFL
            val speckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.argb((baseAlpha * 0.9f).toInt(), 255, 255, 255)
                style = Paint.Style.FILL
            }
            for (k in 0 until 6) {
                rnd = (rnd * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
                val offAngle = ((rnd ushr 16) % 360).toFloat() * 0.017453292f
                val offDist = (((rnd ushr 32) % 100) / 100f) * strokeWidth * 0.7f
                canvas.drawCircle(
                    x + (Math.cos(offAngle.toDouble()) * offDist).toFloat(),
                    y + (Math.sin(offAngle.toDouble()) * offDist).toFloat(),
                    1.5f,
                    speckPaint
                )
            }
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

        // Layer 1: Soft chalk halo / powdery dispersion
        val powderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 1.35f
            this.color = AndroidColor.argb((baseAlpha * 0.35f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 0.45f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, powderPaint)

        // Layer 2: Main textured chalk core stroke
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 0.85f
            this.color = AndroidColor.argb((baseAlpha * 0.85f).toInt(), r, g, b)
        }
        canvas.drawPath(path, corePaint)

        // Layer 3: Grainy chalk specks along segments for authentic chalkboard/pastel grit
        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = AndroidColor.argb((baseAlpha * 0.75f).toInt(), 255, 255, 255)
        }
        var rnd = (seed xor 0x5DEECE66DL) and 0xFFFFFFFFFFFFL
        val maxSpecksPerSeg = 3
        for (seg in segments) {
            for (step in 1..maxSpecksPerSeg) {
                val t = step / (maxSpecksPerSeg + 1f)
                val pt = StrokeInterpolator.sampleQuad(seg, t)
                rnd = (rnd * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
                val off = (((rnd ushr 16) % 100) / 100f - 0.5f) * strokeWidth * 0.8f
                val rad = (((rnd ushr 32) % 3) + 1).toFloat() * 0.8f
                canvas.drawCircle(pt.x * scaleX + off, pt.y * scaleY + off, rad, grainPaint)
            }
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.45f
        val r = width * 0.22f
        val strokeW = 8f
        val color = 0xFFFFB300.toInt()
        val powderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW * 1.3f
            this.color = 0x66FFB300.toInt()
            maskFilter = BlurMaskFilter(3.5f, BlurMaskFilter.Blur.NORMAL)
        }
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW * 0.85f
            this.color = color
        }
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72.0) - 90.0)
            val px = cx + (r * Math.cos(angle)).toFloat()
            val py = cy + (r * Math.sin(angle)).toFloat()
            val path = Path().apply { addCircle(px, py, width * 0.12f, Path.Direction.CW) }
            canvas.drawPath(path, powderPaint)
            canvas.drawPath(path, corePaint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Solid / Flat Marker Brush
// ─────────────────────────────────────────────────────────────────────────────
class SolidMarkerBrush : BrushEffect(
    id = "SOLID_MARKER",
    displayNameEn = "Solid Marker",
    displayNameVi = "Bút dạ khối",
    defaultSize = 22f
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
    ) = drawSolid(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawSolid(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    private fun drawSolid(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) {
        if (points.isEmpty()) return
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        if (points.size == 1) {
            val p = points[0]
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.argb(baseAlpha, r, g, b)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.5f, paint)
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

        // Bold solid stroke with rounded caps and joins
        val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth
            this.color = AndroidColor.argb(baseAlpha, r, g, b)
        }
        canvas.drawPath(path, solidPaint)

        // Subtle specular highlight line along center for marker ink gloss
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = (strokeWidth * 0.18f).coerceAtLeast(1.5f)
            this.color = AndroidColor.argb((baseAlpha * 0.35f).toInt(), 255, 255, 255)
        }
        canvas.drawPath(path, highlightPaint)
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.45f
        val r = width * 0.24f
        val colors = intArrayOf(
            0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
            0xFFE53935.toInt(), 0xFFFB8C00.toInt()
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72.0) - 90.0)
            val px = cx + (r * Math.cos(angle)).toFloat()
            val py = cy + (r * Math.sin(angle)).toFloat()
            paint.color = colors[i]
            canvas.drawCircle(px, py, width * 0.16f, paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Dotted Pearl / Glow Beads Brush
// ─────────────────────────────────────────────────────────────────────────────
class DottedPearlBrush : BrushEffect(
    id = "DOTTED_PEARL",
    displayNameEn = "Dotted Pearl",
    displayNameVi = "Chuỗi hạt sáng",
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
    ) = drawPearls(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawPearls(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    private fun drawPearls(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) {
        if (points.isEmpty()) return
        val radius = strokeWidth * 0.55f
        val stepDist = (radius * 2.1f).coerceAtLeast(14f)
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        val jewelColors = intArrayOf(
            0xFFFF1744.toInt(), // Ruby
            0xFFFF9100.toInt(), // Topaz
            0xFFFFEA00.toInt(), // Amber
            0xFF00E676.toInt(), // Emerald
            0xFF00B0FF.toInt(), // Sapphire
            0xFFD500F9.toInt()  // Amethyst
        )

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter((radius * 0.8f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = AndroidColor.argb((baseAlpha * 0.95f).toInt(), 255, 255, 255)
        }

        fun drawSinglePearl(x: Float, y: Float, idx: Int) {
            val pearlColor = jewelColors[(Math.abs(seed.toInt() + idx)) % jewelColors.size]
            val pr = AndroidColor.red(pearlColor)
            val pg = AndroidColor.green(pearlColor)
            val pb = AndroidColor.blue(pearlColor)

            // Halo
            glowPaint.color = AndroidColor.argb((baseAlpha * 0.45f).toInt(), pr, pg, pb)
            canvas.drawCircle(x, y, radius * 1.35f, glowPaint)

            // Pearl body
            bodyPaint.color = AndroidColor.argb(baseAlpha, pr, pg, pb)
            canvas.drawCircle(x, y, radius, bodyPaint)

            // Pearl gloss highlight (offset towards upper-left)
            val specR = radius * 0.32f
            canvas.drawCircle(x - radius * 0.28f, y - radius * 0.28f, specR, specularPaint)
        }

        if (points.size == 1) {
            drawSinglePearl(points[0].x * scaleX, points[0].y * scaleY, 0)
            return
        }

        var beadIdx = 0
        var prevX = points[0].x * scaleX
        var prevY = points[0].y * scaleY
        var distAcc = 0f

        drawSinglePearl(prevX, prevY, beadIdx++)

        for (i in 1 until points.size) {
            val curX = points[i].x * scaleX
            val curY = points[i].y * scaleY
            val dx = curX - prevX
            val dy = curY - prevY
            val segLen = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (segLen <= 0.001f) continue

            var segRemaining = segLen
            var fromX = prevX
            var fromY = prevY

            while (distAcc + segRemaining >= stepDist) {
                val need = stepDist - distAcc
                val t = need / segRemaining
                val beadX = fromX + (curX - fromX) * t
                val beadY = fromY + (curY - fromY) * t
                drawSinglePearl(beadX, beadY, beadIdx++)

                fromX = beadX
                fromY = beadY
                segRemaining -= need
                distAcc = 0f
            }
            distAcc += segRemaining
            prevX = curX
            prevY = curY
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.46f
        val r = width * 0.25f
        val jewelColors = intArrayOf(
            0xFFFF1744.toInt(), 0xFFFF9100.toInt(), 0xFFFFEA00.toInt(),
            0xFF00E676.toInt(), 0xFF00B0FF.toInt(), 0xFFD500F9.toInt()
        )
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.WHITE
        }

        val totalBeads = 12
        for (i in 0 until totalBeads) {
            val angle = Math.toRadians((i * (360.0 / totalBeads)) - 90.0)
            val px = cx + (r * Math.cos(angle)).toFloat()
            val py = cy + (r * Math.sin(angle)).toFloat()
            bodyPaint.color = jewelColors[i % jewelColors.size]
            canvas.drawCircle(px, py, 5.5f, bodyPaint)
            canvas.drawCircle(px - 1.5f, py - 1.5f, 2f, specPaint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. Soft Blur Shadow Brush
// ─────────────────────────────────────────────────────────────────────────────
class SoftBlurBrush : BrushEffect(
    id = "SOFT_BLUR",
    displayNameEn = "Soft Blur",
    displayNameVi = "Neon bóng mờ",
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
    ) = drawBlur(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawBlur(canvas, points, color, strokeWidth, alpha, scaleX, scaleY)

    private fun drawBlur(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        if (points.isEmpty()) return
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        if (points.size == 1) {
            val p = points[0]
            val x = p.x * scaleX
            val y = p.y * scaleY
            val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.argb((baseAlpha * 0.25f).toInt(), r, g, b)
                maskFilter = BlurMaskFilter((strokeWidth * 1.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(x, y, strokeWidth * 1.8f, paint1)
            val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.argb((baseAlpha * 0.65f).toInt(), r, g, b)
                maskFilter = BlurMaskFilter((strokeWidth * 0.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(x, y, strokeWidth * 0.9f, paint2)
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

        // Pass 1: Dreamy outer diffused glow
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 3.2f
            this.color = AndroidColor.argb((baseAlpha * 0.22f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 1.4f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, outerPaint)

        // Pass 2: Mid glowing body
        val midPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 1.7f
            this.color = AndroidColor.argb((baseAlpha * 0.5f).toInt(), r, g, b)
            maskFilter = BlurMaskFilter((strokeWidth * 0.7f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, midPaint)

        // Pass 3: Soft core line
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * 0.75f
            this.color = AndroidColor.argb((baseAlpha * 0.85f).toInt(), r, g, b)
        }
        canvas.drawPath(path, corePaint)
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        drawSampleFlower(canvas, width, height, 0xFFE040FB.toInt(), 8f) { p, c, w ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = c
                style = Paint.Style.STROKE
                strokeWidth = w
                maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(p, paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. Concentric Rings Brush
// ─────────────────────────────────────────────────────────────────────────────
class ConcentricRingsBrush : BrushEffect(
    id = "CONCENTRIC_RINGS",
    displayNameEn = "Rings",
    displayNameVi = "Vòng tròn đồng tâm",
    defaultSize = 20f
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
    ) = drawRings(canvas, points, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) = drawRings(canvas, points, strokeWidth, alpha, scaleX, scaleY, seed)

    private fun drawRings(
        canvas: Canvas,
        points: List<PointF>,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    ) {
        if (points.isEmpty()) return
        val baseAlpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val stepDist = (strokeWidth * 2.2f).coerceAtLeast(28f)

        val ringColors = intArrayOf(
            0xFFFFEA00.toInt(), // Neon Yellow (core)
            0xFF00E5FF.toInt(), // Neon Cyan
            0xFFFF2D95.toInt(), // Neon Pink
            0xFF7C4DFF.toInt()  // Neon Violet
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 2.4f
        }

        fun drawRingSet(x: Float, y: Float, stampIdx: Int) {
            val shift = Math.abs(seed.toInt() + stampIdx) % ringColors.size

            // Ring 1 (Center dot)
            val c1 = ringColors[shift]
            fillPaint.color = AndroidColor.argb(baseAlpha, AndroidColor.red(c1), AndroidColor.green(c1), AndroidColor.blue(c1))
            canvas.drawCircle(x, y, strokeWidth * 0.22f, fillPaint)

            // Ring 2
            val c2 = ringColors[(shift + 1) % ringColors.size]
            strokePaint.color = AndroidColor.argb(baseAlpha, AndroidColor.red(c2), AndroidColor.green(c2), AndroidColor.blue(c2))
            canvas.drawCircle(x, y, strokeWidth * 0.52f, strokePaint)

            // Ring 3
            val c3 = ringColors[(shift + 2) % ringColors.size]
            strokePaint.color = AndroidColor.argb(baseAlpha, AndroidColor.red(c3), AndroidColor.green(c3), AndroidColor.blue(c3))
            canvas.drawCircle(x, y, strokeWidth * 0.86f, strokePaint)

            // Ring 4 (Outermost)
            val c4 = ringColors[(shift + 3) % ringColors.size]
            strokePaint.color = AndroidColor.argb((baseAlpha * 0.85f).toInt(), AndroidColor.red(c4), AndroidColor.green(c4), AndroidColor.blue(c4))
            canvas.drawCircle(x, y, strokeWidth * 1.22f, strokePaint)
        }

        if (points.size == 1) {
            drawRingSet(points[0].x * scaleX, points[0].y * scaleY, 0)
            return
        }

        var stampIdx = 0
        var prevX = points[0].x * scaleX
        var prevY = points[0].y * scaleY
        var distAcc = 0f

        drawRingSet(prevX, prevY, stampIdx++)

        for (i in 1 until points.size) {
            val curX = points[i].x * scaleX
            val curY = points[i].y * scaleY
            val dx = curX - prevX
            val dy = curY - prevY
            val segLen = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (segLen <= 0.001f) continue

            var segRemaining = segLen
            var fromX = prevX
            var fromY = prevY

            while (distAcc + segRemaining >= stepDist) {
                val need = stepDist - distAcc
                val t = need / segRemaining
                val ringX = fromX + (curX - fromX) * t
                val ringY = fromY + (curY - fromY) * t
                drawRingSet(ringX, ringY, stampIdx++)

                fromX = ringX
                fromY = ringY
                segRemaining -= need
                distAcc = 0f
            }
            distAcc += segRemaining
            prevX = curX
            prevY = curY
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val colors = intArrayOf(0xFFFFEA00.toInt(), 0xFF00E5FF.toInt(), 0xFFFF2D95.toInt(), 0xFF7C4DFF.toInt())
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colors[0]
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawCircle(cx, cy, width * 0.08f, fillPaint)
        for (r in 1..3) {
            strokePaint.color = colors[r % colors.size]
            canvas.drawCircle(cx, cy, r * (width * 0.12f) + width * 0.08f, strokePaint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. Sparkler Neon Brush
// ─────────────────────────────────────────────────────────────────────────────
class SparklerBrush : BrushEffect(
    id = "SPARKLER",
    displayNameEn = "Sparkler",
    displayNameVi = "Pháo bông que",
    defaultSize = 14f
) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawSparkler(canvas, points, strokeWidth, scaleX, scaleY, seed)

    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawSparkler(canvas, points, strokeWidth, scaleX, scaleY, seed)

    private fun drawSparkler(canvas: Canvas, points: List<PointF>, strokeWidth: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val neon = ClassicNeonBrush()
        neon.drawLive(canvas, points, 0xFFFF9100.toInt(), strokeWidth, 1f, scaleX, scaleY, seed)
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF176.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 1.8f
        }
        for (i in points.indices step 3) {
            val p = points[i]
            val x = p.x * scaleX
            val y = p.y * scaleY
            val len = strokeWidth * 1.5f
            canvas.drawLine(x - len, y - len, x + len, y + len, sparkPaint)
            canvas.drawLine(x - len, y + len, x + len, y - len, sparkPaint)
        }
    }

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        // Draw a sparkler heart
        val heartPath = Path().apply {
            val cx = width * 0.5f
            val cy = height * 0.45f
            val s = width * 0.035f
            moveTo(cx, cy + 8 * s)
            cubicTo(cx - 15 * s, cy - 5 * s, cx - 15 * s, cy - 18 * s, cx, cy - 10 * s)
            cubicTo(cx + 15 * s, cy - 18 * s, cx + 15 * s, cy - 5 * s, cx, cy + 8 * s)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF4081.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(heartPath, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12. Fireworks Brush
// ─────────────────────────────────────────────────────────────────────────────
class FireworksBrush : BrushEffect("FIREWORKS", "Fireworks", "Pháo hoa", 16f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFFD700.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 2f
        }
        for (i in points.indices step 6) {
            val p = points[i]
            val x = p.x * scaleX
            val y = p.y * scaleY
            for (deg in 0 until 360 step 45) {
                val rad = Math.toRadians(deg.toDouble())
                val ex = x + (strokeWidth * 1.8f * Math.cos(rad)).toFloat()
                val ey = y + (strokeWidth * 1.8f * Math.sin(rad)).toFloat()
                canvas.drawLine(x, y, ex, ey, paint)
            }
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD54F.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        for (deg in 0 until 360 step 30) {
            val rad = Math.toRadians(deg.toDouble())
            canvas.drawLine(cx, cy, cx + (width * 0.35f * Math.cos(rad)).toFloat(), cy + (width * 0.35f * Math.sin(rad)).toFloat(), paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 13. Neon Stars Brush
// ─────────────────────────────────────────────────────────────────────────────
class NeonStarsBrush : BrushEffect("NEON_STARS", "Stars", "Ngôi sao neon", 16f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFF4081.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 2.5f
            maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        }
        for (i in points.indices step 5) {
            val p = points[i]
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.8f, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF4081.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.28f, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 14. Cross Sparkles Brush
// ─────────────────────────────────────────────────────────────────────────────
class CrossSparklesBrush : BrushEffect("CROSS_SPARKLES", "Sparkles", "Kim cương lấp lánh", 14f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            this.strokeWidth = 2f
        }
        for (i in points.indices step 4) {
            val p = points[i]
            val x = p.x * scaleX
            val y = p.y * scaleY
            val len = strokeWidth * 1.2f
            canvas.drawLine(x - len, y, x + len, y, paint)
            canvas.drawLine(x, y - len, x, y + len, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE1BEE7.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val len = width * 0.35f
        canvas.drawLine(cx - len, cy, cx + len, cy, paint)
        canvas.drawLine(cx, cy - len, cx, cy + len, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 15. Bokeh Dots Brush
// ─────────────────────────────────────────────────────────────────────────────
class BokehDotsBrush : BrushEffect("BOKEH_DOTS", "Bokeh", "Đốm sáng bokeh", 18f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0x88BA68C8.toInt()
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(strokeWidth * 0.5f, BlurMaskFilter.Blur.NORMAL)
        }
        for (i in points.indices step 4) {
            val p = points[i]
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.9f, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAAAB47BC.toInt()
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.28f, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 16. Big Cross Star Brush
// ─────────────────────────────────────────────────────────────────────────────
class BigCrossStarBrush : BrushEffect("BIG_CROSS_STAR", "Cross Star", "Sao chữ thập", 16f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFF1744.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 2.5f
        }
        for (i in points.indices step 5) {
            val p = points[i]
            val x = p.x * scaleX
            val y = p.y * scaleY
            val len = strokeWidth * 2f
            canvas.drawLine(x - len, y, x + len, y, paint)
            canvas.drawLine(x, y - len, x, y + len, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF5252.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawLine(cx - width * 0.35f, cy, cx + width * 0.35f, cy, paint)
        canvas.drawLine(cx, cy - height * 0.35f, cx, cy + height * 0.35f, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 17. Purple Sparkles Brush
// ─────────────────────────────────────────────────────────────────────────────
class PurpleSparklesBrush : BrushEffect("PURPLE_SPARKLES", "Purple Sparkle", "Lấp lánh tím", 14f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFE040FB.toInt()
            style = Paint.Style.FILL
        }
        for (i in points.indices step 3) {
            val p = points[i]
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.4f, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEA80FC.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.18f, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 18. Fairy Dust Brush
// ─────────────────────────────────────────────────────────────────────────────
class FairyDustBrush : BrushEffect("FAIRY_DUST", "Fairy Dust", "Dải bụi tiên", 14f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFF40C4FF.toInt()
            style = Paint.Style.FILL
        }
        for (i in points.indices step 2) {
            val p = points[i]
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.3f, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF40C4FF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.15f, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 19. Snowflakes Brush
// ─────────────────────────────────────────────────────────────────────────────
class SnowflakesBrush : BrushEffect("SNOWFLAKES", "Snowflakes", "Hoa tuyết", 20f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFB3E5FC.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 2f
        }
        for (i in points.indices step 6) {
            val p = points[i]
            val cx = p.x * scaleX
            val cy = p.y * scaleY
            val len = strokeWidth * 0.9f
            for (deg in 0 until 360 step 60) {
                val rad = Math.toRadians(deg.toDouble())
                canvas.drawLine(cx, cy, cx + (len * Math.cos(rad)).toFloat(), cy + (len * Math.sin(rad)).toFloat(), paint)
            }
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF81D4FA.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val len = width * 0.32f
        for (deg in 0 until 360 step 60) {
            val rad = Math.toRadians(deg.toDouble())
            canvas.drawLine(cx, cy, cx + (len * Math.cos(rad)).toFloat(), cy + (len * Math.sin(rad)).toFloat(), paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 20. Heart Bubbles Brush
// ─────────────────────────────────────────────────────────────────────────────
class HeartBubblesBrush : BrushEffect("HEART_BUBBLES", "Heart Bubbles", "Bong bóng tim", 22f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFF5252.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 2.5f
        }
        for (i in points.indices step 5) {
            val p = points[i]
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.6f, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF1744.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.25f, paint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 21. Soap Bubbles Brush
// ─────────────────────────────────────────────────────────────────────────────
class SoapBubblesBrush : BrushEffect("SOAP_BUBBLES", "Soap Bubbles", "Bong bóng nước", 22f) {
    override fun drawLive(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFF80D8FF.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 2.5f
        }
        for (i in points.indices step 5) {
            val p = points[i]
            canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth * 0.7f, paint)
        }
    }
    override fun drawCommitted(canvas: Canvas, points: List<PointF>, color: Int, strokeWidth: Float, alpha: Float, scaleX: Float, scaleY: Float, seed: Long) =
        drawLive(canvas, points, color, strokeWidth, alpha, scaleX, scaleY, seed)

    override fun drawPreview(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF40C4FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.28f, paint)
    }
}

