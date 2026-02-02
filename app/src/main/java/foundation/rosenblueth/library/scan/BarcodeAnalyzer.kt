// kotlin
package foundation.rosenblueth.library.scan

import android.media.Image
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicLong

@androidx.annotation.OptIn(ExperimentalGetImage::class)
class BarcodeAnalyzer(
    private val onIsbnDetected: (String) -> Unit,
    private val minIntervalMs: Long = 2500L // evita repeticiones rápidas
) : ImageAnalysis.Analyzer {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastEmitTime = AtomicLong(0)
    @Volatile private var lastIsbn: String? = null

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage: Image? = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Buscar el primer barcode con rawValue que parezca ISBN
                    val candidate = barcodes.asSequence()
                        .mapNotNull { it.rawValue }
                        .map { it.trim() }
                        .mapNotNull { normalizeIsbn(it) }
                        .firstOrNull()

                    candidate?.let { isbn ->
                        val now = System.currentTimeMillis()
                        val last = lastEmitTime.get()
                        if (isbn != lastIsbn || now - last >= minIntervalMs) {
                            lastIsbn = isbn
                            lastEmitTime.set(now)
                            // Ejecutar callback en hilo principal
                            mainHandler.post { onIsbnDetected(isbn) }
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Ignorar y cerrar
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun normalizeIsbn(raw: String): String? {
        // El raw puede contener texto: extraer solo dígitos y 'X'
        val cleaned = raw.filter { it.isDigit() || it == 'X' || it == 'x' }
        // ISBN-13 suele tener 13 dígitos; ISBN-10 10 (X posible)
        return when {
            cleaned.length == 13 -> cleaned
            cleaned.length == 10 -> isbn10To13(cleaned)
            else -> null
        }
    }

    private fun isbn10To13(isbn10: String): String? {
        // Convertir ISBN-10 a ISBN-13 (prefix 978)
        @Suppress("SwallowedException")
        try {
            val core = "978" + isbn10.substring(0, 9)
            var sum = 0
            for (i in core.indices) {
                val digit = core[i].digitToInt()
                sum += if (i % 2 == 0) digit else digit * 3
            }
            val check = (10 - (sum % 10)) % 10
            return core + check.toString()
        } catch (e: Exception) {
            return null
        }
    }
}
