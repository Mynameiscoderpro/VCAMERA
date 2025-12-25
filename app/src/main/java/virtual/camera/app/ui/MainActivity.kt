package virtual.camera.app.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import virtual.camera.app.R
import virtual.camera.app.databinding.ActivityMainBinding
import virtual.camera.app.utils.PermissionHelper
import virtual.camera.app.viewmodel.AppsViewModel
import virtual.camera.app.viewmodel.CameraViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appsViewModel: AppsViewModel
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var pagerAdapter: MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModels
        appsViewModel = ViewModelProvider(this)[AppsViewModel::class.java]
        cameraViewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayShowTitleEnabled(true)
        }

        // Check permissions
        if (!PermissionHelper.hasAllPermissions(this)) {
            requestPermissions()
        } else {
            setupViewPager()
        }

        // Observe service status
        observeServiceStatus()
    }

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_apps)
                1 -> getString(R.string.tab_camera)
                2 -> getString(R.string.tab_settings)
                else -> ""
            }
        }.attach()

        // Optional: Disable swipe between tabs
        binding.viewPager.isUserInputEnabled = true
    }

    private fun observeServiceStatus() {
        cameraViewModel.serviceStatusLiveData.observe(this) { isRunning ->
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Update camera toggle menu item
        menu?.findItem(R.id.action_toggle_camera)?.apply {
            val isRunning = cameraViewModel.checkServiceStatus()
            title = if (isRunning) {
                getString(R.string.stop_camera)
            } else {
                getString(R.string.start_camera)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_camera -> {
                toggleCameraService()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleCameraService() {
        val isRunning = cameraViewModel.checkServiceStatus()
        if (isRunning) {
            cameraViewModel.stopCameraService(this)
        } else {
            // Switch to camera tab
            binding.viewPager.currentItem = 1
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun requestPermissions() {
        permissionLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupViewPager()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_required)
            .setMessage(R.string.permissions_required_message)
            .setPositiveButton(R.string.grant) { _, _ ->
                requestPermissions()
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop service on activity destroy
    }
}