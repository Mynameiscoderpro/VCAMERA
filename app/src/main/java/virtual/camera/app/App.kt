package virtual.camera.app

import android.app.Application
import android.content.Context
import android.util.Log
import virtual.camera.app.data.database.AppDatabase

class App : Application() {

    companion object {
        private const val TAG = "VCameraApp"

        @Volatile
        private var instance: App? = null

        @JvmStatic
        fun getContext(): Context {
            return instance?.applicationContext
                ?: throw IllegalStateException("App not initialized")
        }

        @JvmStatic
        lateinit var virtualEngine: VirtualEngine
            private set

        @JvmStatic
        lateinit var database: AppDatabase
            private set
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
        Log.d(TAG, "Application context attached")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")

        // Initialize database
        database = AppDatabase.getInstance(this)
        Log.d(TAG, "Database initialized")

        // Initialize virtual engine
        virtualEngine = VirtualEngine.getInstance(this)
        // VirtualEngine.initialize(this)
        Log.d(TAG, "Virtual engine initialized")

        // Detect if running in virtual environment
        detectVirtualEnvironment()
    }

    private fun detectVirtualEnvironment() {
        val isVirtual = virtualEngine.isRunningInVirtualEnvironment()
        if (isVirtual) {
            Log.w(TAG, "⚠️ Running in virtual environment detected")
        } else {
            Log.d(TAG, "✅ Running in real environment")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminated")
    }
}
