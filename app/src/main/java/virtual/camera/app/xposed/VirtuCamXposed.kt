package virtual.camera.app.xposed

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.nio.ByteBuffer

/**
 * VirtuCam - Virtual Camera Using MediaCodec Architecture
 * Based on VCamX/XVirtualCamera approach
 */
class VirtuCamXposed : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "VirtuCam"
        private val activeDecoders = mutableMapOf<String, VideoDecoder>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Don't hook ourselves
        if (lpparam.packageName == "virtual.camera.app") {
            return
        }

        try {
            hookCamera2(lpparam)
            XposedBridge.log("$TAG: ✅ Hooked ${lpparam.packageName}")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Hook failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun hookCamera2(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook CameraManager.openCamera to log camera opening
            val cameraManagerClass = XposedHelpers.findClass(
                "android.hardware.camera2.CameraManager",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                cameraManagerClass,
                "openCamera",
                String::class.java,
                "android.hardware.camera2.CameraDevice\$StateCallback",
                Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val cameraId = param.args[0] as String
                        XposedBridge.log("$TAG: 📷 Opening camera $cameraId for ${lpparam.packageName}")
                    }
                }
            )

            // Hook createCaptureSession - this is where we inject our video
            hookCreateCaptureSession(lpparam)

            XposedBridge.log("$TAG: ✅ Camera2 hooks installed")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Camera2 hook failed: ${e.message}")
            throw e
        }
    }

    private fun hookCreateCaptureSession(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cameraDeviceImplClass = XposedHelpers.findClass(
                "android.hardware.camera2.impl.CameraDeviceImpl",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                cameraDeviceImplClass,
                "createCaptureSession",
                List::class.java,
                "android.hardware.camera2.CameraCaptureSession\$StateCallback",
                Handler::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val surfaces = param.args[0] as? List<Surface>
                            if (surfaces.isNullOrEmpty()) {
                                XposedBridge.log("$TAG: ⚠️ No surfaces provided")
                                return
                            }

                            val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                            if (context == null) {
                                XposedBridge.log("$TAG: ⚠️ Context is null")
                                return
                            }

                            // Find video file
                            val videoPath = findVideoFile(context, lpparam.packageName)
                            if (videoPath == null) {
                                XposedBridge.log("$TAG: ⚠️ No video file found")
                                return
                            }

                            XposedBridge.log("$TAG: 🎥 Found video: $videoPath")

                            // Get the first surface (preview surface)
                            val originalSurface = surfaces[0]

                            // Stop any existing decoder
                            activeDecoders[lpparam.packageName]?.stop()

                            // Create and start video decoder
                            val decoder = VideoDecoder(videoPath, originalSurface, lpparam.packageName)
                            decoder.start()

                            activeDecoders[lpparam.packageName] = decoder

                            XposedBridge.log("$TAG: ✅ Virtual camera activated!")
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG: ❌ Session hook error: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: Hooked createCaptureSession")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to hook createCaptureSession: ${e.message}")
            throw e
        }
    }

    /**
     * Find video file in standard Android locations
     */
    private fun findVideoFile(context: Context, packageName: String): String? {
        // Priority order:
        // 1. App's cache directory: /storage/emulated/0/Android/data/[package]/cache/virtual.mp4
        // 2. Standard paths
        
        val paths = listOf(
            "/storage/emulated/0/Android/data/$packageName/cache/virtual.mp4",
            "/storage/emulated/0/DCIM/Camera/virtual.mp4",
            "/storage/emulated/0/Download/virtual.mp4",
            "/storage/emulated/0/Movies/virtual.mp4",
            "/sdcard/DCIM/Camera/virtual.mp4",
            "/sdcard/Download/virtual.mp4",
            "/sdcard/Movies/virtual.mp4"
        )

        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canRead() && file.length() > 0) {
                XposedBridge.log("$TAG: ✅ Found video at: $path")
                return path
            }
        }

        XposedBridge.log("$TAG: ❌ No video found. Tried: ${paths.joinToString(", ")}")
        return null
    }

    /**
     * Video decoder using MediaCodec
     */
    class VideoDecoder(private val videoPath: String, private val outputSurface: Surface, private val tag: String) {
        private var extractor: MediaExtractor? = null
        private var decoder: MediaCodec? = null
        private var decoderThread: HandlerThread? = null
        @Volatile
        private var running = false

        fun start() {
            try {
                XposedBridge.log("VirtuCam: 🎬 Starting video decoder for $videoPath")

                // Create extractor
                extractor = MediaExtractor().apply {
                    setDataSource(videoPath)
                }

                // Find video track
                val videoTrackIndex = selectVideoTrack(extractor!!)
                if (videoTrackIndex < 0) {
                    XposedBridge.log("VirtuCam: ❌ No video track found")
                    return
                }

                extractor!!.selectTrack(videoTrackIndex)
                val format = extractor!!.getTrackFormat(videoTrackIndex)

                XposedBridge.log("VirtuCam: 🎥 Video format: $format")

                // Create decoder
                val mime = format.getString(MediaFormat.KEY_MIME)!!
                decoder = MediaCodec.createDecoderByType(mime)
                decoder!!.configure(format, outputSurface, null, 0)
                decoder!!.start()

                running = true

                // Start decoding thread
                decoderThread = HandlerThread("VirtuCam-Decoder-$tag").apply {
                    start()
                }

                Handler(decoderThread!!.looper).post {
                    decodeLoop()
                }

                XposedBridge.log("VirtuCam: ✅ Decoder started successfully")
            } catch (e: Exception) {
                XposedBridge.log("VirtuCam: ❌ Decoder start failed: ${e.message}")
                e.printStackTrace()
                stop()
            }
        }

        private fun selectVideoTrack(extractor: MediaExtractor): Int {
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    return i
                }
            }
            return -1
        }

        private fun decodeLoop() {
            val bufferInfo = MediaCodec.BufferInfo()
            var frameCount = 0
            val startTime = System.nanoTime()

            try {
                while (running) {
                    // Feed input
                    val inputBufferId = decoder!!.dequeueInputBuffer(10000)
                    if (inputBufferId >= 0) {
                        val inputBuffer = decoder!!.getInputBuffer(inputBufferId)!!
                        val sampleSize = extractor!!.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            // End of stream, loop video
                            XposedBridge.log("VirtuCam: 🔄 Video ended, looping...")
                            extractor!!.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            continue
                        }

                        val presentationTimeUs = extractor!!.sampleTime
                        decoder!!.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                        extractor!!.advance()
                    }

                    // Get output
                    val outputBufferId = decoder!!.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferId >= 0) {
                        // Render frame to surface
                        val doRender = bufferInfo.size != 0
                        decoder!!.releaseOutputBuffer(outputBufferId, doRender)

                        if (doRender) {
                            frameCount++
                            if (frameCount % 30 == 0) {
                                XposedBridge.log("VirtuCam: 🎬 Frame $frameCount rendered")
                            }
                        }

                        // Maintain frame rate
                        val elapsedTime = (System.nanoTime() - startTime) / 1000
                        val expectedTime = bufferInfo.presentationTimeUs
                        val sleepTime = expectedTime - elapsedTime
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime / 1000, (sleepTime % 1000).toInt())
                        }
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    XposedBridge.log("VirtuCam: ❌ Decode error: ${e.message}")
                    e.printStackTrace()
                }
            } finally {
                XposedBridge.log("VirtuCam: 🛑 Decode loop stopped")
            }
        }

        fun stop() {
            running = false
            try {
                decoder?.stop()
                decoder?.release()
                extractor?.release()
                decoderThread?.quitSafely()
            } catch (e: Exception) {
                XposedBridge.log("VirtuCam: Stop error: ${e.message}")
            }
        }
    }
}
