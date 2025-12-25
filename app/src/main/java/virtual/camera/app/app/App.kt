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
        fun getInstance(): App? = instance

        /**
         * Get application context
         * This method is used throughout the app
         */
        fun getContext(): Context? {
            return instance?.applicationContext
        }
    }

    override fun attachBaseContext(base: Context?) {
        try {
            super.attachBaseContext(base)
            Log.d(TAG, "Base context attached successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in attachBaseContext: ${e.message}", e)
            // Try to continue despite error
            try {
                super.attachBaseContext(base)
            } catch (e2: Exception) {
                // Last resort - log and continue
                Log.e(TAG, "Critical context error", e2)
            }
        }
    }

    override fun onCreate() {
        var initialized = false

        try {
            super.onCreate()
            initialized = true
            instance = this

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
                    Thread.sleep(1000) // 1 second delay
                } catch (e: InterruptedException) {
                    Log.w(TAG, "Initialization delay interrupted")
                }
            }

            // Initialize app components
            initializeComponents()

            Log.d(TAG, "VCamera initialized successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Error during app initialization: ${e.message}", e)
            e.printStackTrace()

            if (!initialized) {
                // If super.onCreate() failed, we can't continue
                Log.e(TAG, "CRITICAL: App failed to initialize properly")
                // Don't throw - let system handle it
            }
        }
    }

    private fun initializeComponents() {
        try {
            Log.d(TAG, "Initializing app components...")

            // Initialize your components here if needed
            // For now, just log success

            Log.d(TAG, "Components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components: ${e.message}", e)
            // Don't crash - components can initialize lazily
        }
    }

    override fun onTerminate() {
        try {
            Log.d(TAG, "App terminating")
            super.onTerminate()
            instance = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during termination: ${e.message}", e)
        }
    }
}
