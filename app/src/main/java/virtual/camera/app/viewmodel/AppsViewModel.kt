package virtual.camera.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import virtual.camera.app.data.models.AppInfo
import virtual.camera.app.data.repository.AppsRepository

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppsRepository(application)

    // StateFlow for installed apps
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // Loading states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadingLiveData = MutableLiveData<Boolean>(false)
    val loadingLiveData: LiveData<Boolean> = _loadingLiveData

    // Launch result
    private val _launchLiveData = MutableLiveData<Boolean>()
    val launchLiveData: LiveData<Boolean> = _launchLiveData

    // Operation results
    private val _resultLiveData = MutableLiveData<String?>()
    val resultLiveData: LiveData<String?> = _resultLiveData

    // Available apps (all device apps)
    private val _availableAppsLiveData = MutableLiveData<List<AppInfo>>()
    val availableAppsLiveData: LiveData<List<AppInfo>> = _availableAppsLiveData

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingLiveData.value = true
            try {
                _installedApps.value = repository.getInstalledApps()
            } catch (e: Exception) {
                _installedApps.value = emptyList()
            } finally {
                _isLoading.value = false
                _loadingLiveData.value = false
            }
        }
    }

    fun getAvailableApps() {
        viewModelScope.launch {
            _loadingLiveData.value = true
            try {
                // Get all device apps excluding VCamera itself
                _availableAppsLiveData.value = repository.getAllDeviceApps()
            } catch (e: Exception) {
                _availableAppsLiveData.value = emptyList()
            } finally {
                _loadingLiveData.value = false
            }
        }
    }

    fun installApp(packageName: String) {
        viewModelScope.launch {
            try {
                val result = repository.installApk(packageName)
                _resultLiveData.value = if (result) "Installed successfully" else "Installation failed"
                loadInstalledApps()
            } catch (e: Exception) {
                _resultLiveData.value = "Error: ${e.message}"
            }
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            val result = repository.launchApk(packageName)
            _launchLiveData.value = result
        }
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            try {
                val result = repository.unInstall(packageName)
                _resultLiveData.value = if (result) "Uninstalled successfully" else "Uninstall failed"
                loadInstalledApps()
            } catch (e: Exception) {
                _resultLiveData.value = "Error: ${e.message}"
            }
        }
    }

    fun clearAppData(packageName: String) {
        viewModelScope.launch {
            try {
                val result = repository.clearData(packageName)
                _resultLiveData.value = if (result) "Data cleared" else "Clear failed"
            } catch (e: Exception) {
                _resultLiveData.value = "Error: ${e.message}"
            }
        }
    }
}
