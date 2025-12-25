package virtual.camera.app.data.database

import android.content.Context
import virtual.camera.app.data.database.dao.InstalledAppDao
import virtual.camera.app.data.database.entities.InstalledAppEntity

@Database(
    entities = [InstalledAppEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun installedAppDao(): InstalledAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vcamera_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}