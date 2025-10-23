package foundation.rosenblueth.library.data.network

import foundation.rosenblueth.library.data.model.MedicineModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface para las APIs de servicios de catalogación de medicamentos
 */
interface MedicineCatalogApiService {
    /**
     * Busca información de medicamento en base de datos farmacéutica
     * @param name El nombre del medicamento a buscar
     * @return Respuesta con la información del medicamento
     */
    @GET("search/medicine/search")
    suspend fun searchMedicineByName(@Query("q") name: String): Response<MedicineSearchResponse>

    /**
     * Busca información de medicamento en base de datos alternativa
     * @param name El nombre del medicamento a buscar
     * @return Respuesta con la información del medicamento
     */
    @GET("search/alternative/search")
    suspend fun searchMedicineAlternative(@Query("q") name: String): Response<MedicineSearchResponse>

    /**
     * Búsqueda de medicamentos por nombre
     * @param query El nombre o términos de búsqueda
     * @param format El formato de respuesta (json)
     * @param filter Filtrar por tipo de contenido (medicines)
     * @return Respuesta con información de medicamentos encontrados
     */
    @GET("search/")
    suspend fun searchMedicines(
        @Query("q") query: String,
        @Query("fo") format: String = "json",
        @Query("at") filter: String = "medicines"
    ): Response<LocResponse>
}

/**
 * Modelo de respuesta de la búsqueda de medicamentos
 */
data class MedicineSearchResponse(
    val items: List<MedicineResponseItem> = emptyList(),
    val totalItems: Int = 0,
    val status: String = "",
    val error: String? = null
)

/**
 * Modelo que representa un ítem de medicamento en la respuesta de la API
 */
data class MedicineResponseItem(
    val name: String,
    val activeIngredient: String = "",
    val manufacturer: String = "",
    val dosage: String = "",
    val pharmaceuticalForm: String = "",
    val therapeuticIndications: String = "",
    val contraindications: String = "",
    val sideEffects: String = "",
    val registrationNumber: String = "",
    val packageImageUrl: String = ""
)

/**
 * Enlaces a imágenes del empaque
 */
data class ImageLinks(
    val smallThumbnail: String = "",
    val thumbnail: String = "",
    val medium: String = "",
    val large: String = ""
)

/**
 * Extensión para convertir la respuesta de la API a nuestro modelo de datos
 */
fun MedicineResponseItem.toMedicineModel(): MedicineModel {
    return MedicineModel(
        name = this.name,
        activeIngredient = this.activeIngredient,
        manufacturer = this.manufacturer,
        dosage = this.dosage,
        pharmaceuticalForm = this.pharmaceuticalForm,
        therapeuticIndications = this.therapeuticIndications,
        contraindications = this.contraindications,
        sideEffects = this.sideEffects,
        registrationNumber = this.registrationNumber,
        packageImageUrl = this.packageImageUrl
    )
}
