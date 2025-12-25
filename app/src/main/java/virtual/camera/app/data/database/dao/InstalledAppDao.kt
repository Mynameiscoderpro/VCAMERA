package virtual.camera.app.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import virtual.camera.app.data.database.entities.InstalledAppEntity

@Dao
interface InstalledAppDao {

    @Query("SELECT * FROM installed_apps WHERE userId = :userId ORDER BY position ASC")
    fun getAppsByUser(userId: Int): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE userId = :userId ORDER BY position ASC")
    suspend fun getAppsByUserSync(userId: Int): List<InstalledAppEntity>

    @Query("SELECT * FROM installed_apps WHERE packageName = :packageName AND userId = :userId")
    suspend fun getApp(packageName: String, userId: Int): InstalledAppEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM installed_apps WHERE packageName = :packageName AND userId = :userId)")
    suspend fun isAppInstalled(packageName: String, userId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: InstalledAppEntity): Long

    @Update
    suspend fun updateApp(app: InstalledAppEntity)

    @Delete
    suspend fun deleteApp(app: InstalledAppEntity)

    @Query("DELETE FROM installed_apps WHERE packageName = :packageName AND userId = :userId")
    suspend fun deleteAppByPackage(packageName: String, userId: Int)

    @Query("UPDATE installed_apps SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("DELETE FROM installed_apps WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Int)

    @Query("SELECT COUNT(*) FROM installed_apps WHERE userId = :userId")
    suspend fun getAppCount(userId: Int): Int
}
