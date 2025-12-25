package virtual.camera.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class VirtualCameraService : Service() {

    companion object {
        private const val TAG = "VirtualCameraService"
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
        Log.d(TAG, "VirtualCameraService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        try {
            when (intent?.action) {
                ACTION_START -> {
                    // Start foreground with proper type based on Android version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        // Android 14+ (API 34+) - Use only CAMERA type
                        startForeground(
                            NOTIFICATION_ID,
                            createNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10-13 - Can use multiple types
                        startForeground(NOTIFICATION_ID, createNotification())
                    } else {
                        // Below Android 10
                        startForeground(NOTIFICATION_ID, createNotification())
                    }

                    startVideoPlayback()
                    Log.d(TAG, "Service started successfully")
                }
                ACTION_STOP -> {
                    Log.d(TAG, "Stopping service")
                    stopSelf()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service: ${e.message}", e)
            // Stop service if we can't start foreground
            stopSelf()
            return START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startVideoPlayback() {
        if (isVideoPlaying) {
            Log.d(TAG, "Video already playing")
            return
        }

        isVideoPlaying = true
        videoThread = Thread {
            try {
                Log.d(TAG, "Video playback thread started")
                while (isVideoPlaying && !Thread.currentThread().isInterrupted) {
                    // TODO: Actual video processing logic goes here
                    Thread.sleep(1000)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Video thread interrupted")
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "Error in video playback: ${e.message}", e)
            }
        }.apply { start() }
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Virtual Camera Active")
            .setContentText("Feeding video to camera...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                pendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
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

        isVideoPlaying = false
        videoThread?.interrupt()

        try {
            videoThread?.join(2000) // Wait max 2 seconds for thread to finish
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for video thread to finish")
            Thread.currentThread().interrupt()
        }

        isRunning = false
        Log.d(TAG, "Service destroyed")
    }
}
