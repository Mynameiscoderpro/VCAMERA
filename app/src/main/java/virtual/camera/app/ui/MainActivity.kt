package virtual.camera.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import virtual.camera.app.databinding.ActivityMainBinding
import virtual.camera.app.utils.VideoManager
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoManager: VideoManager

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleVideoSelected(it) }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            openVideoPicker()
        } else {
            Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoManager = VideoManager(this)

        setupUI()
        updateStatus()
    }

    private fun setupUI() {
        // Select Video Button
        binding.selectVideoButton.setOnClickListener {
            checkPermissionsAndPickVideo()
        }

        // Clear Video Button
        binding.testCameraButton.apply {
            text = "🗑️ Clear Video"
            setOnClickListener {
                clearVideo()
            }
        }
    }

    private fun checkPermissionsAndPickVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - No storage permission needed for SAF
            openVideoPicker()
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            
            if (permissions.all { 
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
            }) {
                openVideoPicker()
            } else {
                permissionLauncher.launch(permissions)
            }
        }
    }

    private fun openVideoPicker() {
        try {
            videoPickerLauncher.launch("video/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening video picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVideoSelected(uri: Uri) {
        binding.statusText.text = "🔄 Processing video..."
        
        try {
            // Copy video to app's internal storage
            val success = videoManager.saveVideo(uri)
            
            if (success) {
                updateStatus()
                Toast.makeText(this, "✅ Video saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                binding.statusText.text = "❌ Failed to save video\n\nPlease try again with a different video."
                Toast.makeText(this, "Failed to save video", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            binding.statusText.text = "❌ Error: ${e.message}"
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearVideo() {
        videoManager.clearVideo()
        updateStatus()
        Toast.makeText(this, "🗑️ Video cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val videoFile = videoManager.getVideoFile()
        val moduleActive = checkModuleActive()
        
        if (videoFile != null && videoFile.exists()) {
            val sizeMB = videoFile.length() / (1024 * 1024)
            binding.statusText.text = """
                |✅ VirtuCam is ready!
                |
                |🎥 Video loaded: ${videoFile.name}
                |💾 Size: ${sizeMB}MB
                |📌 Path: ${videoFile.absolutePath}
                |
                |${if (moduleActive) "✅ Module: Active" else "⚠️ Module: Not detected"}
                |
                |📝 Next steps:
                |1. ${if (moduleActive) "Module detected!" else "Enable in MochiCloner/LSPosed"}
                |2. Select target apps in Xposed settings
                |3. Restart target app
                |4. Test your camera!
                |
                |👉 The video will automatically play in all hooked apps!
            """.trimMargin()
        } else {
            binding.statusText.text = """
                |⚠️ No video selected
                |
                |${if (moduleActive) "✅ Module: Active" else "⚠️ Module: Not detected"}
                |
                |📝 Steps:
                |1. Click "Select Video" below
                |2. Choose an MP4 video
                |3. ${if (moduleActive) "Video will work automatically" else "Enable VirtuCam in MochiCloner"}
                |4. Test in any camera app!
                |
                |👉 Without video: Colorful test pattern will show
            """.trimMargin()
        }
    }

    private fun checkModuleActive(): Boolean {
        // Check if running inside virtual environment
        return try {
            // MochiCloner/MetaWolf specific check
            File("/data/data/virtual.camera.app/").exists() ||
            System.getProperty("xposed.bridge") != null
        } catch (e: Exception) {
            false
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
