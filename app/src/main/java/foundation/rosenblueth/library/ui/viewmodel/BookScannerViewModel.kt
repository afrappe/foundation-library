package foundation.rosenblueth.library.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.data.model.CaptureData
import foundation.rosenblueth.library.data.repository.BookRepository
import foundation.rosenblueth.library.data.store.CaptureDataStore
import foundation.rosenblueth.library.util.TextRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la funcionalidad de escaneo y búsqueda de libros
 */
open class BookScannerViewModel(private val appContext: Context? = null) : ViewModel() {
    protected open val bookRepositoryInstance: BookRepository = BookRepository()
    protected open val textRecognitionHelperInstance: TextRecognitionHelper = TextRecognitionHelper(appContext)
    private val captureDataStore: CaptureDataStore? = appContext?.let { CaptureDataStore(it) }

    // Referencias para mantener compatibilidad
    private val bookRepository get() = bookRepositoryInstance
    private val textRecognitionHelper get() = textRecognitionHelperInstance

    // Estado para la UI
    private val _uiState = MutableStateFlow(BookScannerUiState())
    val uiState: StateFlow<BookScannerUiState> = _uiState.asStateFlow()

    /**
     * Procesa la imagen capturada para extraer el título y buscar información del libro
     */
    fun processBookCover(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Paso 1: Reconocer texto de la imagen
                val recognizedText = textRecognitionHelper.recognizeText(bitmap)

                // Paso 2: Extraer el título del libro
                val bookTitle = textRecognitionHelper.extractBookTitle(recognizedText)

                _uiState.update {
                    it.copy(
                        recognizedText = recognizedText,
                        bookTitle = bookTitle
                    )
                }

                // Si se encontró un título, buscar información del libro
                if (bookTitle.isNotEmpty()) {
                    searchBookInfo(bookTitle)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudo detectar el título del libro"
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al procesar la imagen: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Procesa la imagen capturada para extraer el ISBN y buscar información del libro
     */
    fun processBookISBN(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Paso 1: Reconocer texto de la imagen
                val recognizedText = textRecognitionHelper.recognizeText(bitmap)

                // Paso 2: Extraer el ISBN del libro
                val isbn = textRecognitionHelper.extractISBN(recognizedText)

                _uiState.update {
                    it.copy(
                        recognizedText = recognizedText
                    )
                }

                // Si se encontró un ISBN, buscar información del libro
                if (isbn.isNotEmpty()) {
                    searchBookByISBN(isbn)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudo detectar el ISBN del libro"
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al procesar la imagen: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Busca información del libro usando el título reconocido
     */
    private fun searchBookInfo(title: String) {
        viewModelScope.launch {
            try {
                val result = bookRepository.searchBookByTitle(title)

                result.fold(
                    onSuccess = { books ->
                        if (books.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = books,
                                    selectedBook = books.first()
                                )
                            }
                        } else {
                            // Si no se encontraron libros, crear uno con solo el título
                            val basicBook = BookModel.createWithTitle(title)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = listOf(basicBook),
                                    selectedBook = basicBook
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        // Si hay un error en la búsqueda, crear libro básico con el título
                        val basicBook = BookModel.createWithTitle(title)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error al buscar información: ${error.message}",
                                books = listOf(basicBook),
                                selectedBook = basicBook
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar información del libro: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Busca información del libro usando el ISBN reconocido
     */
    private fun searchBookByISBN(isbn: String) {
        viewModelScope.launch {
            try {
                val result = bookRepository.searchBookByISBN(isbn)

                result.fold(
                    onSuccess = { books ->
                        if (books.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = books,
                                    selectedBook = books.first()
                                )
                            }
                        } else {
                            // Si no se encontraron libros, crear uno con solo el ISBN
                            val basicBook = BookModel(title = "", isbn = isbn)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = listOf(basicBook),
                                    selectedBook = basicBook
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        // Si hay un error en la búsqueda, crear libro básico con el ISBN
                        val basicBook = BookModel(title = "", isbn = isbn)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error al buscar información por ISBN: ${error.message}",
                                books = listOf(basicBook),
                                selectedBook = basicBook
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar información del libro por ISBN: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Selecciona un libro de la lista de resultados
     */
    fun selectBook(book: BookModel) {
        _uiState.update { it.copy(selectedBook = book) }
    }

    /**
     * Actualiza manualmente el título del libro
     */
    fun updateBookTitle(title: String) {
        _uiState.update { it.copy(bookTitle = title) }

        // Volver a buscar con el nuevo título
        if (title.isNotEmpty()) {
            searchBookInfo(title)
        }
    }

    /**
     * Envía los datos del libro seleccionado al backend y lo guarda en la biblioteca
     */
    fun sendBookToBackend() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, error = null) }

            uiState.value.selectedBook?.let { book ->
                try {
                    val result = bookRepository.sendBookToBackend(book)

                    result.fold(
                        onSuccess = {
                            // Guardar en la biblioteca local
                            val captureData = CaptureData.fromBookModel(book)
                            captureDataStore?.saveCapturedBook(captureData)
                            
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Libro enviado correctamente al backend y guardado en la biblioteca"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Error al enviar libro: ${error.message}"
                                )
                            }
                        }
                    )
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al enviar libro: ${e.message}"
                        )
                    }
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No hay libro seleccionado para enviar"
                    )
                }
            }
        }
    }

    /**
     * Reinicia el proceso de escaneo
     */
    fun resetScanProcess() {
        _uiState.update { BookScannerUiState() }
    }

    /**
     * Factory para crear el ViewModel con el contexto necesario
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookScannerViewModel::class.java)) {
                return BookScannerViewModel(context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Estado de la UI para la funcionalidad de escaneo de libros
 */
data class BookScannerUiState(
    val isLoading: Boolean = false,
    val recognizedText: String = "",
    val bookTitle: String = "",
    val books: List<BookModel> = emptyList(),
    val selectedBook: BookModel? = null,
    val error: String? = null,
    val successMessage: String? = null
)
