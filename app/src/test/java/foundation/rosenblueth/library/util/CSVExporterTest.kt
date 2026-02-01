package foundation.rosenblueth.library.util

import android.content.Context
import android.os.Environment
import foundation.rosenblueth.library.data.model.CaptureData
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class CSVExporterTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var csvExporter: CSVExporter

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock para getExternalFilesDir
        val mockDocumentsDir = File(System.getProperty("java.io.tmpdir"), "Documents")
        mockDocumentsDir.mkdirs()
        `when`(mockContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(mockDocumentsDir)
        
        csvExporter = CSVExporter(mockContext)
    }

    @Test
    fun `exportToCSV crea archivo con encabezados correctos`() {
        // Preparar
        val books = listOf(
            CaptureData(
                id = "1",
                title = "Test Book",
                author = "Test Author",
                isbn = "1234567890",
                publisher = "Test Publisher",
                publishedYear = 2024,
                pages = 300,
                language = "Español",
                lcClassification = "LC123",
                deweyClassification = "500.1",
                dcuClassification = "5.01"
            )
        )

        // Ejecutar
        val file = csvExporter.exportToCSV(books)

        // Verificar
        assertTrue("El archivo debe existir", file.exists())
        assertTrue("El archivo debe tener extensión .csv", file.name.endsWith(".csv"))
        
        val content = file.readText()
        assertTrue("Debe contener el encabezado", content.contains("Título,Autor,ISBN,Editorial,Año de Publicación,Páginas,Idioma,Clasificación LC,Clasificación Dewey,Clasificación DCU,Fecha de Captura"))
        assertTrue("Debe contener el título del libro", content.contains("Test Book"))
        assertTrue("Debe contener el autor", content.contains("Test Author"))
        assertTrue("Debe contener la clasificación LC", content.contains("LC123"))
        assertTrue("Debe contener la clasificación Dewey", content.contains("500.1"))
        assertTrue("Debe contener la clasificación DCU", content.contains("5.01"))
        
        // Limpiar
        file.delete()
    }

    @Test
    fun `exportToCSV escapa valores con comas correctamente`() {
        // Preparar
        val books = listOf(
            CaptureData(
                id = "1",
                title = "Test Book, Part 1",
                author = "Doe, John",
                isbn = "1234567890"
            )
        )

        // Ejecutar
        val file = csvExporter.exportToCSV(books)

        // Verificar
        val content = file.readText()
        assertTrue("Los valores con comas deben estar entre comillas", content.contains("\"Test Book, Part 1\""))
        assertTrue("Los valores con comas deben estar entre comillas", content.contains("\"Doe, John\""))
        
        // Limpiar
        file.delete()
    }

    @Test
    fun `exportToCSV escapa comillas correctamente`() {
        // Preparar
        val books = listOf(
            CaptureData(
                id = "1",
                title = "The \"Great\" Book",
                author = "Test Author"
            )
        )

        // Ejecutar
        val file = csvExporter.exportToCSV(books)

        // Verificar
        val content = file.readText()
        assertTrue("Las comillas deben estar duplicadas y el valor entre comillas", 
            content.contains("\"The \"\"Great\"\" Book\""))
        
        // Limpiar
        file.delete()
    }

    @Test
    fun `exportToCSV maneja lista vacía`() {
        // Preparar
        val books = emptyList<CaptureData>()

        // Ejecutar
        val file = csvExporter.exportToCSV(books)

        // Verificar
        assertTrue("El archivo debe existir", file.exists())
        val content = file.readText()
        val lines = content.lines().filter { it.isNotEmpty() }
        assertEquals("Solo debe tener el encabezado", 1, lines.size)
        
        // Limpiar
        file.delete()
    }

    @Test
    fun `exportToCSV maneja múltiples libros`() {
        // Preparar
        val books = listOf(
            CaptureData(id = "1", title = "Book 1", author = "Author 1"),
            CaptureData(id = "2", title = "Book 2", author = "Author 2"),
            CaptureData(id = "3", title = "Book 3", author = "Author 3")
        )

        // Ejecutar
        val file = csvExporter.exportToCSV(books)

        // Verificar
        val content = file.readText()
        val lines = content.lines().filter { it.isNotEmpty() }
        assertEquals("Debe tener encabezado + 3 libros", 4, lines.size)
        
        // Limpiar
        file.delete()
    }

    @Test
    fun `exportToCSV maneja valores nulos correctamente`() {
        // Preparar
        val books = listOf(
            CaptureData(
                id = "1",
                title = "Test Book",
                author = "",
                isbn = "",
                publisher = "",
                publishedYear = null,
                pages = null,
                language = ""
            )
        )

        // Ejecutar
        val file = csvExporter.exportToCSV(books)

        // Verificar
        assertTrue("El archivo debe crearse correctamente incluso con valores nulos", file.exists())
        val content = file.readText()
        assertTrue("Debe contener el título", content.contains("Test Book"))
        
        // Limpiar
        file.delete()
    }
}
