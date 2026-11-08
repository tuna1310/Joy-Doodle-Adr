package com.doodlestudio.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkDao {
    @Query("SELECT * FROM artworks ORDER BY updatedAt DESC")
    fun getAllArtworks(): Flow<List<ArtworkEntity>>

    @Query("SELECT * FROM artworks WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteArtworks(): Flow<List<ArtworkEntity>>

    @Query("SELECT * FROM artworks WHERE id = :id LIMIT 1")
    suspend fun getArtworkById(id: String): ArtworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtwork(artwork: ArtworkEntity)

    @Update
    suspend fun updateArtwork(artwork: ArtworkEntity)

    @Delete
    suspend fun deleteArtwork(artwork: ArtworkEntity)

    @Query("DELETE FROM artworks WHERE id = :id")
    suspend fun deleteArtworkById(id: String)

    @Query("UPDATE artworks SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleFavorite(id: String)
}
