package virtual.camera.app.data.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val sourceDir: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val isSystemApp: Boolean = false
)
