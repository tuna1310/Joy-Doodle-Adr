package com.doodlestudio.app.domain.repository

import android.graphics.Bitmap
import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.Artwork
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    fun getAllArtworks(): Flow<List<Artwork>>

    fun getFavoriteArtworks(): Flow<List<Artwork>>

    suspend fun getArtworkById(id: String): Artwork?

    suspend fun saveArtwork(
        artwork: Artwork,
        actionLog: ActionLog,
        thumbnailBitmap: Bitmap? = null,
        fullBitmap: Bitmap? = null
    ): Artwork

    suspend fun loadActionLog(artworkId: String): ActionLog?

    suspend fun toggleFavorite(artworkId: String)

    suspend fun deleteArtwork(artworkId: String)
}

