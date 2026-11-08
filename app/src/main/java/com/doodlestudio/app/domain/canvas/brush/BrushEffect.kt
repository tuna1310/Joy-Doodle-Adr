package com.doodlestudio.app.domain.canvas.brush

import android.graphics.Canvas
import com.doodlestudio.app.domain.model.PointF

/**
 * Base abstract class for all 21 brushes in DoodleStudio.
 * Each brush encapsulates its visual rendering algorithm, color dynamics,
 * seed-based particle generation, and preview thumbnail generation.
 */
abstract class BrushEffect(
    val id: String,
    val displayNameEn: String,
    val displayNameVi: String,
    val defaultSize: Float = 14f,
    val hasDynamicAnimation: Boolean = false
) {
    /**
     * Draws the live in-progress stroke while finger is on screen.
     */
    abstract fun drawLive(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    )

    /**
     * Draws the completed stroke into the persistent offscreen Bitmap cache.
     * Must be deterministic for a given (points, color, strokeWidth, seed)
     * so that replay and export match interactive drawing exactly.
     */
    abstract fun drawCommitted(
        canvas: Canvas,
        points: List<PointF>,
        color: Int,
        strokeWidth: Float,
        alpha: Float,
        scaleX: Float,
        scaleY: Float,
        seed: Long
    )

    /**
     * Draws a preview icon (e.g., stylized flower or signature sample)
     * onto a thumbnail canvas of size [width] x [height].
     */
    abstract fun drawPreview(
        canvas: Canvas,
        width: Int,
        height: Int
    )
}

