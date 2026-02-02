package foundation.rosenblueth.library.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.data.model.CaptureData
import foundation.rosenblueth.library.data.repository.BookRepository
import foundation.rosenblueth.library.data.store.CaptureDataStore
import foundation.rosenblueth.library.network.EnhancedBookSearchService
import foundation.rosenblueth.library.network.ISBNBookSearchService
import foundation.rosenblueth.library.network.OpenLibraryService
import foundation.rosenblueth.library.util.TextRecognitionHelper
import foundation.rosenblueth.library.ui.model.ScanMode
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

    // Estado para el modo de escaneo actual
    private val _currentScanMode = MutableStateFlow(ScanMode.COVER)
    val currentScanMode: StateFlow<ScanMode> = _currentScanMode.asStateFlow()

    // Estado para las secciones escaneadas (título, autor, editorial)
    private val _scannedTitle = MutableStateFlow("")
    val scannedTitle: StateFlow<String> = _scannedTitle.asStateFlow()

    private val _scannedAuthor = MutableStateFlow("")
    val scannedAuthor: StateFlow<String> = _scannedAuthor.asStateFlow()

    private val _scannedPublisher = MutableStateFlow("")
    val scannedPublisher: StateFlow<String> = _scannedPublisher.asStateFlow()

    // Tipo de sección que se está escaneando actualmente
    private val _currentSectionType = MutableStateFlow<String?>(null)
    val currentSectionType: StateFlow<String?> = _currentSectionType.asStateFlow()

    /**
     * Establece el tipo de sección que se va a escanear
     */
    fun setCurrentSectionType(sectionType: String?) {
        _currentSectionType.value = sectionType
    }

    /**
     * Actualiza el valor de una sección específica
     */
    fun updateSectionValue(sectionType: String, value: String) {
        when (sectionType) {
            "TITLE" -> _scannedTitle.value = value
            "AUTHOR" -> _scannedAuthor.value = value
            "PUBLISHER" -> _scannedPublisher.value = value
        }
    }

    /**
     * Limpia los valores de las secciones escaneadas
     */
    fun clearSectionValues() {
        _scannedTitle.value = ""
        _scannedAuthor.value = ""
        _scannedPublisher.value = ""
        _currentSectionType.value = null
    }

    /**
     * Procesa la imagen capturada para una sección específica.
     * Extrae el texto y lo asigna automáticamente a la sección correspondiente.
     */
    fun processSectionImage(bitmap: Bitmap, sectionType: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Reconocer texto de la imagen
                val recognizedText = textRecognitionHelper.recognizeText(bitmap)

                // Extraer el texto más relevante
                val extractedText = textRecognitionHelper.extractBookTitle(recognizedText)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recognizedText = recognizedText,
                        bookTitle = extractedText
                    )
                }

                // Actualizar la sección correspondiente con el texto extraído
                if (extractedText.isNotEmpty()) {
                    updateSectionValue(sectionType, extractedText)
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
     * Establece el modo de escaneo actual
     */
    fun setScanMode(mode: ScanMode) {
        _currentScanMode.value = mode
    }

    /**
     * Procesa la búsqueda de libro por secciones (título, autor, editorial)
     * y busca los números de clasificación LC, Dewey y DCU
     */
    fun processBookBySections(title: String, author: String, publisher: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Buscar libro con la información proporcionada
                val searchQuery = buildSearchQuery(title, author, publisher)

                if (searchQuery.isNotEmpty()) {
                    searchBookWithClassifications(title, author, publisher)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Ingresa al menos el título o el autor del libro"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al procesar la búsqueda: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Construye la consulta de búsqueda combinando título, autor y editorial
     */
    private fun buildSearchQuery(title: String, author: String, publisher: String): String {
        return listOf(title, author, publisher)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    /**
     * Busca información del libro incluyendo clasificaciones LC, Dewey y DCU
     * usando el servicio de búsqueda mejorado
     */
    private fun searchBookWithClassifications(title: String, author: String, publisher: String) {
        viewModelScope.launch {
            try {
                Log.d("BookScannerVM", "=== BÚSQUEDA POR SECCIONES CON CLASIFICACIONES MEJORADAS ===")
                Log.d("BookScannerVM", "Título: '$title', Autor: '$author', Editorial: '$publisher'")

                // PRIMERA OPCIÓN: Usar EnhancedBookSearchService para búsqueda por título/autor
                val completeBookInfo = if (title.isNotBlank()) {
                    EnhancedBookSearchService.searchByTitleAuthor(title, author.ifBlank { null })
                } else {
                    null
                }

                if (completeBookInfo != null) {
                    Log.d("BookScannerVM", "✓ Encontrado con clasificaciones mejoradas")
                    Log.d("BookScannerVM", "Fuentes: ${completeBookInfo.sources.joinToString(", ")}")
                    Log.d("BookScannerVM", "Clasificaciones: ${completeBookInfo.getClassificationSummary()}")

                    // Combinar con información de editorial si no está presente
                    val finalBook = if (completeBookInfo.finalBook.publisher.isBlank() && publisher.isNotBlank()) {
                        completeBookInfo.finalBook.copy(publisher = publisher)
                    } else {
                        completeBookInfo.finalBook
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            books = listOf(finalBook),
                            selectedBook = finalBook,
                            bookTitle = title,
                            successMessage = "Libro encontrado con clasificaciones de ${completeBookInfo.sources.size} fuente(s)"
                        )
                    }
                    return@launch
                }

                // FALLBACK: Usar método tradicional de BookRepository
                Log.d("BookScannerVM", "Fallback a búsqueda tradicional por título")

                val result = if (title.isNotBlank()) {
                    bookRepository.searchBookByTitle(title)
                } else if (author.isNotBlank()) {
                    bookRepository.searchBookByTitle(author)
                } else {
                    bookRepository.searchBookByTitle(publisher)
                }

                result.fold(
                    onSuccess = { books ->
                        if (books.isNotEmpty()) {
                            // Filtrar por autor si está disponible
                            val filteredBooks = if (author.isNotBlank()) {
                                books.filter { book ->
                                    book.author.contains(author, ignoreCase = true)
                                }.ifEmpty { books }
                            } else {
                                books
                            }

                            // Intentar obtener clasificaciones adicionales para el primer libro
                            val bookWithClassifications = if (filteredBooks.first().isbn.isNotBlank()) {
                                // Si el libro tiene ISBN, buscar clasificaciones por ISBN
                                val classifications = OpenLibraryService.fetchClassifications(filteredBooks.first().isbn)
                                filteredBooks.first().copy(
                                    lcClassification = classifications?.lcClassification ?: filteredBooks.first().lcClassification,
                                    deweyClassification = classifications?.dewey ?: filteredBooks.first().deweyClassification,
                                    dcuClassification = classifications?.cdu ?: filteredBooks.first().dcuClassification
                                )
                            } else {
                                // Sin ISBN, intentar búsqueda por título/autor si el título no está vacío
                                val enhancedInfo = if (filteredBooks.first().title.isNotBlank()) {
                                    EnhancedBookSearchService.searchByTitleAuthor(
                                        title = filteredBooks.first().title,
                                        author = filteredBooks.first().author.ifBlank { null }
                                    )
                                } else {
                                    null
                                }
                                enhancedInfo?.finalBook ?: filteredBooks.first()
                            }

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = listOf(bookWithClassifications),
                                    selectedBook = bookWithClassifications,
                                    bookTitle = title
                                )
                            }
                        } else {
                            // Crear libro básico pero intentar obtener clasificaciones
                            createEnhancedBasicBook(title, author, publisher)
                        }
                    },
                    onFailure = { error ->
                        // En caso de error, crear libro básico con clasificaciones si es posible
                        createEnhancedBasicBook(title, author, publisher, error.message)
                    }
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar clasificaciones: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Crea un libro básico pero intenta obtener clasificaciones usando el servicio mejorado
     */
    private suspend fun createEnhancedBasicBook(
        title: String,
        author: String,
        publisher: String,
        errorMessage: String? = null
    ) {
        Log.d("BookScannerVM", "Creando libro básico con clasificaciones mejoradas")

        // Intentar obtener clasificaciones aunque no tengamos información completa
        val enhancedClassifications = if (title.isNotBlank()) {
            EnhancedBookSearchService.searchByTitleAuthor(
                title = title,
                author = author.ifBlank { null }
            )
        } else {
            null
        }

        val basicBook = BookModel(
            title = title.ifBlank { "Libro sin título" },
            author = author,
            publisher = publisher,
            lcClassification = enhancedClassifications?.finalBook?.lcClassification ?: "",
            deweyClassification = enhancedClassifications?.finalBook?.deweyClassification ?: "",
            dcuClassification = enhancedClassifications?.finalBook?.dcuClassification ?: ""
        )

        val successMsg = if (enhancedClassifications != null) {
            "Clasificaciones obtenidas de: ${enhancedClassifications.sources.joinToString(", ")}"
        } else {
            null
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                error = errorMessage?.let { msg -> "Búsqueda limitada: $msg" },
                books = listOf(basicBook),
                selectedBook = basicBook,
                bookTitle = title,
                successMessage = successMsg
            )
        }
    }

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
     * Busca información del libro usando un código ISBN escaneado directamente.
     * Esta función se usa con el escáner de códigos de barras.
     */
    fun searchBookByScannedISBN(isbn: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Buscar información del libro por ISBN
                searchBookByISBN(isbn)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar libro por ISBN: ${e.message}"
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
     * Busca información del libro usando el ISBN reconocido.
     * Usa búsqueda exhaustiva en múltiples fuentes para máxima obtención de clasificaciones.
     */
    private fun searchBookByISBN(isbn: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                Log.d("BookScannerVM", "=== INICIANDO BÚSQUEDA EXHAUSTIVA PARA ISBN: $isbn ===")

                // NUEVA ESTRATEGIA: Usar EnhancedBookSearchService para búsqueda completa
                // Esto incluye información del libro + clasificaciones de múltiples fuentes
                val completeBookInfo = EnhancedBookSearchService.searchCompleteBookInfoParallel(isbn)

                if (completeBookInfo != null) {
                    Log.d("BookScannerVM", "✓ Búsqueda exhaustiva exitosa")
                    Log.d("BookScannerVM", "Libro encontrado: ${completeBookInfo.finalBook.title}")
                    Log.d("BookScannerVM", "Estrategia: ${completeBookInfo.searchStrategy}")
                    Log.d("BookScannerVM", "Fuentes consultadas: ${completeBookInfo.sources.joinToString(", ")}")
                    Log.d("BookScannerVM", "Clasificaciones finales:")
                    Log.d("BookScannerVM", "  - LC: '${completeBookInfo.finalBook.lcClassification}'")
                    Log.d("BookScannerVM", "  - Dewey: '${completeBookInfo.finalBook.deweyClassification}'")
                    Log.d("BookScannerVM", "  - UDC: '${completeBookInfo.finalBook.dcuClassification}'")

                    // Mostrar resumen de clasificaciones adicionales
                    completeBookInfo.enhancedClassifications?.let { enhanced ->
                        if (enhanced.lcClassification.size > 1) {
                            Log.d("BookScannerVM", "  - LC adicionales: ${enhanced.lcClassification.drop(1)}")
                        }
                        if (enhanced.deweyClassification.size > 1) {
                            Log.d("BookScannerVM", "  - Dewey adicionales: ${enhanced.deweyClassification.drop(1)}")
                        }
                        if (enhanced.subjectHeadings.isNotEmpty()) {
                            Log.d("BookScannerVM", "  - Materias: ${enhanced.subjectHeadings.take(3)}")
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            books = listOf(completeBookInfo.finalBook),
                            selectedBook = completeBookInfo.finalBook,
                            bookTitle = completeBookInfo.finalBook.title,
                            successMessage = "Libro encontrado con clasificaciones de ${completeBookInfo.sources.size} fuente(s)"
                        )
                    }
                    return@launch
                }

                // FALLBACK: Usar método original si el nuevo falla
                Log.d("BookScannerVM", "Búsqueda exhaustiva falló, usando método de fallback...")

                val bookFromISBNService = ISBNBookSearchService.searchBookByISBN(isbn)

                if (bookFromISBNService != null) {
                    Log.d("BookScannerVM", "✓ Fallback exitoso con información básica")

                    // Intentar enriquecer con clasificaciones adicionales si faltan
                    val enrichedBook = if (bookFromISBNService.lcClassification.isBlank() ||
                                           bookFromISBNService.deweyClassification.isBlank()) {
                        Log.d("BookScannerVM", "Buscando clasificaciones adicionales...")
                        val classifications = OpenLibraryService.fetchClassifications(isbn)

                        bookFromISBNService.copy(
                            lcClassification = bookFromISBNService.lcClassification.ifBlank {
                                classifications?.lcClassification ?: ""
                            },
                            deweyClassification = bookFromISBNService.deweyClassification.ifBlank {
                                classifications?.dewey ?: ""
                            },
                            dcuClassification = bookFromISBNService.dcuClassification.ifBlank {
                                classifications?.cdu ?: ""
                            }
                        )
                    } else {
                        bookFromISBNService
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            books = listOf(enrichedBook),
                            selectedBook = enrichedBook,
                            bookTitle = enrichedBook.title
                        )
                    }
                    return@launch
                }

                // FALLBACK FINAL: Usar BookRepository + clasificaciones
                val result = bookRepository.searchBookByISBN(isbn)

                result.fold(
                    onSuccess = { books ->
                        if (books.isNotEmpty()) {
                            // Buscar clasificaciones adicionales
                            val classifications = OpenLibraryService.fetchClassifications(isbn)

                            val updatedBook = if (classifications != null) {
                                books.first().copy(
                                    lcClassification = classifications.lcClassification ?: books.first().lcClassification,
                                    deweyClassification = classifications.dewey ?: books.first().deweyClassification,
                                    dcuClassification = classifications.cdu ?: books.first().dcuClassification
                                )
                            } else {
                                books.first()
                            }

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = books,
                                    selectedBook = updatedBook,
                                    bookTitle = updatedBook.title
                                )
                            }
                        } else {
                            createBasicBookWithISBNAndClassifications(isbn)
                        }
                    },
                    onFailure = { error ->
                        createBasicBookWithISBNAndClassifications(isbn, error.message)
                    }
                )

            } catch (e: Exception) {
                Log.e("BookScannerVM", "Error en búsqueda exhaustiva", e)
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
     * Crea un libro básico con ISBN y busca clasificaciones usando servicios mejorados.
     */
    private suspend fun createBasicBookWithISBNAndClassifications(isbn: String, errorMessage: String? = null) {
        Log.d("BookScannerVM", "Creando libro básico con clasificaciones mejoradas para ISBN: $isbn")

        // Intentar obtener solo clasificaciones aunque no tengamos información del libro
        val enhancedInfo = EnhancedBookSearchService.searchCompleteBookInfo(isbn)

        val basicBook = if (enhancedInfo != null) {
            Log.d("BookScannerVM", "Clasificaciones obtenidas de fuentes mejoradas")
            enhancedInfo.finalBook.copy(
                title = if (enhancedInfo.finalBook.title == "Libro no identificado") "Libro sin título" else enhancedInfo.finalBook.title
            )
        } else {
            // Fallback a OpenLibrary únicamente
            val classifications = OpenLibraryService.fetchClassifications(isbn)
            BookModel(
                title = "Libro sin título",
                isbn = isbn,
                lcClassification = classifications?.lcClassification ?: "",
                deweyClassification = classifications?.dewey ?: "",
                dcuClassification = classifications?.cdu ?: ""
            )
        }

        val successMsg = enhancedInfo?.let {
            "Clasificaciones de: ${it.sources.joinToString(", ")}"
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                error = errorMessage?.let { msg -> "No se encontró información completa: $msg" },
                books = listOf(basicBook),
                selectedBook = basicBook,
                bookTitle = basicBook.title,
                successMessage = successMsg
            )
        }
    }

    /**
     * Crea un modelo de libro básico con solo el ISBN
     */
    private fun createBasicBookWithISBN(isbn: String): BookModel {
        return BookModel(title = "Libro sin título", isbn = isbn)
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
                            Log.d("BookScannerVM", "Guardando libro: ${captureData.title}")
                            Log.d("BookScannerVM", "Clasificaciones al guardar - LC: '${captureData.lcClassification}', Dewey: '${captureData.deweyClassification}', DCU: '${captureData.dcuClassification}'")
                            captureDataStore?.saveCapturedBook(captureData)
                            
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "✓ Libro guardado en Mi Biblioteca correctamente"
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

    /**
     * Función de debug para probar el reconocimiento de ISBN directamente
     */
    fun debugSearchISBN(isbn: String) {
        Log.d("BookScannerVM", "=== DEBUG: Iniciando búsqueda de ISBN: $isbn ===")
        searchBookByISBN(isbn)
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
