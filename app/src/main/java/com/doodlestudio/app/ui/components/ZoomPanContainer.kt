package com.doodlestudio.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Zoom and Pan gesture container with coordinate translation.
 * Maps screen touch coordinates back to original Canvas coordinates (0..canvasSize).
 *
 * Rules:
 * - 1 finger touch is ALWAYS drawing (zero touch slop, immediate stroke start and dot support).
 * - Zoom & pan is ONLY triggered when 2 or more fingers are down.
 * - Single-finger events are NEVER swallowed or consumed by zoom/pan.
 */
@Composable
fun ZoomPanContainer(
    canvasWidth: Int,
    canvasHeight: Int,
    isDrawMode: Boolean = true,
    onCanvasTap: (canvasX: Int, canvasY: Int) -> Unit = { _, _ -> },
    onStrokeStart: (canvasX: Float, canvasY: Float) -> Unit = { _, _ -> },
    onStrokeMove: (canvasX: Float, canvasY: Float) -> Unit = { _, _ -> },
    onStrokeEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
    minScale: Float = 1.0f,
    maxScale: Float = 5.0f,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val currentStrokeStart by rememberUpdatedState(onStrokeStart)
    val currentStrokeMove by rememberUpdatedState(onStrokeMove)
    val currentStrokeEnd by rememberUpdatedState(onStrokeEnd)
    val currentCanvasTap by rememberUpdatedState(onCanvasTap)

    fun screenToCanvas(screenX: Float, screenY: Float): Pair<Float, Float> {
        if (containerSize.width <= 0 || containerSize.height <= 0) return 0f to 0f
        val cx = containerSize.width / 2f
        val cy = containerSize.height / 2f
        val localX = (screenX - cx - offset.x) / scale + cx
        val localY = (screenY - cy - offset.y) / scale + cy
        val canvasX = (localX / containerSize.width * canvasWidth).coerceIn(0f, canvasWidth.toFloat())
        val canvasY = (localY / containerSize.height * canvasHeight).coerceIn(0f, canvasHeight.toFloat())
        return canvasX to canvasY
    }

    val gestureModifier = Modifier.pointerInput(containerSize, isDrawMode) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()

            var isMultiTouch = false
            var activePointerId = down.id

            if (isDrawMode) {
                val (cx, cy) = screenToCanvas(down.position.x, down.position.y)
                currentStrokeStart(cx, cy)
            }

            while (true) {
                val event = awaitPointerEvent()
                val pointerCount = event.changes.count { it.pressed }

                if (pointerCount >= 2) {
                    // Two fingers active: perform zoom / pan
                    if (!isMultiTouch && isDrawMode) {
                        // Cancel the drawing stroke cleanly
                        currentStrokeEnd()
                    }
                    isMultiTouch = true

                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()

                    scale = (scale * zoom).coerceIn(minScale, maxScale)
                    val maxOffsetX = (containerSize.width * (scale - 1) / 2).coerceAtLeast(0f)
                    val maxOffsetY = (containerSize.height * (scale - 1) / 2).coerceAtLeast(0f)
                    val newOffsetX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                    val newOffsetY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    offset = Offset(newOffsetX, newOffsetY)

                    event.changes.forEach { it.consume() }
                } else if (!isMultiTouch) {
                    // Single finger: guaranteed drawing
                    val change = event.changes.firstOrNull { it.id == activePointerId }
                    if (change == null || !change.pressed) {
                        if (isDrawMode) {
                            currentStrokeEnd()
                        } else {
                            val (cx, cy) = screenToCanvas(down.position.x, down.position.y)
                            currentCanvasTap(cx.toInt().coerceIn(0, canvasWidth - 1), cy.toInt().coerceIn(0, canvasHeight - 1))
                        }
                        break
                    }

                    change.consume()
                    if (isDrawMode) {
                        val (cx, cy) = screenToCanvas(change.position.x, change.position.y)
                        currentStrokeMove(cx, cy)
                    }
                } else {
                    // Released after multi-touch
                    if (pointerCount == 0) break
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .then(gestureModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            content()
        }
    }
}
