package co.edu.udea.compumovil.gr09_20251.traductorlsc.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Bitmap.Config
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.BitmapImageBuilder

data class HandDetectionResult(
    val boundingBox: RectF,
    val confidence: Float
)

class HandDetector(context: Context) {
    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .build()
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detectHandRegion(bitmap: Bitmap): HandDetectionResult? {
        try {
            println("HandDetector: Processing bitmap ${bitmap.width}x${bitmap.height}")
            
            // Convertir bitmap a ARGB_8888 si es necesario
            val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                println("HandDetector: Converting bitmap to ARGB_8888")
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }
            
            val mpImage = BitmapImageBuilder(argbBitmap).build()
            val result: HandLandmarkerResult = handLandmarker.detect(mpImage)
            println("HandDetector: Detection result has ${result.landmarks().size} hands")
            val landmarks = result.landmarks().firstOrNull() ?: return null
            
            // Obtener la confianza de la detección
            val confidence = result.handedness().firstOrNull()?.firstOrNull()?.score() ?: 0.0f
            println("HandDetector: Detection confidence = $confidence")

        val minX = landmarks.minOf { it.x() }
        val maxX = landmarks.maxOf { it.x() }
        val minY = landmarks.minOf { it.y() }
        val maxY = landmarks.maxOf { it.y() }

            // Bounding box normalizado (valores entre 0 y 1)
            val rawBoundingBox = RectF(minX, minY, maxX, maxY)
            println("HandDetector: Raw bounding box = $rawBoundingBox")
            
            // Optimizar la región: hacerla cuadrada y agregar padding
            val optimizedBoundingBox = optimizeHandRegion(rawBoundingBox)
            println("HandDetector: Optimized bounding box = $optimizedBoundingBox")
            
            return HandDetectionResult(optimizedBoundingBox, confidence)
        } catch (e: Exception) {
            println("HandDetector: Error detecting hand: ${e.message}")
            return null
        }
    }

    fun release() {
        handLandmarker.close()
    }
    
    /**
     * Optimiza la región de la mano: la hace cuadrada y agrega padding
     */
    private fun optimizeHandRegion(rect: RectF): RectF {
        val centerX = (rect.left + rect.right) / 2f
        val centerY = (rect.top + rect.bottom) / 2f
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        
        // Usar el lado más largo para hacer un cuadrado
        val maxSide = maxOf(width, height)
        
        // Agregar padding (20% del lado)
        val padding = maxSide * 0.2f
        val finalSide = maxSide + padding
        
        // Calcular los nuevos límites
        val halfSide = finalSide / 2f
        val newLeft = (centerX - halfSide).coerceIn(0f, 1f)
        val newTop = (centerY - halfSide).coerceIn(0f, 1f)
        val newRight = (centerX + halfSide).coerceIn(0f, 1f)
        val newBottom = (centerY + halfSide).coerceIn(0f, 1f)
        
        return RectF(newLeft, newTop, newRight, newBottom)
    }
} 