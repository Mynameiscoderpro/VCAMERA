package virtual.camera.app.view.permission

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import virtual.camera.core.service.VirtualCameraService
import virtual.camera.core.CameraConfig

/**
 * Transparent activity to request MediaProjection permission for screen capture.
 * Required for Android 10+ to create virtual display.
 */
class CameraPermissionActivity : AppCompatActivity() {

    private val REQUEST_MEDIA_PROJECTION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = intent.getParcelableExtra<CameraConfig>(VirtualCameraService.EXTRA_CONFIG)
            ?: CameraConfig.load(this)

        // Request media projection permission
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            // Start service with permission result
            val serviceIntent = Intent(this, VirtualCameraService::class.java).apply {
                action = VirtualCameraService.ACTION_START
                putExtra(VirtualCameraService.EXTRA_CONFIG, intent.getParcelableExtra(VirtualCameraService.EXTRA_CONFIG))
                putExtra("media_projection_data", data)
            }
            startForegroundService(serviceIntent)
        } else {
            Toast.makeText(this, "Camera permission denied. Virtual camera cannot start.", Toast.LENGTH_LONG).show()
        }

        finish()
    }
}