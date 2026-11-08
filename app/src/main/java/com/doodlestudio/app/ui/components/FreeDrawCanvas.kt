package com.doodlestudio.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.doodlestudio.app.domain.canvas.BrushConfig
import com.doodlestudio.app.domain.canvas.BrushTool
import com.doodlestudio.app.domain.canvas.brush.BrushRegistry
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF

/**
 * FreeDrawCanvas renders neon glow strokes with a hardware-accelerated
 * offscreen Bitmap cache.
 *
 * Committed strokes are cached into a Bitmap layer to eliminate re-blurring overhead
 * and guarantee 60+ FPS on mid-range devices. Active touch strokes are rendered live.
 *
 * KEY FIX: Cache sync is now SYNCHRONOUS inside the Canvas draw block.
 * This eliminates the 1-2 frame gap where activePoints is cleared but the cache
 * bitmap hasn't been updated yet, which previously caused strokes to disappear
 * briefly after lifting the finger.
 */
@Composable
fun FreeDrawCanvas(
    actions: List<EditAction>,
    activePoints: List<PointF>,
    brushConfig: BrushConfig,
    strokeRevision: Int,
    activeSeed: Long = 0L,
    backgroundColor: Color = Color.Black,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 1920,
    fillBitmap: Bitmap? = null,
    fillBitmapRevision: Int = 0,
    templateBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val validWidth = canvasWidth.coerceAtLeast(1)
    val validHeight = canvasHeight.coerceAtLeast(1)

    // Persistent offscreen cache Bitmap and Canvas – recreated only when size changes
    val cacheBitmap = remember(validWidth, validHeight) {
        Bitmap.createBitmap(validWidth, validHeight, Bitmap.Config.ARGB_8888)
    }
    val cacheCanvas = remember(cacheBitmap) {
        AndroidCanvas(cacheBitmap)
    }

    // Mutable box so the draw lambda can read and write without triggering recomposition
    val renderedActionCount = remember(cacheBitmap) { intArrayOf(0) }

    DisposableEffect(cacheBitmap) {
        onDispose {
            if (!cacheBitmap.isRecycled) {
                cacheBitmap.recycle()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Reading strokeRevision and fillBitmapRevision forces redraw on changes
        @Suppress("UNUSED_VARIABLE")
        val observedStrokeRev = strokeRevision
        @Suppress("UNUSED_VARIABLE")
        val observedFillRev = fillBitmapRevision

        val scaleX = if (validWidth > 0) size.width / validWidth else 1f
        val scaleY = if (validHeight > 0) size.height / validHeight else 1f

        // ── SYNCHRONOUS cache update ────────────────────────────────────────
        val currentCount = renderedActionCount[0]
        if (actions.isEmpty()) {
            cacheBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            renderedActionCount[0] = 0
        } else if (actions.size < currentCount) {
            cacheBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            for (action in actions) {
                renderAction(cacheCanvas, action, fillBitmap, validWidth, validHeight)
            }
            renderedActionCount[0] = actions.size
        } else if (actions.size > currentCount) {
            for (i in currentCount until actions.size) {
                renderAction(cacheCanvas, actions[i], fillBitmap, validWidth, validHeight)
            }
            renderedActionCount[0] = actions.size
        }
        // ───────────────────────────────────────────────────────────────────

        // 1. Draw solid background
        drawRect(color = backgroundColor)

        // 2. Draw Region Fill Layer (under the template outlines)
        if (fillBitmap != null && !fillBitmap.isRecycled) {
            drawImage(
                image = fillBitmap.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }

        // 3. Draw cached committed freehand strokes
        if (!cacheBitmap.isRecycled) {
            drawImage(
                image = cacheBitmap.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }

        // 4. Draw the live in-progress stroke (finger still on screen)
        if (activePoints.isNotEmpty()) {
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                val toolType = if (brushConfig.tool == BrushTool.ERASER) "ERASER" else brushConfig.tool.name
                val colorInt = if (brushConfig.tool == BrushTool.ERASER) {
                    backgroundColor.toArgb()
                } else {
                    brushConfig.color.toArgb()
                }

                if (brushConfig.tool == BrushTool.ERASER) {
                    NeonBrushRenderer.drawNeonStroke(
                        canvas = nativeCanvas,
                        points = activePoints,
                        color = android.graphics.Color.TRANSPARENT,
                        strokeWidth = brushConfig.size * 1.6f,
                        alpha = 1.0f,
                        toolType = "ERASER",
                        scaleX = scaleX,
                        scaleY = scaleY
                    )
                } else {
                    BrushRegistry.get(toolType).drawLive(
                        canvas = nativeCanvas,
                        points = activePoints,
                        color = colorInt,
                        strokeWidth = brushConfig.size * ((scaleX + scaleY) / 2f),
                        alpha = brushConfig.alpha,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        seed = activeSeed
                    )
                }
            }
        }

        // 5. Draw Template Outline Layer ON TOP so lines remain crisp and visible
        if (templateBitmap != null && !templateBitmap.isRecycled) {
            drawImage(
                image = templateBitmap.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }
    }
}

private fun renderAction(
    canvas: AndroidCanvas,
    action: EditAction,
    fillBitmap: Bitmap? = null,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 1920
) {
    when (action) {
        is EditAction.Stroke -> {
            BrushRegistry.get(action.toolType).drawCommitted(
                canvas = canvas,
                points = action.points,
                color = action.color,
                strokeWidth = action.strokeWidth,
                alpha = action.alpha,
                scaleX = 1f,
                scaleY = 1f,
                seed = action.seed
            )
        }
        is EditAction.Erase -> {
            NeonBrushRenderer.drawNeonStroke(
                canvas = canvas,
                points = action.points,
                color = android.graphics.Color.TRANSPARENT,
                strokeWidth = action.strokeWidth,
                alpha = 1.0f,
                toolType = "ERASER",
                scaleX = 1f,
                scaleY = 1f
            )
            // Also erase on fill layer if a template or fill is present
            if (fillBitmap != null && !fillBitmap.isRecycled) {
                val fScaleX = fillBitmap.width.toFloat() / canvasWidth.coerceAtLeast(1)
                val fScaleY = fillBitmap.height.toFloat() / canvasHeight.coerceAtLeast(1)
                NeonBrushRenderer.drawNeonStroke(
                    canvas = AndroidCanvas(fillBitmap),
                    points = action.points,
                    color = android.graphics.Color.TRANSPARENT,
                    strokeWidth = action.strokeWidth * ((fScaleX + fScaleY) / 2f),
                    alpha = 1.0f,
                    toolType = "ERASER",
                    scaleX = fScaleX,
                    scaleY = fScaleY
                )
            }
        }
        is EditAction.Fill -> {
            // Handled when fill mode is used
        }
    }
}
