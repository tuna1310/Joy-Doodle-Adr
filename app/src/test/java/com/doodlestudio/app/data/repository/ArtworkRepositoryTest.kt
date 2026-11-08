package com.doodlestudio.app.data.repository

import com.doodlestudio.app.data.local.ArtworkDao
import com.doodlestudio.app.data.local.ArtworkEntity
import com.doodlestudio.app.domain.canvas.ActionLogSerializer
import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArtworkRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var fakeDao: FakeArtworkDao
    private lateinit var repository: ArtworkRepositoryImpl

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("doodle_repo_test").toFile()
        fakeDao = FakeArtworkDao()
        repository = ArtworkRepositoryImpl(fakeDao, tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testSaveAndGetArtworkById() = runBlocking {
        val artwork = Artwork(
            id = "art_101",
            title = "My Zen Lotus",
            templateId = "flower_lotus_zen",
            isBlankCanvas = false
        )
        val actionLog = ActionLog(
            version = 1,
            artworkId = "art_101",
            templateId = "flower_lotus_zen",
            actions = listOf(
                EditAction.Fill(regionId = 2, color = 0xFFFF5722.toInt(), timestampMs = 1000L),
                EditAction.Stroke(listOf(PointF(10f, 10f), PointF(20f, 20f)), 0xFF4CAF50.toInt(), 8f, timestampMs = 2000L)
            )
        )

        // Save artwork
        val saved = repository.saveArtwork(artwork, actionLog)
        assertNotNull("Saved artwork must not be null", saved)
        assertEquals("art_101", saved.id)
        assertNotNull("State path must be generated", saved.statePath)

        // File must exist on disk
        val actionLogFile = File(tempDir, "artworks/art_101/action_log.json")
        assertTrue("action_log.json must exist on disk", actionLogFile.exists())

        // Fetch from repository
        val fetched = repository.getArtworkById("art_101")
        assertNotNull("Fetched artwork must exist", fetched)
        assertEquals("My Zen Lotus", fetched?.title)
        assertEquals("flower_lotus_zen", fetched?.templateId)

        // Load action log from disk
        val loadedLog = repository.loadActionLog("art_101")
        assertNotNull("ActionLog must be loaded from disk", loadedLog)
        assertEquals(2, loadedLog?.actions?.size)
        assertTrue("Action 0 must be Fill", loadedLog?.actions?.get(0) is EditAction.Fill)
        assertTrue("Action 1 must be Stroke", loadedLog?.actions?.get(1) is EditAction.Stroke)
    }

    @Test
    fun testToggleFavorite() = runBlocking {
        val artwork = Artwork(
            id = "art_fav_1",
            title = "Star Shield",
            templateId = "mandala_star_shield",
            isFavorite = false
        )
        val log = ActionLog(artworkId = "art_fav_1")
        repository.saveArtwork(artwork, log)

        assertFalse("Initial favorite status must be false", fakeDao.getArtworkById("art_fav_1")?.isFavorite ?: true)

        repository.toggleFavorite("art_fav_1")
        assertTrue("Favorite status must toggle to true", fakeDao.getArtworkById("art_fav_1")?.isFavorite ?: false)

        repository.toggleFavorite("art_fav_1")
        assertFalse("Favorite status must toggle back to false", fakeDao.getArtworkById("art_fav_1")?.isFavorite ?: true)
    }

    @Test
    fun testDeleteArtworkRemovesFromDbAndDisk() = runBlocking {
        val artwork = Artwork(
            id = "art_to_delete",
            title = "Delete Me",
            isBlankCanvas = true
        )
        val log = ActionLog(artworkId = "art_to_delete")
        repository.saveArtwork(artwork, log)

        val dir = File(tempDir, "artworks/art_to_delete")
        assertTrue("Directory must exist before deletion", dir.exists())

        repository.deleteArtwork("art_to_delete")

        assertNull("Artwork must be deleted from DB", repository.getArtworkById("art_to_delete"))
        assertFalse("Directory must be deleted from disk", dir.exists())
    }

    @Test
    fun testGetFavoriteArtworksFlow() = runBlocking {
        val a1 = Artwork(id = "art_1", title = "Art 1", isFavorite = false)
        val a2 = Artwork(id = "art_2", title = "Art 2", isFavorite = true)
        val a3 = Artwork(id = "art_3", title = "Art 3", isFavorite = true)

        repository.saveArtwork(a1, ActionLog(artworkId = "art_1"))
        repository.saveArtwork(a2, ActionLog(artworkId = "art_2"))
        repository.saveArtwork(a3, ActionLog(artworkId = "art_3"))

        val favorites = repository.getFavoriteArtworks().first()
        assertEquals("Must return only favorite artworks", 2, favorites.size)
        assertTrue(favorites.any { it.id == "art_2" })
        assertTrue(favorites.any { it.id == "art_3" })
        assertFalse(favorites.any { it.id == "art_1" })
    }

    /**
     * Fake DAO implementation for clean unit testing without SQLite native binaries.
     */
    class FakeArtworkDao : ArtworkDao {
        private val storage = mutableMapOf<String, ArtworkEntity>()
        private val flow = MutableStateFlow<List<ArtworkEntity>>(emptyList())

        override fun getAllArtworks(): Flow<List<ArtworkEntity>> = flow.map { list ->
            list.sortedByDescending { it.updatedAt }
        }

        override fun getFavoriteArtworks(): Flow<List<ArtworkEntity>> = flow.map { list ->
            list.filter { it.isFavorite }.sortedByDescending { it.updatedAt }
        }

        override suspend fun getArtworkById(id: String): ArtworkEntity? = storage[id]

        override suspend fun insertArtwork(artwork: ArtworkEntity) {
            storage[artwork.id] = artwork
            flow.value = storage.values.toList()
        }

        override suspend fun updateArtwork(artwork: ArtworkEntity) {
            storage[artwork.id] = artwork
            flow.value = storage.values.toList()
        }

        override suspend fun deleteArtwork(artwork: ArtworkEntity) {
            storage.remove(artwork.id)
            flow.value = storage.values.toList()
        }

        override suspend fun deleteArtworkById(id: String) {
            storage.remove(id)
            flow.value = storage.values.toList()
        }

        override suspend fun toggleFavorite(id: String) {
            val entity = storage[id] ?: return
            val updated = entity.copy(isFavorite = !entity.isFavorite)
            storage[id] = updated
            flow.value = storage.values.toList()
        }
    }
}

