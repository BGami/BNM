package com.example.drowzeeapp

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

private const val TAG = "LivePredictionScreen"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LivePredictionScreen(
    driverId: String,
    supervisorMobile: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val predictionText = remember { mutableStateOf("Analyzing...") }
    val detectionStatus = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val sessionStartTime = remember { SystemClock.elapsedRealtime() }
    val gracePeriodMillis = 7000L
    var alertSent by remember { mutableStateOf(false) }

    val alertManager = remember { AlertManager(context) }
    val reportManager = remember { ReportManager(context, driverId) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    alertManager.stopAlertSound()
                    scope.launch {
                        reportManager.generateAndSendReport(supervisorMobile)
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    alertManager.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            alertManager.release()
            scope.launch {
                reportManager.generateAndSendReport(supervisorMobile)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Live Drowsiness Detection") },
            navigationIcon = {
                IconButton(onClick = {
                    scope.launch {
                        reportManager.generateAndSendReport(supervisorMobile)
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        )

        SmsPermissionHandler {
            CameraPermissionHandler {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetResolution(android.util.Size(224, 224))
                                .build().apply {
                                    setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                        try {
                                            val bitmap = imageProxy.toBitmap()
                                            val resultProbs = runModel(context, bitmap)
                                            val resultText = interpretResult(resultProbs)

                                            predictionText.value = resultText
                                            detectionStatus.value = resultText

                                            val isDrowsy = resultText.contains("Drowsy") && resultProbs[0] > 0.6f
                                            val elapsedTime = SystemClock.elapsedRealtime() - sessionStartTime

                                            if (isDrowsy && elapsedTime > gracePeriodMillis && !alertSent) {
                                                alertSent = true
                                                scope.launch {
                                                    alertManager.triggerAlert()
                                                    reportManager.logEvent("Drowsy detected", "Alert")
                                                    delay(60000)
                                                    alertSent = false
                                                }
                                            }
                                        } catch (e: Exception) {
                                            predictionText.value = "Error: ${e.message}"
                                            Log.e(TAG, "Prediction error", e)
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }, modifier = Modifier.fillMaxSize())

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = detectionStatus.value,
                                    fontSize = 18.sp,
                                    color = Color.Red
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Driver ID: $driverId")
                                Text("Supervisor: $supervisorMobile")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun runModel(context: Context, bitmap: Bitmap): FloatArray {
    val modelBuffer = context.assets.open("drowsiness_model.tflite").readBytes()
    val interpreter = Interpreter(modelBuffer)
    val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
    val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }

    for (y in 0 until 224) {
        for (x in 0 until 224) {
            val pixel = resized.getPixel(x, y)
            input[0][y][x][0] = ((pixel shr 16 and 0xFF) / 255.0f)
            input[0][y][x][1] = ((pixel shr 8 and 0xFF) / 255.0f)
            input[0][y][x][2] = ((pixel and 0xFF) / 255.0f)
        }
    }

    val output = Array(1) { FloatArray(2) }
    interpreter.run(input, output)
    return output[0]
}

fun interpretResult(probabilities: FloatArray): String {
    val classLabels = listOf("Drowsy", "Alert")
    val probs = probabilities.map { it.coerceIn(0f, 1f) }
    val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: -1
    val confidence = probs[maxIndex] * 100
    return if (confidence >= 50f) {
        "${classLabels[maxIndex]} detected with %.2f%% confidence".format(confidence)
    } else {
        "Analyzing..."
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    return android.graphics.BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
}
