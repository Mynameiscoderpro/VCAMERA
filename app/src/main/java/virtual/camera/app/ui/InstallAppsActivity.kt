package virtual.camera.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import virtual.camera.app.databinding.ActivityInstallAppsBinding
import virtual.camera.app.ui.adapters.InstallAppsAdapter
import virtual.camera.app.viewmodel.AppsViewModel

@AndroidEntryPoint
class InstallAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallAppsBinding
    private val viewModel: AppsViewModel by viewModels()
    private lateinit var adapter: InstallAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.getAvailableApps()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Install Apps"
        }
    }

    private fun setupRecyclerView() {
        adapter = InstallAppsAdapter(
            onInstallClick = { appInfo ->
                viewModel.installApp(appInfo.packageName)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@InstallAppsActivity)
            adapter = this@InstallAppsActivity.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.availableAppsLiveData.observe(this) { apps ->
            apps?.let {
                adapter.submitList(it)
                binding.emptyView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadingLiveData.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }

        viewModel.resultLiveData.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
