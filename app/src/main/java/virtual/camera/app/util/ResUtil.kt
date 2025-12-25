package virtual.camera.app.util

import android.content.Context
import androidx.annotation.StringRes

object ResUtil {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getString(@StringRes resId: Int): String {
        return if (::appContext.isInitialized) {
            appContext.getString(resId)
        } else {
            ""
        }
    }
}
