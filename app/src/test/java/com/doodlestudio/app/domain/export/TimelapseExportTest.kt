package com.doodlestudio.app.domain.export

import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

class TimelapseExportTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("doodle_export_test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testExportConfigDefaultsAndFrames() {
        val config = ExportConfig(
            width = 720,
            height = 720,
            fps = 30,
            durationSeconds = 12
        )
        assertEquals(360, config.totalFrames)
        assertEquals(50L * 1024L * 1024L, ExportConfig.MIN_FREE_SPACE_BYTES)
    }

    @Test
    fun testWatermarkBoundsCalculation() {
        val bounds = WatermarkRenderer.calculateBounds(
            canvasWidth = 720,
            canvasHeight = 720,
            text = "Created with Doodle Studio",
            margin = 20f,
            padding = 10f
        )

        assertNotNull(bounds)
        // Rect must fit inside canvas boundaries
        assertTrue("Watermark right (${bounds.rect.right}) must be <= 700", bounds.rect.right <= 700f)
        assertTrue("Watermark bottom (${bounds.rect.bottom}) must be <= 700", bounds.rect.bottom <= 700f)
        assertTrue("Watermark left (${bounds.rect.left}) must be >= 0", bounds.rect.left >= 0f)
        assertTrue("Watermark top (${bounds.rect.top}) must be >= 0", bounds.rect.top >= 0f)
        assertTrue("Font size must be in reasonable range", bounds.fontSize in 14f..28f)
    }

    @Test
    fun testAmbientAudioSynthesizerGeneratesValidWav() {
        val wavFile = File(tempDir, "ambient_test.wav")
        AmbientAudioSynthesizer.synthesizeAmbientWav(wavFile, durationSeconds = 2)

        assertTrue("WAV file must exist", wavFile.exists())
        assertTrue("WAV file size must be > 0", wavFile.length() > 0)

        // Expected size: 44 bytes header + 44100 samples/sec * 2 sec * 2 bytes/sample (16-bit mono)
        val expectedDataBytes = 44100 * 2 * 2
        val expectedTotalBytes = 44L + expectedDataBytes
        assertEquals(expectedTotalBytes, wavFile.length())

        // Verify WAV Header
        RandomAccessFile(wavFile, "r").use { raf ->
            val riffBytes = ByteArray(4)
            raf.read(riffBytes)
            assertEquals("RIFF", String(riffBytes))

            raf.seek(8)
            val waveBytes = ByteArray(4)
            raf.read(waveBytes)
            assertEquals("WAVE", String(waveBytes))

            raf.seek(12)
            val fmtBytes = ByteArray(4)
            raf.read(fmtBytes)
            assertEquals("fmt ", String(fmtBytes))
        }
    }

    @Test
    fun testTimelapseFrameChronologicalProgression() {
        val actions = (1..10).map { i ->
            EditAction.Stroke(
                points = listOf(PointF(i * 10f, i * 10f), PointF(i * 10f + 5f, i * 10f + 5f)),
                color = 0xFFFF0000.toInt(),
                strokeWidth = 10f,
                timestampMs = 1000L * i
            )
        }
        val actionLog = ActionLog(
            actions = actions,
            totalDurationMs = 10000L
        )

        val player = com.doodlestudio.app.domain.canvas.ActionLogPlayer(actionLog)

        // Verify progression: frame 0 has at least 1 action, frame 29 has all 10 actions
        val frame0Actions = player.getActionsForTimelapseFrame(0, 30)
        assertEquals(1, frame0Actions.size)

        val frame29Actions = player.getActionsForTimelapseFrame(29, 30)
        assertEquals(10, frame29Actions.size)

        // Monotonic progression
        var previousCount = 0
        for (f in 0 until 30) {
            val count = player.getActionsForTimelapseFrame(f, 30).size
            assertTrue("Frame $f count ($count) must be >= previous ($previousCount)", count >= previousCount)
            previousCount = count
        }
    }
}

