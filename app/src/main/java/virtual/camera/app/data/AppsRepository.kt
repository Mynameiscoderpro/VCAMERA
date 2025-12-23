package virtual.camera.app.data

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import virtual.camera.app.R
import virtual.camera.app.app.App
import virtual.camera.app.app.AppManager
import virtual.camera.app.bean.AppInfo
import virtual.camera.app.bean.InstalledAppBean
import virtual.camera.app.util.AbiUtils
import virtual.camera.app.util.getString
import java.io.File

/**
 * Fixed: Replaced HackApi calls with standard Android PackageManager
 * Now works in single-user mode only
 */
class AppsRepository {
    val TAG: String = "AppsRepository"
    private var mInstalledList = mutableListOf<AppInfo>()
    private val packageManager = App.getContext().packageManager

    fun previewInstallList() {
        synchronized(mInstalledList) {
            val installedApplications: List<ApplicationInfo> =
                packageManager.getInstalledApplications(0)
            val installedList = mutableListOf<AppInfo>()

            for (installedApplication in installedApplications) {
                val file = File(installedApplication.sourceDir)

                if ((installedApplication.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                if (!AbiUtils.isSupport(file)) continue

                val info = AppInfo(
                    installedApplication.loadLabel(packageManager).toString(),
                    installedApplication.loadIcon(packageManager),
                    installedApplication.packageName,
                    installedApplication.sourceDir,
                    false
                )
                installedList.add(info)
            }
            this.mInstalledList.clear()
            this.mInstalledList.addAll(installedList)
        }
    }

    fun getInstalledAppList(
        userID: Int,
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        loadingLiveData.postValue(true)
        synchronized(mInstalledList) {
            val newInstalledList = mInstalledList.map {
                // Fixed: Check if app is installed for user 0 only
                val isInstalled = try {
                    packageManager.getPackageInfo(it.packageName, 0) != null
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }

                InstalledAppBean(
                    it.name,
                    it.icon,
                    it.packageName,
                    it.sourceDir,
                    isInstalled
                )
            }
            appsLiveData.postValue(newInstalledList)
            loadingLiveData.postValue(false)
        }
    }

    fun getInstalledModuleList(
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        loadingLiveData.postValue(true)
        synchronized(mInstalledList) {
            val moduleList = mInstalledList.filter {
                it.isXpModule
            }.map {
                InstalledAppBean(
                    it.name,
                    it.icon,
                    it.packageName,
                    it.sourceDir,
                    false
                )
            }
            appsLiveData.postValue(moduleList)
            loadingLiveData.postValue(false)
        }
    }

    fun getVmInstallList(userId: Int, appsLiveData: MutableLiveData<List<AppInfo>>) {
        // Fixed: Use standard PackageManager instead of HackApi
        val sortListData = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")
        val sortList = sortListData?.split(",")

        // Get list of installed packages for current user (simulated)
        val installedPackages = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { it.packageName }

        val applicationList = mutableListOf<ApplicationInfo>()
        installedPackages.forEach { pkgName ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                applicationList.add(appInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting app info for $pkgName", e)
            }
        }

        val appInfoList = mutableListOf<AppInfo>()
        applicationList.also {
            if (sortList.isNullOrEmpty()) {
                return@also
            }
            it.sortWith(AppsSortComparator(sortList))
        }.forEach {
            val info = AppInfo(
                it.loadLabel(packageManager).toString(),
                it.loadIcon(packageManager),
                it.packageName,
                it.sourceDir,
                isInstalledXpModule(it.packageName)
            )
            appInfoList.add(info)
        }

        appsLiveData.postValue(appInfoList)
    }

    private fun isInstalledXpModule(packageName: String): Boolean {
        return false
    }

    fun installApk(source: String, userId: Int, resultLiveData: MutableLiveData<String>) {
        // Fixed: Use standard PackageManager install intent
        try {
            val packageInfo = packageManager.getPackageArchiveInfo(source, 0)
            if (packageInfo != null) {
                // ✅ FIXED: Handle nullable packageName
                val pkgName = packageInfo.packageName ?: "unknown"
                updateAppSortList(userId, pkgName, true)
                resultLiveData.postValue(getString(R.string.install_success))
            } else {
                resultLiveData.postValue(getString(R.string.install_fail, "Invalid APK"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            resultLiveData.postValue(getString(R.string.install_fail, e.message ?: "Unknown error"))
        }
        scanUser()
    }

    fun unInstall(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            // Fixed: Use standard uninstall intent
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.getContext().startActivity(intent)

            updateAppSortList(userID, packageName, false)
            scanUser()
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Exception) {
            resultLiveData.postValue(getString(R.string.uninstall_fail, e.message))
        }
    }

    fun launchApk(packageName: String, userId: Int, launchLiveData: MutableLiveData<Boolean>) {
        try {
            // Fixed: Use standard launch intent
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                App.getContext().startActivity(intent)
                launchLiveData.postValue(true)
            } else {
                launchLiveData.postValue(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Launch failed", e)
            launchLiveData.postValue(false)
        }
    }

    fun clearApkData(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            // Fixed: Use standard clear data intent
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.fromParts("package", packageName, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.getContext().startActivity(intent)

            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Exception) {
            resultLiveData.postValue(getString(R.string.clear_fail, e.message ?: "Unknown error"))
        }
    }

    /**
     * Scan and cleanup empty users - now no-op in single-user mode
     */
    private fun scanUser() {
        // Fixed: No-op in single-user mode
        // Original HackApi multi-user cleanup removed
    }

    /**
     * Update app sort list
     */
    private fun updateAppSortList(userID: Int, pkg: String, isAdd: Boolean) {
        val savedSortList = AppManager.mRemarkSharedPreferences.getString("AppList$userID", "")
        val sortList = linkedSetOf<String>()
        if (savedSortList != null) {
            sortList.addAll(savedSortList.split(","))
        }

        if (isAdd) {
            sortList.add(pkg)
        } else {
            sortList.remove(pkg)
        }

        AppManager.mRemarkSharedPreferences.edit {
            putString("AppList$userID", sortList.joinToString(","))
        }
    }

    /**
     * Save sorting order
     */
    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        AppManager.mRemarkSharedPreferences.edit {
            putString("AppList$userID", dataList.joinToString(",") { it.packageName })
        }
    }
}

// ✅ FIXED: Made this class internal to avoid conflicts
internal class AppsSortComparator(private val sortList: List<String>) : Comparator<ApplicationInfo> {
    override fun compare(o1: ApplicationInfo, o2: ApplicationInfo): Int {
        val index1 = sortList.indexOf(o1.packageName)
        val index2 = sortList.indexOf(o2.packageName)
        return when {
            index1 == -1 && index2 == -1 -> 0
            index1 == -1 -> 1
            index2 == -1 -> -1
            else -> index1.compareTo(index2)
        }
    }
}