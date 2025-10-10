package foundation.rosenblueth.library.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import foundation.rosenblueth.library.R

/**
 * Vista personalizada que permite seleccionar un área rectangular sobre otra vista (como la previsualización de la cámara)
 */
class SelectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Rectángulo de selección
    private var selectionRect = Rect()

    // Coordenadas iniciales para el rectángulo
    private var startX = 0f
    private var startY = 0f

    // Indica si estamos actualmente dibujando un rectángulo
    private var isDrawing = false

    // Pintura para el rectángulo de selección
    val selectionPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f  // Línea más gruesa para mejor visibilidad
    }

    // Pintura para el área semitransparente fuera de la selección
    val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 70  // Más transparente para mejor visibilidad
        style = Paint.Style.FILL
    }

    // Listener para cuando se completa una selección
    var onSelectionCompleteListener: ((Rect) -> Unit)? = null

    // Indica si se debe mostrar la selección
    var showSelection = true
        set(value) {
            field = value
            invalidate() // Redibujar la vista cuando cambia esta propiedad
        }

    init {
        // Hacer que la vista sea transparente
        setBackgroundColor(Color.TRANSPARENT)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showSelection) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Guardar punto inicial
                startX = event.x
                startY = event.y
                isDrawing = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    // Actualizar rectángulo mientras se arrastra
                    updateSelectionRect(event.x, event.y)
                    invalidate() // Redibujar
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDrawing) {
                    // Finalizar el rectángulo
                    updateSelectionRect(event.x, event.y)
                    isDrawing = false

                    // Ya no notificamos automáticamente que se completó la selección
                    // sino que esperamos a que el usuario confirme con un botón

                    invalidate() // Redibujar
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                invalidate() // Redibujar
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showSelection || (selectionRect.width() <= 0 || selectionRect.height() <= 0)) {
            return
        }

        // Dibujar el área oscurecida fuera de la selección
        // Arriba
        canvas.drawRect(0f, 0f, width.toFloat(), selectionRect.top.toFloat(), overlayPaint)
        // Izquierda
        canvas.drawRect(
            0f, selectionRect.top.toFloat(),
            selectionRect.left.toFloat(), selectionRect.bottom.toFloat(), overlayPaint
        )
        // Derecha
        canvas.drawRect(
            selectionRect.right.toFloat(), selectionRect.top.toFloat(),
            width.toFloat(), selectionRect.bottom.toFloat(), overlayPaint
        )
        // Abajo
        canvas.drawRect(
            0f, selectionRect.bottom.toFloat(),
            width.toFloat(), height.toFloat(), overlayPaint
        )

        // Dibujar el borde del rectángulo de selección
        canvas.drawRect(
            selectionRect.left.toFloat(),
            selectionRect.top.toFloat(),
            selectionRect.right.toFloat(),
            selectionRect.bottom.toFloat(),
            selectionPaint
        )
    }

    private fun updateSelectionRect(endX: Float, endY: Float) {
        selectionRect.apply {
            left = minOf(startX.toInt(), endX.toInt())
            top = minOf(startY.toInt(), endY.toInt())
            right = maxOf(startX.toInt(), endX.toInt())
            bottom = maxOf(startY.toInt(), endY.toInt())
        }
    }

    /**
     * Limpia la selección actual
     */
    fun clearSelection() {
        selectionRect = Rect()
        invalidate()
    }

    /**
     * Devuelve el rectángulo de selección actual
     */
    fun getSelectionRect(): Rect = Rect(selectionRect)

    /**
     * Método para confirmar la selección actual manualmente
     * Se llamará desde un botón en la interfaz
     */
    fun confirmSelection() {
        if (selectionRect.width() > 50 && selectionRect.height() > 50) {
            onSelectionCompleteListener?.invoke(selectionRect)
        }
    }
}
