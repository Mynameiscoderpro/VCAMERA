package virtual.camera.app.app

import android.app.Application
import android.content.Context
import android.util.Log

class App : Application() {

    companion object {
        private const val TAG = "VCameraApp"

        @Volatile
        private var instance: App? = null

        /**
         * Get app instance
         */
        @JvmStatic
        fun getInstance(): App? = instance

        /**
         * Get application context - SAFE version
         * Returns application context or throws meaningful error
         */
        @JvmStatic
        fun getContext(): Context {
            val app = instance
            if (app == null) {
                Log.e(TAG, "CRITICAL: App.getContext() called before Application.onCreate()")
                Log.e(TAG, "Stack trace:", Exception("Context access before init"))
                throw IllegalStateException(
                    "Application not initialized. Make sure App class is declared in AndroidManifest.xml"
                )
            }
            return app.applicationContext
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // Set instance as early as possible
        instance = this
        Log.d(TAG, "App instance initialized in attachBaseContext")
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "=== VCamera App Starting ===")

        // Detect virtual environment by checking package name
        val packageName = try {
            applicationContext.packageName
        } catch (e: Exception) {
            "unknown"
        }

        val originalPackage = "virtual.camera.app"
        val isVirtual = packageName != originalPackage

        Log.d(TAG, "Current Package: $packageName")
        Log.d(TAG, "Original Package: $originalPackage")
        Log.d(TAG, "Running in Virtual Environment: $isVirtual")

        if (isVirtual) {
            Log.d(TAG, "Detected MochiCloner or similar virtual space")
            Log.d(TAG, "Applying virtual environment optimizations...")

            // Give virtual environment time to fully initialize
            try {
                Thread.sleep(500) // Reduced to 500ms
            } catch (e: InterruptedException) {
                Log.w(TAG, "Initialization delay interrupted")
            }
        }

        Log.d(TAG, "VCamera initialized successfully!")
    }

    override fun onTerminate() {
        Log.d(TAG, "App terminating")
        super.onTerminate()
        instance = null
    }
}
