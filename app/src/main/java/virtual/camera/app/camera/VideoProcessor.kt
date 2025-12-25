package virtual.camera.app.camera

import android.content.Context
import android.graphics.*
import virtual.camera.app.data.models.VideoTransform

class VideoProcessor(private val context: Context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private var currentTransform: VideoTransform = VideoTransform.DEFAULT

    /**
     * Update transformation parameters
     */
    fun updateTransform(transform: VideoTransform) {
        this.currentTransform = transform
    }

    /**
     * Apply transformations to bitmap
     */
    fun processBitmap(source: Bitmap): Bitmap {
        if (currentTransform == VideoTransform.DEFAULT) {
            return source
        }

        // Calculate output dimensions
        val width = source.width
        val height = source.height

        // Create transformation matrix
        matrix.reset()

        // Apply translation (center pivot point)
        matrix.postTranslate(-width / 2f, -height / 2f)

        // Apply rotation
        if (currentTransform.rotation != 0f) {
            matrix.postRotate(currentTransform.rotation)
        }

        // Apply scale
        if (currentTransform.scaleX != 1f || currentTransform.scaleY != 1f) {
            matrix.postScale(currentTransform.scaleX, currentTransform.scaleY)
        }

        // Apply flip
        if (currentTransform.flipHorizontal || currentTransform.flipVertical) {
            val flipX = if (currentTransform.flipHorizontal) -1f else 1f
            val flipY = if (currentTransform.flipVertical) -1f else 1f
            matrix.postScale(flipX, flipY)
        }

        // Translate back and apply custom translation
        matrix.postTranslate(
            width / 2f + currentTransform.translateX,
            height / 2f + currentTransform.translateY
        )

        // Create output bitmap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Apply brightness/contrast/saturation
        val colorMatrix = ColorMatrix()

        // Brightness
        if (currentTransform.brightness != 0f) {
            colorMatrix.setScale(
                1f + currentTransform.brightness,
                1f + currentTransform.brightness,
                1f + currentTransform.brightness,
                1f
            )
        }

        // Contrast
        if (currentTransform.contrast != 1f) {
            val contrast = currentTransform.contrast
            val translate = (1f - contrast) / 2f * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastMatrix)
        }

        // Saturation
        if (currentTransform.saturation != 1f) {
            val saturationMatrix = ColorMatrix()
            saturationMatrix.setSaturation(currentTransform.saturation)
            colorMatrix.postConcat(saturationMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        // Draw transformed bitmap
        canvas.drawBitmap(source, matrix, paint)

        return output
    }

    /**
     * Resize bitmap maintaining aspect ratio
     */
    fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int, maintainAspectRatio: Boolean): Bitmap {
        if (!maintainAspectRatio) {
            return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        }

        val sourceRatio = source.width.toFloat() / source.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        val width: Int
        val height: Int

        if (sourceRatio > targetRatio) {
            // Source is wider
            width = targetWidth
            height = (targetWidth / sourceRatio).toInt()
        } else {
            // Source is taller
            height = targetHeight
            width = (targetHeight * sourceRatio).toInt()
        }

        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    /**
     * Convert bitmap to YUV format for camera
     */
    fun bitmapToYUV(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        encodeYUV420SP(yuv, pixels, width, height)

        return yuv
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height

        var yIndex = 0
        var uvIndex = frameSize

        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int

        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                R = argb[index] and 0xff0000 shr 16
                G = argb[index] and 0xff00 shr 8
                B = argb[index] and 0xff

                // Convert to YUV
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()

                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                }

                index++
            }
        }
    }
}
