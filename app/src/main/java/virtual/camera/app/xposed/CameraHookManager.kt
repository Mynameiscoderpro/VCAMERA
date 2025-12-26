package virtual.camera.app.xposed

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager class for VirtuCam Xposed module settings
 * Used by the main app to configure camera hooking behavior
 */
object CameraHookManager {

    private const val PREFS_NAME = "vcamera_settings"
    private const val KEY_ENABLED = "camera_hook_enabled"
    private const val KEY_VIDEO_PATH = "selected_video_path"
    private const val KEY_TARGET_PACKAGES = "target_packages"

    /**
     * Enable or disable camera hooking
     */
    fun setCameraHookEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    /**
     * Check if camera hooking is enabled
     */
    fun isCameraHookEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, false)
    }

    /**
     * Set the video file path to use as camera feed
     */
    fun setVideoPath(context: Context, path: String?) {
        getPrefs(context).edit()
            .putString(KEY_VIDEO_PATH, path)
            .apply()
    }

    /**
     * Get configured video path
     */
    fun getVideoPath(context: Context): String? {
        return getPrefs(context).getString(KEY_VIDEO_PATH, null)
    }

    /**
     * Set target packages to hook (empty = hook all apps)
     */
    fun setTargetPackages(context: Context, packages: Set<String>) {
        getPrefs(context).edit()
            .putStringSet(KEY_TARGET_PACKAGES, packages)
            .apply()
    }

    /**
     * Get target packages
     */
    fun getTargetPackages(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_TARGET_PACKAGES, emptySet()) ?: emptySet()
    }

    /**
     * Add a package to target list
     */
    fun addTargetPackage(context: Context, packageName: String) {
        val current = getTargetPackages(context).toMutableSet()
        current.add(packageName)
        setTargetPackages(context, current)
    }

    /**
     * Remove a package from target list
     */
    fun removeTargetPackage(context: Context, packageName: String) {
        val current = getTargetPackages(context).toMutableSet()
        current.remove(packageName)
        setTargetPackages(context, current)
    }

    /**
     * Clear all target packages (hook all apps)
     */
    fun clearTargetPackages(context: Context) {
        setTargetPackages(context, emptySet())
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
    }
}
