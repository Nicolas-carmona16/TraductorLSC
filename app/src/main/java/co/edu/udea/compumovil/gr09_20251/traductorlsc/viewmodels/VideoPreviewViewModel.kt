package co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.HandDetector
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.HandDetectionResult
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ml.SignLanguageClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoPreviewViewModel : ViewModel() {

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _currentPrediction = MutableStateFlow<String>("")
    val currentPrediction: StateFlow<String> = _currentPrediction.asStateFlow()

    private val _handBoundingBox = MutableStateFlow<RectF?>(null)
    val handBoundingBox: StateFlow<RectF?> = _handBoundingBox.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var handDetector: HandDetector? = null
    private var signClassifier: SignLanguageClassifier? = null

    fun initializeML(context: Context) {
        handDetector = HandDetector(context)
        signClassifier = SignLanguageClassifier(context)
    }

    fun processFrame(bitmap: Bitmap) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                // Detectar región de la mano
                val handDetection = handDetector?.detectHandRegion(bitmap)
                _handBoundingBox.value = handDetection?.boundingBox

                if (handDetection != null) {
                    // Extraer la región de la mano
                    val handBitmap = extractHandRegion(bitmap, handDetection.boundingBox)
                    
                    // Clasificar la seña
                    val prediction = signClassifier?.classify(handBitmap)
                    _currentPrediction.value = prediction ?: ""
                } else {
                    _currentPrediction.value = ""
                }
            } catch (e: Exception) {
                _currentPrediction.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun extractHandRegion(bitmap: Bitmap, region: RectF): Bitmap {
        val left = (region.left * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val top = (region.top * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val right = (region.right * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val bottom = (region.bottom * bitmap.height).toInt().coerceIn(0, bitmap.height)
        
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    fun setVideo(uri: Uri, name: String) {
        _videoUri.value = uri
        _fileName.value = name
    }

    fun clearVideo() {
        _videoUri.value = null
        _fileName.value = ""
        _currentPrediction.value = ""
        _handBoundingBox.value = null
    }

    override fun onCleared() {
        super.onCleared()
        handDetector?.release()
        signClassifier?.close()
    }
}