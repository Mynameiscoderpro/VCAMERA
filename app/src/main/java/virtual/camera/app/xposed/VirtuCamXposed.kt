package virtual.camera.app.xposed

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Handler
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

/**
 * VirtuCam Xposed Module - vcamsx Architecture
 * 
 * SIMPLIFIED APPROACH:
 * - Video stored in /data/data/virtual.camera.app/files/virtual_camera.mp4
 * - All hooked apps read from this single location
 * - User selects video once via app UI
 * - Works in MochiCloner/MetaWolf without root
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val VIRTUCAM_PACKAGE = "virtual.camera.app"
        
        // Centralized video storage path
        private const val VIDEO_PATH = "/data/data/virtual.camera.app/files/virtual_camera.mp4"
        
        // Active decoders per package
        private val activeDecoders = mutableMapOf<String, VideoDecoder>()
        
        // Synthetic frame generator for when video file doesn't exist
        private val syntheticGenerators = mutableMapOf<String, SyntheticFrameGenerator>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Don't hook ourselves
        if (lpparam.packageName == VIRTUCAM_PACKAGE) {
            return
        }

        try {
            hookCamera2(lpparam)
            XposedBridge.log("$TAG: ✅ Hooked ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Hook failed for ${lpparam.packageName}: ${e.message}")
        }
    }

    /**
     * Hook Camera2 API at the CaptureCallback level
     */
    private fun hookCamera2(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook camera opening to log and initialize
            hookCameraOpen(lpparam)
            
            // Hook session creation to wrap callbacks
            hookCaptureSession(lpparam)
            
            XposedBridge.log("$TAG: ⚙️ Camera2 hooks installed for ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Camera2 hook error: ${e.message}")
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
                        XposedBridge.log("$TAG: 📷 Camera $cameraId opening in ${lpparam.packageName}")
                        
                        // Initialize video source
                        initializeFrameSource(lpparam.packageName)
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Failed to hook openCamera: ${e.message}")
        }
    }

    private fun hookCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val sessionImplClass = XposedHelpers.findClass(
                "android.hardware.camera2.impl.CameraCaptureSessionImpl",
                lpparam.classLoader
            )

            // Hook setRepeatingRequest (preview stream)
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
                                val wrappedCallback = VirtualCaptureCallback(
                                    originalCallback,
                                    lpparam.packageName
                                )
                                param.args[1] = wrappedCallback
                                
                                XposedBridge.log("$TAG: 🎬 Preview callback wrapped for ${lpparam.packageName}")
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: ❌ Callback wrap error: ${e.message}")
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
                            XposedBridge.log("$TAG: ❌ Single capture wrap error: ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Session hook error: ${e.message}")
        }
    }

    /**
     * Initialize video decoder or synthetic frame generator
     */
    private fun initializeFrameSource(packageName: String) {
        try {
            // Check centralized video location
            val videoFile = File(VIDEO_PATH)
            
            if (videoFile.exists() && videoFile.canRead() && videoFile.length() > 0) {
                val sizeMB = videoFile.length() / (1024 * 1024)
                XposedBridge.log("$TAG: 🎥 Video found: $VIDEO_PATH (${sizeMB}MB)")
                
                // Initialize video decoder
                val decoder = VideoDecoder(VIDEO_PATH)
                if (decoder.initialize()) {
                    decoder.start()
                    activeDecoders[packageName] = decoder
                    XposedBridge.log("$TAG: ✅ Video decoder started for $packageName")
                } else {
                    XposedBridge.log("$TAG: ⚠️ Decoder init failed, using synthetic frames")
                    startSyntheticGenerator(packageName)
                }
            } else {
                XposedBridge.log("$TAG: ⚠️ No video file found at $VIDEO_PATH")
                XposedBridge.log("$TAG: 📝 Please select a video in VirtuCam app")
                XposedBridge.log("$TAG: 🎨 Using synthetic test pattern for now")
                startSyntheticGenerator(packageName)
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Init error: ${e.message}")
            e.printStackTrace()
            startSyntheticGenerator(packageName)
        }
    }

    private fun startSyntheticGenerator(packageName: String) {
        val generator = SyntheticFrameGenerator()
        generator.start()
        syntheticGenerators[packageName] = generator
        XposedBridge.log("$TAG: 🎨 Synthetic frame generator started for $packageName")
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
            originalCallback.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            originalCallback.onCaptureProgressed(session, request, partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            frameCount++
            
            if (frameCount % 30 == 0) {
                XposedBridge.log("$TAG: 🎬 Frame $frameCount delivered to $packageName")
            }
            
            // TODO: Inject virtual frame data here
            // For now, pass through to original callback
            originalCallback.onCaptureCompleted(session, request, result)
        }
    }

    /**
     * Generates synthetic test pattern frames
     */
    class SyntheticFrameGenerator {
        private var running = false
        private var frameCount = 0

        fun start() {
            running = true
            XposedBridge.log("$TAG: 🎨 Synthetic generator initialized")
        }

        fun getNextFrame(): ByteArray? {
            if (!running) return null
            frameCount++
            // TODO: Generate YUV test pattern
            return null
        }

        fun stop() {
            running = false
        }
    }
}
