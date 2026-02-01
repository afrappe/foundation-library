package foundation.rosenblueth.library.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Clase de utilidad para el reconocimiento de texto en imágenes
 */
open class TextRecognitionHelper(private val context: Context? = null) {
    // Permite que las pruebas inyecten un recognizer mock
    protected open val textRecognizer: TextRecognizer by lazy {
        if (context == null) {
            throw IllegalStateException("Se requiere un contexto para inicializar ML Kit")
        }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extrae texto de una imagen (portada de libro)
     *
     * @param bitmap La imagen de la portada del libro
     * @return El texto reconocido en la imagen
     */
    open suspend fun recognizeText(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)

                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val detectedText = visionText.text
                        continuation.resume(detectedText)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }

                continuation.invokeOnCancellation {
                    // No es necesario cancelar la tarea de ML Kit
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Extrae texto de un área específica de una imagen
     *
     * @param bitmap La imagen completa
     * @param rect El área rectangular donde se debe realizar el reconocimiento
     * @return El texto reconocido en la región especificada
     */
    suspend fun recognizeTextInRegion(bitmap: Bitmap, rect: Rect): String {
        // Validar que el rectángulo está dentro de los límites del bitmap
        val validRect = Rect(
            rect.left.coerceIn(0, bitmap.width),
            rect.top.coerceIn(0, bitmap.height),
            rect.right.coerceIn(0, bitmap.width),
            rect.bottom.coerceIn(0, bitmap.height)
        )

        // Verificar que el rectángulo tiene un área válida
        if (validRect.width() <= 0 || validRect.height() <= 0) {
            return ""
        }

        // Crear un recorte de la imagen original
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            validRect.left,
            validRect.top,
            validRect.width(),
            validRect.height()
        )

        return recognizeText(croppedBitmap)
    }

    /**
     * Procesa una imagen y devuelve todos los bloques de texto detectados con sus coordenadas
     *
     * @param bitmap La imagen a procesar
     * @return Lista de bloques de texto detectados con sus coordenadas y contenido
     */
    suspend fun getTextBlocks(bitmap: Bitmap): List<TextBlock> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)

                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val textBlocks = visionText.textBlocks.map { block ->
                            TextBlock(
                                text = block.text,
                                boundingBox = block.boundingBox,
                                lines = block.lines.map { line ->
                                    TextLine(
                                        text = line.text,
                                        boundingBox = line.boundingBox,
                                        elements = line.elements.map { element ->
                                            TextElement(
                                                text = element.text,
                                                boundingBox = element.boundingBox
                                            )
                                        }
                                    )
                                }
                            )
                        }
                        continuation.resume(textBlocks)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }

                continuation.invokeOnCancellation {
                    // No es necesario cancelar la tarea de ML Kit
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Selecciona los bloques de texto que están dentro o intersectan con un área específica
     *
     * @param textBlocks Lista de bloques de texto detectados
     * @param selectionArea El área de selección
     * @return Lista de bloques de texto dentro del área seleccionada
     */
    fun selectTextBlocksInRegion(textBlocks: List<TextBlock>, selectionArea: Rect): List<TextBlock> {
        return textBlocks.filter { block ->
            block.boundingBox?.let { box ->
                Rect.intersects(box, selectionArea)
            } ?: false
        }
    }

    /**
     * Extrae el texto concatenado de una lista de bloques de texto
     *
     * @param textBlocks Los bloques de texto a procesar
     * @return El texto combinado de todos los bloques
     */
    fun getTextFromBlocks(textBlocks: List<TextBlock>): String {
        return textBlocks.joinToString("\n") { it.text }
    }

    /**
     * Intenta extraer el título de un libro a partir del texto reconocido en su portada.
     *
     * Esta función utiliza heurísticas para identificar el título del libro:
     * 1. Busca texto con fuente más grande (primeras líneas)
     * 2. Elimina palabras comunes que suelen no ser parte del título
     *
     * @param recognizedText El texto completo reconocido de la imagen
     * @return El título del libro extraído
     */
    fun extractBookTitle(recognizedText: String): String {
        if (recognizedText.isBlank()) return ""

        val stopWords = listOf("edición", "autor", "editorial", "presenta", "colección", "volumen")
        val lines = recognizedText.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && stopWords.none { stop -> it.lowercase().contains(stop) } }

        // Selecciona las 3 líneas más largas y las une
        val titleLines = lines.sortedByDescending { it.length }.take(3)
        val title = titleLines.joinToString(" ")

        // Devuelve el título con formato adecuado
        return title.replace("\\s+".toRegex(), " ").trim()
    }

    /**
     * Extrae un posible título del libro a partir de bloques de texto seleccionados
     *
     * @param textBlocks Los bloques de texto seleccionados
     * @return El título extraído de los bloques seleccionados
     */
    fun extractBookTitleFromBlocks(textBlocks: List<TextBlock>): String {
        val text = getTextFromBlocks(textBlocks)
        return extractBookTitle(text)
    }

    /**
     * Intenta extraer el ISBN de un libro a partir del texto reconocido.
     *
     * Esta función busca patrones de ISBN-10 e ISBN-13 en el texto:
     * - ISBN-10: 10 dígitos (puede incluir X como último dígito)
     * - ISBN-13: 13 dígitos que típicamente empiezan con 978 o 979
     *
     * @param recognizedText El texto completo reconocido de la imagen
     * @return El ISBN extraído (sin guiones ni espacios) o cadena vacía si no se encuentra
     */
    fun extractISBN(recognizedText: String): String {
        if (recognizedText.isBlank()) return ""

        // Patrón para ISBN-13 (13 dígitos, puede incluir guiones o espacios)
        val isbn13Pattern = Regex("""(?:ISBN(?:-13)?:?\s*)?(\d{3}[-\s]?\d{1,5}[-\s]?\d{1,7}[-\s]?\d{1,7}[-\s]?\d)""", RegexOption.IGNORE_CASE)
        
        // Patrón para ISBN-10 (10 dígitos o 9 dígitos + X, puede incluir guiones o espacios)
        val isbn10Pattern = Regex("""(?:ISBN(?:-10)?:?\s*)?(\d{1,5}[-\s]?\d{1,7}[-\s]?\d{1,7}[-\s]?[\dX])""", RegexOption.IGNORE_CASE)

        // Obtener todas las coincidencias y validarlas
        val allMatches = mutableListOf<String>()
        
        // Buscar ISBN-13 primero (más común actualmente)
        isbn13Pattern.findAll(recognizedText).forEach { match ->
            val isbn = match.groupValues[1].replace(Regex("[-\\s]"), "")
            if (isbn.length == 13 && isbn.all { it.isDigit() }) {
                allMatches.add(isbn)
            }
        }
        
        // Si encontramos ISBN-13, retornamos el primero
        if (allMatches.isNotEmpty()) {
            return allMatches.first()
        }

        // Si no se encuentra ISBN-13, buscar ISBN-10
        isbn10Pattern.findAll(recognizedText).forEach { match ->
            val isbn = match.groupValues[1].replace(Regex("[-\\s]"), "")
            if (isbn.length == 10 && (isbn.take(9).all { it.isDigit() } && (isbn.last().isDigit() || isbn.last() == 'X'))) {
                allMatches.add(isbn)
            }
        }
        
        // Retornar el primer ISBN-10 válido si se encontró
        return allMatches.firstOrNull() ?: ""
    }

}

/**
 * Clase que representa un bloque de texto reconocido
 */
data class TextBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<TextLine> = emptyList()
)

/**
 * Clase que representa una línea de texto reconocido
 */
data class TextLine(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<TextElement> = emptyList()
)

/**
 * Clase que representa un elemento de texto reconocido (palabra o carácter)
 */
data class TextElement(
    val text: String,
    val boundingBox: Rect?
)
