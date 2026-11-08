package com.doodlestudio.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Artwork(
    val id: String,
    val title: String,
    val templateId: String? = null,
    val isBlankCanvas: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val statePath: String? = null,
    val isFavorite: Boolean = false,
    val labelMapVersion: Int = 1
)

@Serializable
sealed class EditAction {
    abstract val timestampMs: Long

    @Serializable
    data class Fill(
        val regionId: Int,
        val color: Int,
        val prevColor: Int = 0,
        val normalizedX: Float = 0f,
        val normalizedY: Float = 0f,
        override val timestampMs: Long = 0L
    ) : EditAction()

    @Serializable
    data class Stroke(
        val points: List<PointF>,
        val color: Int,
        val strokeWidth: Float,
        val alpha: Float = 1.0f,
        val toolType: String = "PEN",
        val seed: Long = 0L,
        override val timestampMs: Long = 0L
    ) : EditAction()

    @Serializable
    data class Erase(
        val points: List<PointF>,
        val strokeWidth: Float,
        override val timestampMs: Long = 0L
    ) : EditAction()
}

@Serializable
data class PointF(val x: Float, val y: Float)
