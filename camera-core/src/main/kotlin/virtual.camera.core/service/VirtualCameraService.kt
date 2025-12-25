package virtual.camera.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import virtual.camera.core.EnvironmentDetector
import virtual.camera.core.VirtualEnvironmentHelper

class VirtualCameraService : Service() {

    companion object {
        private const val TAG = "VirtualCameraService"
        private const val CHANNEL_ID = "virtual_camera_service"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "virtual.camera.action.START"
        const val ACTION_STOP = "virtual.camera.action.STOP"
        const val ACTION_UPDATE_VIDEO = "virtual.camera.action.UPDATE_VIDEO"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_VIDEO_PATH = "video_path"
        const val EXTRA_VIDEO_URL = "video_url"

        var isRunning: Boolean = false
        var currentVideoSource: String? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var videoThread: Thread? = null
    private var isVideoPlaying = false
    private var isInVirtualEnvironment = false
    private var virtualEnvironmentName = "Unknown"

    override fun onCreate() {
        super.onCreate()

        // Detect virtual environment
        isInVirtualEnvironment = EnvironmentDetector.isRunningInVirtualEnvironment(this)
        virtualEnvironmentName = EnvironmentDetector.getVirtualEnvironmentName()

        Log.d(TAG, "=== VirtualCameraService Created ===")
        Log.d(TAG, "Virtual Environment: $virtualEnvironmentName")
        Log.d(TAG, "Is Virtual: $isInVirtualEnvironment")

        EnvironmentDetector.logEnvironmentInfo()

        isRunning = true
        createNotificationChannel()

        // Apply startup delay if in virtual environment
        if (isInVirtualEnvironment) {
            val delay = VirtualEnvironmentHelper.getServiceStartupDelay(this)
            Log.d(TAG, "Applying startup delay: ${delay}ms for $virtualEnvironmentName")
            Thread.sleep(delay)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        try {
            when (intent?.action) {
                ACTION_START -> {
                    // Extract video source
                    currentVideoSource = intent.getStringExtra(EXTRA_VIDEO_PATH)
                        ?: intent.getStringExtra(EXTRA_VIDEO_URL)

                    Log.d(TAG, "Starting service with video source: $currentVideoSource")

                    // Start foreground with proper handling
                    startForegroundWithCompatibility()

                    // Start video playback
                    startVideoPlayback()

                    Log.d(TAG, "Service started successfully in $virtualEnvironmentName")
                }

                ACTION_UPDATE_VIDEO -> {
                    // Update video source without restarting service
                    currentVideoSource = intent.getStringExtra(EXTRA_VIDEO_PATH)
                        ?: intent.getStringExtra(EXTRA_VIDEO_URL)

                    Log.d(TAG, "Updating video source: $currentVideoSource")

                    // Restart video playback with new source
                    stopVideoPlayback()
                    startVideoPlayback()
                }

                ACTION_STOP -> {
                    Log.d(TAG, "Stopping service")
                    stopSelf()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            showErrorNotification("Permission denied. Please grant Camera permission.")
            stopSelf()
            return START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
            showErrorNotification("Error: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    /**
     * Start foreground service with compatibility for different Android versions
     * and virtual environments
     */
    private fun startForegroundWithCompatibility() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ (API 34+)
                if (isInVirtualEnvironment) {
                    // More lenient for virtual environments
                    Log.d(TAG, "Using CAMERA type for Android 14+ in virtual environment")
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-13
                startForeground(NOTIFICATION_ID, createNotification())
            } else {
                // Below Android 10
                startForeground(NOTIFICATION_ID, createNotification())
            }

            Log.d(TAG, "Foreground service started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground: ${e.message}", e)
            throw e
        }
    }

    private fun startVideoPlayback() {
        if (isVideoPlaying) {
            Log.d(TAG, "Video already playing")
            return
        }

        isVideoPlaying = true

        // Get optimized settings for current environment
        val codecSettings = VirtualEnvironmentHelper.getOptimizedCodecSettings(this)
        val useAggressiveHooking = VirtualEnvironmentHelper.shouldUseAggressiveHooking(this)

        Log.d(TAG, "Starting video playback:")
        Log.d(TAG, "  - Resolution: ${codecSettings.width}x${codecSettings.height}")
        Log.d(TAG, "  - FPS: ${codecSettings.fps}")
        Log.d(TAG, "  - Bitrate: ${codecSettings.bitrate}")
        Log.d(TAG, "  - Aggressive Hooking: $useAggressiveHooking")
        Log.d(TAG, "  - Video Source: $currentVideoSource")

        videoThread = Thread {
            try {
                Log.d(TAG, "Video playback thread started")

                while (isVideoPlaying && !Thread.currentThread().isInterrupted) {
                    // TODO: Actual video processing logic
                    // This is where you'll integrate Media3 ExoPlayer
                    // to read the video and feed it to the camera

                    if (isInVirtualEnvironment) {
                        // Add extra compatibility checks for virtual environments
                        Thread.sleep(33) // ~30 FPS
                    } else {
                        Thread.sleep(16) // ~60 FPS
                    }
                }

                Log.d(TAG, "Video playback loop ended normally")

            } catch (e: InterruptedException) {
                Log.d(TAG, "Video thread interrupted")
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "Error in video playback: ${e.message}", e)
                handler.post {
                    showErrorNotification("Video playback error: ${e.message}")
                }
            }
        }.apply { start() }
    }

    private fun stopVideoPlayback() {
        Log.d(TAG, "Stopping video playback")
        isVideoPlaying = false
        videoThread?.interrupt()

        try {
            videoThread?.join(2000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for video thread")
            Thread.currentThread().interrupt()
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, VirtualCameraService::class.java).apply {
            action = ACTION_STOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getService(
            this, 0, stopIntent, pendingIntentFlags
        )

        // Different notification text for virtual environments
        val notificationText = if (isInVirtualEnvironment) {
            "Feeding video to $virtualEnvironmentName apps"
        } else {
            "Virtual camera active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VCamera Running")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                pendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$notificationText\nEnvironment: $virtualEnvironmentName\nVideo: ${currentVideoSource ?: "Not set"}"))
            .build()
    }

    private fun showErrorNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VCamera Error")
            .setContentText(errorMessage)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Virtual Camera Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the virtual camera running in background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying")

        stopVideoPlayback()

        isRunning = false
        currentVideoSource = null

        Log.d(TAG, "Service destroyed")
    }
}
