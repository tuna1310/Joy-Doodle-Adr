package com.doodlestudio.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.doodlestudio.app.data.preferences.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Low-latency Sound Effect Manager using Android SoundPool.
 * Sounds are 100% procedurally synthesized in pure Kotlin (CC0):
 * - Pop: Soft bubble pop for tap fill
 * - Click: Subtle tactile click for undo/redo
 * - Chime: Joyful harmonic chime for save success
 */
class SoundEffectManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    enum class Sound {
        POP,
        CLICK,
        CHIME
    }

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Sound, Int>()
    private var isLoaded = false

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        scope.launch {
            try {
                val soundDir = File(context.cacheDir, "sounds").apply { mkdirs() }

                // 1. Synthesize Pop (water bubble pop: 480Hz -> 240Hz pitch drop)
                val popFile = File(soundDir, "pop.wav")
                synthesizePopSound(popFile)
                soundPool?.let { pool ->
                    soundIds[Sound.POP] = pool.load(popFile.absolutePath, 1)
                }

                // 2. Synthesize Click (woodblock click: 880Hz short impulse)
                val clickFile = File(soundDir, "click.wav")
                synthesizeClickSound(clickFile)
                soundPool?.let { pool ->
                    soundIds[Sound.CLICK] = pool.load(clickFile.absolutePath, 1)
                }

                // 3. Synthesize Chime (two-tone chime: 523Hz + 659Hz)
                val chimeFile = File(soundDir, "chime.wav")
                synthesizeChimeSound(chimeFile)
                soundPool?.let { pool ->
                    soundIds[Sound.CHIME] = pool.load(chimeFile.absolutePath, 1)
                }

                isLoaded = true
            } catch (e: Exception) {
                // Graceful degradation if sound fails to initialize
            }
        }
    }

    fun play(sound: Sound) {
        scope.launch {
            try {
                val soundEnabled = settingsDataStore.isSoundEnabled.first()
                if (!soundEnabled) return@launch

                val pool = soundPool ?: return@launch
                val soundId = soundIds[sound] ?: return@launch
                pool.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f)
            } catch (e: Exception) {
                // Ignore audio play errors
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        isLoaded = false
    }

    companion object {
        const val SAMPLE_RATE = 22050

        fun synthesizePopSound(file: File) {
            val numSamples = (SAMPLE_RATE * 0.12).toInt() // 120ms
            val data = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 520.0 - (t / 0.12) * 260.0 // downward sweep
                val env = exp(-t * 28.0) * (1.0 - exp(-t * 100.0))
                val sample = sin(2.0 * PI * freq * t) * env * 22000.0
                data[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
            }
            writeWav(file, data, SAMPLE_RATE)
        }

        fun synthesizeClickSound(file: File) {
            val numSamples = (SAMPLE_RATE * 0.05).toInt() // 50ms
            val data = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 80.0)
                val sample = sin(2.0 * PI * 880.0 * t) * env * 18000.0
                data[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
            }
            writeWav(file, data, SAMPLE_RATE)
        }

        fun synthesizeChimeSound(file: File) {
            val numSamples = (SAMPLE_RATE * 0.45).toInt() // 450ms
            val data = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env1 = exp(-t * 8.0)
                val env2 = if (t > 0.1) exp(-(t - 0.1) * 7.0) else 0.0
                val sample1 = sin(2.0 * PI * 523.25 * t) * env1 * 14000.0
                val sample2 = sin(2.0 * PI * 659.25 * (t - 0.1)) * env2 * 14000.0
                data[i] = (sample1 + sample2).toInt().coerceIn(-32768, 32767).toShort()
            }
            writeWav(file, data, SAMPLE_RATE)
        }

        private fun writeWav(file: File, pcmData: ShortArray, sampleRate: Int) {
            val byteDataLength = pcmData.size * 2
            val totalDataLen = byteDataLength + 36

            FileOutputStream(file).use { fos ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray())
                header.putInt(totalDataLen)
                header.put("WAVE".toByteArray())
                header.put("fmt ".toByteArray())
                header.putInt(16)
                header.putShort(1)
                header.putShort(1)
                header.putInt(sampleRate)
                header.putInt(sampleRate * 2)
                header.putShort(2)
                header.putShort(16)
                header.put("data".toByteArray())
                header.putInt(byteDataLength)
                fos.write(header.array())

                val buf = ByteBuffer.allocate(byteDataLength).order(ByteOrder.LITTLE_ENDIAN)
                for (s in pcmData) buf.putShort(s)
                fos.write(buf.array())
            }
        }
    }
}

