package virtual.camera.app.data.models

import android.net.Uri

data class CameraConfig(
    val source: VideoSource = VideoSource.NONE,
    val sourceUri: Uri? = null,
    val sourcePath: String = "",
    val transform: VideoTransform = VideoTransform.DEFAULT,
    val isEnabled: Boolean = false,
    val loopVideo: Boolean = true,
    val maintainAspectRatio: Boolean = true,
    val targetWidth: Int = 1920,
    val targetHeight: Int = 1080,
    val targetFps: Int = 30
) {
    companion object {
        val DEFAULT = CameraConfig()
    }
}
