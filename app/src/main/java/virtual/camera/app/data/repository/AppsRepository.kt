package virtual.camera.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import virtual.camera.app.data.models.AppInfo

class AppsRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
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

    suspend fun installApk(packageName: String): Boolean {
        // TODO: Implement app installation logic
        return false
    }

    suspend fun launchApk(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                context.startActivity(it)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unInstall(packageName: String): Boolean {
        // TODO: Implement app uninstallation logic
        return false
    }

    suspend fun clearData(packageName: String): Boolean {
        // TODO: Implement clear data logic
        return false
    }

    suspend fun updateAppPosition(packageName: String, position: Int) {
        // TODO: Implement position update logic
    }
}
