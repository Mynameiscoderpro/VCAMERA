package virtual.camera.app.xposed

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
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

/**
 * VirtuCam Xposed Module - Phase 2: Synthetic Video Injection
 * 
 * Generates a synthetic test pattern to replace camera feed.
 * No external storage needed - everything is generated in memory!
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val WIDTH = 1920
        private const val HEIGHT = 1080
        private const val FPS = 30
        
        private val activeRenderers = mutableMapOf<String, SyntheticVideoRenderer>()
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
                        XposedBridge.log("$TAG: 📷 Camera $cameraId opening for ${lpparam.packageName}")
                        XposedBridge.log("$TAG: 🎨 Will inject SYNTHETIC TEST PATTERN")
                    }
                }
            )

            hookCreateCaptureSession(lpparam)
            XposedBridge.log("$TAG: Camera2 API hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook Camera2: ${e.message}")
        }
    }

    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hookCallback = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        XposedBridge.log("$TAG: 🎬 createCaptureSession called")
                        
                        val surfaces = param.args[0] as? List<Surface>
                        if (surfaces.isNullOrEmpty()) {
                            XposedBridge.log("$TAG: ⚠️ No surfaces provided")
                            return
                        }

                        XposedBridge.log("$TAG: 🎨 Creating synthetic video surface...")
                        val virtualSurface = createSyntheticVideoSurface(lpparam.packageName)
                        
                        if (virtualSurface != null) {
                            param.args[0] = listOf(virtualSurface)
                            XposedBridge.log("$TAG: ✅ SYNTHETIC VIDEO INJECTED!")
                        } else {
                            XposedBridge.log("$TAG: ❌ Failed to create synthetic surface")
                        }
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
            XposedBridge.log("$TAG: Error hooking createCaptureSession: ${e.message}")
        }
    }

    /**
     * Create a synthetic video surface with animated test pattern
     */
    private fun createSyntheticVideoSurface(packageName: String): Surface? {
        return try {
            XposedBridge.log("$TAG: 🎨 Generating synthetic video...")
            
            cleanupRenderer(packageName)

            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT)
            val surface = Surface(surfaceTexture)
            
            val renderer = SyntheticVideoRenderer(surface, packageName)
            renderer.start()
            
            activeRenderers[packageName] = renderer
            
            XposedBridge.log("$TAG: ✅ Synthetic video renderer started!")
            surface
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Failed: ${e.message}")
            e.printStackTrace()
            null
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

            XposedHelpers.findAndHookMethod(
                cameraClass,
                "setPreviewTexture",
                SurfaceTexture::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            XposedBridge.log("$TAG: 🎨 Injecting synthetic texture to legacy camera")
                            val customTexture = createLegacyCameraTexture(lpparam.packageName)
                            if (customTexture != null) {
                                param.args[0] = customTexture
                                XposedBridge.log("$TAG: ✅ Legacy camera texture replaced!")
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error: ${e.message}")
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: Legacy Camera API hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook legacy Camera: ${e.message}")
        }
    }

    private fun createLegacyCameraTexture(packageName: String): SurfaceTexture? {
        return try {
            cleanupRenderer(packageName)
            
            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT)
            val surface = Surface(surfaceTexture)
            
            val renderer = SyntheticVideoRenderer(surface, packageName)
            renderer.start()
            
            activeRenderers[packageName] = renderer
            
            surfaceTexture
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to create legacy texture: ${e.message}")
            null
        }
    }

    private fun cleanupRenderer(packageName: String) {
        try {
            activeRenderers[packageName]?.stop()
            activeRenderers.remove(packageName)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Generates animated test pattern (colorful moving bars)
     */
    class SyntheticVideoRenderer(private val surface: Surface, private val tag: String) {
        private var timer: Timer? = null
        private var frameCount = 0
        private val paint = Paint().apply {
            isAntiAlias = true
            textSize = 60f
            textAlign = Paint.Align.CENTER
        }

        fun start() {
            timer = Timer()
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        renderFrame()
                    } catch (e: Exception) {
                        XposedBridge.log("VirtuCam: Render error: ${e.message}")
                    }
                }
            }, 0, 1000L / FPS)
        }

        private fun renderFrame() {
            try {
                val canvas = surface.lockCanvas(null)
                if (canvas != null) {
                    drawTestPattern(canvas)
                    surface.unlockCanvasAndPost(canvas)
                    frameCount++
                }
            } catch (e: Exception) {
                // Surface might be destroyed
            }
        }

        private fun drawTestPattern(canvas: Canvas) {
            // Animated color bars
            val barCount = 7
            val barWidth = WIDTH / barCount
            val colors = arrayOf(
                Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE
            )

            // Rotating colors
            val offset = (frameCount / 10) % barCount
            for (i in 0 until barCount) {
                val colorIndex = (i + offset) % barCount
                paint.color = colors[colorIndex]
                canvas.drawRect(
                    (i * barWidth).toFloat(),
                    0f,
                    ((i + 1) * barWidth).toFloat(),
                    HEIGHT.toFloat(),
                    paint
                )
            }

            // Add text overlay
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
            canvas.drawText(
                "VirtuCam ACTIVE",
                WIDTH / 2f,
                HEIGHT / 2f - 50,
                paint
            )
            canvas.drawText(
                "Synthetic Video Feed",
                WIDTH / 2f,
                HEIGHT / 2f + 50,
                paint
            )
            canvas.drawText(
                "Frame: $frameCount",
                WIDTH / 2f,
                HEIGHT / 2f + 150,
                paint
            )
        }

        fun stop() {
            timer?.cancel()
            timer = null
        }
    }
}
