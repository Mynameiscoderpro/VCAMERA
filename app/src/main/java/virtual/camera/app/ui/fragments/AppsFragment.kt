package virtual.camera.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import virtual.camera.app.R
import virtual.camera.app.databinding.FragmentAppsBinding
import virtual.camera.app.ui.InstallAppsActivity
import virtual.camera.app.ui.adapters.InstalledAppsAdapter
import virtual.camera.app.viewmodel.AppsViewModel

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppsViewModel
    private lateinit var adapter: InstalledAppsAdapter
    private val userId = 0 // Single user mode

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

        viewModel = ViewModelProvider(requireActivity())[AppsViewModel::class.java]

        setupRecyclerView()
        setupFab()
        observeViewModel()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = InstalledAppsAdapter(
            onAppClick = { app -> launchApp(app.packageName) },
            onAppLongClick = { app -> showAppOptions(app.packageName, app.name) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AppsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val intent = Intent(requireContext(), InstallAppsActivity::class.java)
            installAppLauncher.launch(intent)
        }
    }

    private fun observeViewModel() {
        // Observe installed apps
        lifecycleScope.launch {
            viewModel.getInstalledApps(userId).collect { apps ->
                adapter.submitList(apps)
                binding.emptyView.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe loading state
        viewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe launch result
        viewModel.launchLiveData.observe(viewLifecycleOwner) { success ->
            if (!success) {
                showError(getString(R.string.failed_to_launch_app))
            }
        }

        // Observe result messages
        viewModel.resultLiveData.observe(viewLifecycleOwner) { message ->
            showMessage(message)
        }
    }

    private fun loadApps() {
        // Apps are automatically loaded through Flow observation
    }

    private fun launchApp(packageName: String) {
        viewModel.launchApp(packageName, userId)
    }

    private fun showAppOptions(packageName: String, appName: String) {
        val options = arrayOf(
            getString(R.string.launch),
            getString(R.string.clear_data),
            getString(R.string.uninstall)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(packageName)
                    1 -> confirmClearData(packageName, appName)
                    2 -> confirmUninstall(packageName, appName)
                }
            }
            .show()
    }

    private fun confirmClearData(packageName: String, appName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_data)
            .setMessage(getString(R.string.clear_data_confirm, appName))
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.clearAppData(packageName, userId)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun confirmUninstall(packageName: String, appName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.uninstall)
            .setMessage(getString(R.string.uninstall_confirm, appName))
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.uninstallApp(packageName, userId)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private val installAppLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Apps will auto-refresh through Flow
        }
    }

    private fun showMessage(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AppsFragment()
    }
}
