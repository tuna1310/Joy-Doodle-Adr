package com.doodlestudio.app.domain.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.doodlestudio.app.domain.canvas.ActionLogPlayer
import com.doodlestudio.app.domain.canvas.brush.BrushRegistry
import com.doodlestudio.app.domain.canvas.StrokeInterpolator
import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.ui.components.NeonBrushRenderer

/**
 * Pure frame rendering engine for Timelapse generation.
 * Takes ActionLog and renders individual chronological frames with high fidelity.
 */
class TimelapseFrameRenderer(
    val actionLog: ActionLog,
    private val templateBitmap: Bitmap? = null,
    val config: ExportConfig = ExportConfig()
) {
    private val player = ActionLogPlayer(actionLog)

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * Renders frame [frameIndex] in [0 until config.totalFrames] onto [targetBitmap].
     */
    fun renderFrame(frameIndex: Int, targetBitmap: Bitmap) {
        val canvas = Canvas(targetBitmap)
        val actions = player.getActionsForTimelapseFrame(frameIndex, config.totalFrames)

        val targetW = targetBitmap.width
        val targetH = targetBitmap.height
        val scaleX = targetW.toFloat() / actionLog.canvasWidth
        val scaleY = targetH.toFloat() / actionLog.canvasHeight

        // 1. Background
        canvas.drawColor(actionLog.backgroundColor)

        // 2. Separate Fills and Strokes
        for (action in actions) {
            when (action) {
                is EditAction.Fill -> {
                    // Fill actions could be rendered onto a shared fill layer or region mask
                    // For timelapse, if normalized coordinates are present:
                }
                is EditAction.Stroke -> {
                    renderStroke(canvas, action, scaleX, scaleY)
                }
                is EditAction.Erase -> {
                    renderErase(canvas, action, scaleX, scaleY)
                }
            }
        }

        // 3. Template Line-art on top if available
        if (templateBitmap != null) {
            val srcRect = android.graphics.Rect(0, 0, templateBitmap.width, templateBitmap.height)
            val dstRect = android.graphics.Rect(0, 0, targetW, targetH)
            canvas.drawBitmap(templateBitmap, srcRect, dstRect, null)
        }

        // 4. Watermark branding
        if (config.includeWatermark) {
            WatermarkRenderer.renderWatermark(
                canvas = canvas,
                canvasWidth = targetW,
                canvasHeight = targetH,
                text = config.watermarkText
            )
        }
    }

    private fun renderStroke(canvas: Canvas, stroke: EditAction.Stroke, scaleX: Float, scaleY: Float) {
        if (stroke.points.isEmpty()) return
        BrushRegistry.get(stroke.toolType).drawCommitted(
            canvas = canvas,
            points = stroke.points,
            color = stroke.color,
            strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f),
            alpha = stroke.alpha,
            scaleX = scaleX,
            scaleY = scaleY,
            seed = stroke.seed
        )
    }

    private fun renderErase(canvas: Canvas, erase: EditAction.Erase, scaleX: Float, scaleY: Float) {
        if (erase.points.isEmpty()) return
        NeonBrushRenderer.drawNeonStroke(
            canvas = canvas,
            points = erase.points,
            color = android.graphics.Color.TRANSPARENT,
            strokeWidth = erase.strokeWidth * ((scaleX + scaleY) / 2f),
            alpha = 1.0f,
            toolType = "ERASER",
            scaleX = scaleX,
            scaleY = scaleY
        )
    }
}

