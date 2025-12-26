package virtual.camera.app.xposed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.nio.ByteBuffer
import kotlin.math.sin

/**
 * VirtuCam Xposed Module - Uses ImageReader for synthetic video
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val WIDTH = 640
        private const val HEIGHT = 480
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "virtual.camera.app") {
            return
        }

        try {
            hookCamera2Impl(lpparam)
            XposedBridge.log("$TAG: ✅ Hooked ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Error: ${e.message}")
        }
    }

    private fun hookCamera2Impl(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cameraManagerClass = XposedHelpers.findClass(
                "android.hardware.camera2.CameraManager",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                cameraManagerClass,
                "openCamera",
                String::class.java,
                "android.hardware.camera2.CameraDevice\$StateCallback",
                android.os.Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val cameraId = param.args[0] as String
                        XposedBridge.log("$TAG: 📷 Camera $cameraId opening")
                    }
                }
            )

            hookCreateCaptureSession(lpparam)

            XposedBridge.log("$TAG: Camera2 hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed: ${e.message}")
        }
    }

    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hookCallback = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val surfaces = param.args[0] as? List<Surface> ?: return
                        if (surfaces.isEmpty()) return

                        XposedBridge.log("$TAG: 🎨 Creating virtual camera with ImageReader...")
                        
                        // Create ImageReader with YUV format (camera native format)
                        val imageReader = ImageReader.newInstance(
                            WIDTH,
                            HEIGHT,
                            ImageFormat.YUV_420_888,
                            2
                        )
                        
                        // Start generating frames
                        val generator = VirtualFrameGenerator(imageReader)
                        generator.start()
                        
                        // Replace the app's surface with our ImageReader surface
                        val virtualSurface = imageReader.surface
                        param.args[0] = listOf(virtualSurface)
                        
                        XposedBridge.log("$TAG: ✅ Virtual camera surface injected!")
                    } catch (e: Exception) {
                        XposedBridge.log("$TAG: ❌ Error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }

            val implClass = XposedHelpers.findClass(
                "android.hardware.camera2.impl.CameraDeviceImpl",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                implClass,
                "createCaptureSession",
                List::class.java,
                "android.hardware.camera2.CameraCaptureSession\$StateCallback",
                android.os.Handler::class.java,
                hookCallback
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Hook error: ${e.message}")
        }
    }

    /**
     * Generates synthetic YUV frames
     */
    class VirtualFrameGenerator(private val imageReader: ImageReader) {
        @Volatile
        private var running = false
        private var frameCount = 0
        
        private val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        private val canvas = Canvas(bitmap)
        private val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
        
        private val generatorThread = Thread {
            XposedBridge.log("VirtuCam: 🎬 Frame generator started")
            
            while (running) {
                try {
                    generateAndSendFrame()
                    Thread.sleep(33) // 30 FPS
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    XposedBridge.log("VirtuCam: Frame error: ${e.message}")
                }
            }
            
            XposedBridge.log("VirtuCam: 🛑 Frame generator stopped")
        }

        fun start() {
            running = true
            generatorThread.start()
        }

        private fun generateAndSendFrame() {
            try {
                // Draw synthetic frame to bitmap
                drawFrame(canvas)
                
                // Get image from ImageReader
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    // Convert bitmap to YUV and write to image
                    bitmapToYuv(bitmap, image)
                    image.close()
                    
                    frameCount++
                    
                    if (frameCount % 30 == 0) {
                        XposedBridge.log("VirtuCam: ✅ Frame $frameCount sent")
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    XposedBridge.log("VirtuCam: Send error: ${e.message}")
                }
            }
        }

        private fun drawFrame(canvas: Canvas) {
            val time = frameCount / 30.0
            
            // Animated rainbow background
            val hue = ((time * 60) % 360).toFloat()
            val bgColor = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.95f))
            canvas.drawColor(bgColor)
            
            val centerX = WIDTH / 2f
            val centerY = HEIGHT / 2f
            
            // Main title with shadow
            paint.textSize = 80f
            paint.color = Color.BLACK
            canvas.drawText("VirtuCam", centerX + 3, centerY - 47, paint)
            paint.color = Color.WHITE
            canvas.drawText("VirtuCam", centerX, centerY - 50, paint)
            
            // Status
            paint.textSize = 40f
            paint.color = Color.BLACK
            canvas.drawText("ACTIVE", centerX + 2, centerY + 23, paint)
            paint.color = Color.WHITE
            canvas.drawText("ACTIVE", centerX, centerY + 20, paint)
            
            // Frame counter
            paint.textSize = 30f
            paint.color = Color.WHITE
            canvas.drawText("Frame: $frameCount", centerX, centerY + 70, paint)
            
            // Pulsing circle
            val radius = 30f + (sin(time * 3) * 10).toFloat()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.WHITE
            canvas.drawCircle(centerX, centerY + 120, radius, paint)
            paint.style = Paint.Style.FILL
        }

        /**
         * Convert ARGB bitmap to YUV_420_888 format
         */
        private fun bitmapToYuv(bitmap: Bitmap, image: Image) {
            val width = bitmap.width
            val height = bitmap.height
            
            val planes = image.planes
            val yPlane = planes[0].buffer
            val uPlane = planes[1].buffer
            val vPlane = planes[2].buffer
            
            val yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            var yIndex = 0
            var uvIndex = 0
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[y * width + x]
                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff
                    
                    // Convert RGB to YUV
                    val yValue = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                    
                    yPlane.put(yIndex, yValue.toByte())
                    yIndex++
                    
                    // Sample U and V every 2x2 pixels
                    if (y % 2 == 0 && x % 2 == 0) {
                        val uValue = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                        val vValue = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                        
                        uPlane.put(uvIndex, uValue.toByte())
                        vPlane.put(uvIndex, vValue.toByte())
                        uvIndex += uvPixelStride
                    }
                }
            }
        }

        fun stop() {
            running = false
        }
    }
}
