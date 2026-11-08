package com.doodlestudio.app.domain.export

import android.net.Uri

data class ExportConfig(
    val width: Int = 720,
    val height: Int = 720,
    val fps: Int = 30,
    val durationSeconds: Int = 12,
    val bitrate: Int = 3_500_000,
    val watermarkText: String = "Created with Doodle Studio",
    val includeWatermark: Boolean = true,
    val includeAudio: Boolean = true
) {
    val totalFrames: Int get() = durationSeconds * fps

    companion object {
        const val MIN_FREE_SPACE_BYTES = 50L * 1024L * 1024L // 50MB
    }
}

sealed class ExportResult {
    data class SuccessVideo(
        val uri: Uri?,
        val filePath: String,
        val durationMs: Long,
        val fileSizeBytes: Long
    ) : ExportResult()

    data class FallbackImage(
        val uri: Uri?,
        val filePath: String,
        val fileSizeBytes: Long,
        val reason: String
    ) : ExportResult()

    data class Error(val message: String) : ExportResult()
}

