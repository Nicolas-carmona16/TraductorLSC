package co.edu.udea.compumovil.gr09_20251.traductorlsc.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import co.edu.udea.compumovil.gr09_20251.traductorlsc.navigation.AppRoutes
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.SecondBlue
import co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels.CameraViewModel
import kotlinx.coroutines.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraViewModel: CameraViewModel = viewModel()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasAudioPermission by remember { mutableStateOf(false) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var isRecording by remember { mutableStateOf(false) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }

    var elapsedTime by remember { mutableLongStateOf(0L) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Estados del ViewModel
    val currentText by cameraViewModel.currentText.collectAsState()
    val currentPrediction by cameraViewModel.currentPrediction.collectAsState()
    val handBoundingBox by cameraViewModel.handBoundingBox.collectAsState()
    val handDetectionConfidence by cameraViewModel.handDetectionConfidence.collectAsState()
    val currentLetterStability by cameraViewModel.currentLetterStability.collectAsState()
    val timeRemaining by cameraViewModel.timeRemaining.collectAsState()

    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    DisposableEffect(Unit) {
        onDispose {
            timerJob?.cancel()
        }
    }

    // Inicializar el modelo cuando se carga la pantalla
    LaunchedEffect(Unit) {
        cameraViewModel.initializeModel(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)

        hasCameraPermission = cameraPermission == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = audioPermission == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    LaunchedEffect(previewView, cameraSelector) {
        val cameraProvider = context.getCameraProvider()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Configurar análisis de imagen para procesar frames
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context)
                ) { imageProxy ->
                    try {
                        val bitmap = imageProxy.toBitmapOptimized()
                        if (bitmap != null) {
                            cameraViewModel.processFrame(bitmap)
                        }
                    } catch (e: Exception) {
                        // Manejar errores silenciosamente
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        val qualitySelector = QualitySelector.from(
            Quality.HD,
            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
        )

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
                videoCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasCameraPermission && hasAudioPermission) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.matchParentSize()
                )
                // Overlay para bounding box
                if (handBoundingBox != null) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        val rect = handBoundingBox!!
                        val left = rect.left * size.width
                        val top = rect.top * size.height
                        val right = rect.right * size.width
                        val bottom = rect.bottom * size.height
                        
                        // Color basado en la confianza
                        val confidenceColor = when {
                            handDetectionConfidence > 0.8f -> Color.Green
                            handDetectionConfidence > 0.6f -> Color.Yellow
                            else -> Color.Red
                        }
                        
                        drawRect(
                            color = confidenceColor,
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información del modelo y predicciones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Información de detección de mano
                if (handBoundingBox != null) {
                    Text(
                        text = "Mano detectada: ${(handDetectionConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            handDetectionConfidence > 0.8f -> Color.Green
                            handDetectionConfidence > 0.6f -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Predicción actual
                currentPrediction?.let { prediction ->
                    Text(
                        text = "Detectado: ${prediction.letter} (${(prediction.confidence * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (prediction.isSpecialFunction) Color.Blue else Color.Black
                    )
                }
                
                // Información de estabilización
                if (currentLetterStability.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentLetterStability,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Blue
                    )
                    
                    // Mostrar tiempo restante
                    if (timeRemaining > 0) {
                        Text(
                            text = "Agregando en: ${timeRemaining}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Texto acumulado
                Text(
                    text = "Texto: $currentText",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para limpiar texto
            Button(
                onClick = { cameraViewModel.clearText() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text("Limpiar Texto", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controles de grabación
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = if (isRecording) formatTime(elapsedTime) else "00:00",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isRecording) Color.Red else Color.Gray,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp, bottom = 32.dp)
                )

                Button(
                    onClick = {
                        if (!isRecording) {
                            elapsedTime = 0L
                            timerJob = CoroutineScope(Dispatchers.Main).launch {
                                while (isActive) {
                                    delay(1000)
                                    elapsedTime += 1000
                                }
                            }

                            startRecording(
                                context = context,
                                videoCapture = videoCapture,
                                onRecordingStarted = { recordingInstance ->
                                    recording = recordingInstance
                                    isRecording = true
                                },
                                onRecordingFinished = { uri ->
                                    navController.navigate(AppRoutes.getVideoPreviewRoute(uri.toString()))
                                }
                            )
                        } else {
                            recording?.stop()
                            recording = null
                            isRecording = false
                            timerJob?.cancel()
                            timerJob = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .padding(bottom = 32.dp)
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, shape = RoundedCornerShape(4.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar grabación",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(80.dp)
                        .padding(bottom = 32.dp, end = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cambiar cámara",
                        tint = SecondBlue
                    )
                }
            }
        } else {
            Text("Esperando permisos de cámara y audio...")
        }
    }
}

// Extensión optimizada para convertir ImageProxy a Bitmap
fun androidx.camera.core.ImageProxy.toBitmapOptimized(): Bitmap? {
    return try {
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
        val out = java.io.ByteArrayOutputStream()
        
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 80, out)
        val imageBytes = out.toByteArray()
        
        val options = BitmapFactory.Options().apply {
            inSampleSize = 2
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    } catch (e: Exception) {
        null
    }
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

private fun startRecording(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>?,
    onRecordingStarted: (Recording) -> Unit,
    onRecordingFinished: (Uri) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }
    }

    val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    var currentRecording: Recording? = null

    currentRecording = videoCapture?.output
        ?.prepareRecording(context, mediaStoreOutputOptions)
        ?.withAudioEnabled()
        ?.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    currentRecording?.let { onRecordingStarted(it) }
                }
                is VideoRecordEvent.Finalize -> {
                    val uri = event.outputResults.outputUri
                    onRecordingFinished(uri)
                }
            }
        }
}

