package virtual.camera.app.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import virtual.camera.app.data.models.AppInfo
import virtual.camera.app.data.models.InstalledAppBean
import virtual.camera.app.data.repository.AppsRepository

class AppsViewModel(
    private val repository: AppsRepository = AppsRepository()
) : ViewModel() {

    val appsLiveData = MutableLiveData<List<AppInfo>>()
    val availableAppsLiveData = MutableLiveData<List<InstalledAppBean>>()
    val resultLiveData = MutableLiveData<String>()
    val launchLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String>()
    val loadingLiveData = MutableLiveData<Boolean>()

    /**
     * Get installed apps for user (as Flow)
     */
    fun getInstalledApps(userId: Int): Flow<List<AppInfo>> {
        return repository.getVmInstallList(userId)
    }

    /**
     * Get available apps to install
     */
    fun getAvailableApps() {
        viewModelScope.launch {
            try {
                loadingLiveData.postValue(true)
                val apps = repository.previewInstallList()
                availableAppsLiveData.postValue(apps)
                loadingLiveData.postValue(false)
            } catch (e: Exception) {
                errorLiveData.postValue("Failed to load apps: ${e.message}")
                loadingLiveData.postValue(false)
            }
        }
    }

    /**
     * Install app
     */
    fun installApp(source: String, userId: Int) {
        viewModelScope.launch {
            loadingLiveData.postValue(true)
            repository.installApk(source, userId, resultLiveData)
            loadingLiveData.postValue(false)
        }
    }

    /**
     * Launch app
     */
    fun launchApp(packageName: String, userId: Int) {
        viewModelScope.launch {
            repository.launchApk(packageName, userId, launchLiveData)
        }
    }

    /**
     * Uninstall app
     */
    fun uninstallApp(packageName: String, userId: Int) {
        viewModelScope.launch {
            loadingLiveData.postValue(true)
            val success = repository.unInstall(packageName, userId)
            resultLiveData.postValue(
                if (success) "App uninstalled successfully"
                else "Failed to uninstall app"
            )
            loadingLiveData.postValue(false)
        }
    }

    /**
     * Clear app data
     */
    fun clearAppData(packageName: String, userId: Int) {
        viewModelScope.launch {
            loadingLiveData.postValue(true)
            val success = repository.clearData(packageName, userId)
            resultLiveData.postValue(
                if (success) "App data cleared"
                else "Failed to clear data"
            )
            loadingLiveData.postValue(false)
        }
    }

    /**
     * Update app position for reordering
     */
    fun updateAppPosition(appId: Long, newPosition: Int) {
        viewModelScope.launch {
            repository.updateAppPosition(appId, newPosition)
        }
    }
}
