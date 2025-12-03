package com.example.a_eyes

import android.Manifest
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.a_eyes.ImageAnalyzer.analyzeImage
import com.example.a_eyes.ImageAnalyzer.translateToFrench
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var tts: TextToSpeech

    private lateinit var progressBar: ProgressBar

    private var volumeUpPressedTime: Long = 0
    private var volumeDownPressedTime: Long = 0
    private val simultaneousThreshold = 500L // ms

    var shouldTranslate = true
    private var oldShouldTranslate = true

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val langToggleButton = findViewById<ImageButton>(R.id.langToggleButton)
        langToggleButton.clipToOutline = true

        langToggleButton.setOnClickListener {
            shouldTranslate = !shouldTranslate

            if (shouldTranslate != oldShouldTranslate) {
                tts.stop()
                oldShouldTranslate = shouldTranslate
            }

            // Change icon depending on the mode
            if (shouldTranslate) {
                langToggleButton.setImageResource(R.drawable.ic_flag_fr)
                Toast.makeText(this, "Traduction en français activée", Toast.LENGTH_SHORT).show()
            } else {
                langToggleButton.setImageResource(R.drawable.ic_flag_uk)
                Toast.makeText(this, "Translation set to English", Toast.LENGTH_SHORT).show()
            }
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                tts.stop()
                return true
            }
        })

        previewView = findViewById(R.id.previewView)
        val captureButton: ImageButton = findViewById(R.id.captureButton)
        tts = TextToSpeech(this, this)

        progressBar = findViewById(R.id.progressBar)

        captureButton.setOnClickListener {
            takePhoto()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentTime = System.currentTimeMillis()

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpPressedTime = currentTime
                if (isSimultaneousPress(currentTime, volumeDownPressedTime)) {
                    takePhoto()
                    return true
                }
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownPressedTime = currentTime
                if (isSimultaneousPress(currentTime, volumeUpPressedTime)) {
                    takePhoto()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun isSimultaneousPress(time1: Long, time2: Long): Boolean {
        return kotlin.math.abs(time1 - time2) <= simultaneousThreshold
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        tts.stop()

        val imageCapture = imageCapture

        val photoFile = createImageFile()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(applicationContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Show loading spinner
                    runOnUiThread {
                        progressBar.visibility = android.view.View.VISIBLE
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val rawDescription = kotlinx.coroutines.withTimeout(30000) {
                                analyzeImage(photoFile)
                            }

                            val description = translateToFrench(rawDescription ?: "", shouldTranslate)
                            Log.d("Mistral", "Result : $description")

                            runOnUiThread {
                                progressBar.visibility = android.view.View.GONE
                                speak(description)
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                progressBar.visibility = android.view.View.GONE
                                speak("Erreur pendant l'analyse de l'image.")
                            }
                        }
                    }
                }
            }
        )
    }


    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun speak(text: String) {
        val locale = if (shouldTranslate) Locale.FRENCH else Locale.ENGLISH
        val result = tts.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Selected language is not supported", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    override fun onPause() {
        super.onPause()
        tts.stop()
    }

    override fun onStop() {
        super.onStop()
        tts.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = if (shouldTranslate) Locale.FRENCH else Locale.ENGLISH
            val result = tts.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Selected language is not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
}