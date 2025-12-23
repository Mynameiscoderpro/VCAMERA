package virtual.camera.app.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import virtual.camera.core.service.VirtualCameraService
import virtual.camera.core.CameraConfig

/**
 * Main Application class that manages the virtual camera service lifecycle
 */
class App : Application() {

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var mContext: Context

        @JvmStatic
        fun getContext(): Context = mContext

        /**
         * Start the virtual camera service if it's enabled in settings
         */
        @JvmStatic
        fun startVirtualCamera() {
            val config = CameraConfig.load(mContext)
            if (config.methodType != CameraConfig.METHOD_DISABLE) {
                val intent = Intent(mContext, VirtualCameraService::class.java).apply {
                    action = VirtualCameraService.ACTION_START
                    putExtra(VirtualCameraService.EXTRA_CONFIG, config)
                }
                mContext.startForegroundService(intent)
            }
        }

        /**
         * Stop the virtual camera service
         */
        @JvmStatic
        fun stopVirtualCamera() {
            val intent = Intent(mContext, VirtualCameraService::class.java).apply {
                action = VirtualCameraService.ACTION_STOP
            }
            mContext.startService(intent)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (base != null) {
            mContext = base
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Automatically start virtual camera if it was previously enabled
        startVirtualCamera()
    }
}