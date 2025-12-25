package virtual.camera.app.data.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val sourceDir: String,
    val userId: Int = 0,
    val isXpModule: Boolean = false,
    val installTime: Long = System.currentTimeMillis(),
    val position: Int = 0,
    @Transient var icon: Drawable? = null
)
