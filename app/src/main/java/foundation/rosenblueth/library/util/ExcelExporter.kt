package foundation.rosenblueth.library.util

import android.content.Context
import android.os.Environment
import foundation.rosenblueth.library.data.model.CaptureData
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para exportar la biblioteca a un archivo Excel
 */
class ExcelExporter(private val context: Context) {
    
    /**
     * Exporta una lista de libros capturados a un archivo Excel
     * @return El archivo Excel creado
     */
    fun exportToExcel(books: List<CaptureData>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Mi Biblioteca")
        
        // Crear estilo para el encabezado
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)
            alignment = HorizontalAlignment.CENTER
        }
        
        // Crear fila de encabezado
        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "Título",
            "Autor",
            "ISBN",
            "Editorial",
            "Año de Publicación",
            "Páginas",
            "Idioma",
            "Clasificación LC",
            "Clasificación Dewey",
            "Clasificación DCU",
            "Fecha de Captura"
        )
        
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Agregar datos de libros
        books.forEachIndexed { rowIndex, book ->
            val row = sheet.createRow(rowIndex + 1)
            
            row.createCell(0).setCellValue(book.title)
            row.createCell(1).setCellValue(book.author)
            row.createCell(2).setCellValue(book.isbn)
            row.createCell(3).setCellValue(book.publisher)
            row.createCell(4).setCellValue(book.publishedYear?.toDouble() ?: 0.0)
            row.createCell(5).setCellValue(book.pages?.toDouble() ?: 0.0)
            row.createCell(6).setCellValue(book.language)
            row.createCell(7).setCellValue(book.lcClassification)
            row.createCell(8).setCellValue(book.deweyClassification)
            row.createCell(9).setCellValue(book.dcuClassification)
            row.createCell(10).setCellValue(formatDate(book.captureTimestamp))
        }
        
        // Ajustar ancho de columnas
        for (i in 0..10) {
            sheet.autoSizeColumn(i)
            // Añadir un poco más de espacio
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 500)
        }
        
        // Crear archivo en el directorio de descargas
        val fileName = "mi_biblioteca_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx"
        
        val file = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Para Android 10 y superior, usar el directorio de documentos de la app
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        } else {
            // Para versiones anteriores
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        }
        
        // Asegurar que el directorio existe
        file.parentFile?.mkdirs()
        
        // Escribir el archivo
        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        
        workbook.close()
        
        return file
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
