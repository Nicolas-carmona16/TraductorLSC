package co.edu.udea.compumovil.gr09_20251.traductorlsc.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

class SignLanguageClassifier(context: Context) {
    
    companion object {
        private const val TAG = "SignLanguageClassifier"
    }
    
    private val modelManager = TFLiteModelManager(context)
    
    /**
     * Clasifica un vector de keypoints y retorna la letra predicha
     */
    fun classify(keypoints: FloatArray): String {
        return try {
            val prediction = modelManager.predictSign(keypoints)
            if (prediction == SignPrediction.ERROR) {
                Log.w(TAG, "Error en la clasificación")
                ""
            } else {
                prediction.letter
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la clasificación: ${e.message}")
            ""
        }
    }
    
    /**
     * Cierra los recursos del modelo
     */
    fun close() {
        try {
            modelManager.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar el modelo: ${e.message}")
        }
    }
} 