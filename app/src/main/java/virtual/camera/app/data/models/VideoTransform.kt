package virtual.camera.app.data.models

data class VideoTransform(
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
) {
    companion object {
        val DEFAULT = VideoTransform()
    }
}
