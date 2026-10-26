package com.love.yourself.vistas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * La mascota grande, asomandose por un costado de la pantalla, a la altura de
 * la info del reel. Entra deslizandose desde el borde, se queda un rato y se
 * vuelve a esconder. Sin dialogo: solo la cara. Al tocarla crece un poco,
 * como un boton.
 *
 * La ventana mide solo lo que se ve de la mascota (mas un margen para que el
 * crecer al tocarla no se corte): el resto queda detras del borde. Solo arma
 * la vista y la anima; la ventana la pone OverlayFriccion.
 */
@SuppressLint("ViewConstructor")
class MascotaAsomada(
    context: Context,
    cara: Int,
    private val caraFinal: Int?,
    /** true: se asoma por la derecha; false: por la izquierda, en espejo. */
    private val derecha: Boolean
) : FrameLayout(context) {

    private val anchoPantalla = resources.displayMetrics.widthPixels
    private val anchoCerebro = (anchoPantalla * PROPORCION_ANCHO).toInt()
    private val altoCerebro: Int
    private val visible = (anchoCerebro * PROPORCION_VISIBLE).toInt()
    /** Lugar alrededor para crecer al tocarla sin cortarse. */
    private val holgura = (anchoCerebro * 0.12f).toInt()

    private val imagen = ImageView(context)
    private val dentro: Float
    private val fuera: Float

    /** Lo que se hace al tocarla, ademas de crecer. */
    var alTocar: (() -> Unit)? = null

    val anchoVentana get() = visible + holgura
    val altoVentana get() = altoCerebro + 2 * holgura

    init {
        val b = cargar(cara)
        altoCerebro = if (b == null) anchoCerebro else anchoCerebro * b.height / b.width
        imagen.setImageBitmap(b)
        imagen.scaleType = ImageView.ScaleType.FIT_XY
        clipChildren = true
        addView(imagen, LayoutParams(anchoCerebro, altoCerebro).apply { topMargin = holgura })
        // Derecha: la ventana va pegada al borde derecho y la imagen sobresale
        // por ahi. Izquierda: al reves.
        dentro = if (derecha) holgura.toFloat() else (visible - anchoCerebro).toFloat()
        fuera = if (derecha) (holgura + visible + holgura).toFloat() else (-anchoCerebro - holgura).toFloat()
        imagen.translationX = fuera
        imagen.pivotY = altoCerebro / 2f
        imagen.pivotX = if (derecha) visible / 2f else anchoCerebro - visible / 2f
        imagen.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.12f).scaleY(1.12f)
                        .setDuration(110L).setInterpolator(Animacion.FRENA).start()
                    alTocar?.invoke()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f)
                    .setDuration(320L).setInterpolator(Animacion.REBOTA).start()
            }
            true
        }
    }

    /** La cara, en espejo si se asoma por la izquierda: siempre mira hacia adentro. */
    private fun cargar(id: Int): Bitmap? {
        val b = Imagenes.cargar(resources, id, anchoCerebro) ?: return null
        if (derecha) return b
        val espejo = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, espejo, true)
    }

    /**
     * Entra en entradaMs, se queda quedaMs (a mitad cambia a caraFinal, si hay)
     * y sale un poco mas rapido de lo que entro. Avisa al terminar.
     */
    fun asomar(entradaMs: Long, quedaMs: Long, alTerminar: () -> Unit) {
        if (!Animacion.activas()) {
            imagen.translationX = dentro
            postDelayed({ alTerminar() }, quedaMs)
            return
        }
        imagen.animate().translationX(dentro).setDuration(entradaMs).setInterpolator(Animacion.REBOTA)
            .withEndAction {
                caraFinal?.let { id ->
                    postDelayed({ cargar(id)?.let { imagen.setImageBitmap(it) } }, quedaMs / 2)
                }
                imagen.animate().translationX(fuera).setStartDelay(quedaMs)
                    .setDuration((entradaMs * 0.7f).toLong()).setInterpolator(Animacion.ACELERA)
                    .withEndAction { alTerminar() }
                    .start()
            }
            .start()
    }

    private companion object {
        /** Que tan grande es, respecto del ancho de la pantalla. */
        const val PROPORCION_ANCHO = 0.55f
        /** Cuanto de ella se asoma. */
        const val PROPORCION_VISIBLE = 0.62f
    }
}
