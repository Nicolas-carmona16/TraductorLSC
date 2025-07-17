package co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.HandDetector
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.HandDetectionResult
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.SignPrediction
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.SpecialFunctionType
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.TFLiteModelManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CameraViewModel : ViewModel() {
    
    private var modelManager: TFLiteModelManager? = null
    private var handDetector: HandDetector? = null
    
    // Estados para la UI
    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()
    
    private val _currentPrediction = MutableStateFlow<SignPrediction?>(null)
    val currentPrediction: StateFlow<SignPrediction?> = _currentPrediction.asStateFlow()
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // Estado para el bounding box de la mano (para UI)
    private val _handBoundingBox = MutableStateFlow<RectF?>(null)
    val handBoundingBox: StateFlow<RectF?> = _handBoundingBox.asStateFlow()
    
    // Estado para la confianza de detección de mano
    private val _handDetectionConfidence = MutableStateFlow<Float>(0f)
    val handDetectionConfidence: StateFlow<Float> = _handDetectionConfidence.asStateFlow()
    
    // Estado para mostrar información de estabilización
    private val _currentLetterStability = MutableStateFlow<String>("")
    val currentLetterStability: StateFlow<String> = _currentLetterStability.asStateFlow()
    
    // Estado para mostrar tiempo restante
    private val _timeRemaining = MutableStateFlow<Int>(0)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()
    
    // Sistema de estabilización para evitar capturas múltiples
    private var lastPredictedLetter = ""
    private var letterStabilityCount = 0
    private val REQUIRED_STABILITY_COUNT = 3 // La letra debe detectarse 3 veces consecutivas
    private val LETTER_ADD_INTERVAL = 3000L // 3 segundos para agregar letra al texto
    private var lastLetterTime = 0L
    private var frameCount = 0
    private val PREDICTION_INTERVAL = 90 // Aumentado de 45 a 90 frames (más tiempo entre predicciones)
    private val isProcessingFrame = AtomicBoolean(false)
    private var lastProcessedFrame = 0L
    private val MIN_FRAME_INTERVAL = 500L // Aumentado de 100ms a 500ms entre predicciones
    
    /**
     * Inicializa el modelo TFLite y el detector de manos
     */
    fun initializeModel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                modelManager = TFLiteModelManager(context)
                handDetector = HandDetector(context)
                println("CameraViewModel: ModelManager initialized = ${modelManager?.isModelReady()}")
                println("CameraViewModel: HandDetector initialized = ${handDetector != null}")
                val ready = modelManager?.isModelReady() ?: false
                _isModelReady.value = ready
            } catch (e: Exception) {
                println("CameraViewModel: Error initializing models: ${e.message}")
                _isModelReady.value = false
            }
        }
    }
    
    /**
     * Procesa un frame de la cámara con detección automática de manos
     */
    fun processFrame(bitmap: Bitmap) {
        // Verificaciones rápidas antes de procesar
        if (!_isModelReady.value || isProcessingFrame.get()) return
        
        frameCount++
        if (frameCount % PREDICTION_INTERVAL != 0) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedFrame < MIN_FRAME_INTERVAL) return
        
        // Marcar como procesando
        if (!isProcessingFrame.compareAndSet(false, true)) return
        
        println("CameraViewModel: Processing frame $frameCount")
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _isProcessing.value = true
                lastProcessedFrame = currentTime
                
                // Detectar la región de la mano
                val handDetection = handDetector?.detectHandRegion(bitmap)
                println("HandDetector: handDetection = $handDetection")
                withContext(Dispatchers.Main) {
                    _handBoundingBox.value = handDetection?.boundingBox
                    _handDetectionConfidence.value = handDetection?.confidence ?: 0f
                    println("CameraViewModel: Updated handBoundingBox = ${_handBoundingBox.value}")
                    println("CameraViewModel: Hand confidence = ${_handDetectionConfidence.value}")
                }
                
                // Procesar el frame usando la región de la mano si existe
                val optimizedBitmap = if (handDetection != null) {
                    extractHandRegion(bitmap, handDetection.boundingBox)
                } else {
                    optimizeBitmapForProcessing(bitmap, null)
                }
                val prediction = modelManager?.predictSign(optimizedBitmap)
                prediction?.let { handlePrediction(it) }
                
                // Actualizar UI en el hilo principal
                withContext(Dispatchers.Main) {
                    _currentPrediction.value = prediction
                }
                
            } catch (e: Exception) {
                // Manejar errores silenciosamente
            } finally {
                _isProcessing.value = false
                isProcessingFrame.set(false)
            }
        }
    }
    
    /**
     * Optimiza el bitmap para el procesamiento
     */
    private fun optimizeBitmapForProcessing(originalBitmap: Bitmap, handRect: Rect?): Bitmap {
        return try {
            // Si tenemos una región de mano, recortar primero
            val croppedBitmap = if (handRect != null) {
                cropBitmap(originalBitmap, handRect)
            } else {
                // Si no hay región específica, usar el centro de la imagen
                cropCenterRegion(originalBitmap)
            }
            
            // Redimensionar directamente a 28x28 para el modelo
            val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 28, 28, true)
            
            // Liberar memoria del bitmap intermedio
            if (croppedBitmap != originalBitmap) {
                croppedBitmap.recycle()
            }
            
            resizedBitmap
            
        } catch (e: Exception) {
            // En caso de error, usar el bitmap original redimensionado
            Bitmap.createScaledBitmap(originalBitmap, 28, 28, true)
        }
    }
    
    /**
     * Recorta una región específica del bitmap
     */
    private fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        val validRect = Rect(
            rect.left.coerceAtLeast(0),
            rect.top.coerceAtLeast(0),
            rect.right.coerceAtMost(bitmap.width),
            rect.bottom.coerceAtMost(bitmap.height)
        )
        
        return if (validRect.width() > 20 && validRect.height() > 20) {
            Bitmap.createBitmap(bitmap, validRect.left, validRect.top, validRect.width(), validRect.height())
        } else {
            bitmap
        }
    }
    
    /**
     * Recorta la región central del bitmap
     */
    private fun cropCenterRegion(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * Extrae la región de la mano del bitmap usando un RectF normalizado
     */
    private fun extractHandRegion(bitmap: Bitmap, region: RectF): Bitmap {
        val left = (region.left * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val top = (region.top * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val right = (region.right * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val bottom = (region.bottom * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
    
    /**
     * Maneja la predicción del modelo con estabilización
     */
    private fun handlePrediction(prediction: SignPrediction) {
        when {
            prediction.letter == "ERROR" -> {
                return
            }
            
            prediction.isSpecialFunction -> {
                // Las funciones especiales se procesan inmediatamente
                handleSpecialFunction(prediction.specialFunctionType)
            }
            
            else -> {
                // Sistema de estabilización para letras normales
                val currentTime = System.currentTimeMillis()
                
                if (prediction.letter == lastPredictedLetter) {
                    letterStabilityCount++
                    println("CameraViewModel: Letter stability count for '${prediction.letter}': $letterStabilityCount")
                    
                    // Actualizar información de estabilización
                    _currentLetterStability.value = "Estabilizando '${prediction.letter}': $letterStabilityCount/$REQUIRED_STABILITY_COUNT"
                    
                    // Solo agregar la letra si se ha detectado consistentemente Y han pasado 3 segundos
                    if (letterStabilityCount >= REQUIRED_STABILITY_COUNT && 
                        currentTime - lastLetterTime >= LETTER_ADD_INTERVAL) {
                        _currentText.value += prediction.letter
                        println("CameraViewModel: Added letter '${prediction.letter}' to text")
                        
                        // Resetear para la siguiente letra
                        letterStabilityCount = 0
                        lastPredictedLetter = ""
                        _currentLetterStability.value = ""
                        _timeRemaining.value = 0
                        lastLetterTime = currentTime
                    } else if (letterStabilityCount >= REQUIRED_STABILITY_COUNT) {
                        // Calcular tiempo restante
                        val timeElapsed = currentTime - lastLetterTime
                        val remainingSeconds = ((LETTER_ADD_INTERVAL - timeElapsed) / 1000).toInt().coerceAtLeast(0)
                        _timeRemaining.value = remainingSeconds
                    }
                } else {
                    // Nueva letra detectada
                    lastPredictedLetter = prediction.letter
                    letterStabilityCount = 1
                    println("CameraViewModel: New letter detected: '${prediction.letter}'")
                }
            }
        }
    }
    
    /**
     * Maneja las funciones especiales
     */
    private fun handleSpecialFunction(functionType: SpecialFunctionType?) {
        when (functionType) {
            SpecialFunctionType.BORRAR -> {
                if (_currentText.value.isNotEmpty()) {
                    _currentText.value = _currentText.value.dropLast(1)
                }
            }
            
            SpecialFunctionType.ESPACIO -> {
                _currentText.value += " "
            }
            
            SpecialFunctionType.GUARDAR -> {
                saveTextToFile()
            }
            
            null -> {
                // No es una función especial válida
            }
        }
    }
    
    /**
     * Guarda el texto actual en un archivo
     */
    private fun saveTextToFile() {
        if (_currentText.value.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                        .format(Date())
                    val fileName = "texto_$timestamp.txt"
                    
                    println("Guardando texto: ${_currentText.value}")
                    
                    withContext(Dispatchers.Main) {
                        _currentText.value = ""
                    }
                    
                } catch (e: Exception) {
                    // Manejar error de guardado
                }
            }
        }
    }
    
    /**
     * Limpia el texto actual
     */
    fun clearText() {
        _currentText.value = ""
    }
    
    /**
     * Obtiene el texto actual
     */
    fun getCurrentText(): String = _currentText.value
    
    /**
     * Obtiene la predicción actual
     */
    fun getCurrentPrediction(): SignPrediction? = _currentPrediction.value
    
    /**
     * Ajusta la frecuencia de predicción
     */
    fun setPredictionInterval(interval: Int) {
        // Permitir ajuste dinámico de la frecuencia
        // interval debe ser >= 15 para evitar sobrecarga
    }
    
    override fun onCleared() {
        super.onCleared()
        handDetector?.release()
        modelManager?.close()
    }
} 