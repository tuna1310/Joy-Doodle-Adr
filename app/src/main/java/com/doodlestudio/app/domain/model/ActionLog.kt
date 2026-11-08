package com.doodlestudio.app.domain.model

import kotlinx.serialization.Serializable

/**
 * ActionLog records all creative operations (fills, free-hand strokes, erasures)
 * with timestamps for saving, continuing edits, and timelapse replay (P5).
 */
@Serializable
data class ActionLog(
    val version: Int = 1,
    val artworkId: String = "",
    val templateId: String? = null,
    val isBlankCanvas: Boolean = false,
    val canvasWidth: Int = 1024,
    val canvasHeight: Int = 1024,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val actions: List<EditAction> = emptyList(),
    val totalDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

