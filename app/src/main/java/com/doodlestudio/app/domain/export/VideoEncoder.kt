package com.doodlestudio.app.domain.export

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.view.Surface
import com.doodlestudio.app.domain.model.ActionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hardware-accelerated H.264 (AVC) Video Encoder using MediaCodec + Surface + MediaMuxer.
 * Automatically checks codec availability and falls back gracefully to PNG.
 */
class VideoEncoder(
    private val context: Context,
    private val exportConfig: ExportConfig = ExportConfig()
) {
    /**
     * Checks if the device has a hardware/software H.264 encoder.
     */
    fun isVideoEncoderSupported(): Boolean {
        return try {
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                exportConfig.width,
                exportConfig.height
            )
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            codecList.findEncoderForFormat(format) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encodes an ActionLog into an MP4 Timelapse video.
     * Progress callback provides values from 0.0f to 1.0f.
     */
    suspend fun encodeTimelapse(
        actionLog: ActionLog,
        templateBitmap: Bitmap? = null,
        onProgress: (Float) -> Unit = {}
    ): ExportResult = withContext(Dispatchers.IO) {
        // 1. Storage check
        if (!MediaStoreExporter.hasSufficientStorage(context)) {
            return@withContext exportFallback(
                actionLog = actionLog,
                templateBitmap = templateBitmap,
                reason = "Dung lượng bộ nhớ không đủ (yêu cầu tối thiểu 50MB)"
            )
        }

        // 2. Hardware Codec check
        if (!isVideoEncoderSupported()) {
            return@withContext exportFallback(
                actionLog = actionLog,
                templateBitmap = templateBitmap,
                reason = "Thiết bị không hỗ trợ bộ mã hóa video H.264 (video/avc)"
            )
        }

        val outputDir = File(context.cacheDir, "timelapse").apply { mkdirs() }
        val outputFile = File(outputDir, "doodle_${System.currentTimeMillis()}.mp4")

        try {
            val frameRenderer = TimelapseFrameRenderer(
                actionLog = actionLog,
                templateBitmap = templateBitmap,
                config = exportConfig
            )

            val width = exportConfig.width
            val height = exportConfig.height
            val fps = exportConfig.fps
            val totalFrames = exportConfig.totalFrames

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, exportConfig.bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = codec.createInputSurface()
            codec.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Render loop
            for (f in 0 until totalFrames) {
                frameRenderer.renderFrame(f, frameBitmap)

                // Render frame onto surface canvas
                val surfaceCanvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                surfaceCanvas.drawBitmap(frameBitmap, 0f, 0f, null)
                inputSurface.unlockCanvasAndPost(surfaceCanvas)

                // Drain encoder output
                drainEncoder(codec, muxer, bufferInfo, endOfStream = (f == totalFrames - 1)) { trackIndex ->
                    videoTrackIndex = trackIndex
                    muxerStarted = true
                }

                onProgress((f + 1).toFloat() / totalFrames)
            }

            // Signal EOS
            codec.signalEndOfInputStream()
            drainEncoder(codec, muxer, bufferInfo, endOfStream = true) { /* no-op */ }

            codec.stop()
            codec.release()
            inputSurface.release()

            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }

            // Export to public Gallery MediaStore
            val mediaStoreUri = MediaStoreExporter.exportVideoToGallery(context, outputFile)

            ExportResult.SuccessVideo(
                uri = mediaStoreUri,
                filePath = outputFile.absolutePath,
                durationMs = (exportConfig.durationSeconds * 1000L),
                fileSizeBytes = outputFile.length()
            )
        } catch (e: Exception) {
            // In case of any encoding exception, seamlessly fall back to high-res PNG!
            exportFallback(
                actionLog = actionLog,
                templateBitmap = templateBitmap,
                reason = "Lỗi mã hóa video: ${e.message ?: "Unknown"}. Đã chuyển sang xuất ảnh tĩnh chất lượng cao."
            )
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        onFormatChanged: (Int) -> Unit
    ) {
        val timeoutUs = 10000L
        while (true) {
            val status = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                val trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                onFormatChanged(trackIndex)
            } else if (status >= 0) {
                val outputBuffer = codec.getOutputBuffer(status)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(0, outputBuffer, bufferInfo)
                }
                codec.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private suspend fun exportFallback(
        actionLog: ActionLog,
        templateBitmap: Bitmap?,
        reason: String
    ): ExportResult {
        return try {
            val w = actionLog.canvasWidth
            val h = actionLog.canvasHeight
            val fallbackBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

            val renderer = TimelapseFrameRenderer(
                actionLog = actionLog,
                templateBitmap = templateBitmap,
                config = exportConfig
            )
            // Render the final frame
            renderer.renderFrame(exportConfig.totalFrames - 1, fallbackBitmap)

            val uri = MediaStoreExporter.exportImageToGallery(context, fallbackBitmap)
            val fallbackFile = File(context.cacheDir, "doodle_fallback_${System.currentTimeMillis()}.png").apply {
                outputStream().use { out ->
                    fallbackBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            }

            ExportResult.FallbackImage(
                uri = uri,
                filePath = fallbackFile.absolutePath,
                fileSizeBytes = fallbackFile.length(),
                reason = reason
            )
        } catch (e: Exception) {
            ExportResult.Error("Xuất tác phẩm thất bại: ${e.message}")
        }
    }
}

