package foundation.rosenblueth.library.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import foundation.rosenblueth.library.data.model.MedicineModel
import foundation.rosenblueth.library.data.repository.MedicineRepository
import foundation.rosenblueth.library.util.TextRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la funcionalidad de escaneo y búsqueda de medicamentos
 */
open class MedicineScannerViewModel(private val appContext: Context? = null) : ViewModel() {
    protected open val medicineRepositoryInstance: MedicineRepository = MedicineRepository()
    protected open val textRecognitionHelperInstance: TextRecognitionHelper = TextRecognitionHelper(appContext)

    // Referencias para mantener compatibilidad
    private val medicineRepository get() = medicineRepositoryInstance
    private val textRecognitionHelper get() = textRecognitionHelperInstance

    // Estado para la UI
    private val _uiState = MutableStateFlow(MedicineScannerUiState())
    val uiState: StateFlow<MedicineScannerUiState> = _uiState.asStateFlow()

    /**
     * Procesa la imagen capturada para extraer el nombre y buscar información del medicamento
     */
    fun processMedicinePackage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Paso 1: Reconocer texto de la imagen
                val recognizedText = textRecognitionHelper.recognizeText(bitmap)

                // Paso 2: Extraer el nombre del medicamento
                val medicineName = textRecognitionHelper.extractMedicineName(recognizedText)

                _uiState.update {
                    it.copy(
                        recognizedText = recognizedText,
                        medicineName = medicineName
                    )
                }

                // Si se encontró un nombre, buscar información del medicamento
                if (medicineName.isNotEmpty()) {
                    searchMedicineInfo(medicineName)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudo detectar el nombre del medicamento"
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al procesar la imagen: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Busca información del medicamento usando el nombre reconocido
     */
    private fun searchMedicineInfo(name: String) {
        viewModelScope.launch {
            try {
                val result = medicineRepository.searchMedicineByName(name)

                result.fold(
                    onSuccess = { medicines ->
                        if (medicines.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    medicines = medicines,
                                    selectedMedicine = medicines.first()
                                )
                            }
                        } else {
                            // Si no se encontraron medicamentos, crear uno con solo el nombre
                            val basicMedicine = MedicineModel.createWithName(name)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    medicines = listOf(basicMedicine),
                                    selectedMedicine = basicMedicine
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        // Si hay un error en la búsqueda, crear medicamento básico con el nombre
                        val basicMedicine = MedicineModel.createWithName(name)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error al buscar información: ${error.message}",
                                medicines = listOf(basicMedicine),
                                selectedMedicine = basicMedicine
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar información del medicamento: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Selecciona un medicamento de la lista de resultados
     */
    fun selectMedicine(medicine: MedicineModel) {
        _uiState.update { it.copy(selectedMedicine = medicine) }
    }

    /**
     * Actualiza manualmente el nombre del medicamento
     */
    fun updateMedicineName(name: String) {
        _uiState.update { it.copy(medicineName = name) }

        // Volver a buscar con el nuevo nombre
        if (name.isNotEmpty()) {
            searchMedicineInfo(name)
        }
    }

    /**
     * Envía los datos del medicamento seleccionado al backend
     */
    fun sendMedicineToBackend() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, error = null) }

            uiState.value.selectedMedicine?.let { medicine ->
                try {
                    val result = medicineRepository.sendMedicineToBackend(medicine)

                    result.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Medicamento enviado correctamente al backend"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Error al enviar medicamento: ${error.message}"
                                )
                            }
                        }
                    )
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al enviar medicamento: ${e.message}"
                        )
                    }
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No hay medicamento seleccionado para enviar"
                    )
                }
            }
        }
    }

    /**
     * Reinicia el proceso de escaneo
     */
    fun resetScanProcess() {
        _uiState.update { MedicineScannerUiState() }
    }

    /**
     * Factory para crear el ViewModel con el contexto necesario
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MedicineScannerViewModel::class.java)) {
                return MedicineScannerViewModel(context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Estado de la UI para la funcionalidad de escaneo de medicamentos
 */
data class MedicineScannerUiState(
    val isLoading: Boolean = false,
    val recognizedText: String = "",
    val medicineName: String = "",
    val medicines: List<MedicineModel> = emptyList(),
    val selectedMedicine: MedicineModel? = null,
    val error: String? = null,
    val successMessage: String? = null
)
