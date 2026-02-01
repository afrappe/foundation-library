package foundation.rosenblueth.library.data.repository

import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.data.network.RetrofitClient
import foundation.rosenblueth.library.data.network.toBookModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestionar las operaciones relacionadas con libros.
 */
open class BookRepository {
    open val bookApiService = RetrofitClient.bookApiService

    /**
     * Busca información de un libro por título en la Biblioteca del Congreso (LOC).
     *
     * @param title El título del libro a buscar
     * @return Una lista de modelos de libros que coinciden con la búsqueda
     */
    suspend fun searchBookByTitle(title: String): Result<List<BookModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Buscar en la API de la Biblioteca del Congreso
                val response = bookApiService.searchBooks(query = title)

                if (response.isSuccessful && response.body() != null) {
                    val locResponse = response.body()!!
                    // Convertir los resultados al modelo de libro de la aplicación
                    val books = locResponse.items.map { it.toBookModel() }
                    // Devolver la lista de libros (puede estar vacía)
                    Result.success(books)
                } else {
                    Result.failure(Exception("Error en la búsqueda: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Busca información de un libro por ISBN en la Biblioteca del Congreso (LOC).
     *
     * @param isbn El ISBN del libro a buscar (ISBN-10 o ISBN-13)
     * @return Una lista de modelos de libros que coinciden con la búsqueda
     */
    suspend fun searchBookByISBN(isbn: String): Result<List<BookModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Buscar en la API de la Biblioteca del Congreso usando el ISBN
                val response = bookApiService.searchBooks(query = isbn)

                if (response.isSuccessful && response.body() != null) {
                    val locResponse = response.body()!!
                    // Convertir los resultados al modelo de libro de la aplicación
                    val books = locResponse.items.map { it.toBookModel() }
                    // Devolver la lista de libros (puede estar vacía)
                    Result.success(books)
                } else {
                    Result.failure(Exception("Error en la búsqueda por ISBN: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Envía los datos del libro al backend
     *
     * @param book El modelo de libro a enviar
     * @return Resultado de la operación
     */
    suspend fun sendBookToBackend(book: BookModel): Result<Boolean> {
        // En una implementación real, aquí se llamaría a un endpoint de API para
        // enviar los datos del libro al backend

        return withContext(Dispatchers.IO) {
            try {
                // Simulando una llamada exitosa con los datos del libro
                // En este caso sólo lo imprimimos para demostrar su uso
                println("Enviando libro al backend: ${book.title} por ${book.author}")
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
