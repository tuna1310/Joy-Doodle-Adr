package com.doodlestudio.app.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.doodlestudio.app.domain.model.TemplateCategory
import com.doodlestudio.app.domain.model.TemplateDifficulty
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class TemplateRepositoryTest {

    private val sampleJson = """
    [
      {
        "id": "mandala_blossom_8",
        "nameEn": "Blossom Mandala",
        "nameVi": "Mandala hoa nở",
        "category": "mandala",
        "difficulty": "medium",
        "imagePath": "templates/images/mandala_blossom_8.png",
        "regionCountHint": 48
      },
      {
        "id": "flower_lotus_zen",
        "nameEn": "Zen Lotus Flower",
        "nameVi": "Hoa sen thiền",
        "category": "flowers",
        "difficulty": "easy",
        "imagePath": "templates/images/flower_lotus_zen.png",
        "regionCountHint": 18
      },
      {
        "id": "animal_geometric_owl",
        "nameEn": "Geometric Owl",
        "nameVi": "Chim cú hình học",
        "category": "animals",
        "difficulty": "hard",
        "imagePath": "templates/images/animal_geometric_owl.png",
        "regionCountHint": 52
      }
    ]
    """.trimIndent()

    private lateinit var context: Context
    private lateinit var assetManager: AssetManager
    private lateinit var repository: TemplateRepositoryImpl

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        assetManager = mockk(relaxed = true)
        every { context.assets } returns assetManager
        every { assetManager.open("templates/index.json") } answers {
            ByteArrayInputStream(sampleJson.toByteArray(Charsets.UTF_8))
        }
        repository = TemplateRepositoryImpl(context)
    }

    @Test
    fun testGetTemplatesFlowReturnsAllTemplates() = runTest {
        val templates = repository.getTemplates().first()
        assertEquals(3, templates.size)
        assertEquals("mandala_blossom_8", templates[0].id)
        assertEquals("flower_lotus_zen", templates[1].id)
        assertEquals("animal_geometric_owl", templates[2].id)
    }

    @Test
    fun testGetTemplateById() = runTest {
        val found = repository.getTemplateById("flower_lotus_zen")
        assertNotNull(found)
        assertEquals("Zen Lotus Flower", found?.nameEn)
        assertEquals("Hoa sen thiền", found?.nameVi)

        val notFound = repository.getTemplateById("non_existing_id")
        assertNull(notFound)
    }

    @Test
    fun testFilterByCategory() = runTest {
        val all = repository.getTemplatesByCategory(TemplateCategory.ALL)
        assertEquals(3, all.size)

        val mandala = repository.getTemplatesByCategory(TemplateCategory.MANDALA)
        assertEquals(1, mandala.size)
        assertEquals("mandala_blossom_8", mandala[0].id)

        val flowers = repository.getTemplatesByCategory(TemplateCategory.FLOWERS)
        assertEquals(1, flowers.size)
        assertEquals("flower_lotus_zen", flowers[0].id)

        val geometry = repository.getTemplatesByCategory(TemplateCategory.GEOMETRY)
        assertTrue(geometry.isEmpty())
    }

    @Test
    fun testFilterByDifficulty() = runTest {
        val easy = repository.getTemplatesByDifficulty(TemplateDifficulty.EASY)
        assertEquals(1, easy.size)
        assertEquals("flower_lotus_zen", easy[0].id)

        val medium = repository.getTemplatesByDifficulty(TemplateDifficulty.MEDIUM)
        assertEquals(1, medium.size)
        assertEquals("mandala_blossom_8", medium[0].id)

        val hard = repository.getTemplatesByDifficulty(TemplateDifficulty.HARD)
        assertEquals(1, hard.size)
        assertEquals("animal_geometric_owl", hard[0].id)
    }
}

