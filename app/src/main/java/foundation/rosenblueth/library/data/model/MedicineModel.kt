package foundation.rosenblueth.library.data.model

/**
 * Modelo de datos que representa la información de un medicamento.
 */
data class MedicineModel(
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
) {
    companion object {
        /**
         * Crea un modelo inicial solo con nombre,
         * útil cuando el reconocimiento de texto solo identificó el nombre
         */
        fun createWithName(name: String): MedicineModel {
            return MedicineModel(name = name)
        }
    }
}
