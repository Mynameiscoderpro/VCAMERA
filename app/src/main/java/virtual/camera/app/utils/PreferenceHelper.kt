package virtual.camera.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import virtual.camera.app.data.models.CameraConfig
import virtual.camera.app.data.models.VideoSource
import virtual.camera.app.data.models.VideoTransform

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vcamera_prefs",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_USER_REMARK = "user_remark"
        private const val KEY_CAMERA_CONFIG = "camera_config"
        private const val KEY_CAMERA_ENABLED = "camera_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_VIDEO_SOURCE = "video_source"
        private const val KEY_VIDEO_URI = "video_uri"
        private const val KEY_VIDEO_TRANSFORM = "video_transform"
    }

    /**
     * User remark/label
     */
    var userRemark: String
        get() = prefs.getString(KEY_USER_REMARK, "My VCamera") ?: "My VCamera"
        set(value) = prefs.edit().putString(KEY_USER_REMARK, value).apply()

    /**
     * Camera configuration
     */
    var cameraConfig: CameraConfig
        get() {
            val json = prefs.getString(KEY_CAMERA_CONFIG, null)
            return if (json != null) {
                try {
                    gson.fromJson(json, CameraConfig::class.java)
                } catch (e: Exception) {
                    CameraConfig.DEFAULT
                }
            } else {
                CameraConfig.DEFAULT
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_CAMERA_CONFIG, json).apply()
        }

    /**
     * Camera enabled state
     */
    var isCameraEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAMERA_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CAMERA_ENABLED, value).apply()

    /**
     * First launch flag
     */
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    /**
     * Video source type
     */
    var videoSource: VideoSource
        get() {
            val ordinal = prefs.getInt(KEY_VIDEO_SOURCE, VideoSource.NONE.ordinal)
            return VideoSource.values().getOrNull(ordinal) ?: VideoSource.NONE
        }
        set(value) = prefs.edit().putInt(KEY_VIDEO_SOURCE, value.ordinal).apply()

    /**
     * Video URI string
     */
    var videoUriString: String?
        get() = prefs.getString(KEY_VIDEO_URI, null)
        set(value) = prefs.edit().putString(KEY_VIDEO_URI, value).apply()

    /**
     * Video transform
     */
    var videoTransform: VideoTransform
        get() {
            val json = prefs.getString(KEY_VIDEO_TRANSFORM, null)
            return if (json != null) {
                try {
                    gson.fromJson(json, VideoTransform::class.java)
                } catch (e: Exception) {
                    VideoTransform.DEFAULT
                }
            } else {
                VideoTransform.DEFAULT
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_VIDEO_TRANSFORM, json).apply()
        }

    /**
     * Clear all preferences
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Save complete camera setup
     */
    fun saveCameraSetup(config: CameraConfig) {
        cameraConfig = config
        isCameraEnabled = config.isEnabled
        videoSource = config.source
        videoUriString = config.sourceUri?.toString()
        videoTransform = config.transform
    }

    /**
     * Load complete camera setup
     */
    fun loadCameraSetup(): CameraConfig {
        return CameraConfig(
            source = videoSource,
            sourceUri = videoUriString?.let { Uri.parse(it) },
            transform = videoTransform,
            isEnabled = isCameraEnabled
        )
    }
}
