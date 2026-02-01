package foundation.rosenblueth.library.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import foundation.rosenblueth.library.ui.model.ScanMode

/**
 * Pantalla para seleccionar el modo de escaneo de libros.
 * Presenta tres opciones: por secciones, por ISBN o por portada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanModeScreen(
    onModeSelected: (ScanMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Escaneo de Libros",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selecciona el modo de escaneo",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        ScanModeButton(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Por Secciones",
            description = "Escanea título, autor y editorial por separado",
            onClick = { onModeSelected(ScanMode.SECTIONS) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ScanModeButton(
            icon = Icons.Default.QrCodeScanner,
            title = "ISBN",
            description = "Escanea el código de barras o número ISBN",
            onClick = { onModeSelected(ScanMode.ISBN) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ScanModeButton(
            icon = Icons.Default.PhotoCamera,
            title = "Portada",
            description = "Escanea la portada completa del libro",
            onClick = { onModeSelected(ScanMode.COVER) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Se buscarán clasificaciones LC, Dewey y DCU",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Botón para seleccionar un modo de escaneo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanModeButton(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
