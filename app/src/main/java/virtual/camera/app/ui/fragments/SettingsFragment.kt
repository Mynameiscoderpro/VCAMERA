package virtual.camera.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import virtual.camera.app.R
import virtual.camera.app.databinding.FragmentSettingsBinding
import virtual.camera.app.utils.PreferenceHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceHelper = PreferenceHelper(requireContext())

        setupViews()
        loadSettings()
    }

    private fun setupViews() {
        // User remark
        binding.editUserRemark.setText(preferenceHelper.userRemark)

        binding.buttonSaveRemark.setOnClickListener {
            val remark = binding.editUserRemark.text.toString()
            preferenceHelper.userRemark = remark
            showMessage(getString(R.string.settings_saved))
        }

        // Clear all data
        binding.buttonClearAllData.setOnClickListener {
            showClearDataDialog()
        }

        // About
        binding.buttonAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun loadSettings() {
        binding.textAppVersion.text = getAppVersion()
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun showClearDataDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_all_data)
            .setMessage(R.string.clear_all_data_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                preferenceHelper.clearAll()
                showMessage(getString(R.string.data_cleared))
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.about)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showMessage(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
