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
     * Hook CameraManager instead of abstract CameraDevice
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
                            // TODO: Inject custom camera device here in Phase 2
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in openCamera hook: ${e.message}")
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
                            XposedBridge.log("$TAG: Preview texture being set for ${lpparam.packageName}")
                            // TODO: Replace with custom texture in Phase 2
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: Error in setPreviewTexture hook: ${e.message}")
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
     * Get application context from method parameters
     */
    private fun getContextFromParam(param: XC_MethodHook.MethodHookParam): Context? {
        return try {
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
