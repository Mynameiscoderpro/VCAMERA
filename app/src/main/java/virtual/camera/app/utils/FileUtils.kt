package virtual.camera.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

object FileUtils {

    /**
     * Get file name from URI
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    /**
     * Get file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var result: Long = 0
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) {
                        result = cursor.getLong(index)
                    }
                }
            }
        } else if (uri.scheme == "file") {
            uri.path?.let { path ->
                result = File(path).length()
            }
        }
        return result
    }

    /**
     * Format file size to human readable string
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(
            size / Math.pow(1024.0, digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    /**
     * Copy file from URI to destination
     */
    fun copyFile(context: Context, sourceUri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if file is video
     */
    fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp", "webm")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in videoExtensions
    }

    /**
     * Check if file is image
     */
    fun isImageFile(fileName: String): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    /**
     * Delete file
     */
    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Create directory if not exists
     */
    fun ensureDirectory(dir: File): Boolean {
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }
}
