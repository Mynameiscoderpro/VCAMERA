package virtual.camera.app.view.base

import android.os.Handler
import android.os.Looper

/**
 * Fixed: Removed CatLoadingView dependency
 * Now uses a simple delay mechanism
 */
abstract class LoadingActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isLoadingShown = false

    fun showLoading() {
        if (!isLoadingShown) {
            isLoadingShown = true
            // Show a simple toast or nothing - cat loading removed
            // You could add a ProgressBar here if needed
        }
    }

    fun hideLoading() {
        if (isLoadingShown) {
            isLoadingShown = false
            // Hide loading indicator
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}