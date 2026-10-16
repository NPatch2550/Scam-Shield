package com.scamshield.app.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scamshield.app.R
import com.scamshield.app.ScamShieldApplication
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.network.SearchRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaProjectionOcrService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "MediaProjectionOcr"
        const val ACTION_CAPTURE_SCREEN = "ACTION_CAPTURE_SCREEN"
        var projectionData: Intent? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CAPTURE_SCREEN) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(1002, createOcrNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                } else {
                    startForeground(1002, createOcrNotification())
                }
            } catch (e: Exception) {
                // Fallback for Android 14+ media projection restrictions
            }
            captureAndProcessScreen()
        }
        return START_NOT_STICKY
    }

    private fun createOcrNotification(): Notification {
        return NotificationCompat.Builder(this, ScamShieldApplication.CHANNEL_ID_PROTECTION)
            .setContentTitle("Scam Shield OCR Screen Scanner")
            .setContentText("Processing on-device visual text recognition...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun captureAndProcessScreen() {
        val data = projectionData
        if (data == null) {
            Log.e(TAG, "MediaProjection permission intent not available. Request via Activity first.")
            stopSelf()
            return
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(android.app.Activity.RESULT_OK, data)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScamShieldScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Clean up capture objects immediately (ephemeral memory)
                releaseCapture()

                // Run on-device Google ML Kit OCR
                runOnDeviceOcr(bitmap)
            }
        }, null)
    }

    private fun runOnDeviceOcr(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullOcrText = visionText.text
                Log.d(TAG, "On-device OCR complete: extracted ${fullOcrText.length} characters")

                // 100% On-Device Screen Parsing: Raw bitmap is recycled here
                bitmap.recycle()

                // Check for PII leaks
                PiiFinancialBrakeGuard.checkTextForPii(this, fullOcrText)

                // Send extracted text to backend for entity cross-checking
                serviceScope.launch {
                    try {
                        val response = RetrofitClient.api.search(SearchRequest(rawText = fullOcrText))
                        if (response.isSuccessful && response.body() != null) {
                            val scorecard = response.body()!!
                            val updateIntent = Intent(this@MediaProjectionOcrService, FloatingShieldService::class.java).apply {
                                action = FloatingShieldService.ACTION_UPDATE_RISK
                                putExtra("riskScore", scorecard.riskScore)
                                putExtra("riskLevel", scorecard.verdict)
                            }
                            startService(updateIntent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to analyze OCR text: ${e.message}")
                    } finally {
                        stopSelf()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ML Kit OCR failed: ${e.message}")
                bitmap.recycle()
                stopSelf()
            }
    }

    private fun releaseCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCapture()
    }
}
