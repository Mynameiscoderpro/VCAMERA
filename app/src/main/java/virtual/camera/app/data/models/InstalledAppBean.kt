package virtual.camera.app.data.models

import android.graphics.drawable.Drawable

data class InstalledAppBean(
    val name: String,
    val packageName: String,
    val sourceDir: String,
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    var isInstalled: Boolean = false,
    @Transient var icon: Drawable? = null
)
