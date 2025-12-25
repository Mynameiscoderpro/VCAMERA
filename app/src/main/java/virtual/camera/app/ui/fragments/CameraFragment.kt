package virtual.camera.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import virtual.camera.app.R
import virtual.camera.app.databinding.FragmentCameraBinding
import virtual.camera.app.data.models.CameraConfig
import virtual.camera.app.data.models.VideoSource
import virtual.camera.app.data.models.VideoTransform
import virtual.camera.app.viewmodel.CameraViewModel

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CameraViewModel
    private var currentTransform = VideoTransform.DEFAULT
    private var currentUri: Uri? = null
    private var currentSource = VideoSource.NONE

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

        viewModel = ViewModelProvider(requireActivity())[CameraViewModel::class.java]

        setupSourceSelection()
        setupTransformControls()
        setupButtons()
        observeViewModel()
    }

    private fun setupSourceSelection() {
        binding.radioGroupSource.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioRealCamera -> {
                    currentSource = VideoSource.REAL_CAMERA
                    showTransformControls(false)
                }
                R.id.radioImage -> {
                    selectImage()
                }
                R.id.radioLocalVideo -> {
                    selectVideo()
                }
                R.id.radioNetworkVideo -> {
                    showNetworkVideoDialog()
                }
                R.id.radioNone -> {
                    currentSource = VideoSource.NONE
                    showTransformControls(false)
                }
            }
        }
    }

    private fun setupTransformControls() {
        // Rotation
        binding.seekBarRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rotation = (progress - 180).toFloat()
                binding.textRotation.text = getString(R.string.rotation_value, rotation)
                currentTransform = currentTransform.copy(rotation = rotation)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateTransform()
            }
        })

        // Scale
        binding.seekBarScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = progress / 100f
                binding.textScale.text = getString(R.string.scale_value, scale)
                currentTransform = currentTransform.copy(scaleX = scale, scaleY = scale)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateTransform()
            }
        })

        // Brightness
        binding.seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = (progress - 50) / 100f
                binding.textBrightness.text = getString(R.string.brightness_value, brightness)
                currentTransform = currentTransform.copy(brightness = brightness)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateTransform()
            }
        })

        // Flip horizontal
        binding.switchFlipHorizontal.setOnCheckedChangeListener { _, isChecked ->
            currentTransform = currentTransform.copy(flipHorizontal = isChecked)
            updateTransform()
        }

        // Flip vertical
        binding.switchFlipVertical.setOnCheckedChangeListener { _, isChecked ->
            currentTransform = currentTransform.copy(flipVertical = isChecked)
            updateTransform()
        }
    }

    private fun setupButtons() {
        binding.buttonStartCamera.setOnClickListener {
            startCameraService()
        }

        binding.buttonStopCamera.setOnClickListener {
            stopCameraService()
        }

        binding.buttonResetTransform.setOnClickListener {
            resetTransform()
        }
    }

    private fun observeViewModel() {
        viewModel.serviceStatusLiveData.observe(viewLifecycleOwner) { isRunning ->
            updateServiceStatus(isRunning)
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            showError(error)
        }
    }

    private fun updateServiceStatus(isRunning: Boolean) {
        binding.textServiceStatus.text = if (isRunning) {
            getString(R.string.service_running)
        } else {
            getString(R.string.service_stopped)
        }

        binding.buttonStartCamera.isEnabled = !isRunning
        binding.buttonStopCamera.isEnabled = isRunning
    }

    private fun showTransformControls(show: Boolean) {
        binding.layoutTransformControls.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun selectImage() {
        imagePicker.launch("image/*")
    }

    private fun selectVideo() {
        videoPicker.launch("video/*")
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            currentUri = it
            currentSource = VideoSource.IMAGE
            binding.textSelectedFile.text = getString(R.string.file_selected)
            showTransformControls(true)
        }
    }

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            currentUri = it
            currentSource = VideoSource.LOCAL_VIDEO
            binding.textSelectedFile.text = getString(R.string.file_selected)
            showTransformControls(true)
        }
    }

    private fun showNetworkVideoDialog() {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "https://example.com/video.mp4"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.network_video_url)
            .setView(editText)
            .setPositiveButton(R.string.ok) { _, _ ->
                val url = editText.text.toString()
                if (url.isNotEmpty()) {
                    currentUri = Uri.parse(url)
                    currentSource = VideoSource.NETWORK_VIDEO
                    binding.textSelectedFile.text = url
                    showTransformControls(true)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startCameraService() {
        val config = CameraConfig(
            source = currentSource,
            sourceUri = currentUri,
            transform = currentTransform,
            isEnabled = true
        )
        viewModel.startCameraService(requireContext(), config)
    }

    private fun stopCameraService() {
        viewModel.stopCameraService(requireContext())
    }

    private fun updateTransform() {
        viewModel.setVideoTransform(currentTransform)
    }

    private fun resetTransform() {
        currentTransform = VideoTransform.DEFAULT
        binding.seekBarRotation.progress = 180
        binding.seekBarScale.progress = 100
        binding.seekBarBrightness.progress = 50
        binding.switchFlipHorizontal.isChecked = false
        binding.switchFlipVertical.isChecked = false
        updateTransform()
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
        fun newInstance() = CameraFragment()
    }
}
