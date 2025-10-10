package foundation.rosenblueth.library.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import foundation.rosenblueth.library.ui.screens.CameraScreen
import foundation.rosenblueth.library.ui.screens.ResultsScreen
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
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    NavHost(
        navController = navController,
        startDestination = "camera"
    ) {
        composable("camera") {
            CameraScreen(
                onPhotoTaken = { bitmap ->
                    capturedBitmap = bitmap
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
                    navController.navigate("camera") {
                        popUpTo("camera") { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
    }
}
