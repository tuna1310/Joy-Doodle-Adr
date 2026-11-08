package com.doodlestudio.app.domain.export

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure Kotlin CC0 Ambient Sound Synthesizer.
 * Generates soft, calming pentatonic chime background music as standard 16-bit PCM WAV.
 * 100% Original procedural generation with zero external audio dependencies.
 */
object AmbientAudioSynthesizer {

    const val SAMPLE_RATE = 44100
    private val CHIME_FREQUENCIES = doubleArrayOf(
        261.63, // C4
        329.63, // E4
        392.00, // G4
        440.00, // A4
        523.25, // C5
        659.25  // E5
    )

    /**
     * Synthesizes a soothing ambient background audio track into a WAV file.
     */
    fun synthesizeAmbientWav(outputFile: File, durationSeconds: Int = 12): File {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val pcmData = ShortArray(totalSamples)

        // Generate gentle chimes every 1.5 - 2 seconds
        val chimeInterval = (SAMPLE_RATE * 1.8).toInt()
        var currentChimeIndex = 0

        for (startSample in 0 until totalSamples step chimeInterval) {
            val freq = CHIME_FREQUENCIES[currentChimeIndex % CHIME_FREQUENCIES.size]
            currentChimeIndex++

            val chimeLength = (SAMPLE_RATE * 3.5).toInt().coerceAtMost(totalSamples - startSample)
            val decayTime = 1.2 // Seconds for amplitude decay

            for (i in 0 until chimeLength) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-t / decayTime) * (1.0 - exp(-t * 20.0)) // Soft attack + exponential decay
                val sampleValue = (sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * freq * 2.0 * t) * 0.3) * envelope * 12000.0

                val sampleIdx = startSample + i
                val existing = pcmData[sampleIdx].toInt()
                val blended = (existing + sampleValue.toInt()).coerceIn(-32768, 32767)
                pcmData[sampleIdx] = blended.toShort()
            }
        }

        writeWavFile(outputFile, pcmData, SAMPLE_RATE)
        return outputFile
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteDataLength = pcmData.size * 2
        val totalDataLen = byteDataLength + 36

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF chunk
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())

            // fmt sub-chunk
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size (16 for PCM)
            header.putShort(1) // AudioFormat (1 for PCM)
            header.putShort(1) // NumChannels (1 = Mono)
            header.putInt(sampleRate) // SampleRate
            header.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
            header.putShort(2) // BlockAlign (NumChannels * BitsPerSample/8)
            header.putShort(16) // BitsPerSample (16 bit)

            // data sub-chunk
            header.put("data".toByteArray())
            header.putInt(byteDataLength)

            fos.write(header.array())

            // Write PCM samples
            val sampleBuffer = ByteBuffer.allocate(byteDataLength).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                sampleBuffer.putShort(sample)
            }
            fos.write(sampleBuffer.array())
        }
    }
}

