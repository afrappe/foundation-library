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
                else -> "Paracetamol\n500mg\nLaboratorio XYZ" // Caso normal
            }
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        textRecognitionHelper = TestTextRecognitionHelper()
    }

    @Test
    fun `extraer nombre de texto vacío devuelve cadena vacía`() {
        // Caso límite: Texto vacío
        val result = textRecognitionHelper.extractMedicineName("")
        assertEquals("", result)
    }

    @Test
    fun `extraer nombre de texto con una sola línea devuelve esa línea`() {
        // Caso límite: Una sola línea
        val result = textRecognitionHelper.extractMedicineName("Ibuprofeno")
        assertEquals("Ibuprofeno", result)
    }

    @Test
    fun `extraer nombre de texto con múltiples líneas devuelve la primera línea`() {
        // Caso normal: Múltiples líneas
        val result = textRecognitionHelper.extractMedicineName("Amoxicilina\n500mg\nLaboratorio ABC")
        assertEquals("Amoxicilina", result)
    }

    @Test
    fun `extraer nombre de texto con líneas muy cortas prioriza líneas más largas`() {
        // Caso límite: Líneas cortas al inicio
        val result = textRecognitionHelper.extractMedicineName("10\nDiclofenaco Sodico\n50mg")
        assertEquals("Diclofenaco Sodico", result)
    }

    @Test
    fun `extraer nombre de texto con múltiples líneas largas prioriza las primeras`() {
        // Caso borde: Múltiples líneas largas
        val result = textRecognitionHelper.extractMedicineName("Acetilcisteína Solución Oral\nIndicado para afecciones respiratorias\nConcentración 200mg por 5ml")
        assertEquals("Acetilcisteína Solución Oral", result)
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
            assertTrue(result.contains("Paracetamol"))
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
}
