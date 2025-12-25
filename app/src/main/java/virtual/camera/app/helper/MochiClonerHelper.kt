package virtual.camera.app.helper

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import virtual.camera.core.EnvironmentDetector
import virtual.camera.core.service.VirtualCameraService

/**
 * Helper class for MochiCloner integration
 */
object MochiClonerHelper {

    private const val TAG = "MochiClonerHelper"

    /**
     * Check if app is running inside MochiCloner
     */
    fun isRunningInMochiCloner(): Boolean {
        return EnvironmentDetector.isRunningInMochiCloner()
    }

    /**
     * Show appropriate message based on environment
     */
    fun showEnvironmentInfo(context: Context) {
        val envName = EnvironmentDetector.getVirtualEnvironmentName()

        if (envName != "Native") {
            Toast.makeText(
                context,
                "✓ Running in $envName\nVCamera is optimized for this environment",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "⚠️ Running on native device\nFor best results, use VCamera inside MochiCloner",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Start VirtualCameraService with MochiCloner optimizations
     */
    fun startOptimizedService(context: Context, videoSource: String?) {
        try {
            Log.d(TAG, "Starting optimized service for ${EnvironmentDetector.getVirtualEnvironmentName()}")

            val intent = Intent(context, VirtualCameraService::class.java).apply {
                action = VirtualCameraService.ACTION_START

                // Add video source
                if (videoSource != null) {
                    if (videoSource.startsWith("http")) {
                        putExtra(VirtualCameraService.EXTRA_VIDEO_URL, videoSource)
                    } else {
                        putExtra(VirtualCameraService.EXTRA_VIDEO_PATH, videoSource)
                    }
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(TAG, "Service start command sent")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Get user-friendly instructions based on environment
     */
    fun getSetupInstructions(context: Context): String {
        return if (isRunningInMochiCloner()) {
            """
            ✓ MochiCloner Detected!
            
            HOW TO USE:
            1. Choose a video or enter a URL
            2. Click "Enable Virtual Camera"
            3. Open any app in MochiCloner
            4. The app's camera will show your video!
            
            SUPPORTED APPS:
            • Snapchat
            • Instagram
            • TikTok
            • B612
            • And more!
            """.trimIndent()
        } else {
            """
            ⚠️ Not running in MochiCloner
            
            FOR BEST RESULTS:
            1. Install MochiCloner from Play Store
            2. Clone your target app (e.g., Snapchat)
            3. Open VCamera inside MochiCloner
            4. Configure and enable virtual camera
            
            This ensures the camera feed works properly!
            """.trimIndent()
        }
    }
}
