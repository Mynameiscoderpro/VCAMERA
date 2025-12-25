package virtual.camera.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import virtual.camera.app.camera.VirtualCameraService
import virtual.camera.app.data.models.CameraConfig
import virtual.camera.app.data.models.VideoTransform

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _cameraEnabled = MutableLiveData<Boolean>(false)
    val cameraEnabled: LiveData<Boolean> = _cameraEnabled

    private val _selectedVideo = MutableLiveData<String?>()
    val selectedVideo: LiveData<String?> = _selectedVideo

    private val _serviceStatusLiveData = MutableLiveData<Boolean>(false)
    val serviceStatusLiveData: LiveData<Boolean> = _serviceStatusLiveData

    private val _errorLiveData = MutableLiveData<String?>()
    val errorLiveData: LiveData<String?> = _errorLiveData

    fun setVideoSource(videoPath: String) {
        _selectedVideo.value = videoPath
    }

    fun startCameraService(videoPath: String) {
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), VirtualCameraService::class.java)
                intent.action = VirtualCameraService.ACTION_START
                intent.putExtra("VIDEO_PATH", videoPath)
                getApplication<Application>().startService(intent)
                _cameraEnabled.value = true
                _serviceStatusLiveData.value = true
            } catch (e: Exception) {
                _cameraEnabled.value = false
                _serviceStatusLiveData.value = false
                _errorLiveData.value = e.message
            }
        }
    }

    fun stopCameraService() {
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), VirtualCameraService::class.java)
                intent.action = VirtualCameraService.ACTION_STOP
                getApplication<Application>().stopService(intent)
                _cameraEnabled.value = false
                _serviceStatusLiveData.value = false
            } catch (e: Exception) {
                _errorLiveData.value = e.message
            }
        }
    }

    fun checkServiceStatus(): Boolean {
        val status = _cameraEnabled.value ?: false
        _serviceStatusLiveData.value = status
        return status
    }

    fun setVideoTransform(transform: VideoTransform) {
        viewModelScope.launch {
            // TODO: Apply video transformation
        }
    }

    fun saveCameraConfig(config: CameraConfig) {
        viewModelScope.launch {
            // TODO: Save camera configuration
        }
    }
}
