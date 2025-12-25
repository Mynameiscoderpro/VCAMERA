package virtual.camera.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import virtual.camera.app.databinding.ActivityMainBinding
import virtual.camera.app.viewmodel.CameraViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Camera"
                1 -> "Apps"
                else -> null
            }
        }.attach()
    }

    private fun observeViewModel() {
        cameraViewModel.serviceStatusLiveData.observe(this) { isRunning ->
            updateServiceStatus(isRunning)
        }

        cameraViewModel.errorLiveData.observe(this) { error ->
            error?.let {
                showError(it)
            }
        }
    }

    private fun updateServiceStatus(isRunning: Boolean) {
        // Update UI to reflect service status
        supportActionBar?.subtitle = if (isRunning) "Service Running" else "Service Stopped"
    }

    private fun showError(message: String) {
        // Show error message using Snackbar or Toast
    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }

    private fun checkServiceStatus() {
        cameraViewModel.checkServiceStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optional: stop service on app close
    }
}
