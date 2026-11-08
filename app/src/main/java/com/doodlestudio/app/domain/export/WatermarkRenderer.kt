package com.doodlestudio.app.domain.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Renders an elegant branding watermark on exported timelapse frames and fallback images.
 */
object WatermarkRenderer {

    data class WatermarkBounds(
        val rect: RectF,
        val textX: Float,
        val textY: Float,
        val fontSize: Float
    )

    fun calculateBounds(
        canvasWidth: Int,
        canvasHeight: Int,
        text: String,
        margin: Float = 20f,
        padding: Float = 10f
    ): WatermarkBounds {
        val fontSize = (canvasWidth * 0.025f).coerceIn(14f, 28f)
        val paint = Paint().apply {
            textSize = fontSize
        }
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)

        val pillWidth = textBounds.width() + padding * 2f
        val pillHeight = textBounds.height() + padding * 1.5f

        val right = canvasWidth - margin
        val bottom = canvasHeight - margin
        val left = right - pillWidth
        val top = bottom - pillHeight

        val pillRect = RectF(left, top, right, bottom)
        val textX = left + padding
        val textY = bottom - padding

        return WatermarkBounds(
            rect = pillRect,
            textX = textX,
            textY = textY,
            fontSize = fontSize
        )
    }

    fun renderWatermark(
        canvas: Canvas,
        canvasWidth: Int,
        canvasHeight: Int,
        text: String = "Created with Doodle Studio"
    ) {
        val bounds = calculateBounds(canvasWidth, canvasHeight, text)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 0, 0, 0) // Semi-transparent black pill
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = bounds.fontSize
            isFakeBoldText = true
        }

        canvas.drawRoundRect(bounds.rect, 8f, 8f, bgPaint)
        canvas.drawText(text, bounds.textX, bounds.textY, textPaint)
    }
}

