package com.doodlestudio.app.domain.canvas.brush

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * In-memory cache for brush preview icons.
 * Pre-renders each brush's preview thumbnail on a pure black background once.
 */
object BrushPreviewCache {
    private val previewBitmaps = mutableMapOf<String, ImageBitmap>()

    fun getPreview(brush: BrushEffect, width: Int = 120, height: Int = 120): ImageBitmap {
        return previewBitmaps.getOrPut(brush.id) {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            // Solid dark background for preview cards
            canvas.drawColor(AndroidColor.BLACK)
            brush.drawPreview(canvas, width, height)
            bmp.asImageBitmap()
        }
    }
}

