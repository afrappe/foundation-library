package foundation.rosenblueth.library.data.network

import foundation.rosenblueth.library.data.model.MedicineModel

/**
 * Extensiones para convertir la respuesta de la API farmacéutica a nuestros modelos
 */

/**
 * Convierte un elemento de la respuesta de la API a un modelo de medicamento
 */
fun LocItem.toMedicineModel(): MedicineModel {
    val activeIngredientValue = contributors.firstOrNull() ?: ""
    val manufacturerValue = publisher.firstOrNull() ?: ""
    val registrationNumberValue = isbn.firstOrNull() ?: ""
    val therapeuticIndicationsValue = description.joinToString("\n").take(500)

    return MedicineModel(
        name = title,
        activeIngredient = activeIngredientValue,
        manufacturer = manufacturerValue,
        registrationNumber = registrationNumberValue,
        therapeuticIndications = therapeuticIndicationsValue
    )
}
