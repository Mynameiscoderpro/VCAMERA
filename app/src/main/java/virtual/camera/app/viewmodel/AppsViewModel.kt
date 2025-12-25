package virtual.camera.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import virtual.camera.app.data.models.AppInfo
import virtual.camera.app.data.repository.AppsRepository

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppsRepository(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _installedApps.value = repository.getInstalledApps()
            } catch (e: Exception) {
                _installedApps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun installApp(packageName: String) {
        viewModelScope.launch {
            try {
                repository.installApk(packageName)
                loadInstalledApps()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            repository.launchApk(packageName)
        }
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            try {
                repository.unInstall(packageName)
                loadInstalledApps()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun clearAppData(packageName: String) {
        viewModelScope.launch {
            try {
                repository.clearData(packageName)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
