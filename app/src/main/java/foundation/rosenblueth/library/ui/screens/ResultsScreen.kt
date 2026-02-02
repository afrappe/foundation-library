package foundation.rosenblueth.library.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import foundation.rosenblueth.library.data.model.BookModel
import foundation.rosenblueth.library.ui.components.ErrorMessage
import foundation.rosenblueth.library.ui.components.LoadingIndicator
import foundation.rosenblueth.library.ui.components.SuccessMessage
import foundation.rosenblueth.library.ui.viewmodel.BookScannerViewModel

/**
 * Pantalla para mostrar y gestionar los resultados del escaneo de libros.
 *
 * @param onNewScan Callback para volver a escanear una nueva portada
 * @param onNavigateToLibrary Callback para navegar a Mi Biblioteca
 * @param viewModel ViewModel que gestiona los datos de la aplicación
 */
@Composable
fun ResultsScreen(
    onNewScan: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: BookScannerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra superior con título y botón para nuevo escaneo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Información del libro",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón para ir a Mi Biblioteca
                Button(onClick = onNavigateToLibrary) {
                    Icon(
                        Icons.Default.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mi Biblioteca")
                }
                
                Button(onClick = {
                    viewModel.resetScanProcess()
                    onNewScan()
                }) {
                    Text("Nuevo escaneo")
                }
            }
        }

        Divider()

        // Contenido principal
        if (uiState.isLoading) {
            LoadingIndicator(message = "Procesando información del libro...")
        } else if (uiState.error != null && uiState.books.isEmpty()) {
            // Solo mostrar error si no hay libros disponibles
            ErrorMessage(
                message = uiState.error ?: "Error desconocido",
                onRetry = onNewScan
            )
        } else {
            // Mostrar mensaje de éxito si existe
            if (uiState.successMessage != null) {
                SuccessMessage(message = uiState.successMessage ?: "")
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Siempre mostrar información del libro si existe
            BookInformationContent(
                bookTitle = uiState.bookTitle,
                selectedBook = uiState.selectedBook,
                onTitleUpdate = viewModel::updateBookTitle,
                onSendToBackend = viewModel::sendBookToBackend,
                errorMessage = uiState.error,
                successMessage = uiState.successMessage
            )
        }
    }
}

/**
 * Componente que muestra la información del libro y permite editarla
 */
@Composable
private fun BookInformationContent(
    bookTitle: String,
    selectedBook: BookModel?,
    onTitleUpdate: (String) -> Unit,
    onSendToBackend: () -> Unit,
    errorMessage: String?,
    successMessage: String? = null
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(bookTitle) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Campo de título (editable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Título detectado:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditingTitle) {
                    // Campo de edición del título
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título del libro") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onTitleUpdate(editedTitle)
                                isEditingTitle = false
                                focusManager.clearFocus()
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                onTitleUpdate(editedTitle)
                                isEditingTitle = false
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Send, "Guardar")
                            }
                        }
                    )
                } else {
                    // Mostrar título con opción para editar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingTitle = true }) {
                            Icon(Icons.Default.Edit, "Editar título")
                        }
                    }
                }
            }
        }

        // Mostrar detalles del libro si está disponible
        selectedBook?.let { book ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Información del libro:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BookDetailItem("Autor", book.author.takeIf { it.isNotEmpty() } ?: "No disponible")
                    BookDetailItem("Editorial", book.publisher.takeIf { it.isNotEmpty() } ?: "No disponible")
                    BookDetailItem("Año", book.publishedYear?.toString() ?: "No disponible")
                    BookDetailItem("ISBN", book.isbn.takeIf { it.isNotEmpty() } ?: "No disponible")
                    BookDetailItem("Páginas", book.pages?.toString() ?: "No disponible")
                    BookDetailItem("Idioma", book.language.takeIf { it.isNotEmpty() } ?: "No disponible")
                    
                    // Clasificaciones
                    if (book.lcClassification.isNotEmpty()) {
                        BookDetailItem("Clasificación LC", book.lcClassification)
                    }
                    if (book.deweyClassification.isNotEmpty()) {
                        BookDetailItem("Clasificación Dewey", book.deweyClassification)
                    }
                    if (book.dcuClassification.isNotEmpty()) {
                        BookDetailItem("Clasificación DCU", book.dcuClassification)
                    }

                    if (book.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Descripción:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Mostrar mensaje de error si existe
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para agregar a la biblioteca
        val isAlreadySaved = successMessage?.contains("guardado") == true
        val buttonText = if (isAlreadySaved) "✓ Ya está en Mi Biblioteca" else "Agregar a Mi Biblioteca"

        Button(
            onClick = onSendToBackend,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAlreadySaved
        ) {
            Icon(
                if (isAlreadySaved) Icons.Default.Check else Icons.Default.LibraryAdd,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
    }
}

/**
 * Componente que muestra un campo de detalle del libro
 */
@Composable
private fun BookDetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
