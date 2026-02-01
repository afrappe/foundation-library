package foundation.rosenblueth.library.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import foundation.rosenblueth.library.ui.model.ScanMode
import foundation.rosenblueth.library.ui.screens.CameraScreen
import foundation.rosenblueth.library.ui.screens.LibraryScreen
import foundation.rosenblueth.library.ui.screens.ResultsScreen
import foundation.rosenblueth.library.ui.screens.ScanModeScreen
import foundation.rosenblueth.library.ui.screens.SectionsScreen
import foundation.rosenblueth.library.ui.viewmodel.BookScannerViewModel

/**
 * Actividad principal de la aplicación.
 * Gestiona la navegación entre las diferentes pantallas y el estado global.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibraryScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LibraryScannerApp()
                }
            }
        }
    }
}

/**
 * Tema principal de la aplicación
 */
@Composable
fun LibraryScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

/**
 * Estructura principal de la aplicación con navegación
 */
@Composable
fun LibraryScannerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: BookScannerViewModel = viewModel(
        factory = BookScannerViewModel.Factory(context)
    )

    NavHost(
        navController = navController,
        startDestination = "scanMode"
    ) {
        // Pantalla de selección de modo de escaneo
        composable("scanMode") {
            ScanModeScreen(
                onModeSelected = { mode ->
                    viewModel.setScanMode(mode)
                    when (mode) {
                        ScanMode.SECTIONS -> navController.navigate("sections")
                        ScanMode.ISBN -> navController.navigate("camera/isbn")
                        ScanMode.COVER -> navController.navigate("camera/cover")
                    }
                }
            )
        }

        // Pantalla de escaneo por secciones
        composable("sections") {
            SectionsScreen(
                onScanSection = { sectionType ->
                    viewModel.setCurrentSectionType(sectionType.name)
                    navController.navigate("camera/section/${sectionType.name}")
                },
                onSearchBook = { title, author, publisher ->
                    viewModel.processBookBySections(title, author, publisher)
                    navController.navigate("results")
                },
                onNavigateBack = {
                    viewModel.clearSectionValues()
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        // Cámara para escanear sección específica
        composable("camera/section/{sectionType}") { backStackEntry ->
            val sectionTypeName = backStackEntry.arguments?.getString("sectionType") ?: ""
            val uiState by viewModel.uiState.collectAsState()

            CameraScreen(
                onPhotoTaken = { bitmap ->
                    viewModel.processBookCover(bitmap)
                },
                onNavigateToResults = {
                    // Obtener el texto reconocido y actualizar la sección correspondiente
                    val recognizedText = uiState.bookTitle
                    if (recognizedText.isNotEmpty()) {
                        viewModel.updateSectionValue(sectionTypeName, recognizedText)
                    }
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        // Cámara para escaneo de ISBN
        composable("camera/isbn") {
            CameraScreen(
                onPhotoTaken = { bitmap ->
                    viewModel.processBookISBN(bitmap)
                },
                onNavigateToResults = {
                    navController.navigate("results")
                },
                viewModel = viewModel
            )
        }

        // Cámara para escaneo de portada (modo original)
        composable("camera/cover") {
            CameraScreen(
                onPhotoTaken = { bitmap ->
                    viewModel.processBookCover(bitmap)
                },
                onNavigateToResults = {
                    navController.navigate("results")
                },
                viewModel = viewModel
            )
        }

        composable("results") {
            ResultsScreen(
                onNewScan = {
                    viewModel.clearSectionValues()
                    viewModel.resetScanProcess()
                    navController.navigate("scanMode") {
                        popUpTo("scanMode") { inclusive = true }
                    }
                },
                onNavigateToLibrary = {
                    navController.navigate("library")
                },
                viewModel = viewModel
            )
        }
        
        composable("library") {
            LibraryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
