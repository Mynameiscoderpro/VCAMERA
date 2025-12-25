package virtual.camera.core

import android.content.Context
import android.util.Log

/**
 * Helper class for optimizing VCamera behavior in virtual environments
 */
object VirtualEnvironmentHelper {

    private const val TAG = "VirtualEnvHelper"

    /**
     * Check if camera hooks need special handling in virtual environment
     */
    fun needsVirtualEnvironmentOptimization(context: Context): Boolean {
        val isVirtual = EnvironmentDetector.isRunningInVirtualEnvironment(context)
        Log.d(TAG, "Needs virtual optimization: $isVirtual")
        return isVirtual
    }

    /**
     * Get recommended camera API for current environment
     */
    fun getRecommendedCameraApi(context: Context): CameraApiType {
        return if (EnvironmentDetector.isRunningInVirtualEnvironment(context)) {
            // Use Camera1 API in virtual environments (more compatible)
            CameraApiType.CAMERA1
        } else {
            // Use Camera2 API on native devices (better performance)
            CameraApiType.CAMERA2
        }
    }

    /**
     * Get optimized video codec settings for virtual environment
     */
    fun getOptimizedCodecSettings(context: Context): VideoCodecSettings {
        return if (EnvironmentDetector.isRunningInVirtualEnvironment(context)) {
            // Lower quality settings for better compatibility in virtual environments
            VideoCodecSettings(
                width = 1280,
                height = 720,
                fps = 30,
                bitrate = 2_000_000 // 2 Mbps
            )
        } else {
            // Higher quality for native devices
            VideoCodecSettings(
                width = 1920,
                height = 1080,
                fps = 60,
                bitrate = 5_000_000 // 5 Mbps
            )
        }
    }

    /**
     * Check if we need to use aggressive camera hooking
     */
    fun shouldUseAggressiveHooking(context: Context): Boolean {
        // MochiCloner requires more aggressive hooking
        return EnvironmentDetector.isRunningInMochiCloner()
    }

    /**
     * Get startup delay for service (some virtual environments need delay)
     */
    fun getServiceStartupDelay(context: Context): Long {
        return if (EnvironmentDetector.isRunningInVirtualEnvironment(context)) {
            2000L // 2 seconds delay for virtual environments
        } else {
            500L // 0.5 seconds for native
        }
    }

    enum class CameraApiType {
        CAMERA1,
        CAMERA2,
        CAMERAX
    }

    data class VideoCodecSettings(
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrate: Int
    )
}
