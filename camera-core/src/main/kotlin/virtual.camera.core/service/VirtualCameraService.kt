package virtual.camera.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat  // ✅ ADDED: Explicit import

class VirtualCameraService : Service() {

    companion object {
        private const val CHANNEL_ID = "virtual_camera_service"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "virtual.camera.action.START"
        const val ACTION_STOP = "virtual.camera.action.STOP"
        const val EXTRA_CONFIG = "config"

        var isRunning: Boolean = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var videoThread: Thread? = null
    private var isVideoPlaying = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startVideoPlayback()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startVideoPlayback() {
        isVideoPlaying = true
        videoThread = Thread {
            while (isVideoPlaying) {
                Thread.sleep(1000) // Placeholder for actual video processing
            }
        }.apply { start() }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, VirtualCameraService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Virtual Camera Running")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Virtual Camera",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        isVideoPlaying = false
        videoThread?.join()
        isRunning = false
    }
}