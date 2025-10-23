package foundation.rosenblueth.library.data.repository

import foundation.rosenblueth.library.data.model.MedicineModel
import foundation.rosenblueth.library.data.network.RetrofitClient
import foundation.rosenblueth.library.data.network.toMedicineModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestionar las operaciones relacionadas con medicamentos.
 */
open class MedicineRepository {
    open val medicineApiService = RetrofitClient.medicineApiService

    /**
     * Busca información de un medicamento por nombre.
     *
     * @param name El nombre del medicamento a buscar
     * @return Una lista de modelos de medicamentos que coinciden con la búsqueda
     */
    suspend fun searchMedicineByName(name: String): Result<List<MedicineModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Buscar en la API de medicamentos
                val response = medicineApiService.searchMedicines(query = name)

                if (response.isSuccessful && response.body() != null) {
                    val locResponse = response.body()!!
                    // Convertir los resultados al modelo de medicamento de la aplicación
                    val medicines = locResponse.items.map { it.toMedicineModel() }
                    // Devolver la lista de medicamentos (puede estar vacía)
                    Result.success(medicines)
                } else {
                    Result.failure(Exception("Error en la búsqueda: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Envía los datos del medicamento al backend
     *
     * @param medicine El modelo de medicamento a enviar
     * @return Resultado de la operación
     */
    suspend fun sendMedicineToBackend(medicine: MedicineModel): Result<Boolean> {
        // En una implementación real, aquí se llamaría a un endpoint de API para
        // enviar los datos del medicamento al backend

        return withContext(Dispatchers.IO) {
            try {
                // Simulando una llamada exitosa con los datos del medicamento
                // En este caso sólo lo imprimimos para demostrar su uso
                println("Enviando medicamento al backend: ${medicine.name} - ${medicine.activeIngredient}")
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
