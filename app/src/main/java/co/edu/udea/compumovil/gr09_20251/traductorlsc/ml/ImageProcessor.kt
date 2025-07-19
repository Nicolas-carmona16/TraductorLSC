package co.edu.udea.compumovil.gr09_20251.traductorlsc.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log

/**
 * Clase utilitaria para procesar imágenes para el reconocimiento de señas
 */
object ImageProcessor {
    
    private const val TAG = "ImageProcessor"
    
    /**
     * Extrae la región de la mano de una imagen
     * @param originalImage Imagen original
     * @param handRect Rectángulo que contiene la mano
     * @return Bitmap recortado de la mano
     */
    fun extractHandRegion(originalImage: Bitmap, handRect: Rect): Bitmap? {
        return try {
            // Verificar que el rectángulo esté dentro de los límites de la imagen
            val validRect = Rect(
                handRect.left.coerceAtLeast(0),
                handRect.top.coerceAtLeast(0),
                handRect.right.coerceAtMost(originalImage.width),
                handRect.bottom.coerceAtMost(originalImage.height)
            )
            
            // Verificar que el rectángulo tenga un tamaño mínimo
            if (validRect.width() < 20 || validRect.height() < 20) {
                Log.w(TAG, "Región de mano demasiado pequeña")
                return null
            }
            
            // Extraer la región de la mano
            Bitmap.createBitmap(
                originalImage,
                validRect.left,
                validRect.top,
                validRect.width(),
                validRect.height()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al extraer región de mano: ${e.message}")
            null
        }
    }
    
    /**
     * Convierte una imagen a escala de grises
     * @param image Imagen original
     * @return Imagen en escala de grises
     */
    fun convertToGrayscale(image: Bitmap): Bitmap {
        val grayBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply { setSaturation(0f) }
            )
        }
        canvas.drawBitmap(image, 0f, 0f, paint)
        return grayBitmap
    }
    
    /**
     * Redimensiona una imagen manteniendo la proporción
     * @param image Imagen original
     * @param targetSize Tamaño objetivo (cuadrado)
     * @return Imagen redimensionada
     */
    fun resizeImage(image: Bitmap, targetSize: Int): Bitmap {
        return Bitmap.createScaledBitmap(image, targetSize, targetSize, true)
    }
    
    /**
     * Normaliza los valores de píxeles a [0, 1]
     * @param image Imagen en escala de grises
     * @return Array de valores normalizados
     */
    fun normalizeImage(image: Bitmap): FloatArray {
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        
        return FloatArray(pixels.size) { index ->
            val pixel = pixels[index]
            val gray = (pixel and 0xFF).toFloat() / 255.0f
            gray
        }
    }
    
    /**
     * Aplica filtros de mejora a la imagen
     * @param image Imagen original
     * @return Imagen mejorada
     */
    fun enhanceImage(image: Bitmap): Bitmap {
        val enhancedBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val pixel = image.getPixel(x, y)
                val gray = (pixel and 0xFF).toFloat()
                
                // Aplicar contraste y brillo
                val enhanced = ((gray - 128) * 1.2f + 128).coerceIn(0f, 255f)
                val enhancedInt = enhanced.toInt()
                
                enhancedBitmap.setPixel(
                    x, y,
                    (0xFF shl 24) or (enhancedInt shl 16) or (enhancedInt shl 8) or enhancedInt
                )
            }
        }
        
        return enhancedBitmap
    }
    
    /**
     * Rota una imagen
     * @param image Imagen original
     * @param degrees Grados de rotación
     * @return Imagen rotada
     */
    fun rotateImage(image: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }
    
    /**
     * Procesa una imagen completa para el modelo
     * @param image Imagen original
     * @param handRect Rectángulo de la mano (opcional)
     * @return Array de valores normalizados para el modelo
     */
    fun processImageForModel(image: Bitmap, handRect: Rect? = null): FloatArray? {
        return try {
            // Extraer región de la mano si se proporciona
            val handImage = if (handRect != null) {
                extractHandRegion(image, handRect) ?: return null
            } else {
                image
            }
            
            // Convertir a escala de grises
            val grayImage = convertToGrayscale(handImage)
            
            // Mejorar la imagen
            val enhancedImage = enhanceImage(grayImage)
            
            // Redimensionar a 28x28
            val resizedImage = resizeImage(enhancedImage, 28)
            
            // Normalizar valores
            normalizeImage(resizedImage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar imagen: ${e.message}")
            null
        }
    }
    
    /**
     * Dibuja un rectángulo en una imagen (para debugging)
     * @param image Imagen original
     * @param rect Rectángulo a dibujar
     * @param color Color del rectángulo
     * @return Imagen con el rectángulo dibujado
     */
    fun drawRectangle(image: Bitmap, rect: Rect, color: Int = Color.RED): Bitmap {
        val resultBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRect(RectF(rect), paint)
        return resultBitmap
    }
} 