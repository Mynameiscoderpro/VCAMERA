package virtual.camera.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import virtual.camera.app.databinding.FragmentAppsBinding
import virtual.camera.app.ui.InstallAppsActivity
import virtual.camera.app.ui.adapters.AppsAdapter
import virtual.camera.app.viewmodel.AppsViewModel

@AndroidEntryPoint
class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppsViewModel by viewModels()
    private lateinit var adapter: AppsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AppsAdapter(
            onAppClick = { app ->
                viewModel.launchApp(app.packageName)
            },
            onAppLongClick = { app ->
                showAppOptions(app)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AppsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabInstallApp.setOnClickListener {
            startActivity(Intent(requireContext(), InstallAppsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.getInstalledApps().collectLatest { apps ->
                adapter.submitList(apps)
                binding.emptyView.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }

        viewModel.launchLiveData.observe(viewLifecycleOwner) { success ->
            if (success == false) {
                showToast("Failed to launch app")
            }
        }

        viewModel.resultLiveData.observe(viewLifecycleOwner) { result ->
            if (!result.isNullOrEmpty()) {
                showToast(result)
            }
        }
    }

    private fun showAppOptions(app: virtual.camera.app.data.models.AppInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(app.appName)
            .setItems(arrayOf("Launch", "Clear Data", "Uninstall")) { _, which ->
                when (which) {
                    0 -> viewModel.launchApp(app.packageName)
                    1 -> showClearDataConfirmation(app)
                    2 -> showUninstallConfirmation(app)
                }
            }
            .show()
    }

    private fun showClearDataConfirmation(app: virtual.camera.app.data.models.AppInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Data")
            .setMessage("Are you sure you want to clear all data for ${app.appName}?")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearAppData(app.packageName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUninstallConfirmation(app: virtual.camera.app.data.models.AppInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Uninstall")
            .setMessage("Are you sure you want to uninstall ${app.appName}?")
            .setPositiveButton("Uninstall") { _, _ ->
                viewModel.uninstallApp(app.packageName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadInstalledApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
