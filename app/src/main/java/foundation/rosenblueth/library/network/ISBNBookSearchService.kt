// kotlin
package foundation.rosenblueth.library.network

import android.util.Log
import com.google.gson.JsonObject
import foundation.rosenblueth.library.data.model.BookModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Servicio mejorado para búsqueda de libros por ISBN con múltiples fuentes.
 * Este servicio busca información completa del libro, no solo clasificaciones.
 */
object ISBNBookSearchService {
    private val TAG = "ISBNBookSearch"

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
    private val openLibSearchApi = openLibRetrofit.create(OpenLibSearchApi::class.java)

    /**
     * Busca información completa del libro usando 4 fuentes en cascada.
     * Retorna BookModel con todos los datos disponibles.
     */
    suspend fun searchBookByISBN(isbn: String): BookModel? = withContext(Dispatchers.IO) {
        // Normalizar ISBN
        val normalizedIsbn = normalizeISBN(isbn)
        Log.d(TAG, "Buscando libro con ISBN: $normalizedIsbn")

        // Intentar OpenLibrary API primero (más completo)
        Log.d(TAG, "1/4 Intentando OpenLibrary API...")
        val fromOpenLib = tryOpenLibraryApi(normalizedIsbn)
        if (fromOpenLib != null) {
            Log.d(TAG, "✓ Libro encontrado en OpenLibrary API")
            return@withContext fromOpenLib
        }

        // Intentar OpenLibrary directo
        Log.d(TAG, "2/4 Intentando OpenLibrary directo...")
        val fromOpenLibDirect = tryOpenLibraryDirect(normalizedIsbn)
        if (fromOpenLibDirect != null) {
            Log.d(TAG, "✓ Libro encontrado en OpenLibrary directo")
            return@withContext fromOpenLibDirect
        }

        // Intentar OpenLibrary Search
        Log.d(TAG, "3/4 Intentando OpenLibrary Search...")
        val fromOpenLibSearch = tryOpenLibrarySearch(normalizedIsbn)
        if (fromOpenLibSearch != null) {
            Log.d(TAG, "✓ Libro encontrado en OpenLibrary Search")
            return@withContext fromOpenLibSearch
        }

        // Intentar Google Books
        Log.d(TAG, "4/4 Intentando Google Books...")
        val fromGoogleBooks = tryGoogleBooks(normalizedIsbn)
        if (fromGoogleBooks != null) {
            Log.d(TAG, "✓ Libro encontrado en Google Books")
            return@withContext fromGoogleBooks
        }

        Log.w(TAG, "✗ No se encontró el libro con ISBN $normalizedIsbn en ninguna fuente")
        return@withContext null
    }

    private fun normalizeISBN(isbn: String): String {
        // Remover guiones, espacios, etc.
        val cleaned = isbn.filter { it.isDigit() || it.uppercaseChar() == 'X' }
        return when {
            cleaned.length == 13 -> cleaned
            cleaned.length == 10 -> convertISBN10to13(cleaned)
            else -> cleaned // Intentar de todas formas
        }
    }

    private fun convertISBN10to13(isbn10: String): String {
        @Suppress("SwallowedException")
        try {
            val core = "978" + isbn10.substring(0, 9)
            var sum = 0
            for (i in core.indices) {
                val digit = core[i].digitToInt()
                sum += if (i % 2 == 0) digit else digit * 3
            }
            val check = (10 - (sum % 10)) % 10
            return core + check.toString()
        } catch (e: Exception) {
            return isbn10
        }
    }

    private suspend fun tryOpenLibraryApi(isbn: String): BookModel? {
        return try {
            val key = "ISBN:$isbn"
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

            parseOpenLibraryApiData(entry, isbn)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OpenLibrary API", e)
            null
        }
    }

    private suspend fun tryOpenLibraryDirect(isbn: String): BookModel? {
        return try {
            val resp = openLibApi.getByIsbnDirect(isbn)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.d(TAG, "OpenLibrary directo: respuesta no exitosa (${resp.code()})")
                return null
            }

            val body = resp.body()!!
            parseOpenLibraryDirectData(body, isbn)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OpenLibrary directo", e)
            null
        }
    }

    private suspend fun tryOpenLibrarySearch(isbn: String): BookModel? {
        return try {
            val resp = openLibSearchApi.searchByIsbn(isbn)
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
            parseOpenLibrarySearchData(firstDoc, isbn)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OpenLibrary Search", e)
            null
        }
    }

    private suspend fun tryGoogleBooks(isbn: String): BookModel? {
        return try {
            val query = "isbn:$isbn"
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
            parseGoogleBooksData(volumeInfo, isbn)
        } catch (e: Exception) {
            Log.e(TAG, "Error en Google Books", e)
            null
        }
    }

    private fun parseOpenLibraryApiData(entry: JsonObject, isbn: String): BookModel? {
        try {
            Log.d(TAG, "Parseando datos de OpenLibrary API para ISBN: $isbn")
            Log.d(TAG, "JSON recibido: ${entry}")

            val title = entry.getAsJsonPrimitive("title")?.asString ?: ""
            Log.d(TAG, "Título extraído: '$title'")

            // Autores
            val authorsArray = entry.getAsJsonArray("authors")
            val author = if (authorsArray != null && authorsArray.size() > 0) {
                authorsArray[0].asJsonObject.getAsJsonPrimitive("name")?.asString ?: ""
            } else ""
            Log.d(TAG, "Autor extraído: '$author'")

            // Publishers
            val publishersArray = entry.getAsJsonArray("publishers")
            val publisher = if (publishersArray != null && publishersArray.size() > 0) {
                publishersArray[0].asJsonObject.getAsJsonPrimitive("name")?.asString ?: ""
            } else ""
            Log.d(TAG, "Editorial extraída: '$publisher'")

            // Año de publicación
            val publishDate = entry.getAsJsonPrimitive("publish_date")?.asString
            val year = extractYear(publishDate)
            Log.d(TAG, "Año extraído: $year (de fecha: '$publishDate')")

            // Clasificaciones
            val classifications = entry.getAsJsonObject("classifications")
            val lc = classifications?.getAsJsonArray("lc_classifications")?.firstOrNull()?.asString ?: ""
            val dewey = classifications?.getAsJsonArray("dewey_decimal_class")?.firstOrNull()?.asString ?: ""
            val cdu = classifications?.getAsJsonArray("cdu")?.firstOrNull()?.asString ?: ""

            // Log de clasificaciones encontradas
            Log.d(TAG, "Clasificaciones encontradas - LC: '$lc', Dewey: '$dewey', DCU: '$cdu'")

            // Número de páginas
            val pages = entry.getAsJsonPrimitive("number_of_pages")?.asInt
            Log.d(TAG, "Páginas extraídas: $pages")

            if (title.isBlank()) {
                Log.w(TAG, "OpenLibrary API: título vacío - retornando null")
                return null
            }

            Log.d(TAG, "Creando BookModel con datos completos:")
            Log.d(TAG, "  - Título: '$title'")
            Log.d(TAG, "  - Autor: '$author'")
            Log.d(TAG, "  - Editorial: '$publisher'")
            Log.d(TAG, "  - ISBN: '$isbn'")
            Log.d(TAG, "  - Clasificaciones: LC='$lc', Dewey='$dewey', DCU='$cdu'")

            val bookModel = BookModel(
                title = title,
                author = author,
                isbn = isbn,
                publisher = publisher,
                publishedYear = year,
                pages = pages,
                lcClassification = lc,
                deweyClassification = dewey,
                dcuClassification = cdu
            )

            Log.d(TAG, "BookModel creado exitosamente: $bookModel")
            return bookModel
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando datos de OpenLibrary API", e)
            return null
        }
    }

    private fun parseOpenLibraryDirectData(data: JsonObject, isbn: String): BookModel? {
        try {
            Log.d(TAG, "Parseando datos de OpenLibrary Direct para ISBN: $isbn")
            Log.d(TAG, "JSON recibido: ${data}")

            val title = data.getAsJsonPrimitive("title")?.asString ?: ""
            Log.d(TAG, "Título extraído: '$title'")

            // Autores (puede ser array de objetos o strings)
            val authorsArray = data.getAsJsonArray("authors")
            val author = if (authorsArray != null && authorsArray.size() > 0) {
                val firstAuthor = authorsArray[0]
                if (firstAuthor.isJsonObject) {
                    firstAuthor.asJsonObject.getAsJsonPrimitive("name")?.asString ?: ""
                } else {
                    firstAuthor.asString ?: ""
                }
            } else ""
            Log.d(TAG, "Autor extraído: '$author'")

            // Publishers
            val publishersArray = data.getAsJsonArray("publishers")
            val publisher = publishersArray?.firstOrNull()?.asString ?: ""
            Log.d(TAG, "Editorial extraída: '$publisher'")

            // Año
            val publishDate = data.getAsJsonPrimitive("publish_date")?.asString
            val year = extractYear(publishDate)
            Log.d(TAG, "Año extraído: $year (de fecha: '$publishDate')")

            // Clasificaciones
            val lc = data.getAsJsonArray("lc_classifications")?.firstOrNull()?.asString ?: ""
            val dewey = data.getAsJsonArray("dewey_decimal_class")?.firstOrNull()?.asString ?: ""

            // Log de clasificaciones
            Log.d(TAG, "OpenLibrary Direct - Clasificaciones: LC='$lc', Dewey='$dewey'")

            // Páginas
            val pages = data.getAsJsonPrimitive("number_of_pages")?.asInt
            Log.d(TAG, "Páginas extraídas: $pages")

            if (title.isBlank()) {
                Log.w(TAG, "OpenLibrary directo: título vacío - retornando null")
                return null
            }

            Log.d(TAG, "Creando BookModel desde OpenLibrary Direct:")
            Log.d(TAG, "  - Título: '$title'")
            Log.d(TAG, "  - Autor: '$author'")
            Log.d(TAG, "  - Editorial: '$publisher'")
            Log.d(TAG, "  - ISBN: '$isbn'")

            val bookModel = BookModel(
                title = title,
                author = author,
                isbn = isbn,
                publisher = publisher,
                publishedYear = year,
                pages = pages,
                lcClassification = lc,
                deweyClassification = dewey,
                dcuClassification = ""
            )

            Log.d(TAG, "BookModel creado exitosamente desde Direct: $bookModel")
            return bookModel
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando datos de OpenLibrary directo", e)
            return null
        }
    }

    private fun parseOpenLibrarySearchData(doc: JsonObject, isbn: String): BookModel? {
        try {
            val title = doc.getAsJsonPrimitive("title")?.asString ?: ""

            // Autores
            val authorArray = doc.getAsJsonArray("author_name")
            val author = authorArray?.firstOrNull()?.asString ?: ""

            // Publisher
            val publisherArray = doc.getAsJsonArray("publisher")
            val publisher = publisherArray?.firstOrNull()?.asString ?: ""

            // Año
            val firstPublishYear = doc.getAsJsonPrimitive("first_publish_year")?.asInt

            // Clasificaciones
            val lccArray = doc.getAsJsonArray("lcc")
            val ddcArray = doc.getAsJsonArray("ddc")
            val lc = lccArray?.firstOrNull()?.asString ?: ""
            val dewey = ddcArray?.firstOrNull()?.asString ?: ""

            // Log de clasificaciones
            Log.d(TAG, "OpenLibrary Search - Clasificaciones: LC='$lc', Dewey='$dewey'")

            if (title.isBlank()) {
                Log.d(TAG, "OpenLibrary Search: título vacío")
                return null
            }

            return BookModel(
                title = title,
                author = author,
                isbn = isbn,
                publisher = publisher,
                publishedYear = firstPublishYear,
                lcClassification = lc,
                deweyClassification = dewey,
                dcuClassification = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando datos de OpenLibrary Search", e)
            return null
        }
    }

    private fun parseGoogleBooksData(volumeInfo: JsonObject?, isbn: String): BookModel? {
        if (volumeInfo == null) {
            Log.w(TAG, "Google Books: volumeInfo es null")
            return null
        }

        try {
            Log.d(TAG, "Parseando datos de Google Books para ISBN: $isbn")
            Log.d(TAG, "VolumeInfo JSON: $volumeInfo")

            val title = volumeInfo.getAsJsonPrimitive("title")?.asString ?: ""
            Log.d(TAG, "Título extraído: '$title'")

            // Autores
            val authorsArray = volumeInfo.getAsJsonArray("authors")
            val author = authorsArray?.firstOrNull()?.asString ?: ""
            Log.d(TAG, "Autor extraído: '$author'")

            // Publisher
            val publisher = volumeInfo.getAsJsonPrimitive("publisher")?.asString ?: ""
            Log.d(TAG, "Editorial extraída: '$publisher'")

            // Año
            val publishedDate = volumeInfo.getAsJsonPrimitive("publishedDate")?.asString
            val year = extractYear(publishedDate)
            Log.d(TAG, "Año extraído: $year (de fecha: '$publishedDate')")

            // Páginas
            val pages = volumeInfo.getAsJsonPrimitive("pageCount")?.asInt
            Log.d(TAG, "Páginas extraídas: $pages")

            // Descripción
            val description = volumeInfo.getAsJsonPrimitive("description")?.asString ?: ""
            Log.d(TAG, "Descripción extraída: ${description.take(100)}...")

            // Imagen de portada
            val imageLinks = volumeInfo.getAsJsonObject("imageLinks")
            val coverImage = imageLinks?.getAsJsonPrimitive("thumbnail")?.asString ?: ""
            Log.d(TAG, "Imagen de portada: '$coverImage'")

            if (title.isBlank()) {
                Log.w(TAG, "Google Books: título vacío - retornando null")
                return null
            }

            Log.d(TAG, "Creando BookModel desde Google Books:")
            Log.d(TAG, "  - Título: '$title'")
            Log.d(TAG, "  - Autor: '$author'")
            Log.d(TAG, "  - Editorial: '$publisher'")
            Log.d(TAG, "  - ISBN: '$isbn'")

            val bookModel = BookModel(
                title = title,
                author = author,
                isbn = isbn,
                publisher = publisher,
                publishedYear = year,
                pages = pages,
                description = description,
                coverImageUrl = coverImage,
                lcClassification = "",
                deweyClassification = "",
                dcuClassification = ""
            )

            Log.d(TAG, "BookModel creado exitosamente desde Google Books: $bookModel")
            return bookModel
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando datos de Google Books", e)
            return null
        }
    }

    private fun extractYear(dateString: String?): Int? {
        if (dateString == null) return null
        // Intentar extraer año de diferentes formatos: "2023", "2023-01-15", "January 2023", etc.
        val yearPattern = Regex("\\d{4}")
        val match = yearPattern.find(dateString)
        return match?.value?.toIntOrNull()
    }
}
