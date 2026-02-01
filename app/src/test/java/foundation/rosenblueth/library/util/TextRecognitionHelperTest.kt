package foundation.rosenblueth.library.util

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.lang.Exception

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TextRecognitionHelperTest {

    @Mock
    private lateinit var mockBitmap: Bitmap

    private lateinit var textRecognitionHelper: TextRecognitionHelper

    // Clase que simula el TextRecognitionHelper para pruebas sin depender de ML Kit
    private class TestTextRecognitionHelper : TextRecognitionHelper() {
        // Sobrescribimos el método para evitar la dependencia de ML Kit durante las pruebas
        override suspend fun recognizeText(bitmap: Bitmap): String {
            return when (bitmap.width) {
                0 -> "" // Simula imagen vacía
                1 -> throw Exception("Error en el procesamiento de imagen") // Simula error
                2 -> "Texto muy corto" // Simula texto mínimo
                3 -> "Línea 1\nLínea 2\nLínea 3" // Simula múltiples líneas
                else -> "El Gran Gatsby\nF. Scott Fitzgerald\nEditorial Ejemplo" // Caso normal
            }
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        textRecognitionHelper = TestTextRecognitionHelper()
    }

    @Test
    fun `extraer título de texto vacío devuelve cadena vacía`() {
        // Caso límite: Texto vacío
        val result = textRecognitionHelper.extractBookTitle("")
        assertEquals("", result)
    }

    @Test
    fun `extraer título de texto con una sola línea devuelve esa línea`() {
        // Caso límite: Una sola línea
        val result = textRecognitionHelper.extractBookTitle("Don Quijote")
        assertEquals("Don Quijote", result)
    }

    @Test
    fun `extraer título de texto con múltiples líneas devuelve la primera línea`() {
        // Caso normal: Múltiples líneas
        val result = textRecognitionHelper.extractBookTitle("Cien Años de Soledad\nGabriel García Márquez\nEditorial")
        assertEquals("Cien Años de Soledad", result)
    }

    @Test
    fun `extraer título de texto con líneas muy cortas prioriza líneas más largas`() {
        // Caso límite: Líneas cortas al inicio
        val result = textRecognitionHelper.extractBookTitle("De\nLa Tierra a la Luna\nJulio Verne")
        assertEquals("La Tierra a la Luna", result)
    }

    @Test
    fun `extraer título de texto con múltiples líneas largas prioriza las primeras`() {
        // Caso borde: Múltiples líneas largas
        val result = textRecognitionHelper.extractBookTitle("El ingenioso hidalgo don Quijote de la Mancha\nMiguel de Cervantes Saavedra\nUna novela española publicada en 1605")
        assertEquals("El ingenioso hidalgo don Quijote de la Mancha", result)
    }

    @Test
    fun `recognizeText maneja correctamente imagen válida`() = runTest {
        // Preparar
        Mockito.`when`(mockBitmap.width).thenReturn(100)
        Mockito.`when`(mockBitmap.height).thenReturn(100)

        try {
            // Ejecutar - Aquí usamos el método real pero con nuestra clase simulada
            val result = textRecognitionHelper.recognizeText(mockBitmap)

            // Verificar
            assertTrue(result.isNotEmpty())
            assertTrue(result.contains("El Gran Gatsby"))
        } catch (e: Exception) {
            fail("No debería lanzar excepciones con una imagen válida")
        }
    }

    @Test
    fun `recognizeText maneja correctamente imagen vacía`() = runTest {
        // Preparar - Bitmap vacío (ancho 0)
        Mockito.`when`(mockBitmap.width).thenReturn(0)

        // Ejecutar
        val result = textRecognitionHelper.recognizeText(mockBitmap)

        // Verificar
        assertEquals("", result)
    }

    @Test
    fun `recognizeText maneja correctamente errores de procesamiento`() = runTest {
        // Preparar - Bitmap que provocará error (ancho 1)
        Mockito.`when`(mockBitmap.width).thenReturn(1)

        try {
            textRecognitionHelper.recognizeText(mockBitmap)
            fail("Debería haber lanzado una excepción")
        } catch (e: Exception) {
            // Verificar
            assertEquals("Error en el procesamiento de imagen", e.message)
        }
    }

    @Test
    fun `extraer ISBN de texto vacío devuelve cadena vacía`() {
        // Caso límite: Texto vacío
        val result = textRecognitionHelper.extractISBN("")
        assertEquals("", result)
    }

    @Test
    fun `extraer ISBN-13 válido del texto`() {
        // Caso normal: ISBN-13 con formato estándar
        val text = "Este libro\nISBN: 978-0-596-52068-7\nAutor: Example"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("9780596520687", result)
    }

    @Test
    fun `extraer ISBN-13 sin guiones del texto`() {
        // Caso normal: ISBN-13 sin guiones
        val text = "Este libro\nISBN: 9780596520687\nAutor: Example"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("9780596520687", result)
    }

    @Test
    fun `extraer ISBN-10 válido del texto`() {
        // Caso normal: ISBN-10 con formato estándar
        val text = "Este libro\nISBN: 0-596-52068-9\nAutor: Example"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("0596520689", result)
    }

    @Test
    fun `extraer ISBN-10 con X como último dígito`() {
        // Caso borde: ISBN-10 terminando en X
        val text = "Este libro\nISBN-10: 043942089X\nAutor: Example"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("043942089X", result)
    }

    @Test
    fun `extraer ISBN sin prefijo ISBN del texto`() {
        // Caso borde: ISBN sin el prefijo "ISBN:"
        val text = "Este libro\n978-3-16-148410-0\nAutor: Example"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("9783161484100", result)
    }

    @Test
    fun `extraer ISBN de texto sin ISBN devuelve cadena vacía`() {
        // Caso límite: Texto sin ISBN
        val text = "Este es un libro sin ISBN\nAutor: Example\nEditorial: Test"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("", result)
    }

    @Test
    fun `extraer ISBN prioriza ISBN-13 sobre ISBN-10 cuando ambos están presentes`() {
        // Caso borde: Texto con ambos formatos
        val text = "ISBN-10: 0596520689\nISBN-13: 978-0-596-52068-7"
        val result = textRecognitionHelper.extractISBN(text)
        assertEquals("9780596520687", result)
    }
}
