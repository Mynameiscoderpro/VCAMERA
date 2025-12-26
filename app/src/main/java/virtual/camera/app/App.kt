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
        appContext = applicationContext

        // TODO: Initialize VirtualEngine when ready
        // VirtualEngine.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        // TODO: Shutdown VirtualEngine when ready
        // VirtualEngine.shutdown()
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
            private set

        @JvmStatic
        private lateinit var appContext: Context

        @JvmStatic
        fun getContext(): Context {
            return appContext
        }
    }
}
