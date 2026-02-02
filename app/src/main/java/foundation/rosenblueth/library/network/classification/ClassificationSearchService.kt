// kotlin
package foundation.rosenblueth.library.network.classification

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio simplificado para clasificaciones bibliográficas
 * Versión mínima que funciona sin errores de conectividad
 */
object ClassificationSearchService {
    private val TAG = "ClassificationSearch"

    /**
     * Busca clasificaciones usando datos del libro (versión simplificada)
     */
    suspend fun searchClassificationsByBookData(
        isbn: String? = null,
        title: String? = null,
        author: String? = null,
        publisher: String? = null
    ): EnhancedClassifications? = withContext(Dispatchers.IO) {

        Log.d(TAG, "=== Búsqueda de clasificaciones iniciada (MODO SIMPLIFICADO) ===")
        Log.d(TAG, "ISBN: '$isbn', Título: '$title', Autor: '$author', Editorial: '$publisher'")

        // TEMPORALMENTE DESHABILITADO para resolver problemas de conectividad
        // Retorna null para que use solo las fuentes básicas que ya funcionan
        Log.d(TAG, "Servicios avanzados temporalmente deshabilitados")
        Log.d(TAG, "=== Resultado: Sin clasificaciones adicionales ===")

        return@withContext null
    }
}

/**
 * Modelo de datos enriquecido para clasificaciones múltiples
 */
data class EnhancedClassifications(
    val lcClassification: MutableList<String> = mutableListOf(),
    val deweyClassification: MutableList<String> = mutableListOf(),
    val udcClassification: MutableList<String> = mutableListOf(), // UDC/CDU
    val subjectHeadings: MutableList<String> = mutableListOf(),
    val otherClassifications: MutableMap<String, String> = mutableMapOf(),
    val sources: MutableList<String> = mutableListOf()
) {
    fun hasAnyClassification(): Boolean {
        return lcClassification.isNotEmpty() ||
               deweyClassification.isNotEmpty() ||
               udcClassification.isNotEmpty() ||
               otherClassifications.isNotEmpty()
    }

    fun merge(other: EnhancedClassifications) {
        lcClassification.addAll(other.lcClassification)
        deweyClassification.addAll(other.deweyClassification)
        udcClassification.addAll(other.udcClassification)
        subjectHeadings.addAll(other.subjectHeadings)
        otherClassifications.putAll(other.otherClassifications)
        sources.addAll(other.sources)

        // Remover duplicados
        val uniqueLC = lcClassification.distinct()
        lcClassification.clear()
        lcClassification.addAll(uniqueLC)

        val uniqueDewey = deweyClassification.distinct()
        deweyClassification.clear()
        deweyClassification.addAll(uniqueDewey)

        val uniqueUDC = udcClassification.distinct()
        udcClassification.clear()
        udcClassification.addAll(uniqueUDC)

        val uniqueSubjects = subjectHeadings.distinct()
        subjectHeadings.clear()
        subjectHeadings.addAll(uniqueSubjects)
    }

    fun getBestLC(): String? = lcClassification.firstOrNull()
    fun getBestDewey(): String? = deweyClassification.firstOrNull()
    fun getBestUDC(): String? = udcClassification.firstOrNull()
}
