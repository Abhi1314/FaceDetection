package com.example.facedetection.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.facedetection.graphic.GraphicOverlay
import com.example.facedetection.graphic.RectangleOverlay
import com.example.facedetection.utils.CameraUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraManager(
    private val context: Context,
    private val previewView: PreviewView,
    private val graphicOverlay: GraphicOverlay<*>,
    private val imageView: ImageView,
    private val lifecycleOwner: LifecycleOwner
) {

    private lateinit var preview: Preview
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture: ImageCapture
    private var outputDirectory: File
    lateinit var rect: Rect


    init {
        outputDirectory = getOutputDirectory()
    }

    fun cameraStart() {
        val cameraProcessProvider = ProcessCameraProvider.getInstance(context)

        cameraProcessProvider.addListener({
            cameraProvider = cameraProcessProvider.get()
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, CameraAnalyzer(graphicOverlay))
                }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraOption)
                .build()
            setCameraConfig(cameraProvider, cameraSelector)
            try {
                // Unbind any existing use cases before rebinding
                cameraProvider.unbindAll()

                // Bind the camera use cases to the lifecycle
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )

            } catch (exc: Exception) {
                // Handle any exceptions
                Log.e(ContentValues.TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))

    }

    private fun getOutputDirectory(): File {
        // Get the external files directory, usually: /storage/emulated/0/Android/data/com.your.app/files/Pictures
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraXDemo").apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    fun captureImage() {

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Handle error
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Handle image saved
                    Toast.makeText(context, "ImageCapture", Toast.LENGTH_SHORT).show()
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d("uri", savedUri.toString())
                    processFaceContourDetectionResultFromURI(savedUri)
                }
            }
        )
    }

    private fun processFaceContourDetectionResultFromURI(imageUri: Uri) {
        val image = InputImage.fromFilePath(context, imageUri)
        rect = Rect()
        val detector = FaceDetection.getClient()
        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    Toast.makeText(context, "No face found", Toast.LENGTH_SHORT).show()
                } else {
                    for (face in faces) {
                        val faceGraphic = RectangleOverlay(graphicOverlay, face, rect)
                        graphicOverlay.add(faceGraphic)
                        graphicOverlay.updateFace(faceGraphic)

                    }
                    imageView.setImageURI(imageUri)
                }

            }
            .addOnFailureListener { e ->
                // Handle face detection failure
                Toast.makeText(context, "Face detection failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun setCameraConfig(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector
    ) {
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "setCameraConfig: $e")
        }
    }


    fun changeCamera() {
        cameraProvider.unbindAll()
        cameraOption =
            if (cameraOption == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
            else CameraSelector.LENS_FACING_BACK
        CameraUtils.toggleSelector()
        cameraStart()
    }

    fun cameraStop() {
        cameraProvider.unbindAll()
    }


    companion object {
        private const val TAG: String = "CameraManager"
        var cameraOption: Int = CameraSelector.LENS_FACING_FRONT
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }
}
