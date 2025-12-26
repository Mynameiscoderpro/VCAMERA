package virtual.camera.app.xposed

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.media.MediaPlayer
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
 * This module hooks into Camera2 API to replace real camera feed with custom video.
 * Works inside virtual spaces like MochiCloner that support Xposed.
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        
        // Try multiple video paths (in order of preference)
        private val TEST_VIDEO_PATHS = listOf(
            "/sdcard/DCIM/Camera/Rot.mp4",
            "/sdcard/Download/Rot.mp4",
            "/sdcard/Movies/Rot.mp4",
            "/storage/emulated/0/DCIM/Camera/Rot.mp4",
            "/storage/emulated/0/Download/Rot.mp4",
            "/data/local/tmp/Rot.mp4"
        )
        
        // Store active video players per package
        private val activeMediaPlayers = mutableMapOf<String, MediaPlayer>()
        private val activeSurfaceTextures = mutableMapOf<String, SurfaceTexture>()
        
        // Cache the working video path
        private var workingVideoPath: String? = null
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
     * Find the first accessible video path
     */
    private fun findAccessibleVideoPath(): String? {
        if (workingVideoPath != null) {
            return workingVideoPath
        }
        
        for (path in TEST_VIDEO_PATHS) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    XposedBridge.log("$TAG: Found accessible video at: $path (${file.length()} bytes)")
                    workingVideoPath = path
                    return path
                }
            } catch (e: Exception) {
                // Try next path
            }
        }
        
        XposedBridge.log("$TAG: No accessible video found. Tried: ${TEST_VIDEO_PATHS.joinToString()}")
        return null
    }

    /**
     * Hook Camera2 API Implementation (Android 5.0+)
     */
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
                        try {
                            val cameraId = param.args[0] as String
                            XposedBridge.log("$TAG: Camera $cameraId opening for ${lpparam.packageName}")
                            
                            // Try to find accessible video
                            val videoPath = findAccessibleVideoPath()
                            if (videoPath != null) {
                                XposedBridge.log("$TAG: Will inject video: $videoPath")
                            } else {
                                XposedBridge.log("$TAG: No accessible video found, using real camera")
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in openCamera hook: ${e.message}")
                        }
                    }
                }
            )

            // Hook createCaptureSession
            hookCreateCaptureSession(lpparam)

            XposedBridge.log("$TAG: Camera2 API hooked successfully")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook Camera2 API: ${e.message}")
        }
    }

    /**
     * Hook createCaptureSession
     */
    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hookCallback = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        XposedBridge.log("$TAG: createCaptureSession called")
                        
                        val videoPath = findAccessibleVideoPath()
                        if (videoPath == null) {
                            XposedBridge.log("$TAG: No video available, using real camera")
                            return
                        }

                        val surfaces = param.args[0] as? List<Surface>
                        if (surfaces.isNullOrEmpty()) {
                            XposedBridge.log("$TAG: No surfaces provided")
                            return
                        }

                        XposedBridge.log("$TAG: Creating virtual camera with video: $videoPath")

                        // Create virtual surface
                        val virtualSurface = createVirtualCameraSurface(videoPath, lpparam.packageName)
                        if (virtualSurface != null) {
                            param.args[0] = listOf(virtualSurface)
                            XposedBridge.log("$TAG: ✅ Virtual camera surface injected!")
                        } else {
                            XposedBridge.log("$TAG: ❌ Failed to create virtual surface")
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("$TAG: Error in createCaptureSession: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }

            try {
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
                XposedBridge.log("$TAG: Could not hook createCaptureSession: ${e.message}")
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error hooking createCaptureSession: ${e.message}")
        }
    }

    /**
     * Create virtual camera surface with video
     */
    private fun createVirtualCameraSurface(videoPath: String, packageName: String): Surface? {
        return try {
            XposedBridge.log("$TAG: Creating MediaPlayer for: $videoPath")
            
            cleanupMediaPlayer(packageName)

            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(1920, 1080)
            val surface = Surface(surfaceTexture)
            
            XposedBridge.log("$TAG: Surface created, starting MediaPlayer...")
            
            val mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(videoPath)
                    setSurface(surface)
                    isLooping = true
                    prepare()
                    start()
                    XposedBridge.log("$TAG: ✅ MediaPlayer started successfully!")
                } catch (e: Exception) {
                    XposedBridge.log("$TAG: MediaPlayer error: ${e.message}")
                    throw e
                }
            }
            
            activeMediaPlayers[packageName] = mediaPlayer
            activeSurfaceTextures[packageName] = surfaceTexture
            
            surface
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to create virtual surface: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Hook legacy Camera API
     */
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
                            val videoPath = findAccessibleVideoPath()
                            if (videoPath != null) {
                                XposedBridge.log("$TAG: Injecting video into legacy camera")
                                val customTexture = createLegacyCameraTexture(videoPath, lpparam.packageName)
                                if (customTexture != null) {
                                    param.args[0] = customTexture
                                    XposedBridge.log("$TAG: ✅ Legacy camera texture replaced!")
                                }
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in legacy camera hook: ${e.message}")
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: Legacy Camera API hooked")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook legacy Camera: ${e.message}")
        }
    }

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
            
            surfaceTexture
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to create legacy texture: ${e.message}")
            null
        }
    }

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
            // Ignore cleanup errors
        }
    }
}
