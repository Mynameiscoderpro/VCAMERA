package virtual.camera.app.xposed

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.math.sin

/**
 * VirtuCam Xposed Module - Intercepts camera at capture request level
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        
        private val surfaceRenderers = mutableMapOf<Surface, SurfaceRenderer>()
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
            // Hook CameraManager.openCamera
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

            // Hook session creation to track surfaces
            hookCreateCaptureSession(lpparam)
            
            // Hook capture requests
            hookCaptureRequests(lpparam)

            XposedBridge.log("$TAG: Camera2 hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed: ${e.message}")
        }
    }

    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hookCallback = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        // After session is created, start rendering to surfaces
                        val surfaces = param.args[0] as? List<Surface> ?: return
                        
                        XposedBridge.log("$TAG: 🎬 Session created with ${surfaces.size} surfaces")
                        
                        surfaces.forEach { surface ->
                            if (!surfaceRenderers.containsKey(surface)) {
                                val renderer = SurfaceRenderer(surface)
                                renderer.start()
                                surfaceRenderers[surface] = renderer
                                XposedBridge.log("$TAG: ✅ Started rendering to surface")
                            }
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("$TAG: Session hook error: ${e.message}")
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
            XposedBridge.log("$TAG: createCaptureSession error: ${e.message}")
        }
    }

    private fun hookCaptureRequests(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val sessionClass = XposedHelpers.findClass(
                "android.hardware.camera2.impl.CameraCaptureSessionImpl",
                lpparam.classLoader
            )

            // Hook setRepeatingRequest (for preview)
            XposedHelpers.findAndHookMethod(
                sessionClass,
                "setRepeatingRequest",
                CaptureRequest::class.java,
                "android.hardware.camera2.CameraCaptureSession\$CaptureCallback",
                android.os.Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("$TAG: 🔄 Repeating capture request started")
                    }
                }
            )

            XposedBridge.log("$TAG: Capture requests hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Capture hook failed: ${e.message}")
        }
    }

    /**
     * Renders synthetic video to surface continuously
     */
    class SurfaceRenderer(private val surface: Surface) {
        @Volatile
        private var running = false
        private var frameCount = 0
        private val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
        
        private val renderThread = Thread {
            XposedBridge.log("VirtuCam: Render thread started")
            while (running) {
                try {
                    renderFrame()
                    Thread.sleep(33) // 30 FPS
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    if (running) {
                        XposedBridge.log("VirtuCam: Render error: ${e.message}")
                    }
                    break
                }
            }
            XposedBridge.log("VirtuCam: Render thread stopped")
        }

        fun start() {
            if (!running) {
                running = true
                renderThread.start()
            }
        }

        private fun renderFrame() {
            var canvas: Canvas? = null
            try {
                canvas = surface.lockCanvas(null)
                if (canvas != null) {
                    drawFrame(canvas)
                    surface.unlockCanvasAndPost(canvas)
                    frameCount++
                    
                    if (frameCount % 30 == 0) {
                        XposedBridge.log("VirtuCam: 🎨 Frame $frameCount rendered")
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Surface destroyed
                running = false
            } catch (e: Exception) {
                if (running) {
                    XposedBridge.log("VirtuCam: Frame render failed: ${e.message}")
                }
            }
        }

        private fun drawFrame(canvas: Canvas) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val time = frameCount / 30.0
            
            // Animated rainbow background
            val hue = ((time * 30) % 360).toFloat()
            val bgColor = Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.9f))
            canvas.drawColor(bgColor)
            
            // White text with shadow
            paint.color = Color.BLACK
            paint.textSize = width * 0.15f
            paint.style = Paint.Style.FILL
            
            val centerX = width / 2
            val centerY = height / 2
            
            // Shadow
            canvas.drawText("VirtuCam", centerX + 4, centerY - 56, paint)
            
            // Main text
            paint.color = Color.WHITE
            canvas.drawText("VirtuCam", centerX, centerY - 60, paint)
            
            paint.textSize = width * 0.08f
            paint.color = Color.BLACK
            canvas.drawText("ACTIVE", centerX + 3, centerY + 37, paint)
            paint.color = Color.WHITE
            canvas.drawText("ACTIVE", centerX, centerY + 34, paint)
            
            // Frame counter
            paint.textSize = width * 0.05f
            paint.color = Color.WHITE
            canvas.drawText("Frame: $frameCount", centerX, centerY + 100, paint)
            
            // Pulsing circle
            val radius = (width * 0.08f) + (sin(time * 3) * width * 0.02).toFloat()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = width * 0.01f
            canvas.drawCircle(centerX, centerY + 180, radius, paint)
        }

        fun stop() {
            running = false
        }
    }
}
