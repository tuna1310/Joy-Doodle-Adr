package com.doodlestudio.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SoundEffectManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSynthesizePopSound() {
        val popFile = tempFolder.newFile("pop.wav")
        SoundEffectManager.synthesizePopSound(popFile)

        verifyWavFile(popFile, SoundEffectManager.SAMPLE_RATE, expectedDurationMs = 120)
    }

    @Test
    fun testSynthesizeClickSound() {
        val clickFile = tempFolder.newFile("click.wav")
        SoundEffectManager.synthesizeClickSound(clickFile)

        verifyWavFile(clickFile, SoundEffectManager.SAMPLE_RATE, expectedDurationMs = 50)
    }

    @Test
    fun testSynthesizeChimeSound() {
        val chimeFile = tempFolder.newFile("chime.wav")
        SoundEffectManager.synthesizeChimeSound(chimeFile)

        verifyWavFile(chimeFile, SoundEffectManager.SAMPLE_RATE, expectedDurationMs = 450)
    }

    private fun verifyWavFile(file: File, expectedSampleRate: Int, expectedDurationMs: Int) {
        assertTrue("WAV file must exist", file.exists())
        val bytes = file.readBytes()
        assertTrue("File must be larger than 44-byte WAV header", bytes.size > 44)

        // RIFF header
        assertEquals('R'.code.toByte(), bytes[0])
        assertEquals('I'.code.toByte(), bytes[1])
        assertEquals('F'.code.toByte(), bytes[2])
        assertEquals('F'.code.toByte(), bytes[3])

        // WAVE signature
        assertEquals('W'.code.toByte(), bytes[8])
        assertEquals('A'.code.toByte(), bytes[9])
        assertEquals('V'.code.toByte(), bytes[10])
        assertEquals('E'.code.toByte(), bytes[11])

        // fmt chunk
        assertEquals('f'.code.toByte(), bytes[12])
        assertEquals('m'.code.toByte(), bytes[13])
        assertEquals('t'.code.toByte(), bytes[14])
        assertEquals(' '.code.toByte(), bytes[15])

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val audioFormat = buffer.getShort(20) // 1 = PCM
        val channels = buffer.getShort(22)    // 1 = mono
        val sampleRate = buffer.getInt(24)
        val bitsPerSample = buffer.getShort(34)

        assertEquals("Format must be PCM (1)", 1.toShort(), audioFormat)
        assertEquals("Channels must be Mono (1)", 1.toShort(), channels)
        assertEquals("Sample rate must match", expectedSampleRate, sampleRate)
        assertEquals("Bits per sample must be 16", 16.toShort(), bitsPerSample)

        // data chunk
        assertEquals('d'.code.toByte(), bytes[36])
        assertEquals('a'.code.toByte(), bytes[37])
        assertEquals('t'.code.toByte(), bytes[38])
        assertEquals('a'.code.toByte(), bytes[39])

        val dataLength = buffer.getInt(40)
        assertEquals("WAV file size must match header + dataLength", bytes.size, dataLength + 44)

        val samplesCount = dataLength / 2
        val expectedSamples = (expectedSampleRate * (expectedDurationMs / 1000.0)).toInt()
        assertEquals("Sample count must match expected duration", expectedSamples, samplesCount)

        // Check for non-zero audio content
        var hasNonZero = false
        for (i in 44 until bytes.size) {
            if (bytes[i] != 0.toByte()) {
                hasNonZero = true
                break
            }
        }
        assertTrue("Synthesized audio must contain non-zero samples", hasNonZero)
    }
}

