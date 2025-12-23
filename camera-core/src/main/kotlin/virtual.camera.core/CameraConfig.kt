package virtual.camera.core

import android.os.Parcelable
import android.util.Size
import kotlinx.parcelize.Parcelize
import android.content.Context

@Parcelize
data class CameraConfig(
    val methodType: Int = METHOD_DISABLE,
    val videoSource: String = "",
    val resolution: Size = Size(1920, 1080),
    val aspectRatio: Float = 16f / 9f,
    val enableAudio: Boolean = true,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
) : Parcelable {

    // ✅ FIXED: Merged both companion objects into ONE
    companion object {
        const val METHOD_DISABLE = 0
        const val METHOD_GALLERY_VIDEO = 1
        const val METHOD_NETWORK_STREAM = 2
        private const val PREFS_NAME = "virtual_camera_config"

        // ✅ FIXED: Moved load() into the same companion object
        fun load(context: Context): CameraConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return CameraConfig(
                methodType = prefs.getInt("method_type", METHOD_DISABLE),
                videoSource = prefs.getString("video_source", "") ?: "",
                resolution = Size(
                    prefs.getInt("resolution_width", 1920),
                    prefs.getInt("resolution_height", 1080)
                ),
                aspectRatio = prefs.getFloat("aspect_ratio", 16f / 9f),
                enableAudio = prefs.getBoolean("enable_audio", true),
                rotation = prefs.getInt("rotation", 0),
                flipHorizontal = prefs.getBoolean("flip_horizontal", false),
                flipVertical = prefs.getBoolean("flip_vertical", false)
            )
        }
    }

    // ✅ FIXED: save() is now properly accessible
    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt("method_type", methodType)
            putString("video_source", videoSource)
            putInt("resolution_width", resolution.width)
            putInt("resolution_height", resolution.height)
            putFloat("aspect_ratio", aspectRatio)
            putBoolean("enable_audio", enableAudio)
            putInt("rotation", rotation)
            putBoolean("flip_horizontal", flipHorizontal)
            putBoolean("flip_vertical", flipVertical)
            apply()
        }
    }
}