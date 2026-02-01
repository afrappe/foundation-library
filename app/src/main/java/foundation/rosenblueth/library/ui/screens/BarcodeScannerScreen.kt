package foundation.rosenblueth.library.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import foundation.rosenblueth.library.ui.components.ErrorMessage
import foundation.rosenblueth.library.ui.viewmodel.BookScannerViewModel
import java.util.concurrent.Executors

/**
 * Pantalla para escaneo continuo de códigos de barras ISBN.
 * Utiliza ML Kit para detectar códigos EAN-13 (ISBN) en tiempo real.
 */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: BookScannerViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    // Solicitar permiso de cámara al inicio si no está concedido
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear ISBN") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Vista de la cámara con análisis de códigos de barras
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                // Configurar el escáner de códigos de barras para EAN-13 (ISBN)
                val barcodeScanner = remember {
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_ISBN
                        )
                        .build()
                    BarcodeScanning.getClient(options)
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // Configurar análisis de imagen para escaneo de códigos
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        if (!isProcessing && detectedBarcode == null) {
                                            processBarcode(
                                                imageProxy,
                                                barcodeScanner,
                                                onBarcodeFound = { barcode ->
                                                    detectedBarcode = barcode
                                                    isProcessing = true
                                                }
                                            )
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("BarcodeScannerScreen", "Error al iniciar cámara", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay con guía de escaneo
                ScannerOverlay()

                // Instrucciones
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Text(
                            text = "Apunta al código de barras del libro",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Mostrar código detectado
                detectedBarcode?.let { barcode ->
                    LaunchedEffect(barcode) {
                        // Pequeño delay para mostrar el código antes de navegar
                        kotlinx.coroutines.delay(500)
                        onBarcodeDetected(barcode)
                    }

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ISBN Detectado",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = barcode,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Limpiar recursos al salir
                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                        barcodeScanner.close()
                    }
                }
            }
        } else {
            ErrorMessage(
                message = "Se necesita permiso de cámara para escanear códigos de barras",
                onRetry = { launcher.launch(Manifest.permission.CAMERA) }
            )
        }
    }
}

/**
 * Overlay visual con guía para posicionar el código de barras
 */
@Composable
private fun ScannerOverlay() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val overlayColor = Color.Black.copy(alpha = 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Dimensiones del área de escaneo (rectángulo horizontal para códigos de barras)
        val scanAreaWidth = canvasWidth * 0.8f
        val scanAreaHeight = canvasHeight * 0.15f
        val scanAreaLeft = (canvasWidth - scanAreaWidth) / 2
        val scanAreaTop = (canvasHeight - scanAreaHeight) / 2

        // Dibujar fondo semi-transparente
        drawRect(
            color = overlayColor,
            size = size
        )

        // Cortar el área de escaneo (hacerla transparente)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(scanAreaLeft, scanAreaTop),
            size = Size(scanAreaWidth, scanAreaHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // Dibujar borde del área de escaneo
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(scanAreaLeft, scanAreaTop),
            size = Size(scanAreaWidth, scanAreaHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4f)
        )

        // Dibujar línea de escaneo animada (estática por ahora)
        val lineY = scanAreaTop + scanAreaHeight / 2
        drawLine(
            color = primaryColor.copy(alpha = 0.7f),
            start = Offset(scanAreaLeft + 16f, lineY),
            end = Offset(scanAreaLeft + scanAreaWidth - 16f, lineY),
            strokeWidth = 2f
        )
    }
}

/**
 * Procesa una imagen para detectar códigos de barras
 */
@OptIn(ExperimentalGetImage::class)
private fun processBarcode(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    onBarcodeFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        // Verificar si es un ISBN válido (13 dígitos que empiezan con 978 o 979)
                        if (isValidISBN(value)) {
                            onBarcodeFound(value)
                            return@addOnSuccessListener
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeScannerScreen", "Error al procesar código de barras", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Verifica si el código es un ISBN válido
 */
private fun isValidISBN(code: String): Boolean {
    // ISBN-13 comienza con 978 o 979
    if (code.length == 13 && (code.startsWith("978") || code.startsWith("979"))) {
        return code.all { it.isDigit() }
    }
    // ISBN-10 tiene 10 caracteres
    if (code.length == 10) {
        return code.dropLast(1).all { it.isDigit() } &&
               (code.last().isDigit() || code.last().uppercaseChar() == 'X')
    }
    // También aceptar EAN-13 que no sea ISBN pero sea código de libro
    if (code.length == 13 && code.all { it.isDigit() }) {
        return true
    }
    return false
}
