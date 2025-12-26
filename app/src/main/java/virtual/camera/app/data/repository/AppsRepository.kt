package virtual.camera.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import virtual.camera.app.data.models.AppInfo

class AppsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vcamera_apps", Context.MODE_PRIVATE)
    private val INSTALLED_APPS_KEY = "installed_apps"

    // Get apps installed in VCamera virtual environment
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedPackages = getInstalledPackageNames()
        
        installedPackages.mapNotNull { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                AppInfo(
                    packageName = appInfo.packageName,
                    name = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager),
                    sourceDir = appInfo.sourceDir,
                    versionName = try {
                        packageManager.getPackageInfo(appInfo.packageName, 0).versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name }
    }

    // Get all user apps on device (excluding VCamera itself and already installed apps)
    suspend fun getAllDeviceApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val ownPackageName = context.packageName
        val installedPackages = getInstalledPackageNames()

        packages
            .filter { 
                // Include only user-installed apps (not system apps)
                it.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                // Exclude VCamera itself
                it.packageName != ownPackageName &&
                // Exclude already installed apps
                it.packageName !in installedPackages
            }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    name = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager),
                    sourceDir = appInfo.sourceDir,
                    versionName = try {
                        packageManager.getPackageInfo(appInfo.packageName, 0).versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                )
            }
            .sortedBy { it.name }
    }

    suspend fun getVmInstallList(): List<AppInfo> {
        return getInstalledApps()
    }

    suspend fun previewInstallList(): List<AppInfo> {
        return emptyList()
    }

    // Install app to VCamera (currently just adds to list, will integrate with virtual environment later)
    suspend fun installApk(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val installedPackages = getInstalledPackageNames().toMutableSet()
            installedPackages.add(packageName)
            saveInstalledPackageNames(installedPackages)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun launchApk(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Uninstall app from VCamera (removes from list)
    suspend fun unInstall(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val installedPackages = getInstalledPackageNames().toMutableSet()
            installedPackages.remove(packageName)
            saveInstalledPackageNames(installedPackages)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearData(packageName: String): Boolean {
        // TODO: Implement clear data logic when virtual environment is integrated
        return false
    }

    suspend fun updateAppPosition(packageName: String, position: Int) {
        // TODO: Implement position update logic
    }

    // Helper methods for SharedPreferences
    private fun getInstalledPackageNames(): Set<String> {
        return prefs.getStringSet(INSTALLED_APPS_KEY, emptySet()) ?: emptySet()
    }

    private fun saveInstalledPackageNames(packages: Set<String>) {
        prefs.edit().putStringSet(INSTALLED_APPS_KEY, packages).apply()
    }
}
