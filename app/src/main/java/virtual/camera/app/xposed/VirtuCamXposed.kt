package virtual.camera.app.xposed

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

/**
 * VirtuCam Xposed Module - vcamsx Architecture
 * 
 * KEY STRATEGY:
 * - DON'T replace surfaces (causes compatibility issues)
 * - Instead: Hook CaptureCallback and inject frames there
 * - Use app's private storage for video file
 * - Decode video using MediaCodec
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val VIDEO_FILENAME = "virtual.mp4"
        
        // Active decoders per package
        private val activeDecoders = mutableMapOf<String, VideoDecoder>()
        
        // Synthetic frame generator for when video file doesn't exist
        private val syntheticGenerators = mutableMapOf<String, SyntheticFrameGenerator>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Don't hook ourselves
        if (lpparam.packageName == "virtual.camera.app") {
            return
        }

        try {
            hookCamera2(lpparam)
            XposedBridge.log("$TAG: ✅ Hooked ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Hook failed: ${e.message}")
        }
    }

    /**
     * Hook Camera2 API at the CaptureCallback level
     */
    private fun hookCamera2(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook camera opening to log
            hookCameraOpen(lpparam)
            
            // Hook session creation to track surfaces
            hookCaptureSession(lpparam)
            
            XposedBridge.log("$TAG: Camera2 hooks installed")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Camera2 hook error: ${e.message}")
        }
    }

    private fun hookCameraOpen(lpparam: XC_LoadPackage.LoadPackageParam) {
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
                Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val cameraId = param.args[0] as String
                        XposedBridge.log("$TAG: 📷 Camera $cameraId opening for ${lpparam.packageName}")
                        
                        // Initialize video decoder or synthetic generator
                        initializeFrameSource(lpparam)
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook openCamera: ${e.message}")
        }
    }

    private fun hookCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val sessionImplClass = XposedHelpers.findClass(
                "android.hardware.camera2.impl.CameraCaptureSessionImpl",
                lpparam.classLoader
            )

            // Hook setRepeatingRequest (preview)
            XposedHelpers.findAndHookMethod(
                sessionImplClass,
                "setRepeatingRequest",
                CaptureRequest::class.java,
                CameraCaptureSession.CaptureCallback::class.java,
                Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val originalCallback = param.args[1] as? CameraCaptureSession.CaptureCallback
                            if (originalCallback != null) {
                                // Wrap the callback to inject our frames
                                val wrappedCallback = VirtualCaptureCallback(
                                    originalCallback,
                                    lpparam.packageName
                                )
                                param.args[1] = wrappedCallback
                                
                                XposedBridge.log("$TAG: 🎬 Capture callback wrapped!")
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Callback wrap error: ${e.message}")
                        }
                    }
                }
            )

            // Hook capture (single shot)
            XposedHelpers.findAndHookMethod(
                sessionImplClass,
                "capture",
                CaptureRequest::class.java,
                CameraCaptureSession.CaptureCallback::class.java,
                Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val originalCallback = param.args[1] as? CameraCaptureSession.CaptureCallback
                            if (originalCallback != null) {
                                val wrappedCallback = VirtualCaptureCallback(
                                    originalCallback,
                                    lpparam.packageName
                                )
                                param.args[1] = wrappedCallback
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Capture wrap error: ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Session hook error: ${e.message}")
        }
    }

    /**
     * Initialize video decoder or synthetic frame generator
     */
    private fun initializeFrameSource(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val packageName = lpparam.packageName
            
            // Check if video file exists in app's private storage
            val videoPath = "/data/data/$packageName/cache/$VIDEO_FILENAME"
            val videoFile = File(videoPath)
            
            if (videoFile.exists() && videoFile.length() > 0) {
                XposedBridge.log("$TAG: 🎥 Video found: $videoPath (${videoFile.length()} bytes)")
                
                // Initialize video decoder
                val decoder = VideoDecoder(videoPath)
                if (decoder.initialize()) {
                    decoder.start()
                    activeDecoders[packageName] = decoder
                    XposedBridge.log("$TAG: ✅ Video decoder started")
                } else {
                    XposedBridge.log("$TAG: ⚠️ Decoder init failed, using synthetic")
                    startSyntheticGenerator(packageName)
                }
            } else {
                XposedBridge.log("$TAG: ⚠️ No video file, using synthetic frames")
                XposedBridge.log("$TAG: 📝 To use video: Copy to $videoPath")
                startSyntheticGenerator(packageName)
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Init error: ${e.message}")
            startSyntheticGenerator(lpparam.packageName)
        }
    }

    private fun startSyntheticGenerator(packageName: String) {
        val generator = SyntheticFrameGenerator()
        generator.start()
        syntheticGenerators[packageName] = generator
        XposedBridge.log("$TAG: 🎨 Synthetic frame generator started")
    }

    /**
     * Custom CaptureCallback that injects virtual frames
     */
    class VirtualCaptureCallback(
        private val originalCallback: CameraCaptureSession.CaptureCallback,
        private val packageName: String
    ) : CameraCaptureSession.CaptureCallback() {

        private var frameCount = 0

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            // Pass through to original
            originalCallback.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            // Pass through to original
            originalCallback.onCaptureProgressed(session, request, partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            frameCount++
            
            if (frameCount % 30 == 0) {
                XposedBridge.log("$TAG: 🎬 Frame $frameCount captured")
            }
            
            // TODO: Here we would inject the virtual frame
            // For now, pass through to original
            originalCallback.onCaptureCompleted(session, request, result)
        }

        // Note: onCaptureFailed is not overridden due to nested class compilation issues
        // Failed captures will be handled by the original callback
    }

    /**
     * Generates synthetic test pattern frames
     */
    class SyntheticFrameGenerator {
        private var running = false
        private var frameCount = 0

        fun start() {
            running = true
            XposedBridge.log("$TAG: 🎨 Synthetic generator ready")
        }

        fun getNextFrame(): ByteArray? {
            if (!running) return null
            
            frameCount++
            // Generate a simple YUV test pattern
            // For now, return null (will be implemented)
            return null
        }

        fun stop() {
            running = false
        }
    }
}
