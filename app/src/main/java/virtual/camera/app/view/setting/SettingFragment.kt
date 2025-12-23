package virtual.camera.app.view.setting

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import virtual.camera.app.R
import virtual.camera.core.CameraConfig
import virtual.camera.core.service.VirtualCameraService
import java.io.File
import android.util.Size

class SettingFragment : Fragment() {

    private lateinit var methodButton: Button
    private lateinit var methodText: TextView
    private lateinit var tipText: TextView
    private lateinit var videoInput: EditText
    private lateinit var videoSelectButton: Button
    private lateinit var audioSwitch: Switch
    private lateinit var saveButton: Button
    private lateinit var ratioSpinner: Spinner
    private lateinit var resolutionSpinner: Spinner

    private var currentConfig = CameraConfig()
    private val handler = Handler(Looper.getMainLooper())

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            videoInput.setText(it.toString())
            currentConfig = currentConfig.copy(videoSource = it.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_camera_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        loadCurrentConfig()
        setupListeners()
        setupSpinners()
    }

    private fun initializeViews(view: View) {
        methodButton = view.findViewById(R.id.protect_method_btn)
        methodText = view.findViewById(R.id.protect_method_text)
        tipText = view.findViewById(R.id.protect_tip)
        videoInput = view.findViewById(R.id.protect_path)
        videoSelectButton = view.findViewById(R.id.protect_video_select)
        audioSwitch = view.findViewById(R.id.protect_audio_switch)
        saveButton = view.findViewById(R.id.protect_save)
        ratioSpinner = view.findViewById(R.id.ratio_spinner)
        resolutionSpinner = view.findViewById(R.id.resolution_spinner)
    }

    private fun loadCurrentConfig() {
        currentConfig = CameraConfig.load(requireContext())

        when (currentConfig.methodType) {
            CameraConfig.METHOD_DISABLE -> showDisableMode()
            CameraConfig.METHOD_GALLERY_VIDEO -> showGalleryMode()
            CameraConfig.METHOD_NETWORK_STREAM -> showNetworkMode()
        }

        videoInput.setText(currentConfig.videoSource)
        audioSwitch.isChecked = currentConfig.enableAudio

        // Update UI state
        updateResolutionSpinner(currentConfig.resolution)
        updateRatioSpinner(currentConfig.aspectRatio)
    }

    private fun setupListeners() {
        methodButton.setOnClickListener {
            showMethodSelectionDialog()
        }

        videoSelectButton.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }

        saveButton.setOnClickListener {
            saveAndApplyConfig()
        }

        audioSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentConfig = currentConfig.copy(enableAudio = isChecked)
        }
    }

    private fun setupSpinners() {
        // Aspect Ratio Spinner
        val ratios = listOf(
            "16:9" to (16f / 9f),
            "4:3" to (4f / 3f),
            "1:1" to 1f,
            "9:16" to (9f / 16f)
        )

        ratioSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ratios.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        ratioSpinner.setSelection(ratios.indexOfFirst {
            Math.abs(it.second - currentConfig.aspectRatio) < 0.01f
        })

        ratioSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentConfig = currentConfig.copy(aspectRatio = ratios[pos].second)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Resolution Spinner
        val resolutions = listOf(
            "Auto" to null,
            "4K (3840x2160)" to Size(3840, 2160),
            "1080p (1920x1080)" to Size(1920, 1080),
            "720p (1280x720)" to Size(1280, 720),
            "480p (854x480)" to Size(854, 480)
        )

        resolutionSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            resolutions.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        resolutionSpinner.setSelection(resolutions.indexOfFirst {
            it.second?.width == currentConfig.resolution.width &&
                    it.second?.height == currentConfig.resolution.height
        })

        resolutionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                resolutions[pos].second?.let {
                    currentConfig = currentConfig.copy(resolution = it)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showMethodSelectionDialog() {
        val methods = listOf(
            getString(R.string.protect_method_disable_camera),
            getString(R.string.protect_method_local),
            getString(R.string.protect_method_network)
        )

        MaterialDialog(requireContext()).show {
            title(R.string.protect_method)
            listItemsSingleChoice(items = methods, initialSelection = currentConfig.methodType) { _, index, _ ->
                currentConfig = currentConfig.copy(methodType = index)
                when (index) {
                    CameraConfig.METHOD_DISABLE -> showDisableMode()
                    CameraConfig.METHOD_GALLERY_VIDEO -> showGalleryMode()
                    CameraConfig.METHOD_NETWORK_STREAM -> showNetworkMode()
                }
            }
        }
    }

    private fun showDisableMode() {
        methodText.text = getString(R.string.protect_method_disable_camera)
        tipText.text = getString(R.string.protect_tip_disable)
        videoInput.visibility = View.GONE
        videoSelectButton.visibility = View.GONE
        audioSwitch.visibility = View.GONE
        ratioSpinner.visibility = View.GONE
        resolutionSpinner.visibility = View.GONE
    }

    private fun showGalleryMode() {
        methodText.text = getString(R.string.protect_method_local)
        tipText.text = getString(R.string.protect_tip_local)
        videoInput.visibility = View.VISIBLE
        videoSelectButton.visibility = View.VISIBLE
        videoSelectButton.text = getString(R.string.choise_video)
        videoInput.isEnabled = false
        audioSwitch.visibility = View.VISIBLE
        ratioSpinner.visibility = View.VISIBLE
        resolutionSpinner.visibility = View.VISIBLE
    }

    private fun showNetworkMode() {
        methodText.text = getString(R.string.protect_method_network)
        tipText.text = getString(R.string.protect_tip_network)
        videoInput.visibility = View.VISIBLE
        videoSelectButton.visibility = View.GONE
        videoInput.isEnabled = true
        videoInput.hint = getString(R.string.protect_path_hint)
        audioSwitch.visibility = View.VISIBLE
        ratioSpinner.visibility = View.VISIBLE
        resolutionSpinner.visibility = View.VISIBLE
    }

    private fun saveAndApplyConfig() {
        // Validate network URL
        if (currentConfig.methodType == CameraConfig.METHOD_NETWORK_STREAM) {
            val url = videoInput.text.toString()
            if (!url.startsWith("http") && !url.startsWith("rtmp") && !url.startsWith("rtsp")) {
                Toast.makeText(context, "URL must start with http/https/rtmp/rtsp", Toast.LENGTH_SHORT).show()
                return
            }
            currentConfig = currentConfig.copy(videoSource = url)
        }

        // Show progress
        val progressDialog = ProgressDialog(context).apply {
            setMessage("Processing video...")
            setCancelable(false)
            show()
        }

        // Process on background thread
        Thread {
            try {
                // Copy local video to internal storage if needed
                if (currentConfig.methodType == CameraConfig.METHOD_GALLERY_VIDEO) {
                    val uri = Uri.parse(currentConfig.videoSource)
                    val inputStream = context?.contentResolver?.openInputStream(uri)
                    val outputFile = File(context!!.filesDir, "virtual_camera_video.mp4")

                    inputStream?.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    currentConfig = currentConfig.copy(videoSource = outputFile.absolutePath)
                }

                // Save config
                currentConfig.save(requireContext())

                // Restart service if running
                if (VirtualCameraService.isRunning) {
                    context?.stopService(Intent(context, VirtualCameraService::class.java))
                }

                if (currentConfig.methodType != CameraConfig.METHOD_DISABLE) {
                    val serviceIntent = Intent(context, VirtualCameraService::class.java).apply {
                        action = VirtualCameraService.ACTION_START
                        putExtra(VirtualCameraService.EXTRA_CONFIG, currentConfig)
                    }
                    context?.startForegroundService(serviceIntent)
                }

                handler.post {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Settings saved and applied", Toast.LENGTH_SHORT).show()
                    activity?.onBackPressed()
                }

            } catch (e: Exception) {
                handler.post {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun updateResolutionSpinner(resolution: Size) {
        // Update spinner selection based on resolution
    }

    private fun updateRatioSpinner(ratio: Float) {
        // Update spinner selection based on aspect ratio
    }
}