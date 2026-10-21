package com.love.yourself.vistas

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.cos

/**
 * Las curvas y piezas de animacion de todas las pantallas, en un solo lugar.
 *
 * Las curvas van de 0 a 1 y sirven igual para lo que se dibuja a mano (la
 * burbuja, el cielo, las barras) y para lo que se anima con view.animate().
 * Si el telefono tiene las animaciones apagadas (Accesibilidad > Quitar
 * animaciones), todo aparece directo en su lugar.
 *
 * Identico en la app real y en la de diseno: se copia entero.
 */
object Animacion {

    /** Falso si el sistema pide no animar. */
    fun activas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

    // ---- curvas ----

    /** Cuanto avanzo t entre desde y hasta, de 0 a 1. */
    fun tramo(t: Float, desde: Float, hasta: Float) = ((t - desde) / (hasta - desde)).coerceIn(0f, 1f)

    fun lerp(a: Float, b: Float, x: Float) = a + (b - a) * x

    /** Entra y sale suave: la curva de los textos en el prototipo de Figma. */
    fun suave(x: Float) = ((1 - cos(x * PI)) / 2).toFloat()

    /** Llega rapido y frena: lo que aparece. */
    fun frena(x: Float): Float {
        val y = 1f - x
        return 1f - y * y * y
    }

    /** Arranca lento y acelera: lo que cae o se va. */
    fun acelera(x: Float) = x * x

    /** Se pasa un poco y vuelve: lo que crece con ganas. Mas fuerza, mas se pasa. */
    fun rebota(x: Float, fuerza: Float = 1.4f): Float {
        val y = x - 1f
        return 1f + (fuerza + 1f) * y * y * y + fuerza * y * y
    }

    // ---- las mismas curvas, para view.animate() ----

    val FRENA by lazy { PathInterpolator(0.22f, 1f, 0.36f, 1f) }
    val ACELERA by lazy { PathInterpolator(0.55f, 0f, 1f, 0.45f) }
    val SUAVE by lazy { PathInterpolator(0.5f, 0f, 0.5f, 1f) }
    val REBOTA by lazy { OvershootInterpolator(1.6f) }

    // ---- piezas ----

    /** Aparece subiendo un poco. Para las partes de una pantalla, una despues de otra. */
    fun entrar(v: View, demoraMs: Long, desdeDp: Float = 18f, duracionMs: Long = 480L) {
        if (!activas()) return
        v.alpha = 0f
        v.translationY = desdeDp * v.resources.displayMetrics.density
        v.animate().alpha(1f).translationY(0f)
            .setStartDelay(demoraMs).setDuration(duracionMs).setInterpolator(FRENA)
            .start()
    }

    /** Crece desde nada y se pasa un poco: para lo que tiene que llamar la atencion. */
    fun brotar(v: View, demoraMs: Long) {
        if (!activas()) return
        v.scaleX = 0f
        v.scaleY = 0f
        v.animate().scaleX(1f).scaleY(1f)
            .setStartDelay(demoraMs).setDuration(560L).setInterpolator(REBOTA)
            .start()
    }

    /** Un saltito en el lugar, para decir "esto se toca" o "esto cambio". */
    fun latido(v: View, demoraMs: Long = 0L) {
        if (!activas()) return
        v.animate().scaleX(1.18f).scaleY(1.18f)
            .setStartDelay(demoraMs).setDuration(140L).setInterpolator(FRENA)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(0L).setDuration(380L).setInterpolator(REBOTA)
                    .start()
            }
            .start()
    }

    /** Cuenta desde 0 hasta el valor: el agregado se ve juntarse. */
    fun contar(t: TextView, hasta: Long, demoraMs: Long, duracionMs: Long = 900L, formato: (Long) -> String) {
        if (!activas() || hasta <= 0L) {
            t.text = formato(hasta)
            return
        }
        t.text = formato(0L)
        ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = demoraMs
            duration = duracionMs
            interpolator = FRENA
            addUpdateListener { t.text = formato((hasta * (it.animatedValue as Float)).toLong()) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animacion: Animator) {
                    t.text = formato(hasta)
                }
            })
            start()
        }
    }

    /**
     * Se hunde un poco al tocarlo, como un boton de verdad. No cambia lo que
     * hace el toque: el clic sigue igual.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun presionable(v: View) {
        v.setOnTouchListener { vista, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> vista.animate().scaleX(0.95f).scaleY(0.95f)
                    .setStartDelay(0L).setDuration(90L).setInterpolator(FRENA).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> vista.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(0L).setDuration(280L).setInterpolator(REBOTA).start()
            }
            false
        }
    }
}

/**
 * Una pantalla completa que entra y sale con su propia animacion: el cielo,
 * el resumen. La entrada corre sola apenas aparece; para irse se llama a
 * salir(), que anima y despues avisa.
 */
open class Pantalla(context: Context) : FrameLayout(context) {

    /** Como entra. Corre cuando la pantalla ya esta medida: puede usar su alto. */
    var entrada: (() -> Unit)? = null

    /** Como se va. Tiene que llamar a fin cuando termina. */
    var salida: ((fin: () -> Unit) -> Unit)? = null

    var saliendo = false
        private set

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val e = entrada ?: return
        entrada = null
        if (!Animacion.activas()) return
        // Antes del primer dibujo y ya medida: la entrada parte desde su primer
        // fotograma, sin un parpadeo en el lugar final.
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                e()
                return true
            }
        })
    }

    fun salir(fin: () -> Unit) {
        if (saliendo) return
        saliendo = true
        val s = salida
        if (s == null || !Animacion.activas() || !isAttachedToWindow) fin() else s(fin)
    }
}
