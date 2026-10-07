package com.love.yourself.vistas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.love.yourself.R
import com.love.yourself.vistas.Animacion.rebota
import com.love.yourself.vistas.Animacion.tramo
import kotlin.math.PI
import kotlin.math.sin

/**
 * El cielo del frame "sky" de Figma: degradado azul a celeste, un sol con
 * halo, tres nubes y el cerebro sentado en la nube de la izquierda.
 *
 * Todo va en proporciones del lienzo de Figma (1440 x 3120) sobre el tamano
 * real de la vista, asi que calza en cualquier pantalla. Esta vivo, pero
 * despacio: las nubes derivan, el sol respira y el cerebro flota con su nube.
 * Que sea lento es a proposito: es lo contrario del ritmo de los reels.
 */
class CieloView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** El cerebro que se sienta en la nube (un drawable). 0 = sin cerebro. */
    var cara = 0
        set(valor) {
            field = valor
            cerebro = null
            invalidate()
        }

    private val fondo = Paint()
    private val sol = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barraEstado = Paint().apply { color = Color.argb(56, 0, 0, 0) }
    private val pinturaImagen = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var nube: Bitmap? = null
    private var cerebro: Bitmap? = null
    private val destino = RectF()
    private var inicioMs = 0L

    /** Cada nube: x, y, ancho y alto en px del lienzo de Figma. */
    private val nubes = arrayOf(
        floatArrayOf(720f, 515f, 1032f, 561f),
        floatArrayOf(-297f, 887f, 1032f, 561f),
        floatArrayOf(482f, 529f, 773f, 420f)
    )

    /** Cuanto se mueve cada nube de lado a lado (px de Figma) y en cuantos segundos va y vuelve. */
    private val deriva = floatArrayOf(34f, 26f, 44f)
    private val periodo = floatArrayOf(11f, 14f, 9f)
    private val desfase = floatArrayOf(0f, 1.7f, 3.1f)

    /** La nube donde se sienta el cerebro: la de la izquierda. */
    private val nubeDelCerebro = 1

    private fun x(v: Float) = v / 1440f * width
    private fun y(v: Float) = v / 3120f * height

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        inicioMs = SystemClock.uptimeMillis()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // De #497FFF (arriba) a #82E3F0, que llega a un tercio del alto y sigue igual.
        fondo.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#497FFF"), Color.parseColor("#497FFF"), Color.parseColor("#82E3F0"), Color.parseColor("#82E3F0")),
            floatArrayOf(0f, 0.013f, 0.349f, 1f),
            Shader.TileMode.CLAMP
        )
        // Sol: centro amarillo palido y un halo que se desvanece.
        sol.shader = RadialGradient(
            x(1087.5f), y(562.5f), x(352.5f),
            intArrayOf(Color.parseColor("#FFF6CC"), Color.parseColor("#FFF6CC"), Color.argb(90, 255, 246, 204), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 0.35f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        nube = Imagenes.cargar(resources, R.drawable.nube, x(1032f).toInt())
        cerebro = null
    }

    override fun onDraw(canvas: Canvas) {
        val vivo = Animacion.activas()
        val s = if (vivo) (SystemClock.uptimeMillis() - inicioMs) / 1000f else 0f
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fondo)

        // El sol respira: un 3 % cada 6 s.
        val cx = x(1087.5f)
        val cy = y(562.5f)
        val pulso = 1f + 0.03f * sin(s / 6f * 2f * PI.toFloat())
        canvas.save()
        canvas.scale(pulso, pulso, cx, cy)
        canvas.drawCircle(cx, cy, x(352.5f), sol)
        canvas.restore()

        var dxCerebro = 0f
        nube?.let { b ->
            for (i in nubes.indices) {
                val n = nubes[i]
                val dx = x(deriva[i]) * sin(s / periodo[i] * 2f * PI.toFloat() + desfase[i])
                if (i == nubeDelCerebro) dxCerebro = dx
                destino.set(x(n[0]) + dx, y(n[1]), x(n[0] + n[2]) + dx, y(n[1] + n[3]))
                canvas.drawBitmap(b, null, destino, pinturaImagen)
            }
        }
        dibujarCerebro(canvas, dxCerebro, s)

        canvas.drawRect(0f, 0f, width.toFloat(), y(72f), barraEstado)
        if (vivo) postInvalidateOnAnimation()
    }

    /**
     * El cerebro, sentado sobre su nube mirando el cielo: se mueve con ella y
     * flota un poco. Aparece cuando el cielo ya llego, creciendo desde la nube.
     */
    private fun dibujarCerebro(c: Canvas, dx: Float, s: Float) {
        if (cara == 0 || width == 0) return
        val ancho = x(360f)
        val b = cerebro ?: Imagenes.cargar(resources, cara, ancho.toInt()).also { cerebro = it } ?: return
        val alto = ancho * b.height / b.width
        val escala = if (Animacion.activas()) rebota(tramo(s * 1000f, 550f, 1100f), 1.8f) else 1f
        if (escala <= 0f) return
        val flota = y(10f) * sin(s / 3.4f * 2f * PI.toFloat())
        val centro = x(120f) + ancho / 2f + dx
        val base = y(880f) + alto + flota
        destino.set(centro - ancho * escala / 2f, base - alto * escala, centro + ancho * escala / 2f, base)
        c.drawBitmap(b, null, destino, pinturaImagen)
    }
}
