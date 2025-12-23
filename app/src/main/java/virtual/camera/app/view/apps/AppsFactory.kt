package virtual.camera.app.view.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import virtual.camera.app.data.AppsRepository

/**
 * Fixed: Updated to use ViewModelProvider.Factory correctly
 */
class AppsFactory(private val appsRepository: AppsRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
            return AppsViewModel(appsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}