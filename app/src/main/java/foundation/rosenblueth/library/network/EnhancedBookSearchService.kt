// kotlin
package foundation.rosenblueth.library.network

import android.util.Log
import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.network.classification.ClassificationSearchService
import foundation.rosenblueth.library.network.classification.EnhancedClassifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Servicio orquestador que combina búsqueda de información del libro
 * con búsqueda exhaustiva de clasificaciones bibliográficas
 */
object EnhancedBookSearchService {
    private val TAG = "EnhancedBookSearch"

    /**
     * Búsqueda completa: primero obtiene datos del libro, luego busca clasificaciones
     * usando esos datos en múltiples fuentes especializadas
     */
    suspend fun searchCompleteBookInfo(isbn: String): CompleteBookInfo? = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== BÚSQUEDA COMPLETA INICIADA PARA ISBN: $isbn ===")

        // Fase 1: Obtener información básica del libro
        Log.d(TAG, "FASE 1: Obteniendo información básica del libro...")
        val basicBookInfo = ISBNBookSearchService.searchBookByISBN(isbn)

        if (basicBookInfo == null) {
            Log.w(TAG, "No se pudo obtener información básica del libro para ISBN: $isbn")
            // Intentar búsqueda de clasificaciones solo con ISBN
            return@withContext searchClassificationsOnly(isbn)
        }

        Log.d(TAG, "Información básica obtenida:")
        Log.d(TAG, "  - Título: '${basicBookInfo.title}'")
        Log.d(TAG, "  - Autor: '${basicBookInfo.author}'")
        Log.d(TAG, "  - Editorial: '${basicBookInfo.publisher}'")
        Log.d(TAG, "  - Clasificaciones básicas: LC='${basicBookInfo.lcClassification}', Dewey='${basicBookInfo.deweyClassification}'")

        // Fase 2: Búsqueda exhaustiva de clasificaciones usando los datos del libro
        Log.d(TAG, "FASE 2: Búsqueda exhaustiva de clasificaciones...")
        val enhancedClassifications = ClassificationSearchService.searchClassificationsByBookData(
            isbn = isbn,
            title = basicBookInfo.title,
            author = basicBookInfo.author,
            publisher = basicBookInfo.publisher
        )

        // Fase 3: Combinar información básica con clasificaciones mejoradas
        val completeInfo = combineBookInfoWithClassifications(basicBookInfo, enhancedClassifications)

        Log.d(TAG, "=== BÚSQUEDA COMPLETA FINALIZADA ===")
        Log.d(TAG, "Clasificaciones finales:")
        Log.d(TAG, "  - LC: ${completeInfo.finalBook.lcClassification}")
        Log.d(TAG, "  - Dewey: ${completeInfo.finalBook.deweyClassification}")
        Log.d(TAG, "  - UDC: ${completeInfo.finalBook.dcuClassification}")
        Log.d(TAG, "  - Fuentes consultadas: ${completeInfo.sources.joinToString(", ")}")

        return@withContext completeInfo
    }

    /**
     * Búsqueda de clasificaciones cuando no se tiene información básica del libro
     */
    private suspend fun searchClassificationsOnly(isbn: String): CompleteBookInfo? {
        Log.d(TAG, "Búsqueda de clasificaciones únicamente para ISBN: $isbn")

        val classifications = ClassificationSearchService.searchClassificationsByBookData(isbn = isbn)

        return if (classifications != null) {
            val basicBook = BookModel(
                title = "Libro no identificado",
                isbn = isbn,
                lcClassification = classifications.getBestLC() ?: "",
                deweyClassification = classifications.getBestDewey() ?: "",
                dcuClassification = classifications.getBestUDC() ?: ""
            )

            CompleteBookInfo(
                finalBook = basicBook,
                enhancedClassifications = classifications,
                sources = classifications.sources,
                searchStrategy = "Clasificaciones únicamente"
            )
        } else {
            null
        }
    }

    /**
     * Combina información básica del libro con clasificaciones mejoradas
     */
    private fun combineBookInfoWithClassifications(
        basicBook: BookModel,
        enhancedClassifications: EnhancedClassifications?
    ): CompleteBookInfo {

        val sources = mutableListOf<String>()

        // Determinar mejores clasificaciones
        val bestLC = selectBestClassification(
            current = basicBook.lcClassification,
            enhanced = enhancedClassifications?.getBestLC(),
            alternatives = enhancedClassifications?.lcClassification ?: emptyList(),
            type = "LC"
        )

        val bestDewey = selectBestClassification(
            current = basicBook.deweyClassification,
            enhanced = enhancedClassifications?.getBestDewey(),
            alternatives = enhancedClassifications?.deweyClassification ?: emptyList(),
            type = "Dewey"
        )

        val bestUDC = selectBestClassification(
            current = basicBook.dcuClassification,
            enhanced = enhancedClassifications?.getBestUDC(),
            alternatives = enhancedClassifications?.udcClassification ?: emptyList(),
            type = "UDC"
        )

        // Agregar fuentes
        sources.add("Información básica del libro")
        enhancedClassifications?.sources?.let { sources.addAll(it) }

        val finalBook = basicBook.copy(
            lcClassification = bestLC,
            deweyClassification = bestDewey,
            dcuClassification = bestUDC
        )

        return CompleteBookInfo(
            finalBook = finalBook,
            enhancedClassifications = enhancedClassifications,
            sources = sources,
            searchStrategy = "Información básica + Clasificaciones mejoradas"
        )
    }

    /**
     * Selecciona la mejor clasificación entre la actual y las mejoradas
     */
    private fun selectBestClassification(
        current: String,
        enhanced: String?,
        alternatives: List<String>,
        type: String
    ): String {

        Log.d(TAG, "Seleccionando mejor clasificación $type:")
        Log.d(TAG, "  - Actual: '$current'")
        Log.d(TAG, "  - Mejorada: '$enhanced'")
        Log.d(TAG, "  - Alternativas: ${alternatives}")

        // Si no tenemos clasificación actual, usar la mejorada
        if (current.isBlank()) {
            val selected = enhanced ?: ""
            Log.d(TAG, "  - Seleccionada (sin actual): '$selected'")
            return selected
        }

        // Si tenemos clasificación actual pero hay una mejorada más específica
        if (!enhanced.isNullOrBlank() && isMoreSpecific(enhanced, current)) {
            Log.d(TAG, "  - Seleccionada (más específica): '$enhanced'")
            return enhanced
        }

        // Si hay alternativas mejores
        val betterAlternative = alternatives.find { isMoreSpecific(it, current) }
        if (betterAlternative != null) {
            Log.d(TAG, "  - Seleccionada (alternativa mejor): '$betterAlternative'")
            return betterAlternative
        }

        // Mantener la actual
        Log.d(TAG, "  - Seleccionada (mantener actual): '$current'")
        return current
    }

    /**
     * Determina si una clasificación es más específica que otra
     */
    private fun isMoreSpecific(candidate: String, current: String): Boolean {
        return when {
            // Una clasificación más larga generalmente es más específica
            candidate.length > current.length + 2 -> true
            // Para LC: más subdivisiones = más específica
            candidate.contains('.') && !current.contains('.') -> true
            // Para Dewey: más decimales = más específica
            candidate.matches(Regex("\\d{3}\\.\\d+")) && current.matches(Regex("\\d{3}")) -> true
            else -> false
        }
    }

    /**
     * Búsqueda paralela en múltiples servicios para máxima eficiencia
     */
    suspend fun searchCompleteBookInfoParallel(isbn: String): CompleteBookInfo? = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== BÚSQUEDA PARALELA INICIADA PARA ISBN: $isbn ===")

        // Lanzar búsquedas en paralelo
        val basicInfoDeferred = async { ISBNBookSearchService.searchBookByISBN(isbn) }
        val openLibClassificationsDeferred = async { OpenLibraryService.fetchClassifications(isbn) }

        // Esperar resultado de información básica
        val basicBook = basicInfoDeferred.await()

        if (basicBook != null) {
            // Con información básica, lanzar búsqueda de clasificaciones mejoradas
            val enhancedClassificationsDeferred = async {
                ClassificationSearchService.searchClassificationsByBookData(
                    isbn = isbn,
                    title = basicBook.title,
                    author = basicBook.author,
                    publisher = basicBook.publisher
                )
            }

            // Esperar todos los resultados
            val openLibClassifications = openLibClassificationsDeferred.await()
            val enhancedClassifications = enhancedClassificationsDeferred.await()

            // Combinar clasificaciones de OpenLibrary con las mejoradas
            enhancedClassifications?.let { enhanced ->
                openLibClassifications?.let { openLib ->
                    // Agregar clasificaciones de OpenLibrary a las mejoradas
                    openLib.lcClassification?.let { enhanced.lcClassification.add(it) }
                    openLib.dewey?.let { enhanced.deweyClassification.add(it) }
                    openLib.cdu?.let { enhanced.udcClassification.add(it) }
                    enhanced.sources.add("OpenLibrary Service")
                }
            }

            val result = combineBookInfoWithClassifications(basicBook, enhancedClassifications)
            Log.d(TAG, "=== BÚSQUEDA PARALELA FINALIZADA EXITOSAMENTE ===")
            return@withContext result
        } else {
            // Sin información básica, solo usar clasificaciones
            val openLibClassifications = openLibClassificationsDeferred.await()
            return@withContext if (openLibClassifications != null) {
                val basicBookWithClassifications = BookModel(
                    title = "Libro no identificado",
                    isbn = isbn,
                    lcClassification = openLibClassifications.lcClassification ?: "",
                    deweyClassification = openLibClassifications.dewey ?: "",
                    dcuClassification = openLibClassifications.cdu ?: ""
                )
                CompleteBookInfo(
                    finalBook = basicBookWithClassifications,
                    enhancedClassifications = null,
                    sources = listOf("OpenLibrary Service"),
                    searchStrategy = "Solo clasificaciones básicas"
                )
            } else {
                null
            }
        }
    }

    /**
     * Búsqueda específica por título y autor cuando no hay ISBN
     */
    suspend fun searchByTitleAuthor(title: String, author: String? = null): CompleteBookInfo? = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== BÚSQUEDA POR TÍTULO/AUTOR ===")
        Log.d(TAG, "Título: '$title', Autor: '$author'")

        val classifications = ClassificationSearchService.searchClassificationsByBookData(
            title = title,
            author = author
        )

        return@withContext if (classifications != null) {
            val book = BookModel(
                title = title,
                author = author ?: "",
                lcClassification = classifications.getBestLC() ?: "",
                deweyClassification = classifications.getBestDewey() ?: "",
                dcuClassification = classifications.getBestUDC() ?: ""
            )

            CompleteBookInfo(
                finalBook = book,
                enhancedClassifications = classifications,
                sources = classifications.sources,
                searchStrategy = "Búsqueda por título/autor"
            )
        } else {
            null
        }
    }
}

/**
 * Modelo de datos completo que incluye libro e información de clasificación
 */
data class CompleteBookInfo(
    val finalBook: BookModel,
    val enhancedClassifications: EnhancedClassifications?,
    val sources: List<String>,
    val searchStrategy: String,
    val confidence: ClassificationConfidence = ClassificationConfidence.MEDIUM
) {
    fun getClassificationSummary(): String {
        val summary = mutableListOf<String>()

        if (finalBook.lcClassification.isNotBlank()) {
            summary.add("LC: ${finalBook.lcClassification}")
        }
        if (finalBook.deweyClassification.isNotBlank()) {
            summary.add("Dewey: ${finalBook.deweyClassification}")
        }
        if (finalBook.dcuClassification.isNotBlank()) {
            summary.add("UDC: ${finalBook.dcuClassification}")
        }

        enhancedClassifications?.let { enhanced ->
            if (enhanced.subjectHeadings.isNotEmpty()) {
                summary.add("Materias: ${enhanced.subjectHeadings.take(3).joinToString(", ")}")
            }
        }

        return summary.joinToString(" | ")
    }
}

enum class ClassificationConfidence {
    HIGH,    // Múltiples fuentes coinciden
    MEDIUM,  // Una o dos fuentes
    LOW      // Solo una fuente o datos incompletos
}
