package virtual.camera.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import virtual.camera.app.databinding.FragmentCameraBinding
import virtual.camera.app.viewmodel.CameraViewModel

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()
    private var selectedVideoPath: String? = null

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleVideoSelection(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnSelectVideo.setOnClickListener {
            openVideoPicker()
        }

        binding.btnStartCamera.setOnClickListener {
            startVirtualCamera()
        }

        binding.btnStopCamera.setOnClickListener {
            stopVirtualCamera()
        }

        binding.btnSettings.setOnClickListener {
            showSettings()
        }
    }

    private fun observeViewModel() {
        viewModel.cameraEnabled.observe(viewLifecycleOwner) { enabled ->
            updateCameraState(enabled)
        }

        viewModel.selectedVideo.observe(viewLifecycleOwner) { videoPath ->
            selectedVideoPath = videoPath
            updateVideoDisplay(videoPath)
        }

        viewModel.serviceStatusLiveData.observe(viewLifecycleOwner) { isRunning ->
            updateServiceStatus(isRunning)
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
            }
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        videoPickerLauncher.launch(intent)
    }

    private fun handleVideoSelection(uri: Uri) {
        val path = getRealPathFromUri(uri)
        path?.let {
            selectedVideoPath = it
            viewModel.setVideoSource(it)
            binding.txtSelectedVideo.text = "Video: ${uri.lastPathSegment}"
            binding.btnStartCamera.isEnabled = true
        } ?: run {
            showError("Failed to get video path")
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        // Simple implementation - in production, use proper URI to path conversion
        return uri.path
    }

    private fun updateVideoDisplay(videoPath: String?) {
        if (videoPath != null) {
            binding.txtSelectedVideo.text = "Video: $videoPath"
            binding.btnStartCamera.isEnabled = true
        } else {
            binding.txtSelectedVideo.text = "No video selected"
            binding.btnStartCamera.isEnabled = false
        }
    }

    private fun updateCameraState(enabled: Boolean) {
        binding.btnStartCamera.isEnabled = !enabled && selectedVideoPath != null
        binding.btnStopCamera.isEnabled = enabled
        binding.btnSelectVideo.isEnabled = !enabled

        binding.statusIndicator.setBackgroundColor(
            if (enabled) {
                android.graphics.Color.GREEN
            } else {
                android.graphics.Color.RED
            }
        )
    }

    private fun updateServiceStatus(isRunning: Boolean) {
        binding.txtStatus.text = if (isRunning) {
            "Virtual Camera: Active"
        } else {
            "Virtual Camera: Inactive"
        }
    }

    private fun startVirtualCamera() {
        val videoPath = selectedVideoPath
        if (videoPath != null) {
            viewModel.startCameraService(videoPath)
        } else {
            showError("Please select a video first")
        }
    }

    private fun stopVirtualCamera() {
        viewModel.stopCameraService()
    }

    private fun showSettings() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Camera Settings")
            .setMessage("Settings feature coming soon")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
