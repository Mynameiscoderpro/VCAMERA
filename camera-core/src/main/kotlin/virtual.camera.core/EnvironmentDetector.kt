package virtual.camera.core

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Detects if the app is running inside a virtual environment
 * (MochiCloner, VMOS, VirtualXposed, Parallel Space, etc.)
 */
object EnvironmentDetector {

    private const val TAG = "EnvironmentDetector"

    /**
     * Check if running in ANY virtual environment
     */
    fun isRunningInVirtualEnvironment(context: Context): Boolean {
        return isRunningInMochiCloner() ||
                isRunningInVMOS() ||
                isRunningInVirtualXposed() ||
                isRunningInParallelSpace() ||
                isRunningInDualSpace() ||
                hasVirtualEnvironmentIndicators()
    }

    /**
     * Specifically detect MochiCloner
     */
    fun isRunningInMochiCloner(): Boolean {
        try {
            // Check for MochiCloner specific packages
            val mochiPackages = arrayOf(
                "com.mochi.cloner",
                "com.mochi.clone",
                "com.mochicloner",
                "mochi.cloner"
            )

            for (pkg in mochiPackages) {
                if (isPackageInstalled(pkg)) {
                    Log.d(TAG, "MochiCloner detected via package: $pkg")
                    return true
                }
            }

            // Check environment variables
            val mochiEnvVars = arrayOf(
                "MOCHI_ENV",
                "MOCHI_CLONER",
                "IS_MOCHI_CLONE"
            )

            for (envVar in mochiEnvVars) {
                if (System.getenv(envVar) != null) {
                    Log.d(TAG, "MochiCloner detected via env var: $envVar")
                    return true
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error detecting MochiCloner: ${e.message}")
        }

        return false
    }

    /**
     * Check for VMOS virtual environment
     */
    private fun isRunningInVMOS(): Boolean {
        return try {
            isPackageInstalled("com.vmos.app") ||
                    isPackageInstalled("com.vmos.pro") ||
                    File("/system/app/VMOS").exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for VirtualXposed
     */
    private fun isRunningInVirtualXposed(): Boolean {
        return try {
            isPackageInstalled("io.va.exposed") ||
                    System.getenv("VXPOSED") != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for Parallel Space
     */
    private fun isRunningInParallelSpace(): Boolean {
        return try {
            isPackageInstalled("com.lbe.parallel.intl") ||
                    isPackageInstalled("com.parallel.space.lite")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for Dual Space / Multiple Accounts
     */
    private fun isRunningInDualSpace(): Boolean {
        return try {
            isPackageInstalled("com.excelliance.dualaid") ||
                    isPackageInstalled("com.ludashi.dualspace")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for general virtual environment indicators
     */
    private fun hasVirtualEnvironmentIndicators(): Boolean {
        try {
            // Check for suspicious file paths
            val suspiciousPaths = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/data/data/com.noshufou.android.su",
                "/system/app/Superuser.apk"
            )

            // Check mounts for indicators
            val mounts = readFile("/proc/self/maps")
            if (mounts.contains("XposedBridge") ||
                mounts.contains("substrate") ||
                mounts.contains("libva-native")) {
                Log.d(TAG, "Virtual environment detected via mounts")
                return true
            }

            // Check for path length anomalies (virtual apps often have longer paths)
            val dataDir = android.os.Environment.getDataDirectory().absolutePath
            if (dataDir.length > 50) {
                Log.d(TAG, "Suspicious data directory length: ${dataDir.length}")
                return true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking virtual indicators: ${e.message}")
        }

        return false
    }

    /**
     * Check if a package is installed
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("pm list packages")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains(packageName) == true) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read file content
     */
    private fun readFile(path: String): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get virtual environment type name
     */
    fun getVirtualEnvironmentName(): String {
        return when {
            isRunningInMochiCloner() -> "MochiCloner"
            isRunningInVMOS() -> "VMOS"
            isRunningInVirtualXposed() -> "VirtualXposed"
            isRunningInParallelSpace() -> "Parallel Space"
            isRunningInDualSpace() -> "Dual Space"
            hasVirtualEnvironmentIndicators() -> "Virtual Environment"
            else -> "Native"
        }
    }

    /**
     * Log environment information
     */
    /**
     * Log environment information
     */
    fun logEnvironmentInfo() {
        Log.d(TAG, "=== Environment Detection ===")
        Log.d(TAG, "Virtual Environment: ${getVirtualEnvironmentName()}")
        // Note: Can't check isVirtual without Context in static logging
        Log.d(TAG, "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "============================")
    }
}
