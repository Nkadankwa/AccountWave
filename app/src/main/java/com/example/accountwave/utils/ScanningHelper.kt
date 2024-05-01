package com.example.accountwave.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanningHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onBarcodeScanned: (String) -> Unit,
    private val onReceiptScanned: (String) -> Unit
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startScanning(scanMode: Int) {
        when (scanMode) {
            1 -> startBarcodeScanning()
            2 -> startReceiptScanning()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startBarcodeScanning() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val barcodeScanner = BarcodeScanning.getClient()
        val imageAnalysis = ImageAnalysis.Builder().build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                imageProxy.image!!,
                imageProxy.imageInfo.rotationDegrees
            )
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.forEach { barcode ->
                        barcode.rawValue?.let { rawValue ->
                            onBarcodeScanned(rawValue)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScanningHelper", "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

        bindCameraToLifecycle(imageAnalysis)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startReceiptScanning() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val imageAnalysis = ImageAnalysis.Builder().build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                imageProxy.image!!,
                imageProxy.imageInfo.rotationDegrees
            )
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    onReceiptScanned(visionText.text)
                }
                .addOnFailureListener { e ->
                    Log.e("ScanningHelper", "Receipt scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

        bindCameraToLifecycle(imageAnalysis)
    }

    private fun bindCameraToLifecycle(imageAnalysis: ImageAnalysis) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                Log.e("ScanningHelper", "Error binding camera", e)
                Toast.makeText(context, "Error starting scan", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
