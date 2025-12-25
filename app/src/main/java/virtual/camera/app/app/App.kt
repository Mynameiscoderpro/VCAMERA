package virtual.camera.app.app

import android.app.Application
import android.content.Context
import android.util.Log

class App : Application() {

    companion object {
        private const val TAG = "VCameraApp"

        @Volatile
        private var instance: App? = null

        fun getInstance(): App? = instance
    }

    override fun attachBaseContext(base: Context?) {
        try {
            super.attachBaseContext(base)
            Log.d(TAG, "Base context attached")
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching base context: ${e.message}", e)
            // Continue anyway - MochiCloner might handle it
            try {
                super.attachBaseContext(base)
            } catch (e2: Exception) {
                Log.e(TAG, "Critical error in attachBaseContext", e2)
            }
        }
    }

    override fun onCreate() {
        try {
            super.onCreate()
            instance = this

            Log.d(TAG, "=== VCamera App Starting ===")

            // Detect if running in virtual environment
            val packageName = applicationContext.packageName
            val isVirtual = packageName != "virtual.camera.app"

            Log.d(TAG, "Package: $packageName")
            Log.d(TAG, "Is Virtual Environment: $isVirtual")

            if (isVirtual) {
                Log.d(TAG, "Running in MochiCloner or similar virtual environment")
                // Add small delay for virtual environment initialization
                Thread.sleep(500)
            }

            Log.d(TAG, "App initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing app: ${e.message}", e)
            // Don't crash, let MochiCloner handle it
        }
    }
}
