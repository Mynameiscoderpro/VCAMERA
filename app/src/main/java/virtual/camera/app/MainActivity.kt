package virtual.camera.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var selectVideoButton: Button
    private lateinit var testCameraButton: Button

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                copyVideoToStandardLocation(uri)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            updateStatus("✅ Permissions granted!")
        } else {
            updateStatus("❌ Some permissions denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        selectVideoButton = findViewById(R.id.selectVideoButton)
        testCameraButton = findViewById(R.id.testCameraButton)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        selectVideoButton.setOnClickListener {
            openVideoPicker()
        }

        testCameraButton.setOnClickListener {
            openTestWebsite()
        }

        updateStatus("Ready to select video")
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            permissionLauncher.launch(permissions.toTypedArray())
        }

        // Check for MANAGE_EXTERNAL_STORAGE on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                updateStatus("⚠️ Need All Files Access permission")
                Toast.makeText(
                    this,
                    "Please grant 'All Files Access' permission",
                    Toast.LENGTH_LONG
                ).show()
                
                // Open settings
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
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

    private fun copyVideoToStandardLocation(uri: Uri) {
        try {
            updateStatus("📋 Copying video...")

            // Copy to multiple standard locations for maximum compatibility
            val locations = listOf(
                File(Environment.getExternalStorageDirectory(), "DCIM/Camera/virtual.mp4"),
                File(Environment.getExternalStorageDirectory(), "Download/virtual.mp4"),
                File(Environment.getExternalStorageDirectory(), "Movies/virtual.mp4")
            )

            var successCount = 0
            contentResolver.openInputStream(uri)?.use { input ->
                locations.forEach { targetFile ->
                    try {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                        successCount++
                        updateStatus("✅ Copied to: ${targetFile.absolutePath}")
                    } catch (e: Exception) {
                        updateStatus("⚠️ Failed to copy to: ${targetFile.absolutePath}")
                    }
                    // Reset input stream position for next copy
                    input.reset()
                }
            }

            if (successCount > 0) {
                Toast.makeText(
                    this,
                    "✅ Video saved to $successCount locations!",
                    Toast.LENGTH_LONG
                ).show()
                updateStatus("✅ Video ready! Copied to $successCount locations")
            } else {
                Toast.makeText(
                    this,
                    "❌ Failed to save video",
                    Toast.LENGTH_SHORT
                ).show()
                updateStatus("❌ Failed to save video")
            }
        } catch (e: Exception) {
            updateStatus("❌ Error: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTestWebsite() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://veed.io/tools/webcam-test"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }
}
