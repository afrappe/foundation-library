package foundation.rosenblueth.library.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import foundation.rosenblueth.library.data.model.CaptureData
import foundation.rosenblueth.library.data.store.CaptureDataStore
import foundation.rosenblueth.library.util.ExcelExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla para mostrar "Mi Biblioteca" con todos los libros capturados
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onBookSelected: ((CaptureData) -> Unit)? = null
) {
    val context = LocalContext.current
    val captureDataStore = remember { CaptureDataStore(context) }
    val capturedBooks by captureDataStore.getCaptureHistory().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<CaptureData?>(null) }
    var showExportMessage by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Biblioteca") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón de exportar a Excel
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val exporter = ExcelExporter(context)
                                    val file = exporter.exportToExcel(capturedBooks)
                                    exportMessage = "Biblioteca exportada exitosamente a: ${file.name}"
                                    showExportMessage = true
                                } catch (e: Exception) {
                                    exportMessage = "Error al exportar: ${e.message}"
                                    showExportMessage = true
                                }
                            }
                        },
                        enabled = capturedBooks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar a Excel")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (capturedBooks.isEmpty()) {
                // Mostrar mensaje cuando no hay libros
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No hay libros capturados",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Escanea la portada de un libro para agregarlo a tu biblioteca",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Mostrar lista de libros
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedBooks, key = { it.id }) { book ->
                        BookListItem(
                            book = book,
                            onBookClick = { onBookSelected?.invoke(book) },
                            onDeleteClick = {
                                bookToDelete = book
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // Diálogo de confirmación de eliminación
        if (showDeleteDialog && bookToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar libro") },
                text = { Text("¿Estás seguro de que deseas eliminar '${bookToDelete?.title}' de tu biblioteca?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                bookToDelete?.let { captureDataStore.deleteBook(it.id) }
                                showDeleteDialog = false
                                bookToDelete = null
                            }
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Snackbar para mensaje de exportación
        if (showExportMessage) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { showExportMessage = false }) {
                        Text("OK")
                    }
                }
            ) {
                Text(exportMessage)
            }
        }
    }
}

/**
 * Item de la lista de libros
 */
@Composable
private fun BookListItem(
    book: CaptureData,
    onBookClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (book.author.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (book.isbn.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ISBN: ${book.isbn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Mostrar clasificaciones si están disponibles
                val classifications = buildList {
                    if (book.lcClassification.isNotEmpty()) add("LC: ${book.lcClassification}")
                    if (book.deweyClassification.isNotEmpty()) add("Dewey: ${book.deweyClassification}")
                    if (book.dcuClassification.isNotEmpty()) add("DCU: ${book.dcuClassification}")
                }
                
                if (classifications.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = classifications.joinToString(" | "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capturado: ${formatDate(book.captureTimestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Formatea un timestamp a una fecha legible
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
