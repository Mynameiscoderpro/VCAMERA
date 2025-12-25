package virtual.camera.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val sourceDir: String,
    val userId: Int = 0,
    val isXpModule: Boolean = false,
    val installTime: Long = System.currentTimeMillis(),
    val position: Int = 0
)
