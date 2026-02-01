package foundation.rosenblueth.library.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foundation.rosenblueth.library.ui.viewmodel.BookScannerViewModel

/**
 * Tipo de sección que se está escaneando actualmente
 */
enum class SectionType {
    TITLE,
    AUTHOR,
    PUBLISHER
}

/**
 * Pantalla para escanear libro por secciones (título, autor, editorial).
 * Permite al usuario escanear o ingresar manualmente cada sección.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(
    onScanSection: (SectionType) -> Unit,
    onSearchBook: (title: String, author: String, publisher: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: BookScannerViewModel
) {
    // Usar estado del ViewModel para mantener los valores entre navegaciones
    val scannedTitle by viewModel.scannedTitle.collectAsState()
    val scannedAuthor by viewModel.scannedAuthor.collectAsState()
    val scannedPublisher by viewModel.scannedPublisher.collectAsState()

    // Estado local para edición
    var title by remember(scannedTitle) { mutableStateOf(scannedTitle) }
    var author by remember(scannedAuthor) { mutableStateOf(scannedAuthor) }
    var publisher by remember(scannedPublisher) { mutableStateOf(scannedPublisher) }

    // Sincronizar cambios del ViewModel al estado local
    LaunchedEffect(scannedTitle) {
        title = scannedTitle
    }
    LaunchedEffect(scannedAuthor) {
        author = scannedAuthor
    }
    LaunchedEffect(scannedPublisher) {
        publisher = scannedPublisher
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escaneo por Secciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Escanea o ingresa manualmente la información del libro",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de título
            SectionInputField(
                label = "Título",
                value = title,
                onValueChange = { title = it },
                onScanClick = { onScanSection(SectionType.TITLE) }
            )

            // Campo de autor
            SectionInputField(
                label = "Autor",
                value = author,
                onValueChange = { author = it },
                onScanClick = { onScanSection(SectionType.AUTHOR) }
            )

            // Campo de editorial
            SectionInputField(
                label = "Editorial",
                value = publisher,
                onValueChange = { publisher = it },
                onScanClick = { onScanSection(SectionType.PUBLISHER) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Información sobre clasificaciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Al buscar el libro se obtendrán los números de clasificación LC, Dewey y DCU",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de búsqueda
            Button(
                onClick = { onSearchBook(title, author, publisher) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = title.isNotBlank() || author.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buscar Libro",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Campo de entrada para una sección del libro con botón de escaneo
 */
@Composable
private fun SectionInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onScanClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onScanClick) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Escanear $label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
