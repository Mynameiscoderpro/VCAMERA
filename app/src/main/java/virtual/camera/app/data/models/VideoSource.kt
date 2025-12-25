package virtual.camera.app.data.models

enum class VideoSource {
    NONE,           // No video source
    IMAGE,          // Static image
    LOCAL_VIDEO,    // Video from storage
    NETWORK_VIDEO,  // Video from URL
    REAL_CAMERA     // Passthrough real camera
}
