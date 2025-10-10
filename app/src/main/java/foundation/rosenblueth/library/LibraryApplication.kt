package foundation.rosenblueth.library

import android.app.Application
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Clase de aplicación personalizada que inicializa las bibliotecas necesarias
 * como ML Kit al inicio de la aplicación.
 */
class LibraryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar ML Kit al inicio para evitar problemas de inicialización tardía
        try {
            // Inicializar el reconocedor de texto de ML Kit de forma temprana
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Este log es solo para depuración, puede eliminarse en producción
            android.util.Log.d("LibraryApplication", "ML Kit inicializado correctamente")
        } catch (e: Exception) {
            // Capturar y registrar cualquier error durante la inicialización
            android.util.Log.e("LibraryApplication", "Error al inicializar ML Kit: ${e.message}")
        }
    }
}
