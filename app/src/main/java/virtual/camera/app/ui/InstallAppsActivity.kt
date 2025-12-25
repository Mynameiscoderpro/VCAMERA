package virtual.camera.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import virtual.camera.app.R
import virtual.camera.app.databinding.ActivityInstallAppsBinding
import virtual.camera.app.ui.adapters.AvailableAppsAdapter
import virtual.camera.app.viewmodel.AppsViewModel

class InstallAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallAppsBinding
    private lateinit var viewModel: AppsViewModel
    private lateinit var adapter: AvailableAppsAdapter
    private val userId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AppsViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadAvailableApps()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.install_apps)
        }
    }

    private fun setupRecyclerView() {
        adapter = AvailableAppsAdapter { app ->
            if (app.isInstalled) {
                // Already installed, return to main
                finish()
            } else {
                // Install the app
                viewModel.installApp(app.sourceDir, userId)
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@InstallAppsActivity)
            adapter = this@InstallAppsActivity.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.availableAppsLiveData.observe(this) { apps ->
            adapter.submitList(apps)
            binding.emptyView.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadingLiveData.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.resultLiveData.observe(this) { message ->
            showMessage(message)
            if (message.contains("successfully", ignoreCase = true)) {
                // Installation successful, refresh list
                loadAvailableApps()
            }
        }
    }

    private fun loadAvailableApps() {
        viewModel.getAvailableApps()
    }

    private fun showMessage(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
