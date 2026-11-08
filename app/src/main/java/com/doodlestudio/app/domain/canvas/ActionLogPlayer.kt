package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.EditAction

/**
 * Playback and scrubbing engine for ActionLog.
 * Supports chronological step-by-step and time-compressed timelapse frame mapping.
 */
class ActionLogPlayer(val actionLog: ActionLog) {

    val actions: List<EditAction> = actionLog.actions

    val actionCount: Int get() = actions.size

    val startTimeMs: Long = actions.firstOrNull()?.timestampMs ?: 0L
    val endTimeMs: Long = actions.lastOrNull()?.timestampMs ?: 0L

    val rawDurationMs: Long = if (actions.size > 1) (endTimeMs - startTimeMs).coerceAtLeast(1L) else 1L

    /**
     * Retrieves all actions that occurred up to a relative elapsed time from start.
     */
    fun getActionsUpToRelativeTime(relativeTimeMs: Long): List<EditAction> {
        if (actions.isEmpty()) return emptyList()
        val targetAbsoluteTime = startTimeMs + relativeTimeMs
        return actions.filter { it.timestampMs <= targetAbsoluteTime }
    }

    /**
     * Retrieves all actions up to a given 1-based step index.
     */
    fun getActionsUpToStep(step: Int): List<EditAction> {
        if (actions.isEmpty() || step <= 0) return emptyList()
        val clampedStep = step.coerceAtMost(actions.size)
        return actions.subList(0, clampedStep)
    }

    /**
     * Calculates the actions to render for a timelapse frame.
     * Maps frameIndex in 0 until totalFrames linearly across the action timeline.
     */
    fun getActionsForTimelapseFrame(frameIndex: Int, totalFrames: Int): List<EditAction> {
        require(totalFrames > 0) { "Total frames must be positive" }
        if (actions.isEmpty()) return emptyList()
        if (frameIndex <= 0) return listOf(actions.first())
        if (frameIndex >= totalFrames - 1) return actions

        val progress = frameIndex.toFloat() / (totalFrames - 1)
        val targetRelTime = (progress * rawDurationMs).toLong()
        val targetTime = startTimeMs + targetRelTime

        // Find actions up to targetTime; always include at least the first action
        val matching = actions.filter { it.timestampMs <= targetTime }
        return if (matching.isEmpty()) listOf(actions.first()) else matching
    }
}

