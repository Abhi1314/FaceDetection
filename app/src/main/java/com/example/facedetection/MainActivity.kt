package com.example.facedetection

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.facedetection.camera.CameraManager
import com.example.facedetection.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = CameraManager(
            this,
            mainBinding.viewCameraPreview,
            mainBinding.viewGraphicOverlay,
            mainBinding.preview,
            this
        )
        askCameraPermission()
        buttonClick()

    }

    private fun buttonClick() {
        mainBinding.apply {
            buttonTurnCamera.setOnClickListener {
                cameraManager.changeCamera()
            }

            buttonStopCamera.setOnClickListener {
                cameraManager.cameraStop()
                buttonStopCamera.visibility = INVISIBLE
                buttonStartCamera.visibility = VISIBLE
            }

            buttonStartCamera.setOnClickListener {
                cameraManager.cameraStart()
                buttonStopCamera.visibility = VISIBLE
                buttonStartCamera.visibility = INVISIBLE
            }
            capture.setOnClickListener {
                cameraManager.captureImage()
            }
        }
    }

    private fun askCameraPermission() {
        if (arrayOf(android.Manifest.permission.CAMERA).all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            cameraManager.cameraStart()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraManager.cameraStart()
        } else {
            Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }


}