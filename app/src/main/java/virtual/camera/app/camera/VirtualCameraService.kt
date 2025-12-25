package virtual.camera.app.camera

import android.app.Service
import android.content.Intent
import android.os.IBinder

class VirtualCameraService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Implement camera service logic
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: Cleanup camera resources
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
