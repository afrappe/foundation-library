// kotlin
package foundation.rosenblueth.library.network

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

interface OpenLibraryApi {
    // https://openlibrary.org/api/books?bibkeys=ISBN:9780140328721&jscmd=data&format=json
    @GET("api/books")
    suspend fun getByIsbn(
        @Query("bibkeys") bibkeys: String,
        @Query("jscmd") jscmd: String = "data",
        @Query("format") format: String = "json"
    ): Response<JsonObject>

    // Búsqueda alternativa por ISBN usando el endpoint de works
    // https://openlibrary.org/isbn/9780140328721.json
    @GET("isbn/{isbn}.json")
    suspend fun getByIsbnDirect(
        @Path("isbn") isbn: String
    ): Response<JsonObject>
}

interface GoogleBooksApi {
    // https://www.googleapis.com/books/v1/volumes?q=isbn:9780140328721
    @GET("volumes")
    suspend fun searchByIsbn(
        @Query("q") query: String
    ): Response<JsonObject>
}

object OpenLibraryService {
    private val TAG = "OpenLibraryService"

    private val openLibRetrofit = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val googleBooksRetrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/books/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val openLibApi = openLibRetrofit.create(OpenLibraryApi::class.java)
    private val googleBooksApi = googleBooksRetrofit.create(GoogleBooksApi::class.java)

    /**
     * Busca clasificaciones usando múltiples estrategias:
     * 1. OpenLibrary API endpoint (/api/books)
     * 2. OpenLibrary endpoint directo (/isbn/{isbn}.json)
     * 3. Google Books API como fallback
     */
    suspend fun fetchClassifications(isbn13: String): BookClassifications? = withContext(Dispatchers.IO) {
        // Estrategia 1: OpenLibrary API endpoint
        Log.d(TAG, "Buscando ISBN $isbn13 en OpenLibrary API...")
        val fromApi = tryOpenLibraryApi(isbn13)
        if (fromApi != null) {
            Log.d(TAG, "✓ Encontrado en OpenLibrary API")
            return@withContext fromApi
        }

        // Estrategia 2: OpenLibrary endpoint directo
        Log.d(TAG, "Buscando ISBN $isbn13 en OpenLibrary directo...")
        val fromDirect = tryOpenLibraryDirect(isbn13)
        if (fromDirect != null) {
            Log.d(TAG, "✓ Encontrado en OpenLibrary directo")
            return@withContext fromDirect
        }

        // Estrategia 3: Google Books API
        Log.d(TAG, "Buscando ISBN $isbn13 en Google Books...")
        val fromGoogle = tryGoogleBooks(isbn13)
        if (fromGoogle != null) {
            Log.d(TAG, "✓ Encontrado en Google Books")
            return@withContext fromGoogle
        }

        Log.w(TAG, "✗ No se encontró el ISBN $isbn13 en ninguna fuente")
        return@withContext null
    }

    private suspend fun tryOpenLibraryApi(isbn13: String): BookClassifications? {
        return try {
            val key = "ISBN:$isbn13"
            val resp = openLibApi.getByIsbn(key)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "OpenLibrary API: respuesta no exitosa (${resp.code()})")
                return null
            }

            val body = resp.body()!!
            val entry = body.getAsJsonObject(key)
            if (entry == null) {
                Log.d(TAG, "OpenLibrary API: no hay entrada para $key")
                return null
            }

            extractClassificationsFromOpenLibData(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OpenLibrary API", e)
            null
        }
    }

    private suspend fun tryOpenLibraryDirect(isbn13: String): BookClassifications? {
        return try {
            val resp = openLibApi.getByIsbnDirect(isbn13)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "OpenLibrary directo: respuesta no exitosa (${resp.code()})")
                return null
            }

            val body = resp.body()!!
            extractClassificationsFromOpenLibDirect(body)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OpenLibrary directo", e)
            null
        }
    }

    private suspend fun tryGoogleBooks(isbn13: String): BookClassifications? {
        return try {
            val query = "isbn:$isbn13"
            val resp = googleBooksApi.searchByIsbn(query)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "Google Books: respuesta no exitosa (${resp.code()})")
                return null
            }

            val body = resp.body()!!
            val items = body.getAsJsonArray("items")
            if (items == null || items.size() == 0) {
                Log.d(TAG, "Google Books: no hay items")
                return null
            }

            val volumeInfo = items[0].asJsonObject.getAsJsonObject("volumeInfo")
            extractClassificationsFromGoogleBooks(volumeInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error en Google Books", e)
            null
        }
    }

    private fun extractClassificationsFromOpenLibData(entry: JsonObject): BookClassifications? {
        val classifications = entry.getAsJsonObject("classifications")

        val lc = classifications?.getAsJsonArray("lc_classifications")?.firstOrNull()?.asString
        val dewey = classifications?.getAsJsonArray("dewey_decimal_class")?.firstOrNull()?.asString
        val cdu = classifications?.getAsJsonArray("cdu")?.firstOrNull()?.asString
            ?: classifications?.getAsJsonArray("cdus")?.firstOrNull()?.asString

        // Si no encontramos ninguna clasificación, retornar null
        if (lc == null && dewey == null && cdu == null) {
            Log.d(TAG, "OpenLibrary API: no hay clasificaciones")
            return null
        }

        return BookClassifications(
            lcClassification = lc,
            dewey = dewey,
            cdu = cdu
        )
    }

    private fun extractClassificationsFromOpenLibDirect(entry: JsonObject): BookClassifications? {
        // El endpoint directo tiene una estructura diferente
        val lc = entry.getAsJsonArray("lc_classifications")?.firstOrNull()?.asString
        val dewey = entry.getAsJsonArray("dewey_decimal_class")?.firstOrNull()?.asString
        val cdu = entry.getAsJsonArray("cdu")?.firstOrNull()?.asString
            ?: entry.getAsJsonArray("cdus")?.firstOrNull()?.asString

        // También intentar obtener del objeto "classifications" si existe
        val classifications = entry.getAsJsonObject("classifications")
        val lcFromClass = classifications?.getAsJsonArray("lc_classifications")?.firstOrNull()?.asString
        val deweyFromClass = classifications?.getAsJsonArray("dewey_decimal_class")?.firstOrNull()?.asString
        val cduFromClass = classifications?.getAsJsonArray("cdu")?.firstOrNull()?.asString
            ?: classifications?.getAsJsonArray("cdus")?.firstOrNull()?.asString

        val finalLc = lc ?: lcFromClass
        val finalDewey = dewey ?: deweyFromClass
        val finalCdu = cdu ?: cduFromClass

        if (finalLc == null && finalDewey == null && finalCdu == null) {
            Log.d(TAG, "OpenLibrary directo: no hay clasificaciones")
            return null
        }

        return BookClassifications(
            lcClassification = finalLc,
            dewey = finalDewey,
            cdu = finalCdu
        )
    }

    private fun extractClassificationsFromGoogleBooks(volumeInfo: JsonObject?): BookClassifications? {
        if (volumeInfo == null) return null

        // Google Books no suele tener clasificaciones LC/Dewey/CDU directamente
        // pero podemos intentar extraerlas si están en industryIdentifiers u otras secciones
        // Por ahora retornamos null ya que Google Books se enfoca más en metadatos comerciales

        // Intentar obtener categorías (aunque no son lo mismo que clasificaciones)
        val categories = volumeInfo.getAsJsonArray("categories")
        val categoryStr = categories?.firstOrNull()?.asString

        // Google Books generalmente no proporciona LC, Dewey o CDU
        // pero podemos usar las categorías como fallback si es necesario
        Log.d(TAG, "Google Books: categoría encontrada: $categoryStr (no es clasificación estándar)")

        return null // Google Books no provee clasificaciones bibliotecarias estándar
    }
}

data class BookClassifications(
    val lcClassification: String? = null,
    val dewey: String? = null,
    val cdu: String? = null
)
