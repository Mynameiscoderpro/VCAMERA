package virtual.camera.app.app

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import virtual.camera.core.service.VirtualCameraService
import virtual.camera.core.CameraConfig

class App : Application() {

    companion object {
        @Volatile
        private lateinit var instance: App

        @JvmStatic
        fun getContext(): Context = instance.applicationContext  // ✅ FIXED: Use applicationContext

        @JvmStatic
        fun startVirtualCamera() {
            val context = getContext()
            val config = CameraConfig.load(context)

            if (config.methodType != CameraConfig.METHOD_DISABLE) {
                val intent = Intent(context, VirtualCameraService::class.java).apply {
                    action = VirtualCameraService.ACTION_START
                    putExtra(VirtualCameraService.EXTRA_CONFIG, config)
                }

                // ✅ FIXED: Check if service is already running
                if (!VirtualCameraService.isRunning) {
                    ContextCompat.startForegroundService(context, intent)
                }
            }
        }

        @JvmStatic
        fun stopVirtualCamera() {
            val context = getContext()
            val intent = Intent(context, VirtualCameraService::class.java).apply {
                action = VirtualCameraService.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ✅ FIXED: Don't auto-start on app launch
        // Let user manually enable from MainActivity
    }
}
