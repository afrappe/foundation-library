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

interface WorldCatApi {
    // http://xisbn.worldcat.org/webservices/xid/isbn/9780140328721?method=getMetadata&format=json&fl=*
    @GET("webservices/xid/isbn/{isbn}")
    suspend fun getMetadata(
        @Path("isbn") isbn: String,
        @Query("method") method: String = "getMetadata",
        @Query("format") format: String = "json",
        @Query("fl") fields: String = "*"
    ): Response<JsonObject>
}

interface OpenLibSearchApi {
    // https://openlibrary.org/search.json?isbn=9780140328721
    @GET("search.json")
    suspend fun searchByIsbn(
        @Query("isbn") isbn: String,
        @Query("fields") fields: String = "key,title,author_name,publisher,publish_year,isbn,lcc,ddc,subject"
    ): Response<JsonObject>
}

interface ISBNdbApi {
    // https://api2.isbndb.com/book/9780140328721
    // Nota: Requiere API key en header, pero primero intentamos sin key
    @GET("book/{isbn}")
    suspend fun getBookInfo(
        @Path("isbn") isbn: String
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

    private val worldCatRetrofit = Retrofit.Builder()
        .baseUrl("http://xisbn.worldcat.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val openLibSearchRetrofit = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val isbndbRetrofit = Retrofit.Builder()
        .baseUrl("https://api2.isbndb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val openLibApi = openLibRetrofit.create(OpenLibraryApi::class.java)
    private val googleBooksApi = googleBooksRetrofit.create(GoogleBooksApi::class.java)
    private val worldCatApi = worldCatRetrofit.create(WorldCatApi::class.java)
    private val openLibSearchApi = openLibSearchRetrofit.create(OpenLibSearchApi::class.java)
    private val isbndbApi = isbndbRetrofit.create(ISBNdbApi::class.java)

    /**
     * Busca clasificaciones usando múltiples estrategias (6 fuentes):
     * 1. OpenLibrary API endpoint (/api/books)
     * 2. OpenLibrary endpoint directo (/isbn/{isbn}.json)
     * 3. OpenLibrary Search API (/search.json)
     * 4. Google Books API
     * 5. WorldCat xISBN
     * 6. ISBNdb (sin API key, limitado)
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

        // Estrategia 3: OpenLibrary Search API
        Log.d(TAG, "Buscando ISBN $isbn13 en OpenLibrary Search...")
        val fromSearch = tryOpenLibrarySearch(isbn13)
        if (fromSearch != null) {
            Log.d(TAG, "✓ Encontrado en OpenLibrary Search")
            return@withContext fromSearch
        }

        // Estrategia 4: Google Books API
        Log.d(TAG, "Buscando ISBN $isbn13 en Google Books...")
        val fromGoogle = tryGoogleBooks(isbn13)
        if (fromGoogle != null) {
            Log.d(TAG, "✓ Encontrado en Google Books")
            return@withContext fromGoogle
        }

        // Estrategia 5: WorldCat xISBN
        Log.d(TAG, "Buscando ISBN $isbn13 en WorldCat...")
        val fromWorldCat = tryWorldCat(isbn13)
        if (fromWorldCat != null) {
            Log.d(TAG, "✓ Encontrado en WorldCat")
            return@withContext fromWorldCat
        }

        // Estrategia 6: ISBNdb (limitado sin API key)
        Log.d(TAG, "Buscando ISBN $isbn13 en ISBNdb...")
        val fromIsbndb = tryISBNdb(isbn13)
        if (fromIsbndb != null) {
            Log.d(TAG, "✓ Encontrado en ISBNdb")
            return@withContext fromIsbndb
        }

        Log.w(TAG, "✗ No se encontró el ISBN $isbn13 en ninguna de las 6 fuentes")
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

    private suspend fun tryOpenLibrarySearch(isbn13: String): BookClassifications? {
        return try {
            val resp = openLibSearchApi.searchByIsbn(isbn13)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "OpenLibrary Search: respuesta no exitosa (${resp.code()})")
                return null
            }

            val body = resp.body()!!
            val docs = body.getAsJsonArray("docs")
            if (docs == null || docs.size() == 0) {
                Log.d(TAG, "OpenLibrary Search: no hay documentos")
                return null
            }

            val firstDoc = docs[0].asJsonObject
            extractClassificationsFromOpenLibSearch(firstDoc)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OpenLibrary Search", e)
            null
        }
    }

    private suspend fun tryWorldCat(isbn13: String): BookClassifications? {
        return try {
            val resp = worldCatApi.getMetadata(isbn13)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "WorldCat: respuesta no exitosa (${resp.code()})")
                return null
            }

            val body = resp.body()!!
            val stat = body.getAsJsonPrimitive("stat")?.asString
            if (stat != "ok") {
                Log.d(TAG, "WorldCat: stat no es 'ok' (stat=$stat)")
                return null
            }

            val list = body.getAsJsonArray("list")
            if (list == null || list.size() == 0) {
                Log.d(TAG, "WorldCat: no hay items en lista")
                return null
            }

            val firstItem = list[0].asJsonObject
            extractClassificationsFromWorldCat(firstItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error en WorldCat", e)
            null
        }
    }

    private suspend fun tryISBNdb(isbn13: String): BookClassifications? {
        return try {
            val resp = isbndbApi.getBookInfo(isbn13)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "ISBNdb: respuesta no exitosa (${resp.code()}) - puede requerir API key")
                return null
            }

            val body = resp.body()!!
            val book = body.getAsJsonObject("book")
            if (book == null) {
                Log.d(TAG, "ISBNdb: no hay objeto 'book'")
                return null
            }

            extractClassificationsFromISBNdb(book)
        } catch (e: Exception) {
            Log.e(TAG, "Error en ISBNdb", e)
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

    private fun extractClassificationsFromOpenLibSearch(doc: JsonObject): BookClassifications? {
        // OpenLibrary Search API retorna arrays en lugar de valores simples
        val lccArray = doc.getAsJsonArray("lcc")
        val ddcArray = doc.getAsJsonArray("ddc")

        val lc = lccArray?.firstOrNull()?.asString
        val dewey = ddcArray?.firstOrNull()?.asString

        if (lc == null && dewey == null) {
            Log.d(TAG, "OpenLibrary Search: no hay clasificaciones")
            return null
        }

        return BookClassifications(
            lcClassification = lc,
            dewey = dewey,
            cdu = null // OpenLibrary Search no suele tener CDU
        )
    }

    private fun extractClassificationsFromWorldCat(item: JsonObject): BookClassifications? {
        // WorldCat puede tener clasificaciones en formato diferente
        val lccn = item.getAsJsonPrimitive("lccn")?.asString
        val oclcNumber = item.getAsJsonPrimitive("oclcnum")?.asString

        // WorldCat xISBN no suele devolver clasificaciones Dewey/LC directamente
        // pero proporciona LCCN (Library of Congress Control Number)
        // que es diferente de LC Classification

        if (lccn == null && oclcNumber == null) {
            Log.d(TAG, "WorldCat: no hay datos de clasificación útiles")
            return null
        }

        Log.d(TAG, "WorldCat: encontrado LCCN=$lccn, OCLC=$oclcNumber")

        // WorldCat xISBN no provee clasificaciones estándar en metadata básica
        return null
    }

    private fun extractClassificationsFromISBNdb(book: JsonObject): BookClassifications? {
        // ISBNdb tiene su propia estructura
        // Campos posibles: dewey_decimal, subjects, etc.
        val dewey = book.getAsJsonPrimitive("dewey_decimal")?.asString
        val subjects = book.getAsJsonArray("subjects")

        if (dewey == null) {
            Log.d(TAG, "ISBNdb: no hay clasificación Dewey")
            return null
        }

        return BookClassifications(
            lcClassification = null, // ISBNdb no suele tener LC
            dewey = dewey,
            cdu = null // ISBNdb no tiene CDU
        )
    }
}

data class BookClassifications(
    val lcClassification: String? = null,
    val dewey: String? = null,
    val cdu: String? = null
)
