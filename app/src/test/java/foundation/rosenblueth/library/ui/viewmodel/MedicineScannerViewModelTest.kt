package foundation.rosenblueth.library.ui.viewmodel

import android.graphics.Bitmap
import foundation.rosenblueth.library.data.model.MedicineModel
import foundation.rosenblueth.library.data.repository.MedicineRepository
import foundation.rosenblueth.library.util.MainDispatcherRule
import foundation.rosenblueth.library.util.TextRecognitionHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MedicineScannerViewModelTest {

    @Mock
    private lateinit var mockBitmap: Bitmap

    @Mock
    private lateinit var mockMedicineRepository: MedicineRepository

    // No usamos Mock aquí, sino una implementación personalizada para pruebas
    private lateinit var mockTextRecognitionHelper: TestTextRecognitionHelper

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: TestMedicineScannerViewModel

    // Implementación de TextRecognitionHelper para pruebas que no depende de ML Kit
    private class TestTextRecognitionHelper : TextRecognitionHelper() {
        // Variables para controlar el comportamiento en pruebas
        var textToReturn: String = ""
        var shouldThrowException: Boolean = false
        var exceptionToThrow: Exception = RuntimeException("Error de reconocimiento")
        var extractedName: String = ""

        override suspend fun recognizeText(bitmap: Bitmap): String {
            if (shouldThrowException) {
                throw exceptionToThrow
            }
            return textToReturn
        }

        override fun extractMedicineName(recognizedText: String): String {
            return extractedName
        }
    }

    // Clase de prueba que extiende el ViewModel para inyectar dependencias mockeadas
    private class TestMedicineScannerViewModel(
        private val textRecognitionHelper: TextRecognitionHelper,
        private val medicineRepository: MedicineRepository
    ) : MedicineScannerViewModel() {
        // Sobrescribimos para usar los mocks
        override val textRecognitionHelperInstance: TextRecognitionHelper
            get() = textRecognitionHelper
        override val medicineRepositoryInstance: MedicineRepository
            get() = medicineRepository
    }

    @Before
    fun setup() {
        mockTextRecognitionHelper = TestTextRecognitionHelper()
        viewModel = TestMedicineScannerViewModel(mockTextRecognitionHelper, mockMedicineRepository)
    }

    @Test
    fun `processMedicinePackage con imagen válida actualiza estado correctamente`() = runTest {
        // Preparar
        val recognizedText = "Paracetamol\n500mg\nLaboratorio XYZ"
        val name = "Paracetamol"
        val medicines = listOf(MedicineModel(name = name, activeIngredient = "Acetaminofén", dosage = "500mg"))

        mockTextRecognitionHelper.textToReturn = recognizedText
        mockTextRecognitionHelper.extractedName = name
        `when`(mockMedicineRepository.searchMedicineByName(name)).thenReturn(Result.success(medicines))

        // Ejecutar
        viewModel.processMedicinePackage(mockBitmap)
        advanceUntilIdle() // Avanzar el tiempo virtual hasta que todas las coroutines estén inactivas

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(recognizedText, state.recognizedText)
        assertEquals(name, state.medicineName)
        assertEquals(1, state.medicines.size)
        assertEquals(name, state.medicines.first().name)
        assertEquals(medicines.first(), state.selectedMedicine)
        assertNull(state.error)
    }

    @Test
    fun `processMedicinePackage maneja errores de reconocimiento de texto`() = runTest {
        // Preparar
        mockTextRecognitionHelper.shouldThrowException = true
        mockTextRecognitionHelper.exceptionToThrow = RuntimeException("Error de reconocimiento")

        // Ejecutar
        viewModel.processMedicinePackage(mockBitmap)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("Error") ?: false)
        assertEquals("", state.recognizedText)
        assertEquals("", state.medicineName)
        assertTrue(state.medicines.isEmpty())
    }

    @Test
    fun `processMedicinePackage con texto vacío maneja el caso correctamente`() = runTest {
        // Preparar - Caso límite: texto vacío
        mockTextRecognitionHelper.textToReturn = ""
        mockTextRecognitionHelper.extractedName = ""

        // Ejecutar
        viewModel.processMedicinePackage(mockBitmap)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("No se pudo detectar el nombre") ?: false)
        assertEquals("", state.medicineName)
    }

    @Test
    fun `processMedicinePackage con error en búsqueda crea medicamento básico`() = runTest {
        // Preparar
        val recognizedText = "Ibuprofeno"
        val name = "Ibuprofeno"

        mockTextRecognitionHelper.textToReturn = recognizedText
        mockTextRecognitionHelper.extractedName = name
        `when`(mockMedicineRepository.searchMedicineByName(name)).thenReturn(Result.failure(Exception("Error API")))

        // Ejecutar
        viewModel.processMedicinePackage(mockBitmap)
        advanceUntilIdle()

        // Verificar - Debe crear un medicamento básico con el nombre
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(name, state.medicineName)
        assertEquals(1, state.medicines.size)
        assertEquals(name, state.medicines.first().name)
        assertTrue(state.error?.contains("Error") ?: false)
    }

    @Test
    fun `processMedicinePackage con timeout de API maneja error correctamente`() = runTest {
        // Preparar - Caso límite: timeout de red
        val recognizedText = "Amoxicilina"
        val name = "Amoxicilina"

        mockTextRecognitionHelper.textToReturn = recognizedText
        mockTextRecognitionHelper.extractedName = name
        `when`(mockMedicineRepository.searchMedicineByName(name)).thenReturn(Result.failure(TimeoutException("API Timeout")))

        // Ejecutar
        viewModel.processMedicinePackage(mockBitmap)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(name, state.medicineName)
        assertTrue(state.error?.contains("Error") ?: false)
        assertEquals(1, state.medicines.size) // Debería crear un medicamento básico
    }

    @Test
    fun `updateMedicineName actualiza nombre y busca información nuevamente`() = runTest {
        // Preparar
        val newName = "Aspirina"
        val medicines = listOf(MedicineModel(name = newName, activeIngredient = "Ácido acetilsalicílico"))

        `when`(mockMedicineRepository.searchMedicineByName(newName)).thenReturn(Result.success(medicines))

        // Ejecutar
        viewModel.updateMedicineName(newName)
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertEquals(newName, state.medicineName)
        assertEquals(1, state.medicines.size)
        assertEquals(newName, state.medicines.first().name)
    }

    @Test
    fun `updateMedicineName con nombre vacío no inicia búsqueda`() = runTest {
        // Preparar - Caso límite: nombre vacío
        val emptyName = ""

        // Ejecutar
        viewModel.updateMedicineName(emptyName)
        advanceUntilIdle()

        // Verificar
        verify(mockMedicineRepository, never()).searchMedicineByName(anyString())
    }

    @Test
    fun `sendMedicineToBackend con medicamento seleccionado retorna éxito`() = runTest {
        // Preparar
        val medicine = MedicineModel(name = "Loratadina")

        // Primero configuramos un medicamento seleccionado
        val medicines = listOf(medicine)
        `when`(mockMedicineRepository.searchMedicineByName("Loratadina")).thenReturn(Result.success(medicines))
        viewModel.updateMedicineName("Loratadina")
        advanceUntilIdle()

        // Luego preparamos el envío al backend
        `when`(mockMedicineRepository.sendMedicineToBackend(medicine)).thenReturn(Result.success(true))

        // Ejecutar
        viewModel.sendMedicineToBackend()
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.successMessage?.contains("correctamente") ?: false)
        assertNull(state.error)
    }

    @Test
    fun `sendMedicineToBackend sin medicamento seleccionado maneja error`() = runTest {
        // Caso límite: No hay medicamento seleccionado
        // Ejecutar
        viewModel.sendMedicineToBackend()
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("No hay medicamento seleccionado") ?: false)
    }

    @Test
    fun `sendMedicineToBackend maneja error en envío`() = runTest {
        // Preparar
        val medicine = MedicineModel(name = "Diclofenaco")

        // Configuramos un medicamento seleccionado
        val medicines = listOf(medicine)
        `when`(mockMedicineRepository.searchMedicineByName("Diclofenaco")).thenReturn(Result.success(medicines))
        viewModel.updateMedicineName("Diclofenaco")
        advanceUntilIdle()

        // Simulamos error en el envío
        `when`(mockMedicineRepository.sendMedicineToBackend(medicine)).thenReturn(Result.failure(Exception("Error en el backend")))

        // Ejecutar
        viewModel.sendMedicineToBackend()
        advanceUntilIdle()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("Error") ?: false)
        assertNull(state.successMessage)
    }

    @Test
    fun `resetScanProcess restaura estado inicial`() = runTest {
        // Preparar - Primero establecemos algún estado
        val medicine = MedicineModel(name = "Cetirizina")
        viewModel.selectMedicine(medicine)

        // Ejecutar
        viewModel.resetScanProcess()

        // Verificar
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.recognizedText)
        assertEquals("", state.medicineName)
        assertTrue(state.medicines.isEmpty())
        assertNull(state.selectedMedicine)
        assertNull(state.error)
        assertNull(state.successMessage)
    }
}
