package co.edu.udea.compumovil.gr09_20251.traductorlsc.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TFLiteModelManager"
        private const val MODEL_PATH = "hand_sign_vowels_model.tflite"
        private const val INPUT_SIZE = 63 // 21 keypoints * 3 coordenadas
        private const val NUM_CLASSES = 5 // A, B, C, D, E
        
        // Mapeo de las letras
        private val LETTER_MAPPING = mapOf(
            0 to "A", 1 to "B", 2 to "C", 3 to "D", 4 to "E"
        )

        private const val SPECIAL_BORRAR = "Q"
        private const val SPECIAL_ESPACIO = "O"
        private const val SPECIAL_GUARDAR = "B"
    }
    
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    
    // Buffers reutilizables para optimizar memoria
    private var inputBuffer: ByteBuffer? = null
    private var outputBuffer: ByteBuffer? = null
    
    init {
        loadModel()
    }
    
    /**
     * Carga el modelo TFLite desde los assets
     */
    private fun loadModel() {
        try {
            val modelFile = loadModelFile(context, MODEL_PATH)
            val options = Interpreter.Options()
            
            // Configurar opciones para mejor rendimiento
            options.setNumThreads(4)
            
            interpreter = Interpreter(modelFile, options)
            
            // Pre-allocar buffers para evitar crear nuevos en cada predicción
            inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4)
            inputBuffer?.order(ByteOrder.nativeOrder())
            
            outputBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4)
            outputBuffer?.order(ByteOrder.nativeOrder())
            
            isModelLoaded = true
            
            Log.d(TAG, "Modelo cargado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar el modelo: ${e.message}")
            isModelLoaded = false
        }
    }
    
    /**
     * Carga el archivo del modelo desde assets
     */
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Procesa un vector de keypoints y retorna la predicción
     */
    fun predictSign(keypoints: FloatArray): SignPrediction {
        if (!isModelLoaded || interpreter == null || inputBuffer == null || outputBuffer == null) {
            Log.e(TAG, "Modelo no está cargado o buffers no inicializados")
            return SignPrediction.ERROR
        }
        if (keypoints.size != INPUT_SIZE) {
            Log.e(TAG, "El tamaño del vector de keypoints es incorrecto")
            return SignPrediction.ERROR
        }
        return try {
            inputBuffer?.clear()
            outputBuffer?.clear()
            // Llenar el buffer de entrada con los keypoints
            for (value in keypoints) {
                inputBuffer!!.putFloat(value)
            }
            interpreter?.run(inputBuffer!!, outputBuffer!!)
            val results = processOutputBuffer(outputBuffer!!)
            val maxIndex = results.indices.maxByOrNull { results[it] } ?: 0
            val confidence = results[maxIndex]
            val predictedLetter = LETTER_MAPPING[maxIndex] ?: "UNKNOWN"
            Log.d(TAG, "Predicción: $predictedLetter con confianza: $confidence")
            SignPrediction(
                letter = predictedLetter,
                confidence = confidence,
                isSpecialFunction = false,
                specialFunctionType = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la predicción: ${e.message}")
            SignPrediction.ERROR
        }
    }
    
    /**
     * Procesa el buffer de salida
     */
    private fun processOutputBuffer(buffer: ByteBuffer): FloatArray {
        val results = FloatArray(NUM_CLASSES)
        buffer.rewind()
        for (i in 0 until NUM_CLASSES) {
            results[i] = buffer.float
        }
        return results
    }
    
    /**
     * Verifica si la letra es una función especial
     */
    private fun isSpecialFunction(letter: String): Boolean {
        return letter == SPECIAL_BORRAR || letter == SPECIAL_ESPACIO || letter == SPECIAL_GUARDAR
    }
    
    /**
     * Obtiene el tipo de función especial
     */
    private fun getSpecialFunctionType(letter: String): SpecialFunctionType? {
        return when (letter) {
            SPECIAL_BORRAR -> SpecialFunctionType.BORRAR
            SPECIAL_ESPACIO -> SpecialFunctionType.ESPACIO
            SPECIAL_GUARDAR -> SpecialFunctionType.GUARDAR
            else -> null
        }
    }
    
    /**
     * Verifica si el modelo está cargado
     */
    fun isModelReady(): Boolean = isModelLoaded
    
    /**
     * Libera recursos del modelo
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        inputBuffer = null
        outputBuffer = null
        isModelLoaded = false
        Log.d(TAG, "Modelo cerrado")
    }
}

/**
 * Clase de datos para representar una predicción de seña
 */
data class SignPrediction(
    val letter: String,
    val confidence: Float,
    val isSpecialFunction: Boolean = false,
    val specialFunctionType: SpecialFunctionType? = null
) {
    companion object {
        val ERROR = SignPrediction("ERROR", 0.0f)
    }
}

/**
 * Enum para tipos de funciones especiales
 */
enum class SpecialFunctionType {
    BORRAR,    // Q - Eliminar último carácter
    ESPACIO,   // O - Añadir espacio
    GUARDAR    // B - Guardar texto
}