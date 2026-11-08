package com.doodlestudio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.doodlestudio.app.domain.model.Artwork

@Entity(tableName = "artworks")
data class ArtworkEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val templateId: String?,
    val isBlankCanvas: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnailPath: String?,
    val statePath: String?,
    val isFavorite: Boolean = false,
    val labelMapVersion: Int = 1
) {
    fun toDomain(): Artwork = Artwork(
        id = id,
        title = title,
        templateId = templateId,
        isBlankCanvas = isBlankCanvas,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        statePath = statePath,
        isFavorite = isFavorite,
        labelMapVersion = labelMapVersion
    )

    companion object {
        fun fromDomain(artwork: Artwork): ArtworkEntity = ArtworkEntity(
            id = artwork.id,
            title = artwork.title,
            templateId = artwork.templateId,
            isBlankCanvas = artwork.isBlankCanvas,
            createdAt = artwork.createdAt,
            updatedAt = artwork.updatedAt,
            thumbnailPath = artwork.thumbnailPath,
            statePath = artwork.statePath,
            isFavorite = artwork.isFavorite,
            labelMapVersion = artwork.labelMapVersion
        )
    }
}
