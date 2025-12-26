package virtual.camera.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Manages video file storage and retrieval
 * Stores video in app's internal storage
 * Xposed module accesses file directly (world-readable file permissions)
 */
class VideoManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "virtucam_prefs"
        private const val KEY_VIDEO_PATH = "video_path"
        private const val VIDEO_FILENAME = "virtual_camera.mp4"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE // Use MODE_PRIVATE instead of deprecated MODE_WORLD_READABLE
    )

    /**
     * Save video from URI to internal storage
     */
    fun saveVideo(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return false

            // Save to internal storage
            val videoFile = getVideoStorageFile()
            val outputStream = FileOutputStream(videoFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Save path to shared preferences
            prefs.edit().putString(KEY_VIDEO_PATH, videoFile.absolutePath).apply()
            
            // Make file world-readable for Xposed access
            // This works because we're setting permissions on the file itself, not SharedPreferences
            try {
                videoFile.setReadable(true, false)
                videoFile.setExecutable(true, false)
            } catch (e: Exception) {
                // Permissions may fail on some devices, but Xposed can still access via root
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get current video file
     */
    fun getVideoFile(): File? {
        val path = prefs.getString(KEY_VIDEO_PATH, null)
        return if (path != null) {
            val file = File(path)
            if (file.exists()) file else null
        } else {
            // Try default location
            val defaultFile = getVideoStorageFile()
            if (defaultFile.exists()) defaultFile else null
        }
    }

    /**
     * Clear video file
     */
    fun clearVideo() {
        val videoFile = getVideoFile()
        videoFile?.delete()
        prefs.edit().remove(KEY_VIDEO_PATH).apply()
    }

    /**
     * Get video storage file location
     */
    private fun getVideoStorageFile(): File {
        // Store in app's files directory
        return File(context.filesDir, VIDEO_FILENAME)
    }

    /**
     * Get video path for Xposed module to read
     */
    fun getVideoPathForXposed(): String {
        return getVideoFile()?.absolutePath 
            ?: "/data/data/virtual.camera.app/files/$VIDEO_FILENAME"
    }
}
