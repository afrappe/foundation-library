package foundation.rosenblueth.library.data.repository

import foundation.rosenblueth.library.data.model.MedicineModel
import foundation.rosenblueth.library.data.network.MedicineCatalogApiService
import foundation.rosenblueth.library.data.network.MedicineResponseItem
import foundation.rosenblueth.library.data.network.MedicineSearchResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MedicineRepositoryTest {

    @Mock
    private lateinit var mockApiService: MedicineCatalogApiService

    private lateinit var medicineRepository: MedicineRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        medicineRepository = TestMedicineRepository(mockApiService)
    }

    // Clase de prueba que extiende MedicineRepository para inyectar dependencias mockeadas
    private class TestMedicineRepository(private val apiService: MedicineCatalogApiService) : MedicineRepository() {
        // Sobrescribimos para usar el servicio mock en lugar del real
        override val medicineApiService: MedicineCatalogApiService
            get() = apiService
    }

    @Test
    fun `searchMedicineByName con nombre válido devuelve lista de medicamentos`() = runTest {
        // Preparar
        val mockMedicineItem = MedicineResponseItem(
            name = "Paracetamol",
            activeIngredient = "Acetaminofén",
            manufacturer = "Laboratorio Test",
            dosage = "500mg",
            pharmaceuticalForm = "Tabletas",
            therapeuticIndications = "Analgésico y antipirético",
            registrationNumber = "INVIMA-2023-001"
        )

        val mockResponse = Response.success(
            MedicineSearchResponse(
                items = listOf(mockMedicineItem),
                totalItems = 1,
                status = "ok"
            )
        )

        `when`(mockApiService.searchMedicineByName("Paracetamol")).thenReturn(mockResponse)

        // Ejecutar
        val result = medicineRepository.searchMedicineByName("Paracetamol")

        // Verificar
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Paracetamol", result.getOrNull()?.get(0)?.name)
        assertEquals("Acetaminofén", result.getOrNull()?.get(0)?.activeIngredient)
    }

    @Test
    fun `searchMedicineByName con nombre vacío devuelve error`() = runTest {
        // Caso límite: Nombre vacío
        val result = medicineRepository.searchMedicineByName("")

        // Verificar que se maneje adecuadamente
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `searchMedicineByName maneja respuesta vacía`() = runTest {
        // Preparar - Respuesta sin ítems
        val emptyResponse = Response.success(
            MedicineSearchResponse(
                items = emptyList(),
                totalItems = 0,
                status = "ok"
            )
        )

        `when`(mockApiService.searchMedicineByName("Medicamento Inexistente")).thenReturn(emptyResponse)

        // Ejecutar
        val result = medicineRepository.searchMedicineByName("Medicamento Inexistente")

        // Verificar
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun `searchMedicineByName maneja error HTTP`() = runTest {
        // Preparar - Respuesta de error
        val errorResponse = Response.error<MedicineSearchResponse>(
            404,
            "{\"error\":\"Not found\"}".toResponseBody("application/json".toMediaType())
        )

        `when`(mockApiService.searchMedicineByName("Error")).thenReturn(errorResponse)

        // Segunda llamada alternativa también fallará
        `when`(mockApiService.searchMedicineAlternative("Error")).thenReturn(errorResponse)

        // Ejecutar
        val result = medicineRepository.searchMedicineByName("Error")

        // Verificar
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `searchMedicineByName maneja timeout de red`() = runTest {
        // Preparar - Simulación de timeout
        `when`(mockApiService.searchMedicineByName("Timeout")).thenThrow(SocketTimeoutException("Timeout"))

        // Ejecutar
        val result = medicineRepository.searchMedicineByName("Timeout")

        // Verificar
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SocketTimeoutException)
    }

    @Test
    fun `searchMedicineByName maneja error de IO`() = runTest {
        // Preparar - Simulación de error de IO
        `when`(mockApiService.searchMedicineByName("IOError")).thenThrow(IOException("Error de lectura"))

        // Ejecutar
        val result = medicineRepository.searchMedicineByName("IOError")

        // Verificar
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `searchMedicineByName intenta con API alternativa cuando primera falla`() = runTest {
        // Preparar - Primera API falla
        val errorResponse = Response.error<MedicineSearchResponse>(
            500,
            "{\"error\":\"Server error\"}".toResponseBody("application/json".toMediaType())
        )

        // Respuesta exitosa de la segunda API
        val mockMedicineItem = MedicineResponseItem(
            name = "Ibuprofeno",
            activeIngredient = "Ibuprofeno",
            manufacturer = "Laboratorio Alt",
            dosage = "400mg",
            pharmaceuticalForm = "Tabletas",
            therapeuticIndications = "Antiinflamatorio",
            registrationNumber = "INVIMA-2023-002"
        )

        val successResponse = Response.success(
            MedicineSearchResponse(
                items = listOf(mockMedicineItem),
                totalItems = 1,
                status = "ok"
            )
        )

        `when`(mockApiService.searchMedicineByName("Fallback")).thenReturn(errorResponse)
        `when`(mockApiService.searchMedicineAlternative("Fallback")).thenReturn(successResponse)

        // Ejecutar
        val result = medicineRepository.searchMedicineByName("Fallback")

        // Verificar
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Ibuprofeno", result.getOrNull()?.get(0)?.name)
    }

    @Test
    fun `sendMedicineToBackend con medicamento válido retorna éxito`() = runTest {
        // Preparar
        val medicine = MedicineModel(
            name = "Amoxicilina",
            activeIngredient = "Amoxicilina trihidratada",
            manufacturer = "Laboratorio Test"
        )

        // Ejecutar
        val result = medicineRepository.sendMedicineToBackend(medicine)

        // Verificar
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }
}
