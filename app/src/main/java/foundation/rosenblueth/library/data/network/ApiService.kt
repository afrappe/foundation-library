package foundation.rosenblueth.library.data.network

import foundation.rosenblueth.library.data.model.BookModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface para las APIs de servicios de catalogación de libros
 */
interface BookCatalogApiService {
    /**
     * Busca información de libro en OCLC (WorldCat)
     * @param title El título del libro a buscar
     * @return Respuesta con la información del libro
     */
    @GET("search/worldcat/search")
    suspend fun searchBookByTitle(@Query("q") title: String): Response<BookSearchResponse>

    /**
     * Busca información de libro en Library of Congress
     * @param title El título del libro a buscar
     * @return Respuesta con la información del libro
     */
    @GET("search/loc/search")
    suspend fun searchBookInLOC(@Query("q") title: String): Response<BookSearchResponse>

    /**
     * Búsqueda de libros por título en Library of Congress
     * @param query El título o términos de búsqueda
     * @param format El formato de respuesta (json)
     * @param filter Filtrar por tipo de contenido (books)
     * @return Respuesta con información de libros encontrados
     */
    @GET("search/")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fo") format: String = "json",
        @Query("at") filter: String = "books"
    ): Response<LocResponse>
}

/**
 * Modelo de respuesta de la búsqueda de libros
 */
data class BookSearchResponse(
    val items: List<BookResponseItem> = emptyList(),
    val totalItems: Int = 0,
    val status: String = "",
    val error: String? = null
)

/**
 * Modelo que representa un ítem de libro en la respuesta de la API
 */
data class BookResponseItem(
    val title: String,
    val authors: List<String> = emptyList(),
    val publisher: String = "",
    val publishedDate: String = "",
    val description: String = "",
    val isbn: List<String> = emptyList(),
    val pageCount: Int? = null,
    val categories: List<String> = emptyList(),
    val language: String = "",
    val imageLinks: ImageLinks? = null
)

/**
 * Enlaces a imágenes de portada
 */
data class ImageLinks(
    val smallThumbnail: String = "",
    val thumbnail: String = "",
    val medium: String = "",
    val large: String = ""
)

/**
 * Extensión para convertir la respuesta de la API a nuestro modelo de datos
 */
fun BookResponseItem.toBookModel(): BookModel {
    return BookModel(
        title = this.title,
        author = this.authors.joinToString(", "),
        isbn = this.isbn.firstOrNull() ?: "",
        publisher = this.publisher,
        publishedYear = this.publishedDate.split("-").firstOrNull()?.toIntOrNull(),
        pages = this.pageCount,
        subjects = this.categories,
        language = this.language,
        description = this.description,
        coverImageUrl = this.imageLinks?.thumbnail ?: ""
    )
}
