package foundation.rosenblueth.library.ui.viewmodel

import android.graphics.Bitmap
import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.data.repository.BookRepository
import foundation.rosenblueth.library.util.MainDispatcherRule
import foundation.rosenblueth.library.util.TextRecognitionHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class BookScannerViewModelTest {

    @Mock
    private lateinit var mockBitmap: Bitmap

    @Mock
    private lateinit var mockBookRepository: BookRepository

    // No usamos Mock aquí, sino una implementación personalizada para pruebas
    private lateinit var mockTextRecognitionHelper: TestTextRecognitionHelper

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: TestBookScannerViewModel

    // Implementación de TextRecognitionHelper para pruebas que no depende de ML Kit
    private class TestTextRecognitionHelper : TextRecognitionHelper() {
        // Variables para controlar el comportamiento en pruebas
        var textToReturn: String = ""
        var shouldThrowException: Boolean = false
        var exceptionToThrow: Exception = RuntimeException("Error de reconocimiento")
        var extractedTitle: String = ""

        override suspend fun recognizeText(bitmap: Bitmap): String {
            if (shouldThrowException) {
                throw exceptionToThrow
            }
            return textToReturn
        }

        override fun extractBookTitle(recognizedText: String): String {
            return extractedTitle
        }
    }

    // Clase de prueba que extiende el ViewModel para inyectar dependencias mockeadas
    private class TestBookScannerViewModel(
        private val textRecognitionHelper: TextRecognitionHelper,
        private val bookRepository: BookRepository
    ) : BookScannerViewModel() {
        // Sobrescribimos para usar los mocks
        override val textRecognitionHelperInstance: TextRecognitionHelper
            get() = textRecognitionHelper
        override val bookRepositoryInstance: BookRepository
            get() = bookRepository
    }

    @Before
    fun setup() {
        mockTextRecognitionHelper = TestTextRecognitionHelper()
        viewModel = TestBookScannerViewModel(mockTextRecognitionHelper, mockBookRepository)
    }

    @Test
    fun `processBookCover con imagen válida actualiza estado correctamente`() = runTest {
        // Preparar
        val recognizedText = "El Señor de los Anillos\nJ.R.R. Tolkien"
        val title = "El Señor de los Anillos"
        val books = listOf(BookModel(title = title, author = "J.R.R. Tolkien"))

        mockTextRecognitionHelper.textToReturn = recognizedText
        mockTextRecognitionHelper.extractedTitle = title
        `when`(mockBookRepository.searchBookByTitle(title)).thenReturn(Result.success(books))

        // Ejecutar
        viewModel.processBookCover(mockBitmap)
        advanceUntilIdle() // Avanzar el tiempo virtual hasta que todas las coroutines estén inactivas

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(recognizedText, state.recognizedText)
        assertEquals(title, state.bookTitle)
        assertEquals(1, state.books.size)
        assertEquals(title, state.books.first().title)
        assertEquals(books.first(), state.selectedBook)
        assertNull(state.error)
    }

    @Test
    fun `processBookCover maneja errores de reconocimiento de texto`() = runTest {
        // Preparar
        mockTextRecognitionHelper.shouldThrowException = true
        mockTextRecognitionHelper.exceptionToThrow = RuntimeException("Error de reconocimiento")

        // Ejecutar
        viewModel.processBookCover(mockBitmap)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("Error") ?: false)
        assertEquals("", state.recognizedText)
        assertEquals("", state.bookTitle)
        assertTrue(state.books.isEmpty())
    }

    @Test
    fun `processBookCover con texto vacío maneja el caso correctamente`() = runTest {
        // Preparar - Caso límite: texto vacío
        mockTextRecognitionHelper.textToReturn = ""
        mockTextRecognitionHelper.extractedTitle = ""

        // Ejecutar
        viewModel.processBookCover(mockBitmap)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("No se pudo detectar el título") ?: false)
        assertEquals("", state.bookTitle)
    }

    @Test
    fun `processBookCover con error en búsqueda crea libro básico`() = runTest {
        // Preparar
        val recognizedText = "Título de Prueba"
        val title = "Título de Prueba"

        mockTextRecognitionHelper.textToReturn = recognizedText
        mockTextRecognitionHelper.extractedTitle = title
        `when`(mockBookRepository.searchBookByTitle(title)).thenReturn(Result.failure(Exception("Error API")))

        // Ejecutar
        viewModel.processBookCover(mockBitmap)
        advanceUntilIdle()

        // Verificar - Debe crear un libro básico con el título
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(title, state.bookTitle)
        assertEquals(1, state.books.size)
        assertEquals(title, state.books.first().title)
        assertTrue(state.error?.contains("Error") ?: false)
    }

    @Test
    fun `processBookCover con timeout de API maneja error correctamente`() = runTest {
        // Preparar - Caso límite: timeout de red
        val recognizedText = "Libro Timeout"
        val title = "Libro Timeout"

        mockTextRecognitionHelper.textToReturn = recognizedText
        mockTextRecognitionHelper.extractedTitle = title
        `when`(mockBookRepository.searchBookByTitle(title)).thenReturn(Result.failure(TimeoutException("API Timeout")))

        // Ejecutar
        viewModel.processBookCover(mockBitmap)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(title, state.bookTitle)
        assertTrue(state.error?.contains("Error") ?: false)
        assertEquals(1, state.books.size) // Debería crear un libro básico
    }

    @Test
    fun `updateBookTitle actualiza título y busca información nuevamente`() = runTest {
        // Preparar
        val newTitle = "Título Actualizado"
        val books = listOf(BookModel(title = newTitle, author = "Autor Actualizado"))

        `when`(mockBookRepository.searchBookByTitle(newTitle)).thenReturn(Result.success(books))

        // Ejecutar
        viewModel.updateBookTitle(newTitle)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertEquals(newTitle, state.bookTitle)
        assertEquals(1, state.books.size)
        assertEquals(newTitle, state.books.first().title)
    }

    @Test
    fun `updateBookTitle con título vacío no inicia búsqueda`() = runTest {
        // Preparar - Caso límite: título vacío
        val emptyTitle = ""

        // Ejecutar
        viewModel.updateBookTitle(emptyTitle)
        advanceUntilIdle()

        // Verificar
        verify(mockBookRepository, never()).searchBookByTitle(anyString())
    }

    @Test
    fun `sendBookToBackend con libro seleccionado retorna éxito`() = runTest {
        // Preparar
        val book = BookModel(title = "Libro Test")

        // Primero configuramos un libro seleccionado
        val books = listOf(book)
        `when`(mockBookRepository.searchBookByTitle("Libro Test")).thenReturn(Result.success(books))
        viewModel.updateBookTitle("Libro Test")
        advanceUntilIdle()

        // Luego preparamos el envío al backend
        `when`(mockBookRepository.sendBookToBackend(book)).thenReturn(Result.success(true))

        // Ejecutar
        viewModel.sendBookToBackend()
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.successMessage?.contains("correctamente") ?: false)
        assertNull(state.error)
    }

    @Test
    fun `sendBookToBackend sin libro seleccionado maneja error`() = runTest {
        // Caso límite: No hay libro seleccionado
        // Ejecutar
        viewModel.sendBookToBackend()
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("No hay libro seleccionado") ?: false)
    }

    @Test
    fun `sendBookToBackend maneja error en envío`() = runTest {
        // Preparar
        val book = BookModel(title = "Libro Error")

        // Configuramos un libro seleccionado
        val books = listOf(book)
        `when`(mockBookRepository.searchBookByTitle("Libro Error")).thenReturn(Result.success(books))
        viewModel.updateBookTitle("Libro Error")
        advanceUntilIdle()

        // Simulamos error en el envío
        `when`(mockBookRepository.sendBookToBackend(book)).thenReturn(Result.failure(Exception("Error en el backend")))

        // Ejecutar
        viewModel.sendBookToBackend()
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("Error") ?: false)
        assertNull(state.successMessage)
    }

    @Test
    fun `resetScanProcess restaura estado inicial`() = runTest {
        // Preparar - Primero establecemos algún estado
        val book = BookModel(title = "Libro")
        viewModel.selectBook(book)

        // Ejecutar
        viewModel.resetScanProcess()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.recognizedText)
        assertEquals("", state.bookTitle)
        assertTrue(state.books.isEmpty())
        assertNull(state.selectedBook)
        assertNull(state.error)
        assertNull(state.successMessage)
    }
}
