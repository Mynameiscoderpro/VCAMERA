package virtual.camera.app.xposed

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import de.robv.android.xposed.XposedBridge
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * Video decoder that extracts frames from video file
 * Based on vcamsx architecture
 */
class VideoDecoder(private val videoPath: String) {
    companion object {
        private const val TAG = "VirtuCam-Decoder"
        private const val FRAME_QUEUE_SIZE = 30
    }

    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    private var surface: Surface? = null
    private var isDecoding = false
    private var decoderThread: Thread? = null
    private val frameQueue = LinkedBlockingQueue<DecodedFrame>(FRAME_QUEUE_SIZE)
    
    private var width = 0
    private var height = 0
    private var frameRate = 30
    private var durationUs = 0L
    private var currentPositionUs = 0L

    data class DecodedFrame(
        val buffer: ByteBuffer,
        val width: Int,
        val height: Int,
        val timestamp: Long
    )

    /**
     * Initialize the video decoder
     */
    fun initialize(): Boolean {
        try {
            mediaExtractor = MediaExtractor()
            mediaExtractor?.setDataSource(videoPath)

            // Find video track
            var trackIndex = -1
            for (i in 0 until (mediaExtractor?.trackCount ?: 0)) {
                val format = mediaExtractor?.getTrackFormat(i)
                val mime = format?.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    trackIndex = i
                    width = format?.getInteger(MediaFormat.KEY_WIDTH) ?: 640
                    height = format?.getInteger(MediaFormat.KEY_HEIGHT) ?: 480
                    frameRate = format?.getInteger(MediaFormat.KEY_FRAME_RATE) ?: 30
                    durationUs = format?.getLong(MediaFormat.KEY_DURATION) ?: 0L
                    break
                }
            }

            if (trackIndex == -1) {
                XposedBridge.log("$TAG: No video track found")
                return false
            }

            mediaExtractor?.selectTrack(trackIndex)
            val format = mediaExtractor?.getTrackFormat(trackIndex)
            val mime = format?.getString(MediaFormat.KEY_MIME) ?: return false

            // Create decoder
            mediaCodec = MediaCodec.createDecoderByType(mime)
            mediaCodec?.configure(format, null, null, 0)
            
            XposedBridge.log("$TAG: ✅ Initialized: ${width}x${height} @ ${frameRate}fps")
            return true
        } catch (e: Exception) {
            XposedBridge.log("$TAG: ❌ Init failed: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Start decoding video
     */
    fun start() {
        if (isDecoding) return
        
        isDecoding = true
        mediaCodec?.start()
        
        decoderThread = Thread {
            XposedBridge.log("$TAG: 🎬 Decoder thread started")
            
            try {
                decode()
            } catch (e: Exception) {
                XposedBridge.log("$TAG: Decode error: ${e.message}")
                e.printStackTrace()
            }
            
            XposedBridge.log("$TAG: 🛑 Decoder thread stopped")
        }
        decoderThread?.start()
    }

    /**
     * Main decoding loop
     */
    private fun decode() {
        val bufferInfo = MediaCodec.BufferInfo()
        var isInputDone = false
        var isOutputDone = false

        while (isDecoding && !isOutputDone) {
            try {
                // Feed input
                if (!isInputDone) {
                    val inputBufferId = mediaCodec?.dequeueInputBuffer(10000) ?: -1
                    if (inputBufferId >= 0) {
                        val inputBuffer = mediaCodec?.getInputBuffer(inputBufferId)
                        val sampleSize = mediaExtractor?.readSampleData(inputBuffer!!, 0) ?: -1
                        
                        if (sampleSize < 0) {
                            // End of stream - loop video
                            mediaExtractor?.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            currentPositionUs = 0
                            XposedBridge.log("$TAG: 🔄 Looping video")
                        } else {
                            val presentationTimeUs = mediaExtractor?.sampleTime ?: 0
                            mediaCodec?.queueInputBuffer(
                                inputBufferId,
                                0,
                                sampleSize,
                                presentationTimeUs,
                                0
                            )
                            mediaExtractor?.advance()
                            currentPositionUs = presentationTimeUs
                        }
                    }
                }

                // Get output
                val outputBufferId = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                when {
                    outputBufferId >= 0 -> {
                        // Frame ready - release it
                        // In the real implementation, we'd extract the frame here
                        mediaCodec?.releaseOutputBuffer(outputBufferId, true)
                        
                        // Sleep to maintain frame rate
                        Thread.sleep(1000L / frameRate)
                    }
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = mediaCodec?.outputFormat
                        XposedBridge.log("$TAG: Output format changed: $newFormat")
                    }
                }
            } catch (e: Exception) {
                if (isDecoding) {
                    XposedBridge.log("$TAG: Decode loop error: ${e.message}")
                }
                break
            }
        }
    }

    /**
     * Get current frame as byte buffer (YUV format)
     */
    fun getCurrentFrame(): DecodedFrame? {
        return frameQueue.poll()
    }

    /**
     * Stop decoding
     */
    fun stop() {
        isDecoding = false
        decoderThread?.interrupt()
        
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaExtractor?.release()
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Stop error: ${e.message}")
        }
        
        frameQueue.clear()
        XposedBridge.log("$TAG: 🛑 Stopped")
    }

    fun getWidth() = width
    fun getHeight() = height
    fun getFrameRate() = frameRate
}
