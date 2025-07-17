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
        private const val MODEL_PATH = "model3.tflite"
        private const val INPUT_SIZE = 28
        private const val NUM_CHANNELS = 1
        private const val NUM_CLASSES = 19
        
        // Mapeo de letras según el modelo entrenado
        private val LETTER_MAPPING = mapOf(
            0 to "A", 1 to "B", 2 to "C", 3 to "D", 4 to "E", 5 to "F", 6 to "G",
            7 to "I", 8 to "K", 9 to "L", 10 to "M", 11 to "N", 12 to "O", 13 to "P",
            14 to "Q", 15 to "R", 16 to "S", 17 to "T", 18 to "U"
        )
        
        // Funciones especiales
        private const val SPECIAL_BORRAR = "Q"  // Índice 14
        private const val SPECIAL_ESPACIO = "O"  // Índice 12
        private const val SPECIAL_GUARDAR = "B"  // Índice 1
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
            inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS * 4)
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
     * Procesa una imagen y retorna la predicción (optimizado)
     */
    fun predictSign(image: Bitmap): SignPrediction {
        if (!isModelLoaded || interpreter == null || inputBuffer == null || outputBuffer == null) {
            Log.e(TAG, "Modelo no está cargado o buffers no inicializados")
            return SignPrediction.ERROR
        }
        
        return try {
            // Preprocesar la imagen de manera optimizada
            val processedImage = preprocessImageOptimized(image)
            
            // Limpiar y reutilizar buffers
            inputBuffer?.clear()
            outputBuffer?.clear()
            
            // Llenar el buffer de entrada de manera más eficiente
            fillInputBuffer(processedImage, inputBuffer!!)
            
            // Ejecutar inferencia
            interpreter?.run(inputBuffer!!, outputBuffer!!)
            
            // Procesar resultados
            val results = processOutputBuffer(outputBuffer!!)
            
            // Obtener la predicción más probable
            val maxIndex = results.indices.maxByOrNull { results[it] } ?: 0
            val confidence = results[maxIndex]
            val predictedLetter = LETTER_MAPPING[maxIndex] ?: "UNKNOWN"
            
            Log.d(TAG, "Predicción: $predictedLetter con confianza: $confidence")
            
            SignPrediction(
                letter = predictedLetter,
                confidence = confidence,
                isSpecialFunction = isSpecialFunction(predictedLetter),
                specialFunctionType = getSpecialFunctionType(predictedLetter)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la predicción: ${e.message}")
            SignPrediction.ERROR
        }
    }
    
    /**
     * Preprocesa la imagen de manera optimizada
     */
    private fun preprocessImageOptimized(image: Bitmap): Bitmap {
        // Si la imagen ya es 28x28, no necesitamos redimensionar
        if (image.width == INPUT_SIZE && image.height == INPUT_SIZE) {
            return image
        }
        
        // Redimensionar directamente a 28x28
        return Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, true)
    }
    
    /**
     * Llena el buffer de entrada de manera optimizada
     */
    private fun fillInputBuffer(image: Bitmap, buffer: ByteBuffer) {
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        image.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            // Convertir a escala de grises usando la fórmula estándar
            val gray = ((pixel and 0xFF0000 shr 16) * 0.299f +
                        (pixel and 0x00FF00 shr 8) * 0.587f +
                        (pixel and 0x0000FF) * 0.114f) / 255.0f
            buffer.putFloat(gray)
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