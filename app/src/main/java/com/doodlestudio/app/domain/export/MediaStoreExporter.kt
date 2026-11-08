package com.doodlestudio.app.domain.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import android.media.MediaScannerConnection
import androidx.core.content.FileProvider

object MediaStoreExporter {

    /**
     * Checks if target external/internal storage has at least minFreeBytes available.
     */
    fun hasSufficientStorage(context: Context, minFreeBytes: Long = ExportConfig.MIN_FREE_SPACE_BYTES): Boolean {
        return try {
            val path = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = StatFs(path.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= minFreeBytes
        } catch (e: Exception) {
            true // If stat fails, attempt write gracefully
        }
    }

    /**
     * Exports a Bitmap to Pictures/DoodleStudio via MediaStore (or internal fallback on API < 29).
     */
    suspend fun exportImageToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String = "DoodleStudio_${System.currentTimeMillis()}.png"
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DoodleStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return@withContext uri
            }
        } catch (_: Exception) {}

        // Fallback for API < 29 or restricted storage: write to public Pictures directory or externalFilesDir
        try {
            val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DoodleStudio").apply { mkdirs() }
            val targetFile = if (picturesDir.exists() && picturesDir.canWrite()) {
                File(picturesDir, filename)
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir, filename)
            }
            targetFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("image/png"), null)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Exports an existing MP4 file to Movies/DoodleStudio via MediaStore.
     */
    suspend fun exportVideoToGallery(
        context: Context,
        videoFile: File,
        displayName: String = "DoodleStudio_${System.currentTimeMillis()}.mp4"
    ): Uri? = withContext(Dispatchers.IO) {
        if (!videoFile.exists()) return@withContext null
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/DoodleStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        try {
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    FileInputStream(videoFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return@withContext uri
            }
        } catch (_: Exception) {}

        // Fallback for API < 29 or restricted storage: write to public Movies directory or externalFilesDir
        try {
            val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DoodleStudio").apply { mkdirs() }
            val targetFile = if (moviesDir.exists() && moviesDir.canWrite()) {
                File(moviesDir, displayName)
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, displayName)
            }
            targetFile.outputStream().use { outStream ->
                FileInputStream(videoFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("video/mp4"), null)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
        } catch (_: Exception) {
            // Last resort: return file provider for the original file in cache
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
            } catch (_: Exception) {
                null
            }
        }
    }
}

