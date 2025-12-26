package virtual.camera.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        context = applicationContext

        // TODO: Initialize VirtualEngine when ready
        // VirtualEngine.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        // TODO: Shutdown VirtualEngine when ready
        // VirtualEngine.shutdown()
    }

    companion object {
        lateinit var instance: App
        private set

        lateinit var context: Context
        private set

        @JvmStatic
        fun getInstance(): App {
            return instance
        }

        @JvmStatic
        fun getContext(): Context {
            return context
        }
    }
}
