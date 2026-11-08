package com.doodlestudio.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.doodlestudio.app.data.local.ArtworkDao
import com.doodlestudio.app.data.local.ArtworkEntity
import com.doodlestudio.app.domain.canvas.ActionLogSerializer
import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.domain.repository.ArtworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class ArtworkRepositoryImpl(
    private val artworkDao: ArtworkDao,
    private val filesDir: File
) : ArtworkRepository {

    constructor(artworkDao: ArtworkDao, context: Context) : this(artworkDao, context.filesDir)

    override fun getAllArtworks(): Flow<List<Artwork>> {
        return artworkDao.getAllArtworks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFavoriteArtworks(): Flow<List<Artwork>> {
        return artworkDao.getFavoriteArtworks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getArtworkById(id: String): Artwork? = withContext(Dispatchers.IO) {
        artworkDao.getArtworkById(id)?.toDomain()
    }

    override suspend fun saveArtwork(
        artwork: Artwork,
        actionLog: ActionLog,
        thumbnailBitmap: Bitmap?,
        fullBitmap: Bitmap?
    ): Artwork = withContext(Dispatchers.IO) {
        val dir = File(filesDir, "artworks/${artwork.id}")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // 1. Save ActionLog JSON
        val stateFile = File(dir, "action_log.json")
        stateFile.writeText(ActionLogSerializer.serialize(actionLog))

        // 2. Save Thumbnail Bitmap if provided
        var thumbPath = artwork.thumbnailPath
        if (thumbnailBitmap != null) {
            val thumbFile = File(dir, "thumbnail.png")
            thumbFile.outputStream().use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            thumbPath = thumbFile.absolutePath
        }

        // 3. Save Full Canvas Flattened Image if provided
        if (fullBitmap != null) {
            val canvasFile = File(dir, "canvas.png")
            canvasFile.outputStream().use { out ->
                fullBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
        }

        val updatedArtwork = artwork.copy(
            thumbnailPath = thumbPath,
            statePath = stateFile.absolutePath,
            updatedAt = System.currentTimeMillis()
        )

        artworkDao.insertArtwork(ArtworkEntity.fromDomain(updatedArtwork))
        updatedArtwork
    }

    override suspend fun loadActionLog(artworkId: String): ActionLog? = withContext(Dispatchers.IO) {
        val stateFile = File(filesDir, "artworks/$artworkId/action_log.json")
        if (!stateFile.exists()) return@withContext null
        try {
            ActionLogSerializer.deserialize(stateFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun toggleFavorite(artworkId: String) = withContext(Dispatchers.IO) {
        artworkDao.toggleFavorite(artworkId)
    }

    override suspend fun deleteArtwork(artworkId: String) = withContext(Dispatchers.IO) {
        artworkDao.deleteArtworkById(artworkId)
        val dir = File(filesDir, "artworks/$artworkId")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
}

