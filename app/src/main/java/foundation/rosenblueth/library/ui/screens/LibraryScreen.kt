package foundation.rosenblueth.library.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.util.Log
import foundation.rosenblueth.library.data.model.CaptureData
import foundation.rosenblueth.library.data.store.CaptureDataStore
import foundation.rosenblueth.library.util.CSVExporter
import foundation.rosenblueth.library.util.ExcelExporter
import kotlinx.coroutines.launch
import java.io.File
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
    var showExportMenu by remember { mutableStateOf(false) }
    
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
                    // Botón de menú de exportación y compartir
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            enabled = capturedBooks.isNotEmpty()
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exportar a CSV") },
                                onClick = {
                                    showExportMenu = false
                                    scope.launch {
                                        try {
                                            val exporter = CSVExporter(context)
                                            val file = exporter.exportToCSV(capturedBooks)
                                            exportMessage = "Biblioteca exportada exitosamente a CSV: ${file.name}"
                                            showExportMessage = true
                                        } catch (e: Exception) {
                                            exportMessage = "Error al exportar a CSV: ${e.message}"
                                            showExportMessage = true
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar a Excel") },
                                onClick = {
                                    showExportMenu = false
                                    scope.launch {
                                        try {
                                            val exporter = ExcelExporter(context)
                                            val file = exporter.exportToExcel(capturedBooks)
                                            exportMessage = "Biblioteca exportada exitosamente a Excel: ${file.name}"
                                            showExportMessage = true
                                        } catch (e: Exception) {
                                            exportMessage = "Error al exportar a Excel: ${e.message}"
                                            showExportMessage = true
                                        }
                                    }
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Compartir CSV")
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    scope.launch {
                                        try {
                                            val exporter = CSVExporter(context)
                                            val file = exporter.exportToCSV(capturedBooks)
                                            shareFile(context, file, "text/csv")
                                        } catch (e: Exception) {
                                            exportMessage = "Error al compartir CSV: ${e.message}"
                                            showExportMessage = true
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Compartir Excel")
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    scope.launch {
                                        try {
                                            val exporter = ExcelExporter(context)
                                            val file = exporter.exportToExcel(capturedBooks)
                                            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                        } catch (e: Exception) {
                                            exportMessage = "Error al compartir Excel: ${e.message}"
                                            showExportMessage = true
                                        }
                                    }
                                }
                            )
                        }
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
                            onClick = { onBookSelected?.invoke(book) },
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
                onDismissRequest = { 
                    showDeleteDialog = false
                    bookToDelete = null
                },
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
                    TextButton(onClick = { 
                        showDeleteDialog = false
                        bookToDelete = null
                    }) {
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
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    // Log para verificar las clasificaciones
    Log.d("LibraryScreen", "Mostrando libro: ${book.title}, LC='${book.lcClassification}', Dewey='${book.deweyClassification}', DCU='${book.dcuClassification}'")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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

/**
 * Comparte un archivo usando Android's share sheet
 */
private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Mi Biblioteca")
        putExtra(Intent.EXTRA_TEXT, "Catálogo de mi biblioteca personal")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Compartir biblioteca"))
}
