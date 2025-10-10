package foundation.rosenblueth.library.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import foundation.rosenblueth.library.R
import foundation.rosenblueth.library.util.TextBlock
import foundation.rosenblueth.library.util.TextRecognitionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vista compuesta que integra la cámara con una capa de selección para OCR.
 * Esta vista permite seleccionar un área específica de la vista previa de la cámara
 * para realizar reconocimiento de texto solo en esa área.
 */
class OCRSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // PreviewView para la cámara (debe ser configurado externamente)
    private lateinit var previewView: PreviewView

    // Vista de selección que se dibuja encima de la vista previa
    private val selectionOverlay = SelectionOverlayView(context)

    // Helper para reconocimiento de texto
    private val textRecognitionHelper = TextRecognitionHelper(context)

    // Callback para cuando se encuentra texto
    private var onTextRecognizedListener: ((String) -> Unit)? = null

    // Callback para cuando se selecciona un área y se procesan los bloques de texto
    private var onTextBlocksRecognizedListener: ((List<TextBlock>) -> Unit)? = null

    init {
        // Inflar el layout que contiene esta vista compuesta
        LayoutInflater.from(context).inflate(R.layout.view_ocr_selection, this, true)

        // Agregar la capa de selección sobre la vista previa
        addOverlay()

        // Configurar el listener para cuando se completa la selección
        selectionOverlay.onSelectionCompleteListener = { rect ->
            // Capturar la imagen actual y procesarla
            captureImageAndProcess(rect)
        }
    }

    /**
     * Configura la PreviewView de la cámara
     */
    fun setPreviewView(preview: PreviewView) {
        previewView = preview

        // Asegurarse de que el overlay tenga el mismo tamaño que la vista previa
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        selectionOverlay.layoutParams = layoutParams
    }

    /**
     * Agrega la vista de selección como una capa sobre la vista previa
     */
    private fun addOverlay() {
        // Buscar el contenedor de la vista previa
        val previewContainer = findViewById<FrameLayout>(R.id.camera_preview_container)

        // Agregar la vista de selección al contenedor
        previewContainer.addView(selectionOverlay)
    }

    /**
     * Captura la imagen actual de la vista previa y procesa el área seleccionada
     */
    private fun captureImageAndProcess(selectionRect: Rect) {
        // Verificar que la vista previa esté inicializada
        if (!::previewView.isInitialized) {
            Toast.makeText(context, "La vista previa de la cámara no está configurada", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el bitmap de la vista previa actual
        val bitmap = previewView.bitmap ?: run {
            Toast.makeText(context, "No se pudo capturar la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        // Iniciar el procesamiento en un hilo de fondo
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener todos los bloques de texto de la imagen
                val textBlocks = textRecognitionHelper.getTextBlocks(bitmap)

                // Seleccionar solo los bloques que están dentro del área seleccionada
                val selectedBlocks = textRecognitionHelper.selectTextBlocksInRegion(textBlocks, selectionRect)

                // Extraer el texto concatenado de los bloques seleccionados
                val recognizedText = textRecognitionHelper.getTextFromBlocks(selectedBlocks)

                withContext(Dispatchers.Main) {
                    // Notificar el texto reconocido
                    onTextRecognizedListener?.invoke(recognizedText)

                    // Notificar los bloques de texto reconocidos
                    onTextBlocksRecognizedListener?.invoke(selectedBlocks)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al procesar el texto: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Establece un listener para cuando se reconoce texto en el área seleccionada
     */
    fun setOnTextRecognizedListener(listener: (String) -> Unit) {
        onTextRecognizedListener = listener
    }

    /**
     * Establece un listener para cuando se reconocen bloques de texto en el área seleccionada
     */
    fun setOnTextBlocksRecognizedListener(listener: (List<TextBlock>) -> Unit) {
        onTextBlocksRecognizedListener = listener
    }

    /**
     * Limpia la selección actual
     */
    fun clearSelection() {
        selectionOverlay.clearSelection()
    }

    /**
     * Activa o desactiva la posibilidad de seleccionar un área
     */
    fun setSelectionEnabled(enabled: Boolean) {
        selectionOverlay.showSelection = enabled
    }
}
