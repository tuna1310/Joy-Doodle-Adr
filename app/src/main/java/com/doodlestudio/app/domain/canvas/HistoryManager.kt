package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.EditAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/**
 * Manages action history for Undo and Redo operations.
 * Guaranteed minimum 30 steps of history (default 50).
 * Handles both coloring (EditAction.Fill) and free-hand drawing (EditAction.Stroke/Erase).
 * Memory efficient: Only stores lightweight EditAction data classes (~20-100 bytes each).
 */
class HistoryManager(private val maxHistorySize: Int = 50) {

    init {
        require(maxHistorySize >= 30) { "History capacity must be at least 30 steps" }
    }

    private val undoStack = ArrayDeque<EditAction>()
    private val redoStack = ArrayDeque<EditAction>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun recordAction(action: EditAction) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(action)
        redoStack.clear()
        updateFlows()
    }

    fun undo(): EditAction? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.removeLast()
        redoStack.addLast(action)
        updateFlows()
        return action
    }

    fun redo(): EditAction? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.removeLast()
        undoStack.addLast(action)
        updateFlows()
        return action
    }

    fun getActionLog(): List<EditAction> {
        return undoStack.toList()
    }

    fun reset() {
        undoStack.clear()
        redoStack.clear()
        updateFlows()
    }

    val size: Int get() = undoStack.size

    private fun updateFlows() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
