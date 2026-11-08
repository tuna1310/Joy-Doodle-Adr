package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF

/**
 * Manages free-hand drawing state: active touch points, completed strokes,
 * brush settings, and eraser actions.
 */
class FreeDrawManager(
    private val historyManager: HistoryManager = HistoryManager()
) {
    var brushConfig: BrushConfig = BrushConfig()

    // Points of the stroke currently being drawn
    private val _currentPoints = mutableListOf<PointF>()
    val currentPoints: List<PointF> get() = _currentPoints

    // All active completed actions (strokes & erasures)
    private val _actions = mutableListOf<EditAction>()
    val actions: List<EditAction> get() = _actions

    fun setBrushTool(tool: BrushTool) {
        brushConfig = brushConfig.copy(tool = tool)
    }

    fun setBrushSize(size: Float) {
        brushConfig = brushConfig.copy(size = size)
    }

    fun setBrushAlpha(alpha: Float) {
        brushConfig = brushConfig.copy(alpha = alpha.coerceIn(0.1f, 1.0f))
    }

    fun onTouchDown(x: Float, y: Float, timestamp: Long = System.currentTimeMillis()) {
        _currentPoints.clear()
        _currentPoints.add(PointF(x, y))
    }

    fun onTouchMove(x: Float, y: Float, timestamp: Long = System.currentTimeMillis()) {
        if (_currentPoints.isNotEmpty()) {
            val last = _currentPoints.last()
            val dx = x - last.x
            val dy = y - last.y
            // Add if moved at least 1.0 pixel for smooth responsive strokes
            if (dx * dx + dy * dy >= 1.0f) {
                _currentPoints.add(PointF(x, y))
            }
        } else {
            _currentPoints.add(PointF(x, y))
        }
    }

    fun onTouchUp(timestamp: Long = System.currentTimeMillis()): EditAction? {
        if (_currentPoints.isEmpty()) return null

        // Single points (taps) and 2-point micro-strokes are preserved directly without dropping
        val smoothedPoints = if (_currentPoints.size <= 2) {
            _currentPoints.toList()
        } else {
            StrokeInterpolator.downsamplePoints(_currentPoints, minDistance = 1.5f)
        }
        _currentPoints.clear()

        val action: EditAction = if (brushConfig.tool == BrushTool.ERASER) {
            EditAction.Erase(
                points = smoothedPoints,
                strokeWidth = brushConfig.size * 1.5f,
                timestampMs = timestamp
            )
        } else {
            EditAction.Stroke(
                points = smoothedPoints,
                color = brushConfig.effectiveArgb,
                strokeWidth = brushConfig.size,
                alpha = brushConfig.alpha,
                toolType = brushConfig.tool.legacyOrName,
                timestampMs = timestamp
            )
        }

        _actions.add(action)
        historyManager.recordAction(action)
        return action
    }

    fun undo(): EditAction? {
        val undone = historyManager.undo() ?: return null
        _actions.remove(undone)
        return undone
    }

    fun redo(): EditAction? {
        val redone = historyManager.redo() ?: return null
        _actions.add(redone)
        return redone
    }

    fun clear() {
        _currentPoints.clear()
        _actions.clear()
        historyManager.reset()
    }
}

