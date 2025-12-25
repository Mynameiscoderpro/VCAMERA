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
                    appName = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager),
                    isInstalled = true
                )
            }
            .sortedBy { it.appName }
    }

    suspend fun installApp(packageName: String): Boolean {
        // TODO: Implement app installation logic
        return false
    }

    suspend fun uninstallApp(packageName: String): Boolean {
        // TODO: Implement app uninstallation logic
        return false
    }
}
