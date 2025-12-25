package virtual.camera.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import virtual.camera.app.camera.VirtualCameraService
import virtual.camera.app.data.models.CameraConfig
import virtual.camera.app.data.models.VideoSource
import virtual.camera.app.data.models.VideoTransform

class CameraViewModel : ViewModel() {

    val cameraConfigLiveData = MutableLiveData<CameraConfig>()
    val serviceStatusLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String>()

    private var currentConfig = CameraConfig.DEFAULT

    /**
     * Start virtual camera service
     */
    fun startCameraService(context: Context, config: CameraConfig) {
        try {
            currentConfig = config
            VirtualCameraService.startService(context, config)
            serviceStatusLiveData.postValue(true)
            cameraConfigLiveData.postValue(config)
        } catch (e: Exception) {
            errorLiveData.postValue("Failed to start camera service: ${e.message}")
            serviceStatusLiveData.postValue(false)
        }
    }

    /**
     * Stop virtual camera service
     */
    fun stopCameraService(context: Context) {
        try {
            VirtualCameraService.stopService(context)
            serviceStatusLiveData.postValue(false)
        } catch (e: Exception) {
            errorLiveData.postValue("Failed to stop camera service: ${e.message}")
        }
    }

    /**
     * Update camera configuration
     */
    fun updateCameraConfig(config: CameraConfig) {
        currentConfig = config
        VirtualCameraService.getInstance()?.updateConfig(config)
        cameraConfigLiveData.postValue(config)
    }

    /**
     * Set video source
     */
    fun setVideoSource(source: VideoSource, uri: Uri?) {
        val newConfig = currentConfig.copy(
            source = source,
            sourceUri = uri
        )
        updateCameraConfig(newConfig)
    }

    /**
     * Set video transform
     */
    fun setVideoTransform(transform: VideoTransform) {
        val newConfig = currentConfig.copy(transform = transform)
        updateCameraConfig(newConfig)
    }

    /**
     * Toggle camera enabled/disabled
     */
    fun toggleCamera(enabled: Boolean) {
        val newConfig = currentConfig.copy(isEnabled = enabled)
        updateCameraConfig(newConfig)
    }

    /**
     * Check if service is running
     */
    fun checkServiceStatus(): Boolean {
        return VirtualCameraService.getInstance()?.isServiceActive() ?: false
    }

    /**
     * Get current configuration
     */
    fun getCurrentConfig(): CameraConfig = currentConfig
}
