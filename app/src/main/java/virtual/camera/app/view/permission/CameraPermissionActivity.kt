package virtual.camera.app.view.permission

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import virtual.camera.core.service.VirtualCameraService
import virtual.camera.core.CameraConfig

/**
 * Transparent activity to request MediaProjection permission for screen capture.
 * Required for Android 10+ to create virtual display.
 */
class CameraPermissionActivity : AppCompatActivity() {

    // ✅ FIXED: Use modern Activity Result API
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Start service with permission result
            val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(VirtualCameraService.EXTRA_CONFIG, CameraConfig::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(VirtualCameraService.EXTRA_CONFIG)
            } ?: CameraConfig.load(this)

            val serviceIntent = Intent(this, VirtualCameraService::class.java).apply {
                action = VirtualCameraService.ACTION_START
                putExtra(VirtualCameraService.EXTRA_CONFIG, config)
                putExtra("media_projection_data", result.data)
            }
            startForegroundService(serviceIntent)
        } else {
            Toast.makeText(
                this,
                "Camera permission denied. Virtual camera cannot start.",
                Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request media projection permission
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
