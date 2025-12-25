package virtual.camera.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import virtual.camera.app.App
import virtual.camera.app.core.VirtualEngine
import virtual.camera.app.data.database.entities.InstalledAppEntity
import virtual.camera.app.data.models.AppInfo
import virtual.camera.app.data.models.InstalledAppBean

class AppsRepository(private val context: Context = App.getContext()) {

    private val database = App.database
    private val virtualEngine = App.virtualEngine
    private val packageManager = context.packageManager

    /**
     * Get list of all apps that can be installed into virtual environment
     */
    suspend fun previewInstallList(): List<InstalledAppBean> = withContext(Dispatchers.IO) {
        val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val virtualApps = getVmInstallListSync(0).map { it.packageName }

        installedPackages
            .filter { app ->
                // Filter system apps
                !app.sourceDir.startsWith("/system/") &&
                        !app.sourceDir.startsWith("/vendor/") &&
                        app.packageName != context.packageName // Don't include VCamera itself
            }
            .map { app ->
                val packageInfo = try {
                    packageManager.getPackageInfo(app.packageName, 0)
                } catch (e: Exception) {
                    null
                }

                InstalledAppBean(
                    name = app.loadLabel(packageManager).toString(),
                    packageName = app.packageName,
                    sourceDir = app.sourceDir,
                    versionName = packageInfo?.versionName ?: "1.0",
                    versionCode = packageInfo?.versionCode ?: 1,
                    isInstalled = virtualApps.contains(app.packageName),
                    icon = app.loadIcon(packageManager)
                )
            }
            .sortedBy { it.name }
    }

    /**
     * Get apps installed in virtual environment (Flow)
     */
    fun getVmInstallList(userId: Int): Flow<List<AppInfo>> {
        return database.installedAppDao().getAppsByUser(userId).map { entities ->
            entities.map { entity ->
                AppInfo(
                    id = entity.id,
                    name = entity.name,
                    packageName = entity.packageName,
                    versionName = entity.versionName,
                    versionCode = entity.versionCode,
                    sourceDir = entity.sourceDir,
                    userId = entity.userId,
                    isXpModule = entity.isXpModule,
                    installTime = entity.installTime,
                    position = entity.position,
                    icon = loadAppIcon(entity.sourceDir)
                )
            }
        }
    }

    /**
     * Get apps installed in virtual environment (Sync)
     */
    private suspend fun getVmInstallListSync(userId: Int): List<AppInfo> = withContext(Dispatchers.IO) {
        val entities = database.installedAppDao().getAppsByUserSync(userId)
        entities.map { entity ->
            AppInfo(
                id = entity.id,
                name = entity.name,
                packageName = entity.packageName,
                versionName = entity.versionName,
                versionCode = entity.versionCode,
                sourceDir = entity.sourceDir,
                userId = entity.userId,
                isXpModule = entity.isXpModule,
                installTime = entity.installTime,
                position = entity.position,
                icon = loadAppIcon(entity.sourceDir)
            )
        }
    }

    /**
     * Install APK into virtual environment
     */
    suspend fun installApk(
        source: String,
        userId: Int,
        resultLiveData: MutableLiveData<String>
    ) = withContext(Dispatchers.IO) {
        try {
            resultLiveData.postValue("Installing...")

            // Extract APK info
            val packageInfo = packageManager.getPackageArchiveInfo(source, 0)
                ?: throw Exception("Invalid APK file")

            val appName = packageInfo.applicationInfo?.let {
                it.loadLabel(packageManager).toString()
            } ?: packageInfo.packageName

            // Check if already installed
            val isInstalled = database.installedAppDao().isAppInstalled(
                packageInfo.packageName!!,
                userId
            )

            if (isInstalled) {
                resultLiveData.postValue("App already installed")
                return@withContext
            }

            // Install to virtual engine
            val success = virtualEngine.installPackage(
                source,
                userId,
                packageInfo.packageName!!
            )

            if (success) {
                // Save to database
                val position = database.installedAppDao().getAppCount(userId)
                val entity = InstalledAppEntity(
                    name = appName ?: packageInfo.packageName!!,
                    packageName = packageInfo.packageName!!,
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = packageInfo.versionCode,
                    sourceDir = source,
                    userId = userId,
                    installTime = System.currentTimeMillis(),
                    position = position
                )
                database.installedAppDao().insertApp(entity)

                resultLiveData.postValue("Installed successfully")
            } else {
                resultLiveData.postValue("Installation failed")
            }
        } catch (e: Exception) {
            resultLiveData.postValue("Error: ${e.message}")
        }
    }

    /**
     * Launch app in virtual environment
     */
    suspend fun launchApk(
        packageName: String,
        userId: Int,
        launchLiveData: MutableLiveData<Boolean>
    ) = withContext(Dispatchers.IO) {
        try {
            val success = virtualEngine.launchPackage(packageName, userId)
            launchLiveData.postValue(success)
        } catch (e: Exception) {
            launchLiveData.postValue(false)
        }
    }

    /**
     * Uninstall app from virtual environment
     */
    suspend fun unInstall(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            virtualEngine.uninstallPackage(packageName, userId)
            database.installedAppDao().deleteAppByPackage(packageName, userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear app data
     */
    suspend fun clearData(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        virtualEngine.clearPackageData(packageName, userId)
    }

    /**
     * Update app position
     */
    suspend fun updateAppPosition(appId: Long, newPosition: Int) = withContext(Dispatchers.IO) {
        database.installedAppDao().updatePosition(appId, newPosition)
    }

    private fun loadAppIcon(sourceDir: String) = try {
        val packageInfo = packageManager.getPackageArchiveInfo(sourceDir, 0)
        packageInfo?.applicationInfo?.let {
            it.sourceDir = sourceDir
            it.publicSourceDir = sourceDir
            it.loadIcon(packageManager)
        }
    } catch (e: Exception) {
        null
    }
}
