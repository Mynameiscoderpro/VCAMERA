package virtual.camera.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import virtual.camera.app.data.models.VideoTransform
import java.io.IOException

class FrameProvider(
    private val context: Context,
    private val videoProcessor: VideoProcessor
) {

    private val TAG = "FrameProvider"
    private var staticImage: Bitmap? = null
    private var currentTransform: VideoTransform = VideoTransform.DEFAULT

    /**
     * Load static image from URI
     */
    fun loadStaticImage(uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                staticImage = BitmapFactory.decodeStream(inputStream)
                Log.d(TAG, "Static image loaded: ${staticImage?.width}x${staticImage?.height}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load static image", e)
        }
    }

    /**
     * Update transformation
     */
    fun updateTransform(transform: VideoTransform) {
        this.currentTransform = transform
        videoProcessor.updateTransform(transform)
    }

    /**
     * Get current frame (processed)
     */
    fun getCurrentFrame(): Bitmap? {
        val source = staticImage ?: return null

        return try {
            videoProcessor.processBitmap(source)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process frame", e)
            source
        }
    }

    /**
     * Get current frame as YUV data
     */
    fun getCurrentFrameYUV(): ByteArray? {
        val frame = getCurrentFrame() ?: return null
        return videoProcessor.bitmapToYUV(frame)
    }

    /**
     * Release resources
     */
    fun release() {
        staticImage?.recycle()
        staticImage = null
    }
}
