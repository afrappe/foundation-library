package foundation.rosenblueth.library.util

import android.content.Context
import android.os.Environment
import foundation.rosenblueth.library.data.model.CaptureData
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para exportar la biblioteca a un archivo CSV
 */
class CSVExporter(private val context: Context) {
    
    /**
     * Exporta una lista de libros capturados a un archivo CSV
     * @return El archivo CSV creado
     */
    fun exportToCSV(books: List<CaptureData>): File {
        val fileName = "mi_biblioteca_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
        
        val file = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Para Android 10 y superior, usar el directorio de documentos de la app
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        } else {
            // Para versiones anteriores
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        }
        
        // Asegurar que el directorio existe
        file.parentFile?.mkdirs()
        
        // Escribir el archivo CSV
        FileWriter(file).use { writer ->
            // Escribir encabezado
            writer.append("Título,Autor,ISBN,Editorial,Año de Publicación,Páginas,Idioma,Clasificación LC,Clasificación Dewey,Clasificación DCU,Fecha de Captura\n")
            
            // Escribir datos de libros
            books.forEach { book ->
                writer.append(escapeCsvValue(book.title))
                writer.append(",")
                writer.append(escapeCsvValue(book.author))
                writer.append(",")
                writer.append(escapeCsvValue(book.isbn))
                writer.append(",")
                writer.append(escapeCsvValue(book.publisher))
                writer.append(",")
                writer.append(book.publishedYear?.toString() ?: "")
                writer.append(",")
                writer.append(book.pages?.toString() ?: "")
                writer.append(",")
                writer.append(escapeCsvValue(book.language))
                writer.append(",")
                writer.append(escapeCsvValue(book.lcClassification))
                writer.append(",")
                writer.append(escapeCsvValue(book.deweyClassification))
                writer.append(",")
                writer.append(escapeCsvValue(book.dcuClassification))
                writer.append(",")
                writer.append(escapeCsvValue(formatDate(book.captureTimestamp)))
                writer.append("\n")
            }
        }
        
        return file
    }
    
    /**
     * Escapa valores CSV para manejar comas, comillas y saltos de línea
     */
    private fun escapeCsvValue(value: String): String {
        if (value.isEmpty()) return ""
        
        // Si el valor contiene comas, comillas o saltos de línea, debe estar entre comillas
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        
        return if (needsQuotes) {
            // Duplicar las comillas dentro del valor y envolver todo en comillas
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
