package virtual.camera.app.xposed

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

/**
 * VirtuCam Xposed Module - Phase 2: Video Feed Injection
 * 
 * This module hooks into Camera2 API to replace real camera feed with custom video/image.
 * Works inside virtual spaces like MochiCloner that support Xposed.
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private const val PREFS_NAME = "vcamera_settings"
        private const val KEY_ENABLED = "camera_hook_enabled"
        private const val KEY_VIDEO_PATH = "selected_video_path"
        private const val KEY_TARGET_PACKAGES = "target_packages"
        
        // TEST: Hardcoded video path
        private const val TEST_VIDEO_PATH = "/sdcard/DCIM/Camera/Rot.mp4"
        
        // Store active video players per package
        private val activeMediaPlayers = mutableMapOf<String, MediaPlayer>()
        private val activeSurfaceTextures = mutableMapOf<String, SurfaceTexture>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Don't hook our own app
        if (lpparam.packageName == "virtual.camera.app") {
            return
        }

        try {
            // Hook Camera2 API implementation
            hookCamera2Impl(lpparam)
            
            // Hook legacy Camera API (for older apps)
            hookLegacyCamera(lpparam)
            
            XposedBridge.log("$TAG: Hooked package ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error hooking ${lpparam.packageName}: ${e.message}")
        }
    }

    /**
     * Hook Camera2 API Implementation (Android 5.0+)
     */
    private fun hookCamera2Impl(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook CameraManager.openCamera to intercept camera opening
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
                        try {
                            val cameraId = param.args[0] as String
                            
                            XposedBridge.log("$TAG: Camera $cameraId opening for ${lpparam.packageName}")
                            
                            // TEST: Check if video exists
                            val videoFile = File(TEST_VIDEO_PATH)
                            if (videoFile.exists()) {
                                XposedBridge.log("$TAG: TEST MODE - Video found: $TEST_VIDEO_PATH (${videoFile.length()} bytes)")
                            } else {
                                XposedBridge.log("$TAG: TEST MODE - Video NOT found: $TEST_VIDEO_PATH")
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in openCamera hook: ${e.message}")
                        }
                    }
                }
            )

            // Hook createCaptureSession to inject video surface
            hookCreateCaptureSession(lpparam)

            XposedBridge.log("$TAG: Camera2 API hooked successfully")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook Camera2 API: ${e.message}")
        }
    }

    /**
     * Hook createCaptureSession on all CameraDevice implementations
     */
    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook all createCaptureSession variants
            val hookCallback = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        XposedBridge.log("$TAG: createCaptureSession called for ${lpparam.packageName}")
                        
                        val device = param.thisObject as? CameraDevice ?: return
                        
                        // TEST: Use hardcoded video path
                        val videoFile = File(TEST_VIDEO_PATH)
                        if (!videoFile.exists()) {
                            XposedBridge.log("$TAG: Video file not found: $TEST_VIDEO_PATH")
                            return
                        }

                        XposedBridge.log("$TAG: Replacing camera surfaces with video feed from: $TEST_VIDEO_PATH")
                        
                        // Get the surfaces list
                        val surfaces = param.args[0] as? List<Surface>
                        if (surfaces.isNullOrEmpty()) {
                            XposedBridge.log("$TAG: No surfaces provided to createCaptureSession")
                            return
                        }

                        XposedBridge.log("$TAG: Original surfaces count: ${surfaces.size}")

                        // Create virtual camera surface from video
                        val virtualSurface = createVirtualCameraSurface(TEST_VIDEO_PATH, lpparam.packageName)
                        if (virtualSurface != null) {
                            // Replace surfaces with our virtual surface
                            param.args[0] = listOf(virtualSurface)
                            XposedBridge.log("$TAG: ✅ Successfully injected virtual camera surface!")
                        } else {
                            XposedBridge.log("$TAG: ❌ Failed to create virtual camera surface")
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("$TAG: Error in createCaptureSession hook: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }

            // Try to hook implementation class
            try {
                val implClass = XposedHelpers.findClass(
                    "android.hardware.camera2.impl.CameraDeviceImpl",
                    lpparam.classLoader
                )
                
                // Hook createCaptureSession
                XposedHelpers.findAndHookMethod(
                    implClass,
                    "createCaptureSession",
                    List::class.java,
                    "android.hardware.camera2.CameraCaptureSession\$StateCallback",
                    android.os.Handler::class.java,
                    hookCallback
                )
                
                XposedBridge.log("$TAG: Hooked CameraDeviceImpl.createCaptureSession")
            } catch (e: Exception) {
                XposedBridge.log("$TAG: Could not hook CameraDeviceImpl: ${e.message}")
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error hooking createCaptureSession: ${e.message}")
        }
    }

    /**
     * Create a virtual camera surface from video file
     */
    private fun createVirtualCameraSurface(videoPath: String, packageName: String): Surface? {
        return try {
            XposedBridge.log("$TAG: Creating virtual camera surface...")
            
            // Clean up previous player if exists
            cleanupMediaPlayer(packageName)

            // Create SurfaceTexture for video rendering
            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(1920, 1080)
            
            val surface = Surface(surfaceTexture)
            
            XposedBridge.log("$TAG: Surface created, initializing MediaPlayer...")
            
            // Create MediaPlayer to play video
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(videoPath)
                setSurface(surface)
                isLooping = true // Loop video continuously
                prepare()
                start()
            }
            
            // Store for cleanup
            activeMediaPlayers[packageName] = mediaPlayer
            activeSurfaceTextures[packageName] = surfaceTexture
            
            XposedBridge.log("$TAG: MediaPlayer started successfully, video is playing!")
            surface
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to create virtual camera surface: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Hook legacy Camera API (Android 4.x and some 5.x apps)
     */
    private fun hookLegacyCamera(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cameraClass = XposedHelpers.findClass(
                "android.hardware.Camera",
                lpparam.classLoader
            )

            // Hook Camera.open()
            XposedHelpers.findAndHookMethod(
                cameraClass,
                "open",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            XposedBridge.log("$TAG: Legacy camera opened for ${lpparam.packageName}")
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in legacy camera hook: ${e.message}")
                        }
                    }
                }
            )

            // Hook Camera.setPreviewTexture() to inject video
            XposedHelpers.findAndHookMethod(
                cameraClass,
                "setPreviewTexture",
                SurfaceTexture::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val videoFile = File(TEST_VIDEO_PATH)
                            if (videoFile.exists()) {
                                XposedBridge.log("$TAG: Injecting video into legacy camera preview")
                                
                                // Create custom texture with video feed
                                val customTexture = createLegacyCameraTexture(TEST_VIDEO_PATH, lpparam.packageName)
                                if (customTexture != null) {
                                    param.args[0] = customTexture
                                    XposedBridge.log("$TAG: ✅ Successfully replaced legacy camera texture!")
                                }
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error replacing preview texture: ${e.message}")
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: Legacy Camera API hooked successfully")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook legacy Camera API: ${e.message}")
        }
    }

    /**
     * Create SurfaceTexture with video for legacy camera
     */
    private fun createLegacyCameraTexture(videoPath: String, packageName: String): SurfaceTexture? {
        return try {
            cleanupMediaPlayer(packageName)
            
            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(1920, 1080)
            
            val surface = Surface(surfaceTexture)
            
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(videoPath)
                setSurface(surface)
                isLooping = true
                prepare()
                start()
            }
            
            activeMediaPlayers[packageName] = mediaPlayer
            activeSurfaceTextures[packageName] = surfaceTexture
            
            XposedBridge.log("$TAG: Legacy camera texture created with video")
            surfaceTexture
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to create legacy camera texture: ${e.message}")
            null
        }
    }

    /**
     * Clean up MediaPlayer resources
     */
    private fun cleanupMediaPlayer(packageName: String) {
        try {
            activeMediaPlayers[packageName]?.apply {
                if (isPlaying) stop()
                release()
            }
            activeMediaPlayers.remove(packageName)
            
            activeSurfaceTextures[packageName]?.release()
            activeSurfaceTextures.remove(packageName)
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error cleaning up media player: ${e.message}")
        }
    }
}
