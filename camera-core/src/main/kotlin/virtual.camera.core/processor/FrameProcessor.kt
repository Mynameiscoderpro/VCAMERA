package virtual.camera.core.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Size
import virtual.camera.core.CameraConfig

class FrameProcessor {

    fun processFrame(frameData: ByteArray, config: CameraConfig): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
            ?: return frameData

        val matrix = Matrix().apply {
            if (config.rotation != 0) postRotate(config.rotation.toFloat())
            if (config.flipHorizontal) postScale(-1f, 1f)
            if (config.flipVertical) postScale(1f, -1f)
        }

        val transformed = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        val outputStream = java.io.ByteArrayOutputStream()
        transformed.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

        bitmap.recycle()
        transformed.recycle()

        return outputStream.toByteArray()
    }
}