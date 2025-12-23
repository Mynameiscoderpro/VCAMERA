package virtual.camera.app.view.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import virtual.camera.app.data.AppsRepository

/**
 * Fixed: Updated to use ViewModelProvider.Factory correctly
 */
class ListFactory(private val appsRepository: AppsRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            return ListViewModel(appsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}