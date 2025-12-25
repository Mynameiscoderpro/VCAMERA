package virtual.camera.app.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import virtual.camera.app.data.models.AppInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class VirtualEngine private constructor(private val context: Context) {

    private val TAG = "VirtualEngine"
    private val virtualRoot: File = File(context.filesDir, "virtual")
    private val appsDir: File = File(virtualRoot, "apps")
    private val dataDir: File = File(virtualRoot, "data")
    private val processManager = ProcessManager()

    companion object {
        @Volatile
        private var INSTANCE: VirtualEngine? = null

        fun getInstance(context: Context): VirtualEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = VirtualEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Initialize virtual environment
     */
    fun initialize() {
        Log.d(TAG, "Initializing virtual environment")

        // Create directory structure
        virtualRoot.mkdirs()
        appsDir.mkdirs()
        dataDir.mkdirs()

        // Create user directories
        for (userId in 0..9) {
            File(dataDir, "user_$userId").mkdirs()
        }

        Log.d(TAG, "Virtual environment initialized at: ${virtualRoot.absolutePath}")
    }

    /**
     * Install package to virtual environment
     */
    suspend fun installPackage(
        apkPath: String,
        userId: Int,
        packageName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Installing package: $packageName for user $userId")

            // Create app directory
            val appDir = File(appsDir, packageName)
            if (appDir.exists()) {
                appDir.deleteRecursively()
            }
            appDir.mkdirs()

            // Copy APK
            val targetApk = File(appDir, "base.apk")
            File(apkPath).copyTo(targetApk, overwrite = true)

            // Extract native libraries if any
            extractNativeLibs(targetApk, appDir)

            // Create data directory
            val userDataDir = File(dataDir, "user_$userId/$packageName")
            userDataDir.mkdirs()

            // Create standard Android directories
            File(userDataDir, "cache").mkdirs()
            File(userDataDir, "files").mkdirs()
            File(userDataDir, "databases").mkdirs()
            File(userDataDir, "shared_prefs").mkdirs()

            Log.d(TAG, "Package installed successfully: $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install package: $packageName", e)
            false
        }
    }

    /**
     * Launch package in virtual environment
     */
    suspend fun launchPackage(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Launching package: $packageName for user $userId")

            val appDir = File(appsDir, packageName)
            val apkFile = File(appDir, "base.apk")

            if (!apkFile.exists()) {
                Log.e(TAG, "APK not found: ${apkFile.absolutePath}")
                return@withContext false
            }

            // Get package info
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            ) ?: return@withContext false

            // Find launcher activity
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageInfo.packageName!!)
            val activityName = launchIntent?.component?.className

            if (activityName == null) {
                Log.e(TAG, "No launcher activity found for: $packageName")
                return@withContext false
            }

            // Launch in virtual process
            processManager.launchApp(
                context = context,
                packageName = packageName,
                activityName = activityName,
                apkPath = apkFile.absolutePath,
                userId = userId,
                dataDir = File(dataDir, "user_$userId/$packageName").absolutePath
            )

            Log.d(TAG, "Package launched: $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
            false
        }
    }

    /**
     * Uninstall package from virtual environment
     */
    suspend fun uninstallPackage(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Uninstalling package: $packageName")

            // Stop running process
            processManager.killApp(packageName, userId)

            // Delete app directory
            val appDir = File(appsDir, packageName)
            if (appDir.exists()) {
                appDir.deleteRecursively()
            }

            // Delete data directory
            val userDataDir = File(dataDir, "user_$userId/$packageName")
            if (userDataDir.exists()) {
                userDataDir.deleteRecursively()
            }

            Log.d(TAG, "Package uninstalled: $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall package: $packageName", e)
            false
        }
    }

    /**
     * Clear package data
     */
    suspend fun clearPackageData(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Clearing data for: $packageName")

            // Stop running process
            processManager.killApp(packageName, userId)

            // Delete data directory
            val userDataDir = File(dataDir, "user_$userId/$packageName")
            if (userDataDir.exists()) {
                userDataDir.deleteRecursively()
            }

            // Recreate directories
            userDataDir.mkdirs()
            File(userDataDir, "cache").mkdirs()
            File(userDataDir, "files").mkdirs()
            File(userDataDir, "databases").mkdirs()
            File(userDataDir, "shared_prefs").mkdirs()

            Log.d(TAG, "Data cleared for: $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear data for: $packageName", e)
            false
        }
    }

    /**
     * Get installed apps
     */
    suspend fun getInstalledApps(userId: Int): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<AppInfo>()

        appsDir.listFiles()?.forEach { appDir ->
            val apkFile = File(appDir, "base.apk")
            if (apkFile.exists()) {
                try {
                    val packageInfo = context.packageManager.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        0
                    )

                    if (packageInfo != null) {
                        val appInfo = AppInfo(
                            name = packageInfo.applicationInfo?.loadLabel(context.packageManager).toString(),
                            packageName = packageInfo.packageName!!,
                            versionName = packageInfo.versionName ?: "1.0",
                            versionCode = packageInfo.versionCode,
                            sourceDir = apkFile.absolutePath,
                            userId = userId
                        )
                        apps.add(appInfo)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading APK: ${appDir.name}", e)
                }
            }
        }

        apps
    }

    /**
     * Check if running in virtual environment
     */
    fun isRunningInVirtualEnvironment(): Boolean {
        // Detection methods
        val indicators = listOf(
            "/data/data/virtual.camera.app",
            "/data/user/0/virtual.camera.app",
            "/sdcard/Android/data/virtual.camera.app"
        )

        return indicators.any { File(it).exists() }
    }

    /**
     * Kill all running apps
     */
    fun killAllApps() {
        processManager.killAllApps()
    }

    /**
     * Extract native libraries from APK
     */
    private fun extractNativeLibs(apkFile: File, targetDir: File) {
        try {
            val libDir = File(targetDir, "lib")
            libDir.mkdirs()

            ZipInputStream(FileInputStream(apkFile)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("lib/") && !entry.isDirectory) {
                        val outFile = File(libDir, entry.name.substringAfter("lib/"))
                        outFile.parentFile?.mkdirs()

                        FileOutputStream(outFile).use { output ->
                            zip.copyTo(output)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract native libs", e)
        }
    }
}
