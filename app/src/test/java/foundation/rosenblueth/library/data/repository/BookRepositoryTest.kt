package foundation.rosenblueth.library.data.repository

import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.data.network.BookCatalogApiService
import foundation.rosenblueth.library.data.network.BookResponseItem
import foundation.rosenblueth.library.data.network.BookSearchResponse
import foundation.rosenblueth.library.data.network.ImageLinks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BookRepositoryTest {

    @Mock
    private lateinit var mockApiService: BookCatalogApiService

    private lateinit var bookRepository: BookRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        bookRepository = TestBookRepository(mockApiService)
    }

    // Clase de prueba que extiende BookRepository para inyectar dependencias mockeadas
    private class TestBookRepository(private val apiService: BookCatalogApiService) : BookRepository() {
        // Sobrescribimos para usar el servicio mock en lugar del real
        override val bookApiService: BookCatalogApiService
            get() = apiService
    }

    @Test
    fun `searchBookByTitle con título válido devuelve lista de libros`() = runTest {
        // Preparar
        val mockBookItem = BookResponseItem(
            title = "El Título",
            authors = listOf("Autor Ejemplo"),
            isbn = listOf("1234567890"),
            publisher = "Editorial Test",
            publishedDate = "2020",
            description = "Descripción del libro",
            pageCount = 200,
            categories = listOf("Ficción"),
            language = "es",
            imageLinks = ImageLinks(thumbnail = "http://ejemplo.com/imagen.jpg")
        )

        val mockResponse = Response.success(
            BookSearchResponse(
                items = listOf(mockBookItem),
                totalItems = 1,
                status = "ok"
            )
        )

        `when`(mockApiService.searchBookByTitle("El Título")).thenReturn(mockResponse)

        // Ejecutar
        val result = bookRepository.searchBookByTitle("El Título")

        // Verificar
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("El Título", result.getOrNull()?.get(0)?.title)
        assertEquals("Autor Ejemplo", result.getOrNull()?.get(0)?.author)
    }

    @Test
    fun `searchBookByTitle con título vacío devuelve error`() = runTest {
        // Caso límite: Título vacío
        val result = bookRepository.searchBookByTitle("")

        // Verificar que se maneje adecuadamente
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `searchBookByTitle maneja respuesta vacía`() = runTest {
        // Preparar - Respuesta sin ítems
        val emptyResponse = Response.success(
            BookSearchResponse(
                items = emptyList(),
                totalItems = 0,
                status = "ok"
            )
        )

        `when`(mockApiService.searchBookByTitle("Título Inexistente")).thenReturn(emptyResponse)

        // Ejecutar
        val result = bookRepository.searchBookByTitle("Título Inexistente")

        // Verificar
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun `searchBookByTitle maneja error HTTP`() = runTest {
        // Preparar - Respuesta de error
        val errorResponse = Response.error<BookSearchResponse>(
            404,
            "{\"error\":\"Not found\"}".toResponseBody("application/json".toMediaType())
        )

        `when`(mockApiService.searchBookByTitle("Error")).thenReturn(errorResponse)

        // Segunda llamada a LOC también fallará
        `when`(mockApiService.searchBookInLOC("Error")).thenReturn(errorResponse)

        // Ejecutar
        val result = bookRepository.searchBookByTitle("Error")

        // Verificar
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `searchBookByTitle maneja timeout de red`() = runTest {
        // Preparar - Simulación de timeout
        `when`(mockApiService.searchBookByTitle("Timeout")).thenThrow(SocketTimeoutException("Timeout"))

        // Ejecutar
        val result = bookRepository.searchBookByTitle("Timeout")

        // Verificar
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SocketTimeoutException)
    }

    @Test
    fun `searchBookByTitle maneja error de IO`() = runTest {
        // Preparar - Simulación de error de IO
        `when`(mockApiService.searchBookByTitle("IOError")).thenThrow(IOException("Error de lectura"))

        // Ejecutar
        val result = bookRepository.searchBookByTitle("IOError")

        // Verificar
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `searchBookByTitle intenta con LOC cuando OCLC falla`() = runTest {
        // Preparar - Primera API falla
        val errorResponse = Response.error<BookSearchResponse>(
            500,
            "{\"error\":\"Server error\"}".toResponseBody("application/json".toMediaType())
        )

        // Respuesta exitosa de la segunda API
        val mockBookItem = BookResponseItem(
            title = "Título LOC",
            authors = listOf("Autor LOC"),
            isbn = listOf("0987654321"),
            publisher = "LOC Editorial",
            publishedDate = "2021",
            description = "Libro de LOC",
            pageCount = 150,
            categories = listOf("Historia"),
            language = "en",
            imageLinks = null
        )

        val successResponse = Response.success(
            BookSearchResponse(
                items = listOf(mockBookItem),
                totalItems = 1,
                status = "ok"
            )
        )

        `when`(mockApiService.searchBookByTitle("Fallback")).thenReturn(errorResponse)
        `when`(mockApiService.searchBookInLOC("Fallback")).thenReturn(successResponse)

        // Ejecutar
        val result = bookRepository.searchBookByTitle("Fallback")

        // Verificar
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Título LOC", result.getOrNull()?.get(0)?.title)
    }

    @Test
    fun `sendBookToBackend con libro válido retorna éxito`() = runTest {
        // Preparar
        val book = BookModel(
            title = "Título Test",
            author = "Autor Test",
            isbn = "1234567890123"
        )

        // Ejecutar
        val result = bookRepository.sendBookToBackend(book)

        // Verificar
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }
}
