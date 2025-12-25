package virtual.camera.app.camera

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import virtual.camera.app.R
import virtual.camera.app.data.models.CameraConfig
import virtual.camera.app.data.models.VideoSource
import virtual.camera.app.ui.MainActivity

class VirtualCameraService : Service() {

    private val TAG = "VirtualCameraService"
    private val CHANNEL_ID = "virtual_camera_channel"
    private val NOTIFICATION_ID = 1001

    private var exoPlayer: ExoPlayer? = null
    private var videoProcessor: VideoProcessor? = null
    private var frameProvider: FrameProvider? = null
    private var cameraConfig: CameraConfig = CameraConfig.DEFAULT
    private var isActive = false

    companion object {
        private var INSTANCE: VirtualCameraService? = null

        fun getInstance(): VirtualCameraService? = INSTANCE

        fun startService(context: Context, config: CameraConfig) {
            val intent = Intent(context, VirtualCameraService::class.java).apply {
                putExtra("action", "start")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VirtualCameraService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        Log.d(TAG, "Service created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        initializeComponents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        isActive = true
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isActive = false
        cleanup()
        INSTANCE = null
    }

    private fun initializeComponents() {
        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }

        // Initialize processors
        videoProcessor = VideoProcessor(this)
        frameProvider = FrameProvider(this, videoProcessor!!)
    }

    /**
     * Update camera configuration
     */
    fun updateConfig(config: CameraConfig) {
        Log.d(TAG, "Updating camera config: ${config.source}")
        this.cameraConfig = config

        when (config.source) {
            VideoSource.LOCAL_VIDEO -> {
                config.sourceUri?.let { loadVideo(it) }
            }
            VideoSource.NETWORK_VIDEO -> {
                config.sourceUri?.let { loadVideo(it) }
            }
            VideoSource.IMAGE -> {
                config.sourceUri?.let { loadImage(it) }
            }
            VideoSource.REAL_CAMERA -> {
                // Passthrough mode
                stopPlayer()
            }
            VideoSource.NONE -> {
                stopPlayer()
            }
        }

        frameProvider?.updateTransform(config.transform)
    }

    /**
     * Get current camera frame
     */
    fun getCurrentFrame(): Bitmap? {
        return frameProvider?.getCurrentFrame()
    }

    private fun loadVideo(uri: Uri) {
        try {
            exoPlayer?.apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                play()
            }
            Log.d(TAG, "Video loaded: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load video", e)
        }
    }

    private fun loadImage(uri: Uri) {
        try {
            frameProvider?.loadStaticImage(uri)
            Log.d(TAG, "Image loaded: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image", e)
        }
    }

    private fun stopPlayer() {
        exoPlayer?.stop()
    }

    private fun cleanup() {
        exoPlayer?.release()
        exoPlayer = null
        videoProcessor = null
        frameProvider = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Virtual Camera Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls virtual camera feed"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VCamera Active")
            .setContentText("Virtual camera is running")
            .setSmallIcon(R.drawable.ic_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun isServiceActive(): Boolean = isActive
}
