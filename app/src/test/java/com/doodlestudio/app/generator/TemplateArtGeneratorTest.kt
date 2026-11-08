package com.doodlestudio.app.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.util.ArrayDeque

class TemplateArtGeneratorTest {

    private lateinit var assetsDir: File

    @Before
    fun setUp() {
        assetsDir = File("src/main/assets/templates")
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }
    }

    @Test
    fun testGenerateAndVerifyAll41Templates() {
        val results = TemplateArtGenerator.generateAll(assetsDir)

        assertEquals("41 PNGs + 1 index.json must be generated", 42, results.size)
        val imageFiles = results.filter { it.first.name.endsWith(".png") }
        assertEquals(41, imageFiles.size)

        val indexFile = File(assetsDir, "index.json")
        assertTrue("index.json must exist", indexFile.exists())
        assertTrue("index.json must have non-zero size", indexFile.length() > 0)

        var totalAssetBytes = indexFile.length()
        for ((file, _) in imageFiles) {
            assertTrue("File ${file.name} must exist", file.exists())
            val bytes = file.readBytes()

            // Verify PNG header
            assertEquals(0x89.toByte(), bytes[0])
            assertEquals(0x50.toByte(), bytes[1]) // 'P'
            assertEquals(0x4E.toByte(), bytes[2]) // 'N'
            assertEquals(0x47.toByte(), bytes[3]) // 'G'

            // Verify dimensions from IHDR chunk (offset 16: width, offset 20: height)
            val width = ByteBuffer.wrap(bytes, 16, 4).int
            val height = ByteBuffer.wrap(bytes, 20, 4).int
            assertEquals("Width must be 1024", 1024, width)
            assertEquals("Height must be 1024", 1024, height)

            totalAssetBytes += file.length()
        }

        val totalKb = totalAssetBytes / 1024
        println("VERIFIED_ASSET_SIZE_KB: $totalKb KB ($totalAssetBytes bytes)")
        assertTrue("Total template assets must be reasonable (< 3000 KB)", totalKb < 3000)
    }

    @Test
    fun testGeneratorDeterminism() {
        val tempDir1 = File("build/tmp/test_gen1").apply { mkdirs() }
        val tempDir2 = File("build/tmp/test_gen2").apply { mkdirs() }

        val run1 = TemplateArtGenerator.generateAll(tempDir1)
        val run2 = TemplateArtGenerator.generateAll(tempDir2)

        assertEquals("Both runs must have same file count", run1.size, run2.size)

        for (i in run1.indices) {
            val file1 = run1[i].first
            val hash1 = run1[i].second
            val hash2 = run2[i].second
            assertEquals("Hashes for ${file1.name} must be 100% deterministic", hash1, hash2)
        }

        tempDir1.deleteRecursively()
        tempDir2.deleteRecursively()
    }

    @Test
    fun testIndexJsonStructureAndCompleteness() {
        val indexFile = File(assetsDir, "index.json")
        if (!indexFile.exists()) {
            TemplateArtGenerator.generateAll(assetsDir)
        }

        val content = indexFile.readText()
        val templateList = TemplateArtGenerator.templates

        assertEquals(41, templateList.size)
        val ids = mutableSetOf<String>()

        for (tmpl in templateList) {
            assertTrue("Template ID must be unique: ${tmpl.id}", ids.add(tmpl.id))
            assertTrue("index.json must contain ID ${tmpl.id}", content.contains("\"id\": \"${tmpl.id}\""))
            assertTrue("English name must not be blank", tmpl.nameEn.isNotBlank())
            assertTrue("Vietnamese name must not be blank", tmpl.nameVi.isNotBlank())
            assertTrue(
                "Category must be valid",
                tmpl.category in listOf("mandala", "flowers", "animals", "geometry", "scenery", "food")
            )
            assertTrue(
                "Difficulty must be valid",
                tmpl.difficulty in listOf("easy", "medium", "hard")
            )

            val expectedImg = File(assetsDir, "images/${tmpl.id}.png")
            assertTrue("Image file must exist: ${expectedImg.path}", expectedImg.exists())
        }
    }

    @Test
    fun testAllTemplatesHaveClosedBoundariesViaFloodFill() {
        // For every template, verify that flood fill from outside (0, 0) does NOT leak into center (512, 512)
        for (tmpl in TemplateArtGenerator.templates) {
            val canvas = TemplateArtGenerator.render(tmpl)
            val w = canvas.width
            val h = canvas.height

            // Breadth-First-Search flood fill from outside corner (0, 0)
            val visited = BooleanArray(w * h)
            val queue = ArrayDeque<Int>()

            val startIndex = 0 // (0, 0)
            visited[startIndex] = true
            queue.add(startIndex)

            while (!queue.isEmpty()) {
                val idx = queue.removeFirst()
                val x = idx % w
                val y = idx / w

                // 4-directional neighbors
                val neighbors = arrayOf(
                    x to y - 1,
                    x to y + 1,
                    x - 1 to y,
                    x + 1 to y
                )

                for ((nx, ny) in neighbors) {
                    if (nx in 0 until w && ny in 0 until h) {
                        val nIdx = ny * w + nx
                        if (!visited[nIdx] && !canvas.isPixelBorder(nx, ny)) {
                            visited[nIdx] = true
                            queue.add(nIdx)
                        }
                    }
                }
            }

            // Verify center (512, 512) was NOT reached from outside flood fill (proves outer boundary is fully closed)
            val centerIdx = 512 * w + 512
            assertFalse(
                "Template ${tmpl.id} boundary must be closed; outside flood fill must not leak into center",
                visited[centerIdx]
            )
        }
    }
}
