package virtual.camera.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Manages video file storage and retrieval
 * Stores video in app's internal storage and shares path via SharedPreferences
 */
class VideoManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "virtucam_prefs"
        private const val KEY_VIDEO_PATH = "video_path"
        private const val VIDEO_FILENAME = "virtual_camera.mp4"
        
        // Shared preferences accessible by Xposed module
        private const val SHARED_PREFS_PATH = "/data/data/virtual.camera.app/shared_prefs/virtucam_prefs.xml"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_WORLD_READABLE // Accessible by Xposed module
    )

    /**
     * Save video from URI to internal storage
     */
    fun saveVideo(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return false

            // Save to internal storage (accessible to all apps via Xposed)
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
            videoFile.setReadable(true, false)

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
        // Store in app's files directory (accessible via Xposed)
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
