package virtual.camera.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import virtual.camera.app.R
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

        setupNavigation()
        observeViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setupWithNavController(navController)
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
    }

    private fun showError(message: String) {
        // Show error message
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
        if (cameraViewModel.checkServiceStatus() == true) {
            cameraViewModel.stopCameraService()
        }
    }
}
