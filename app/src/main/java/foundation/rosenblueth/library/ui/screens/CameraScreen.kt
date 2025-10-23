package foundation.rosenblueth.library.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import foundation.rosenblueth.library.ui.components.ErrorMessage
import foundation.rosenblueth.library.ui.components.SelectionOverlayView
import foundation.rosenblueth.library.ui.viewmodel.MedicineScannerViewModel
import foundation.rosenblueth.library.util.TextRecognitionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.camera.lifecycle.ProcessCameraProvider

/**
 * Pantalla para escanear el empaque del medicamento utilizando la cámara.
 *
 * @param onPhotoTaken Callback que se ejecuta cuando se captura una foto
 * @param onNavigateToResults Callback para navegar a la pantalla de resultados
 */
@Composable
fun CameraScreen(
    onPhotoTaken: (Bitmap) -> Unit,
    onNavigateToResults: () -> Unit,
    viewModel: MedicineScannerViewModel = viewModel()
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

    // Estado para controlar el modo de selección
    var selectionModeActive by remember { mutableStateOf(false) }

    // Estado para controlar si se ha dibujado un rectángulo de selección
    var selectionDrawn by remember { mutableStateOf(false) }

    // Referencia al PreviewView para poder capturar su imagen
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Referencia a la vista de selección
    var selectionOverlayView by remember { mutableStateOf<SelectionOverlayView?>(null) }

    // Ayudante para reconocimiento de texto
    val textRecognitionHelper = remember { TextRecognitionHelper(context) }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Vista de la cámara
            val imageCapture = remember { ImageCapture.Builder().build() }
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

            AndroidView(
                factory = { ctx ->
                    val preview = PreviewView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val previewUseCase = Preview.Builder().build().also {
                            it.setSurfaceProvider(preview.surfaceProvider)
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                previewUseCase,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    // Guardar referencia al PreviewView
                    previewView = preview

                    // Crear un FrameLayout para contener tanto la vista previa como la capa de selección
                    val container = android.widget.FrameLayout(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    // Añadir el PreviewView al contenedor
                    container.addView(preview)

                    // Crear la vista de selección con las mismas dimensiones que el contenedor
                    val selectionView = SelectionOverlayView(ctx).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        showSelection = selectionModeActive

                        // Siempre mantener la vista visible
                        alpha = 1.0f

                        // Detectar cuando se ha completado un gesto de selección
                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP && selectionModeActive) {
                                // Cuando se suelta el dedo, comprobar si se ha dibujado un rectángulo válido
                                val rect = getSelectionRect()
                                if (rect.width() > 50 && rect.height() > 50) {
                                    selectionDrawn = true
                                }
                            }
                            // Dejar que el evento se procese normalmente por onTouchEvent
                            false
                        }
                    }

                    // Configurar el callback para la selección
                    selectionView.onSelectionCompleteListener = { rect ->
                        if (selectionModeActive) {
                            processSelectedArea(rect, preview, textRecognitionHelper, onPhotoTaken, onNavigateToResults)
                        }
                    }

                    // Guardar referencia a la vista de selección
                    selectionOverlayView = selectionView

                    // Añadir la vista de selección encima del PreviewView
                    container.addView(selectionView)

                    // Devolver el contenedor completo
                    container
                },
                modifier = Modifier.fillMaxSize(),
                update = { preview ->
                    // Actualizar el estado de la vista de selección cuando cambia el modo
                    selectionOverlayView?.showSelection = selectionModeActive
                }
            )

            // Botones de acción en la parte inferior
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón para activar/desactivar el modo de selección
                Button(
                    onClick = {
                        selectionModeActive = !selectionModeActive
                        if (!selectionModeActive) {
                            selectionOverlayView?.clearSelection()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CropFree,
                        contentDescription = "Seleccionar área"
                    )
                    Text(
                        text = if (selectionModeActive) "Cancelar selección" else "Seleccionar área",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Botón de captura normal (solo visible cuando no está en modo selección)
                if (!selectionModeActive) {
                    Button(
                        onClick = {
                            takePhoto(
                                imageCapture = imageCapture,
                                executor = cameraExecutor,
                                context = context,
                                onPhotoTaken = onPhotoTaken,
                                onNavigateToResults = onNavigateToResults
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tomar foto"
                        )
                        Text(
                            text = "Capturar empaque",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Botón de confirmación de selección (solo visible en modo selección)
                if (selectionModeActive) {
                    Button(
                        onClick = {
                            // Confirmar la selección actual y procesarla
                            selectionOverlayView?.confirmSelection()

                            // Desactivar el modo de selección y limpiar variables de estado
                            selectionModeActive = false
                            selectionDrawn = false
                        }
                    ) {
                        Text("Procesar selección")
                    }
                }
            }
        }
    } else {
        // Mostrar mensaje si no hay permiso de cámara
        ErrorMessage(
            message = "Se necesita permiso de cámara para escanear medicamentos",
            onRetry = { launcher.launch(Manifest.permission.CAMERA) }
        )
    }

    // Limpiar recursos cuando se destruye la composición
    DisposableEffect(Unit) {
        onDispose {
            selectionOverlayView?.let { view ->
                val parent = view.parent as? android.view.ViewGroup
                parent?.removeView(view)
            }
        }
    }
}

/**
 * Función para tomar una foto con la cámara
 */
private fun takePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    context: android.content.Context,
    onPhotoTaken: (Bitmap) -> Unit,
    onNavigateToResults: () -> Unit
) {
    // Crear archivo temporal para la foto
    val photoFile = File(
        context.externalCacheDir,
        SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    // Tomar la foto
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                // Utilizamos Handler para ejecutar en el hilo principal
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onPhotoTaken(bitmap)
                    onNavigateToResults()
                }
            }

            override fun onError(exc: ImageCaptureException) {
                // Manejar error (en una implementación real se mostraría un mensaje al usuario)
                exc.printStackTrace()
            }
        }
    )
}

/**
 * Procesa el área seleccionada para reconocimiento de texto
 */
private fun processSelectedArea(
    rect: Rect,
    previewView: PreviewView,
    textRecognitionHelper: TextRecognitionHelper,
    onPhotoTaken: (Bitmap) -> Unit,
    onNavigateToResults: () -> Unit
) {
    // Obtener bitmap de la vista previa
    val bitmap = previewView.bitmap ?: return

    // Cortar el bitmap según la selección
    val croppedBitmap = try {
        Bitmap.createBitmap(
            bitmap,
            rect.left.coerceIn(0, bitmap.width),
            rect.top.coerceIn(0, bitmap.height),
            rect.width().coerceIn(1, bitmap.width - rect.left),
            rect.height().coerceIn(1, bitmap.height - rect.top)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }

    // Procesar el bitmap recortado
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Usar el bitmap recortado
            withContext(Dispatchers.Main) {
                onPhotoTaken(croppedBitmap)
                onNavigateToResults()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
