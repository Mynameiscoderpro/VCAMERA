package virtual.camera.app.xposed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.nio.ByteBuffer
import kotlin.math.sin

/**
 * VirtuCam Xposed Module - Phase 2: Synthetic YUV Video Injection
 * 
 * Generates proper YUV format video for camera surfaces.
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        
        private val activeSurfaces = mutableMapOf<String, VirtualCameraSurface>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "virtual.camera.app") {
            return
        }

        try {
            hookCamera2Impl(lpparam)
            hookLegacyCamera(lpparam)
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
            XposedBridge.log("$TAG: Failed Camera2: ${e.message}")
        }
    }

    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hookCallback = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val surfaces = param.args[0] as? List<Surface> ?: return
                        if (surfaces.isEmpty()) return

                        XposedBridge.log("$TAG: 🎨 Injecting virtual camera...")
                        
                        // Use the FIRST original surface from the app
                        val originalSurface = surfaces[0]
                        
                        // Start rendering to it
                        val virtualCam = VirtualCameraSurface(originalSurface, lpparam.packageName)
                        virtualCam.start()
                        
                        activeSurfaces[lpparam.packageName] = virtualCam
                        
                        XposedBridge.log("$TAG: ✅ Virtual camera rendering started!")
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
            
            XposedBridge.log("$TAG: Hooked createCaptureSession")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error: ${e.message}")
        }
    }

    private fun hookLegacyCamera(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cameraClass = XposedHelpers.findClass(
                "android.hardware.Camera",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                cameraClass,
                "open",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("$TAG: Legacy camera opened")
                    }
                }
            )

            XposedBridge.log("$TAG: Legacy hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Legacy failed: ${e.message}")
        }
    }

    /**
     * Renders synthetic video to camera surface
     */
    class VirtualCameraSurface(private val surface: Surface, private val tag: String) {
        private var running = false
        private var frameCount = 0
        private val paint = Paint().apply {
            isAntiAlias = true
            textSize = 80f
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
        
        private val renderThread = Thread {
            while (running) {
                try {
                    renderFrame()
                    Thread.sleep(33) // ~30 FPS
                } catch (e: Exception) {
                    XposedBridge.log("VirtuCam: Render error: ${e.message}")
                    if (!running) break
                }
            }
        }

        fun start() {
            running = true
            renderThread.start()
        }

        private fun renderFrame() {
            try {
                val canvas = surface.lockCanvas(null)
                canvas?.let {
                    drawColorBars(it)
                    surface.unlockCanvasAndPost(it)
                    frameCount++
                }
            } catch (e: IllegalArgumentException) {
                // Surface destroyed, stop rendering
                running = false
            } catch (e: Exception) {
                XposedBridge.log("VirtuCam: Frame error: ${e.message}")
            }
        }

        private fun drawColorBars(canvas: Canvas) {
            // Solid color background that changes over time
            val time = frameCount / 30.0
            val hue = ((time * 60) % 360).toFloat()
            val color = Color.HSVToColor(floatArrayOf(hue, 0.8f, 0.9f))
            canvas.drawColor(color)
            
            // Draw VirtuCam branding
            paint.color = Color.WHITE
            paint.textSize = 120f
            paint.strokeWidth = 4f
            paint.style = Paint.Style.FILL_AND_STROKE
            
            val centerX = WIDTH / 2f
            val centerY = HEIGHT / 2f
            
            // Main text
            canvas.drawText("VirtuCam", centerX, centerY - 60, paint)
            
            paint.textSize = 60f
            canvas.drawText("ACTIVE", centerX, centerY + 40, paint)
            
            paint.textSize = 40f
            canvas.drawText("Frame: $frameCount", centerX, centerY + 120, paint)
            
            // Animated circle
            val radius = 50f + (sin(time * 2) * 20).toFloat()
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f
            canvas.drawCircle(centerX, centerY + 200, radius, paint)
        }

        fun stop() {
            running = false
        }
    }
}
