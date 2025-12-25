package virtual.camera.app.core

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class ProcessManager {

    private val TAG = "ProcessManager"
    private val runningProcesses = ConcurrentHashMap<String, Process>()

    /**
     * Launch app in isolated process
     */
    fun launchApp(
        context: Context,
        packageName: String,
        activityName: String,
        apkPath: String,
        userId: Int,
        dataDir: String
    ) {
        try {
            Log.d(TAG, "Launching app: $packageName")

            // Create launch intent
            val intent = Intent().apply {
                setClassName(packageName, activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("VIRTUAL_APP", true)
                putExtra("USER_ID", userId)
                putExtra("DATA_DIR", dataDir)
            }

            // Start activity with virtual environment
            context.startActivity(intent)

            Log.d(TAG, "App launched: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
        }
    }

    /**
     * Kill app process
     */
    fun killApp(packageName: String, userId: Int) {
        val key = "$packageName:$userId"
        runningProcesses[key]?.destroy()
        runningProcesses.remove(key)
        Log.d(TAG, "Killed app: $packageName")
    }

    /**
     * Kill all running apps
     */
    fun killAllApps() {
        runningProcesses.values.forEach { it.destroy() }
        runningProcesses.clear()
        Log.d(TAG, "Killed all apps")
    }

    /**
     * Check if app is running
     */
    fun isAppRunning(packageName: String, userId: Int): Boolean {
        val key = "$packageName:$userId"
        return runningProcesses.containsKey(key)
    }
}
