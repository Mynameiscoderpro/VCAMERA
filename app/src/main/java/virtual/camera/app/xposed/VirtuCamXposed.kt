package virtual.camera.app.xposed

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

/**
 * VirtuCam Xposed Module
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
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Don't hook our own app
        if (lpparam.packageName == "virtual.camera.app") {
            return
        }

        try {
            // Hook Camera2 API
            hookCamera2(lpparam)
            
            // Hook legacy Camera API (for older apps)
            hookLegacyCamera(lpparam)
            
            XposedBridge.log("$TAG: Hooked package ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error hooking ${lpparam.packageName}: ${e.message}")
        }
    }

    /**
     * Hook Camera2 API (Android 5.0+)
     */
    private fun hookCamera2(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook CameraDevice.createCaptureSession
            val cameraDeviceClass = XposedHelpers.findClass(
                "android.hardware.camera2.CameraDevice",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                cameraDeviceClass,
                "createCaptureSession",
                List::class.java,
                "android.hardware.camera2.CameraCaptureSession\$StateCallback",
                android.os.Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = getContextFromParam(param)
                            if (context != null && isCameraHookEnabled(context, lpparam.packageName)) {
                                XposedBridge.log("$TAG: Intercepting camera session for ${lpparam.packageName}")
                                // We'll inject our custom surface here
                                val surfaces = param.args[0] as? List<*>
                                surfaces?.let {
                                    replaceSurfacesWithCustomFeed(context, it, param)
                                }
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in camera hook: ${e.message}")
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: Camera2 API hooked successfully")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook Camera2 API: ${e.message}")
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
                            // Store camera instance for later hooking
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in legacy camera hook: ${e.message}")
                        }
                    }
                }
            )

            // Hook Camera.setPreviewTexture()
            XposedHelpers.findAndHookMethod(
                cameraClass,
                "setPreviewTexture",
                SurfaceTexture::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = getContextFromParam(param)
                            if (context != null && isCameraHookEnabled(context, lpparam.packageName)) {
                                XposedBridge.log("$TAG: Replacing preview texture for ${lpparam.packageName}")
                                // Replace with custom texture
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
     * Replace camera surfaces with custom video feed
     */
    private fun replaceSurfacesWithCustomFeed(
        context: Context,
        surfaces: List<*>,
        param: XC_MethodHook.MethodHookParam
    ) {
        try {
            val videoPath = getVideoPath(context)
            if (videoPath.isNullOrEmpty() || !File(videoPath).exists()) {
                XposedBridge.log("$TAG: No valid video path configured")
                return
            }

            XposedBridge.log("$TAG: Using video: $videoPath")
            
            // Create custom surface from video
            // This will be implemented in the next phase
            // For now, we're just logging that we intercepted the camera
            
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error replacing surfaces: ${e.message}")
        }
    }

    /**
     * Get application context from method parameters
     */
    private fun getContextFromParam(param: XC_MethodHook.MethodHookParam): Context? {
        return try {
            // Try to get context from the object instance
            val thisObject = param.thisObject
            XposedHelpers.callMethod(thisObject, "getContext") as? Context
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if camera hook is enabled for this package
     */
    private fun isCameraHookEnabled(context: Context, packageName: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            val enabled = prefs.getBoolean(KEY_ENABLED, false)
            val targetPackages = prefs.getStringSet(KEY_TARGET_PACKAGES, emptySet()) ?: emptySet()
            
            enabled && (targetPackages.isEmpty() || targetPackages.contains(packageName))
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error checking settings: ${e.message}")
            false
        }
    }

    /**
     * Get configured video path from settings
     */
    private fun getVideoPath(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            prefs.getString(KEY_VIDEO_PATH, null)
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error getting video path: ${e.message}")
            null
        }
    }
}
