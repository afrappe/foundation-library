// kotlin
package foundation.rosenblueth.library.network.classification

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

/**
 * Servicio especializado para obtener clasificaciones bibliográficas de múltiples fuentes
 */

// Library of Congress API
interface LOCClassificationApi {
    // https://www.loc.gov/books/?q=title:hamlet+author:shakespeare&fo=json
    @GET("books/")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fo") format: String = "json",
        @Query("c") count: Int = 10
    ): Response<JsonObject>

    // https://id.loc.gov/vocabulary/classSchemes/lcc.json
    @GET("vocabulary/classSchemes/lcc.json")
    suspend fun getLCCScheme(): Response<JsonObject>
}

// OCLC WorldCat API (más completa que xISBN)
interface WorldCatClassificationApi {
    // http://classify.oclc.org/classify2/Classify?isbn=9780140328721&summary=true
    @GET("classify2/Classify")
    suspend fun classifyByISBN(
        @Query("isbn") isbn: String,
        @Query("summary") summary: String = "true"
    ): Response<String> // XML response

    // http://classify.oclc.org/classify2/Classify?title=hamlet&author=shakespeare&summary=true
    @GET("classify2/Classify")
    suspend fun classifyByTitleAuthor(
        @Query("title") title: String,
        @Query("author") author: String? = null,
        @Query("summary") summary: String = "true"
    ): Response<String>
}

// Harvard Library API (para DDC)
interface HarvardLibraryApi {
    // https://api.lib.harvard.edu/v2/items?q=title:hamlet%20author:shakespeare
    @GET("v2/items")
    suspend fun searchItems(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>
}

// British Library API
interface BritishLibraryApi {
    // https://api.bl.uk/metadata/bibnumber/015890393?format=json
    @GET("metadata/bibnumber/{id}")
    suspend fun getByBibNumber(
        @Path("id") bibNumber: String,
        @Query("format") format: String = "json"
    ): Response<JsonObject>
}

// Deutsche Nationalbibliothek (para UDC/DCU)
interface DNBApi {
    // https://services.dnb.de/sru/authorities?version=1.1&operation=searchRetrieve&query=aut.person=Shakespeare
    @GET("sru/authorities")
    suspend fun searchAuthorities(
        @Query("version") version: String = "1.1",
        @Query("operation") operation: String = "searchRetrieve",
        @Query("query") query: String,
        @Query("recordSchema") recordSchema: String = "MARC21-xml"
    ): Response<String> // XML response
}

// Biblioteca Nacional de España (para CDU en español)
interface BNEApi {
    // http://catalogo.bne.es/uhtbin/webcat?searchtype=title&searcharg=hamlet
    @GET("uhtbin/webcat")
    suspend fun searchCatalog(
        @Query("searchtype") searchType: String = "title",
        @Query("searcharg") searchArg: String,
        @Query("format") format: String = "json"
    ): Response<JsonObject>
}

object ClassificationSearchService {
    private val TAG = "ClassificationSearch"

    // Configuración de clientes Retrofit
    private val locRetrofit = Retrofit.Builder()
        .baseUrl("https://www.loc.gov/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val worldCatRetrofit = Retrofit.Builder()
        .baseUrl("http://classify.oclc.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val harvardRetrofit = Retrofit.Builder()
        .baseUrl("https://api.lib.harvard.edu/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val britishLibRetrofit = Retrofit.Builder()
        .baseUrl("https://api.bl.uk/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val dnbRetrofit = Retrofit.Builder()
        .baseUrl("https://services.dnb.de/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val bneRetrofit = Retrofit.Builder()
        .baseUrl("http://catalogo.bne.es/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // APIs
    private val locApi = locRetrofit.create(LOCClassificationApi::class.java)
    private val worldCatApi = worldCatRetrofit.create(WorldCatClassificationApi::class.java)
    private val harvardApi = harvardRetrofit.create(HarvardLibraryApi::class.java)
    private val britishLibApi = britishLibRetrofit.create(BritishLibraryApi::class.java)
    private val dnbApi = dnbRetrofit.create(DNBApi::class.java)
    private val bneApi = bneRetrofit.create(BNEApi::class.java)

    /**
     * Busca clasificaciones usando datos del libro (estrategia de búsqueda híbrida)
     */
    suspend fun searchClassificationsByBookData(
        isbn: String? = null,
        title: String? = null,
        author: String? = null,
        publisher: String? = null
    ): EnhancedClassifications? = withContext(Dispatchers.IO) {

        Log.d(TAG, "=== Búsqueda de clasificaciones iniciada ===")
        Log.d(TAG, "ISBN: '$isbn', Título: '$title', Autor: '$author', Editorial: '$publisher'")

        val results = EnhancedClassifications()

        // Estrategia 1: Buscar por ISBN (más preciso)
        if (!isbn.isNullOrBlank()) {
            Log.d(TAG, "Estrategia 1: Búsqueda por ISBN")

            // WorldCat Classify (muy bueno para LC y DDC)
            val worldCatResult = tryWorldCatByISBN(isbn)
            worldCatResult?.let {
                results.merge(it)
                Log.d(TAG, "WorldCat por ISBN: ${it}")
            }
        }

        // Estrategia 2: Buscar por Título + Autor (cuando no hay ISBN)
        if (!title.isNullOrBlank()) {
            Log.d(TAG, "Estrategia 2: Búsqueda por título y autor")

            // Library of Congress
            val locResult = tryLOCByTitleAuthor(title, author)
            locResult?.let {
                results.merge(it)
                Log.d(TAG, "LOC por título/autor: ${it}")
            }

            // WorldCat por título/autor
            val worldCatTitleResult = tryWorldCatByTitleAuthor(title, author)
            worldCatTitleResult?.let {
                results.merge(it)
                Log.d(TAG, "WorldCat por título/autor: ${it}")
            }

            // Harvard Library
            val harvardResult = tryHarvardByTitleAuthor(title, author)
            harvardResult?.let {
                results.merge(it)
                Log.d(TAG, "Harvard por título/autor: ${it}")
            }

            // British Library (para libros en inglés)
            if (isEnglishTitle(title)) {
                val blResult = tryBritishLibraryByTitle(title, author)
                blResult?.let {
                    results.merge(it)
                    Log.d(TAG, "British Library: ${it}")
                }
            }

            // Biblioteca Nacional de España (para libros en español)
            if (isSpanishTitle(title)) {
                val bneResult = tryBNEByTitle(title)
                bneResult?.let {
                    results.merge(it)
                    Log.d(TAG, "BNE: ${it}")
                }
            }
        }

        // Estrategia 3: Deutsche Nationalbibliothek por autor (para UDC/DCU)
        if (!author.isNullOrBlank()) {
            Log.d(TAG, "Estrategia 3: Búsqueda en DNB por autor")
            val dnbResult = tryDNBByAuthor(author)
            dnbResult?.let {
                results.merge(it)
                Log.d(TAG, "DNB por autor: ${it}")
            }
        }

        Log.d(TAG, "=== Resultado final de clasificaciones ===")
        Log.d(TAG, "LC: ${results.lcClassification.joinToString(", ")}")
        Log.d(TAG, "DDC: ${results.deweyClassification.joinToString(", ")}")
        Log.d(TAG, "UDC: ${results.udcClassification.joinToString(", ")}")
        Log.d(TAG, "Otras: ${results.otherClassifications}")

        return@withContext if (results.hasAnyClassification()) results else null
    }

    private suspend fun tryWorldCatByISBN(isbn: String): EnhancedClassifications? {
        return try {
            Log.d(TAG, "Intentando WorldCat Classify por ISBN: $isbn")
            val response = worldCatApi.classifyByISBN(isbn)
            if (response.isSuccessful && response.body() != null) {
                parseWorldCatXMLResponse(response.body()!!)
            } else {
                Log.d(TAG, "WorldCat por ISBN falló: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en WorldCat por ISBN", e)
            null
        }
    }

    private suspend fun tryWorldCatByTitleAuthor(title: String, author: String?): EnhancedClassifications? {
        return try {
            Log.d(TAG, "Intentando WorldCat por título: '$title', autor: '$author'")
            val response = worldCatApi.classifyByTitleAuthor(title, author)
            if (response.isSuccessful && response.body() != null) {
                parseWorldCatXMLResponse(response.body()!!)
            } else {
                Log.d(TAG, "WorldCat por título/autor falló: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en WorldCat por título/autor", e)
            null
        }
    }

    private suspend fun tryLOCByTitleAuthor(title: String, author: String?): EnhancedClassifications? {
        return try {
            val query = buildLOCQuery(title, author)
            Log.d(TAG, "Intentando LOC con query: '$query'")
            val response = locApi.searchBooks(query)
            if (response.isSuccessful && response.body() != null) {
                parseLOCResponse(response.body()!!)
            } else {
                Log.d(TAG, "LOC falló: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en LOC", e)
            null
        }
    }

    private suspend fun tryHarvardByTitleAuthor(title: String, author: String?): EnhancedClassifications? {
        return try {
            val query = buildHarvardQuery(title, author)
            Log.d(TAG, "Intentando Harvard con query: '$query'")
            val response = harvardApi.searchItems(query)
            if (response.isSuccessful && response.body() != null) {
                parseHarvardResponse(response.body()!!)
            } else {
                Log.d(TAG, "Harvard falló: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en Harvard", e)
            null
        }
    }

    private suspend fun tryBritishLibraryByTitle(title: String, author: String?): EnhancedClassifications? {
        return try {
            // British Library requiere búsqueda por número bibliográfico
            // Por ahora retornamos null - requiere implementación más compleja
            Log.d(TAG, "British Library: implementación pendiente")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error en British Library", e)
            null
        }
    }

    private suspend fun tryBNEByTitle(title: String): EnhancedClassifications? {
        return try {
            Log.d(TAG, "Intentando BNE con título: '$title'")
            val response = bneApi.searchCatalog(searchArg = title)
            if (response.isSuccessful && response.body() != null) {
                parseBNEResponse(response.body()!!)
            } else {
                Log.d(TAG, "BNE falló: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en BNE", e)
            null
        }
    }

    private suspend fun tryDNBByAuthor(author: String): EnhancedClassifications? {
        return try {
            val query = "aut.person=${author}"
            Log.d(TAG, "Intentando DNB con query: '$query'")
            val response = dnbApi.searchAuthorities(query = query)
            if (response.isSuccessful && response.body() != null) {
                parseDNBXMLResponse(response.body()!!)
            } else {
                Log.d(TAG, "DNB falló: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en DNB", e)
            null
        }
    }

    // Funciones de parsing
    private fun parseWorldCatXMLResponse(xmlResponse: String): EnhancedClassifications? {
        return try {
            val classifications = EnhancedClassifications()

            // Buscar LC Classification
            val lcPattern = Regex("<lcc>(.*?)</lcc>")
            lcPattern.findAll(xmlResponse).forEach { match ->
                classifications.lcClassification.add(match.groupValues[1])
            }

            // Buscar DDC Classification
            val ddcPattern = Regex("<ddc>(.*?)</ddc>")
            ddcPattern.findAll(xmlResponse).forEach { match ->
                classifications.deweyClassification.add(match.groupValues[1])
            }

            classifications.sources.add("WorldCat Classify")
            if (classifications.hasAnyClassification()) classifications else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WorldCat XML", e)
            null
        }
    }

    private fun parseLOCResponse(jsonResponse: JsonObject): EnhancedClassifications? {
        return try {
            val classifications = EnhancedClassifications()

            val results = jsonResponse.getAsJsonArray("results")
            results?.forEach { result ->
                val item = result.asJsonObject

                // LC Classification
                val lcClass = item.getAsJsonPrimitive("class")?.asString
                lcClass?.let { classifications.lcClassification.add(it) }

                // Subject headings
                val subjects = item.getAsJsonArray("subject")
                subjects?.forEach { subject ->
                    classifications.subjectHeadings.add(subject.asString)
                }
            }

            classifications.sources.add("Library of Congress")
            if (classifications.hasAnyClassification()) classifications else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LOC response", e)
            null
        }
    }

    private fun parseHarvardResponse(jsonResponse: JsonObject): EnhancedClassifications? {
        return try {
            val classifications = EnhancedClassifications()

            val docs = jsonResponse.getAsJsonObject("docs")
            docs?.let { docsObj ->
                val items = docsObj.getAsJsonArray("items")
                items?.forEach { item ->
                    val itemObj = item.asJsonObject

                    // Buscar clasificaciones en metadatos
                    val classification = itemObj.getAsJsonPrimitive("classification")?.asString
                    classification?.let {
                        if (it.matches(Regex("^[0-9]{3}.*"))) {
                            classifications.deweyClassification.add(it)
                        } else {
                            classifications.otherClassifications["Harvard"] = it
                        }
                    }
                }
            }

            classifications.sources.add("Harvard Library")
            if (classifications.hasAnyClassification()) classifications else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Harvard response", e)
            null
        }
    }

    private fun parseBNEResponse(jsonResponse: JsonObject): EnhancedClassifications? {
        return try {
            val classifications = EnhancedClassifications()

            // Implementación básica - BNE puede usar CDU
            val records = jsonResponse.getAsJsonArray("records")
            records?.forEach { record ->
                val recordObj = record.asJsonObject
                val cdu = recordObj.getAsJsonPrimitive("cdu")?.asString
                cdu?.let { classifications.udcClassification.add(it) }
            }

            classifications.sources.add("Biblioteca Nacional de España")
            if (classifications.hasAnyClassification()) classifications else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing BNE response", e)
            null
        }
    }

    private fun parseDNBXMLResponse(xmlResponse: String): EnhancedClassifications? {
        return try {
            val classifications = EnhancedClassifications()

            // Buscar UDC/DCU en respuesta XML
            val udcPattern = Regex("<dc:subject[^>]*>([0-9]+.*?)</dc:subject>")
            udcPattern.findAll(xmlResponse).forEach { match ->
                val udc = match.groupValues[1]
                if (udc.matches(Regex("^[0-9].*"))) {
                    classifications.udcClassification.add(udc)
                }
            }

            classifications.sources.add("Deutsche Nationalbibliothek")
            if (classifications.hasAnyClassification()) classifications else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing DNB XML", e)
            null
        }
    }

    // Funciones auxiliares
    private fun buildLOCQuery(title: String, author: String?): String {
        return if (!author.isNullOrBlank()) {
            "title:$title author:$author"
        } else {
            "title:$title"
        }
    }

    private fun buildHarvardQuery(title: String, author: String?): String {
        return if (!author.isNullOrBlank()) {
            "title:$title author:$author"
        } else {
            "title:$title"
        }
    }

    private fun isEnglishTitle(title: String): Boolean {
        // Simple heurística para detectar títulos en inglés
        val englishWords = setOf("the", "and", "of", "in", "to", "a", "an", "for", "with", "on", "at", "by", "from")
        val words = title.lowercase().split(" ")
        return words.any { it in englishWords }
    }

    private fun isSpanishTitle(title: String): Boolean {
        // Simple heurística para detectar títulos en español
        val spanishWords = setOf("el", "la", "los", "las", "de", "del", "en", "y", "un", "una", "para", "con", "por")
        val words = title.lowercase().split(" ")
        return words.any { it in spanishWords }
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
        lcClassification.distinct()
        deweyClassification.distinct()
        udcClassification.distinct()
        subjectHeadings.distinct()
    }

    fun getBestLC(): String? = lcClassification.firstOrNull()
    fun getBestDewey(): String? = deweyClassification.firstOrNull()
    fun getBestUDC(): String? = udcClassification.firstOrNull()
}
