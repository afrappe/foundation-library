package foundation.rosenblueth.library.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import foundation.rosenblueth.library.data.model.MedicineModel
import foundation.rosenblueth.library.ui.components.ErrorMessage
import foundation.rosenblueth.library.ui.components.LoadingIndicator
import foundation.rosenblueth.library.ui.components.SuccessMessage
import foundation.rosenblueth.library.ui.viewmodel.MedicineScannerViewModel

/**
 * Pantalla para mostrar y gestionar los resultados del escaneo de medicamentos.
 *
 * @param onNewScan Callback para volver a escanear un nuevo empaque
 * @param viewModel ViewModel que gestiona los datos de la aplicación
 */
@Composable
fun ResultsScreen(
    onNewScan: () -> Unit,
    viewModel: MedicineScannerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra superior con título y botón para nuevo escaneo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Información del medicamento",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(onClick = {
                viewModel.resetScanProcess()
                onNewScan()
            }) {
                Text("Nuevo escaneo")
            }
        }

        Divider()

        // Contenido principal
        if (uiState.isLoading) {
            LoadingIndicator(message = "Procesando información del medicamento...")
        } else if (uiState.error != null && uiState.medicines.isEmpty()) {
            // Solo mostrar error si no hay medicamentos disponibles
            ErrorMessage(
                message = uiState.error ?: "Error desconocido",
                onRetry = onNewScan
            )
        } else if (uiState.successMessage != null) {
            SuccessMessage(message = uiState.successMessage ?: "")

            // Botón para volver a escanear después de éxito
            Button(
                onClick = {
                    viewModel.resetScanProcess()
                    onNewScan()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Escanear otro medicamento")
            }
        } else {
            // Mostrar información del medicamento
            MedicineInformationContent(
                medicineName = uiState.medicineName,
                selectedMedicine = uiState.selectedMedicine,
                onNameUpdate = viewModel::updateMedicineName,
                onSendToBackend = viewModel::sendMedicineToBackend,
                errorMessage = uiState.error
            )
        }
    }
}

/**
 * Componente que muestra la información del medicamento y permite editarla
 */
@Composable
private fun MedicineInformationContent(
    medicineName: String,
    selectedMedicine: MedicineModel?,
    onNameUpdate: (String) -> Unit,
    onSendToBackend: () -> Unit,
    errorMessage: String?
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(medicineName) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Campo de nombre (editable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Nombre detectado:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditingName) {
                    // Campo de edición del nombre
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre del medicamento") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onNameUpdate(editedName)
                                isEditingName = false
                                focusManager.clearFocus()
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                onNameUpdate(editedName)
                                isEditingName = false
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Send, "Guardar")
                            }
                        }
                    )
                } else {
                    // Mostrar nombre con opción para editar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medicineName,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Default.Edit, "Editar nombre")
                        }
                    }
                }
            }
        }

        // Mostrar detalles del medicamento si está disponible
        selectedMedicine?.let { medicine ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Información del medicamento:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MedicineDetailItem("Principio activo", medicine.activeIngredient.takeIf { it.isNotEmpty() } ?: "No disponible")
                    MedicineDetailItem("Fabricante", medicine.manufacturer.takeIf { it.isNotEmpty() } ?: "No disponible")
                    MedicineDetailItem("Dosificación", medicine.dosage.takeIf { it.isNotEmpty() } ?: "No disponible")
                    MedicineDetailItem("Forma farmacéutica", medicine.pharmaceuticalForm.takeIf { it.isNotEmpty() } ?: "No disponible")
                    MedicineDetailItem("Registro sanitario", medicine.registrationNumber.takeIf { it.isNotEmpty() } ?: "No disponible")

                    if (medicine.therapeuticIndications.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Indicaciones terapéuticas:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = medicine.therapeuticIndications,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (medicine.contraindications.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Contraindicaciones:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = medicine.contraindications,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Mostrar mensaje de error si existe
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para enviar al backend
        Button(
            onClick = onSendToBackend,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviar al registro")
        }
    }
}

/**
 * Componente que muestra un campo de detalle del medicamento
 */
@Composable
private fun MedicineDetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
