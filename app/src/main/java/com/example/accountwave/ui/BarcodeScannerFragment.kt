package com.example.accountwave.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.accountwave.R
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class BarcodeScannerFragment : Fragment() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var barcodeScanner: BarcodeScanner
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isScanningBarcode = true

    private val args: BarcodeScannerFragmentArgs by navArgs()

    private lateinit var captureButton: Button
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCameraPreview()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_barcode_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        captureButton = view.findViewById(R.id.captureButton)
        isScanningBarcode = when (args.scanMode) {
            1 -> true
            2 -> false
            else -> true
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        barcodeScanner = BarcodeScanning.getClient()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraPreview()
        } else {
            cameraPermissionResult.launch(Manifest.permission.CAMERA)
        }

        captureButton.setOnClickListener {
            cameraProvider?.let { provider ->
                bindCameraUseCasesForCapture(provider)
            } ?: run {
                Log.e("BarcodeScanner", "CameraProvider not available for capture.")
                Toast.makeText(requireContext(), "Camera not ready. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCameraPreview() {
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.let { provider ->
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
                    Log.d("BarcodeScanner", "Camera preview started, analysis paused.")
                } catch (exc: Exception) {
                    Log.e("BarcodeScanner", "Preview binding failed", exc)
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCasesForCapture(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(requireContext()),
                    if (isScanningBarcode) BarcodeAnalyzer(this::onScanCompleted) else ReceiptAnalyzer(this::onScanCompleted)
                )
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)
            Log.d("BarcodeScanner", "Image analysis started for capture.")
        } catch (exc: Exception) {
            Log.e("BarcodeScanner", "Capture use case binding failed", exc)
        }
    }

    private fun onScanCompleted() {
        cameraProvider?.unbindAll()
        startCameraPreview()
        Log.d("BarcodeScanner", "Scan completed. Image analysis unbound.")
    }


    inner class BarcodeAnalyzer(private val onCompleted: () -> Unit) : ImageAnalysis.Analyzer {
        private var isProcessing = false
        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            if (isProcessing) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            isProcessing = true

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val rawValue = barcodes[0].rawValue
                        Log.d("BarcodeScanner", "Scanned: $rawValue")

                        parentFragmentManager.setFragmentResult("barcodeResult", Bundle().apply {
                            putString("barcode", rawValue)
                        })

                        onCompleted()
                        findNavController().popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeScanner", "Error scanning barcode", e)
                    onCompleted()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing = false
                }
        }
    }

    inner class ReceiptAnalyzer(private val onCompleted: () -> Unit) : ImageAnalysis.Analyzer {
        private var isProcessing = false
        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            if (isProcessing) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            isProcessing = true

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (isAdded) {
                        val resultText = visionText.text
                        Log.d("BarcodeScanner", "Scanned text:\n$resultText")

                        parentFragmentManager.setFragmentResult("receiptResult", Bundle().apply {
                            putString("receipt_text", resultText)
                        })

                        onCompleted()
                        findNavController().popBackStack()
                    } else {
                        Log.w("BarcodeScanner", "Fragment not attached, skipping result sending.")
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Log.e("BarcodeScanner", "Text recognition failed", e)
                    } else {
                        Log.w("BarcodeScanner", "Fragment not attached, skipping failure logging.")
                    }
                    onCompleted()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing = false
                }
        }
    }
}