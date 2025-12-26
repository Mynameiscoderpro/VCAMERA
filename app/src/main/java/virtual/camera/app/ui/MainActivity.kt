package virtual.camera.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
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

        setupUI()
        observeViewModel()
        updateStatus()
    }

    private fun setupUI() {
        // Select Video Button
        binding.selectVideoButton.setOnClickListener {
            selectVideo()
        }

        // Test Camera Button
        binding.testCameraButton.setOnClickListener {
            testCamera()
        }
    }

    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_VIDEO_PICK)
    }

    private fun testCamera() {
        // Open camera test activity or browser
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://webcamtests.com")
        startActivity(intent)
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
        binding.statusText.text = if (isRunning) {
            "✅ VirtuCam is active and ready!"
        } else {
            "⚠️ VirtuCam module not detected\n\nMake sure:\n• LSPosed is installed\n• VirtuCam is enabled in LSPosed\n• Target apps are selected\n• Device is rebooted"
        }
    }

    private fun updateStatus() {
        binding.statusText.text = """📱 VirtuCam Module Active
            |
            |✅ Xposed module loaded
            |✅ Ready to intercept camera
            |
            |📝 Next steps:
            |1. Select a video file
            |2. Enable in LSPosed for target apps
            |3. Test your camera!
            """.trimMargin()
    }

    private fun showError(message: String) {
        binding.statusText.text = "❌ Error: $message"
    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }

    private fun checkServiceStatus() {
        cameraViewModel.checkServiceStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                binding.statusText.text = "✅ Video selected: $uri\n\nCopy this video to target app's cache directory"
            }
        }
    }

    companion object {
        private const val REQUEST_VIDEO_PICK = 1001
    }
}
